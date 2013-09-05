package org.sireum.amandroid.interProcedural

import org.sireum.alir._
import org.sireum.util._
import org.sireum.amandroid.interProcedural.callGraph._
import org.sireum.pilar.ast._
import org.sireum.amandroid.AmandroidProcedure
import org.sireum.pilar.symbol.ProcedureSymbolTable
import org.sireum.amandroid.Center

/**
 * @author Fengguo Wei & Sankardas Roy
 */
trait InterProceduralMonotoneDataFlowAnalysisResult[LatticeElement] {
  def entrySet : CGNode => ISet[LatticeElement]
}

/**
 * @author Fengguo Wei & Sankardas Roy
 */
trait InterProceduralMonotonicFunction[LatticeElement] {
	import org.sireum.pilar.ast._

  def apply(s : ISet[LatticeElement], a : Assignment, currentContext : Context) : ISet[LatticeElement]
  def apply(s : ISet[LatticeElement], e : Exp, currentContext : Context) : ISet[LatticeElement]
}

/**
 * @author Fengguo Wei & Sankardas Roy
 */
trait CallResolver[LatticeElement] {
  def getCalleeSet(s : ISet[LatticeElement], cj : CallJump) : ISet[AmandroidProcedure]
  def getFactsForCalleeAndReturn(s : ISet[LatticeElement], cj : CallJump) : (ISet[LatticeElement], ISet[LatticeElement])
  def mapFactsToCallee(factsToCallee : ISet[LatticeElement], cj : CallJump, calleeProcedure : ProcedureDecl) : ISet[LatticeElement]
  def getAndMapFactsForCaller(s : ISet[LatticeElement], cj : CallJump, calleeProcedure : ProcedureDecl) : ISet[LatticeElement]
}

/**
 * @author Fengguo Wei & Sankardas Roy
 */
object InterProceduralMonotoneDataFlowAnalysisFramework {
  
  final val TITLE = "InterProceduralMonotoneDataFlowAnalysisFramework"
  
	def apply[LatticeElement] = build[LatticeElement] _

  def build[LatticeElement] //
  (cg : CallGraph[CGNode],
   entryPoint : AmandroidProcedure,
   forward : Boolean, lub : Boolean, rapid : Boolean,
   gen : InterProceduralMonotonicFunction[LatticeElement],
   kill : InterProceduralMonotonicFunction[LatticeElement],
   callr : CallResolver[LatticeElement],
   latticeUpdate : (ISet[LatticeElement] => ISet[LatticeElement]),
   iota : ISet[LatticeElement],
   initial : ISet[LatticeElement],
   switchAsOrderedMatch : Boolean = false) : //
   InterProceduralMonotoneDataFlowAnalysisResult[LatticeElement] = {
    type N = CGNode
    val confluence = if (lub) iunion[LatticeElement] _ else iintersect[LatticeElement] _
    val bigConfluence : Iterable[ISet[LatticeElement]] => ISet[LatticeElement] =
      if (lub) bigIUnion else bigIIntersect

    val flow = if (forward) cg else cg.reverse
    val entrySetMap = idmapEmpty[N, ISet[LatticeElement]]
    entrySetMap(flow.entryNode) = latticeUpdate(iota)
    def getEntrySet(n : N) = entrySetMap.getOrElse(n, latticeUpdate(initial))
    
    class IMdaf(val entrySet : N => ISet[LatticeElement],
               initial : ISet[LatticeElement])
        extends InterProceduralMonotoneDataFlowAnalysisResult[LatticeElement] {
      type DFF = ISet[LatticeElement]

      override def toString = {
        val sb = new StringBuilder

        for (n <- cg.nodes) {
          sb.append("%s = %s\n".format(n, entrySet(n).toString))
        }
        sb.append("\n")

        sb.toString
      }
      
      protected def next(l : LocationDecl, pst : ProcedureSymbolTable, pSig : String, callerContext : Context) = {
        val newLoc = pst.location(l.index + 1)
        val newContext = callerContext.copy
        if(!newLoc.name.isDefined)
        	newContext.setContext(pSig, newLoc.index.toString)
        else 
          newContext.setContext(pSig, newLoc.name.get.uri)
        if(cg.isCall(newLoc))
          cg.getCGCallNode(newContext)
        else
        	cg.getCGNormalNode(newContext)
      }

      protected def node(l : LocationDecl, context : Context) = {
        if(cg.isCall(l))
          cg.getCGCallNode(context)
        else
        	cg.getCGNormalNode(context)
      }

      protected def fA(a : Assignment, in : DFF, currentContext : Context) : DFF =
        kill(in, a, currentContext).union(gen(in, a, currentContext))

      protected def fE(e : Exp, in : DFF, currentContext : Context) : DFF =
        kill(in, e, currentContext).union(gen(in, e, currentContext))

      protected def fOE(eOpt : Option[Exp], in : DFF, currentContext : Context) : DFF =
        if (eOpt.isDefined) fE(eOpt.get, in, currentContext) else in

      protected def actionF(in : DFF, a : Action, currentContext : Context) =
        a match {
          case a : AssignAction => fA(a, in, currentContext)
          case a : AssertAction => fE(a.cond, in, currentContext)
          case a : AssumeAction => fE(a.cond, in, currentContext)
          case a : ThrowAction  => fE(a.exp, in, currentContext)
          case a : StartAction =>
            if (forward)
              fOE(a.arg, fOE(a.count, in, currentContext), currentContext)
            else
              fOE(a.count, fOE(a.arg, in, currentContext), currentContext)
          case a : ExtCallAction => fA(a, in, currentContext)
        }

      def update(s : DFF, n : N) : Boolean = {
        val oldS = getEntrySet(n)
        val newS = latticeUpdate(s)
        if (oldS != newS) {
          entrySetMap.update(n, newS)
          true
        } else
          false
      }

//      protected def visitBackward(
//        l : LocationDecl,
//        pst : ProcedureSymbolTable,
//        callerContext : Context,
//        esl : Option[EntrySetListener[LatticeElement]]) : Boolean = {
//        val pSig = pst.procedure.getValueAnnotation("signature") match {
//			      case Some(exp : NameExp) =>
//			        exp.name.name
//			      case _ => throw new RuntimeException("Doing " + TITLE + ": Can not find signature from: " + l)
//			    }
//        val currentContext = callerContext.copy
//        
//        if(!l.name.isDefined)
//        	currentContext.setContext(pSig, l.index.toString)
//        else
//          currentContext.setContext(pSig, l.name.get.uri)
//        val eslb = esl.getOrElse(null)
//          def jumpF(j : Jump) : DFF =
//            j match {
//              case j : IfJump =>
//                var result = initial
//                val numOfIfThens = j.ifThens.size
//                for (i <- 0 until numOfIfThens) {
//                  val ifThen = j.ifThens(i)
//                  val ifThenContext = callerContext.copy
//                  ifThenContext.setContext(pSig, ifThen.target.uri)
//                  val ifThenLoc = pst.location(ifThen.target.uri)
//                  val sn = node(ifThenLoc, ifThenContext)
//                  var r = getEntrySet(sn)
//                  for (k <- tozero(i)) {
//                    val it = j.ifThens(k)
//                    r = fE(it.cond, r, currentContext)
//                  }
//                  result = confluence(result, r)
//                }
//                {
//                  val ifElse = j.ifElse
//                  val ifElseDefined = ifElse.isDefined
//                  val sn =
//                    if (ifElseDefined) {
//                      val ifElseContext = callerContext.copy
//		                  ifElseContext.setContext(pSig, ifElse.get.target.uri)
//		                  val ifElseLoc = pst.location(ifElse.get.target.uri)
//                      node(ifElseLoc, ifElseContext)
//                    }
//                    else next(l, pst, pSig, callerContext)
//                  var r = getEntrySet(sn)
//                  for (k <- tozero(numOfIfThens - 1)) {
//                    val it = j.ifThens(k)
//                    r = fE(it.cond, r, currentContext)
//                  }
//                  if (ifElseDefined && esl.isDefined) eslb.ifElse(ifElse.get, r)
//                  result = confluence(result, r)
//                }
//                if (esl.isDefined) eslb.ifJump(j, result)
//                result
//              case j : SwitchJump =>
//                var result = initial
//                val numOfCases = j.cases.size
//                for (i <- 0 until numOfCases) {
//                  val switchCase = j.cases(i)
//                  val switchCaseContext = callerContext.copy
//                  switchCaseContext.setContext(pSig, switchCase.target.uri)
//                  val switchCaseLoc = pst.location(switchCase.target.uri)
//                  val sn = node(switchCaseLoc, switchCaseContext)
//                  var r = getEntrySet(sn)
//                  if (switchAsOrderedMatch)
//                    for (k <- tozero(i)) {
//                      val sc = j.cases(k)
//                      r = fE(sc.cond, r, currentContext)
//                    }
//                  else
//                    r = fE(switchCase.cond, r, currentContext)
//                  if (esl.isDefined) eslb.switchCase(switchCase, r)
//                  result = confluence(result, r)
//                }
//                {
//                  val switchDefault = j.defaultCase
//                  val switchDefaultDefined = switchDefault.isDefined
//                  val sn =
//                    if (switchDefaultDefined){
//                      val switchDefaultContext = callerContext.copy
//		                  switchDefaultContext.setContext(pSig, switchDefault.get.target.uri)
//		                  val switchDefaultLoc = pst.location(switchDefault.get.target.uri)
//                      node(switchDefaultLoc, switchDefaultContext)
//                    }
//                    else next(l, pst, pSig, callerContext)
//                  var r = getEntrySet(sn)
//                  if (switchAsOrderedMatch)
//                    for (k <- tozero(numOfCases - 1)) {
//                      val sc = j.cases(k)
//                      r = fE(sc.cond, r, currentContext)
//                    }
//                  if (esl.isDefined && switchDefaultDefined)
//                    eslb.switchDefault(switchDefault.get, r)
//                  result = confluence(result, r)
//                }
//                if (esl.isDefined)
//                  eslb.switchJump(j, result)
//                result
//              case j : GotoJump =>
//                val jContext = callerContext.copy
//                jContext.setContext(pSig, j.target.uri)
//                val jLoc = pst.location(j.target.uri)
//                val sn = node(jLoc, jContext)
//                val result = getEntrySet(sn)
//                if (esl.isDefined)
//                  eslb.gotoJump(j, result)
//                result
//              case j : ReturnJump =>
//                val exitContext = callerContext.copy
//                exitContext.setContext(pSig, pSig)
//                val result = fOE(j.exp, s, currentContext)
//                if (esl.isDefined)
//                  eslb.returnJump(j, result)
//                result
//              case j : CallJump =>
//                val s =
//                  if (j.jump.isEmpty)
//                    getEntrySet(next(l, pst, pSig, callerContext))
//                  else
//                    jumpF(j.jump.get)
//                val result = fA(j, s, currentContext)
//                if (esl.isDefined)
//                  eslb.callJump(j, result)
//                result
//            }
//        val ln = node(l, currentContext)
//        l match {
//          case l : ComplexLocation =>
//            val result = bigConfluence(l.transformations.map { t =>
//              var r =
//                if (t.jump.isEmpty)
//                  getEntrySet(next(l))
//                else
//                  jumpF(t.jump.get)
//              val numOfActions = t.actions.size
//              for (i <- untilzero(numOfActions)) {
//                val a = t.actions(i)
//                r = actionF(r, a)
//                if (esl.isDefined) eslb.action(a, r)
//              }
//              if (esl.isDefined) eslb.exitSet(None, r)
//              r
//            })
//            update(result, ln)
//          case l : ActionLocation =>
//            val result = actionF(getEntrySet(next(l)), l.action)
//            if (esl.isDefined) {
//              eslb.action(l.action, result)
//              eslb.exitSet(None, result)
//            }
//            update(result, ln)
//          case l : JumpLocation =>
//            val result = jumpF(l.jump)
//            if (esl.isDefined) {
//              eslb.exitSet(None, result)
//            }
//            update(result, ln)
//          case l : EmptyLocation =>
//            false
//        }
//      }

      protected def visitForward(
        l : LocationDecl,
        pst : ProcedureSymbolTable,
        callerContext : Context,
        esl : Option[EntrySetListener[LatticeElement]]) : Boolean = {
        val pSig = pst.procedure.getValueAnnotation("signature") match {
			      case Some(exp : NameExp) =>
			        exp.name.name
			      case _ => throw new RuntimeException("Doing " + TITLE + ": Can not find signature from: " + l)
			    }
        val currentContext = callerContext.copy
        
        if(!l.name.isDefined)
        	currentContext.setContext(pSig, l.index.toString)
        else
          currentContext.setContext(pSig, l.name.get.uri)
        val eslb = esl.getOrElse(null)
          def jumpF(s : DFF, j : Jump) : Boolean =
            j match {
              case j : IfJump =>
                var r = s
                if (esl.isDefined) eslb.ifJump(j, s)
                var updated = false
                for (ifThen <- j.ifThens) {
                  r = fE(ifThen.cond, r, currentContext)
                  val ifThenContext = callerContext.copy
                  ifThenContext.setContext(pSig, ifThen.target.uri)
                  val ifThenLoc = pst.location(ifThen.target.uri)
                  val sn = node(ifThenLoc, ifThenContext)
                  if (esl.isDefined) {
                    eslb.ifThen(ifThen, r)
                    eslb.exitSet(Some(ifThen), r)
                  }
                  updated = update(confluence(r, getEntrySet(sn)), sn) || updated
                }
                if (j.ifElse.isEmpty) {
                  val sn = next(l, pst, pSig, callerContext)
                  if (esl.isDefined) eslb.exitSet(None, r)
                  update(confluence(r, getEntrySet(sn)), sn) || updated
                } else {
                  val ifElse = j.ifElse.get
                  val ifElseContext = callerContext.copy
                  ifElseContext.setContext(pSig, ifElse.target.uri)
                  val ifElseLoc = pst.location(ifElse.target.uri)
                  val sn = node(ifElseLoc, ifElseContext)
                  if (esl.isDefined) {
                    eslb.ifElse(ifElse, r)
                    eslb.exitSet(Some(ifElse), r)
                  }
                  update(confluence(r, getEntrySet(sn)), sn) || updated
                }
              case j : SwitchJump =>
                var r = s
                if (esl.isDefined) eslb.switchJump(j, s)
                var updated = false
                for (switchCase <- j.cases) {
                  r =
                    if (switchAsOrderedMatch)
                      fE(switchCase.cond, r, currentContext)
                    else
                      fE(switchCase.cond, s, currentContext)
                  val switchCaseContext = callerContext.copy
                  switchCaseContext.setContext(pSig, switchCase.target.uri)
                  val switchCaseLoc = pst.location(switchCase.target.uri)
                  val sn = node(switchCaseLoc, switchCaseContext)
                  if (esl.isDefined) {
                    eslb.switchCase(switchCase, r)
                    eslb.exitSet(Some(switchCase), r)
                  }
                  updated = update(confluence(r, getEntrySet(sn)), sn) || updated
                }
                if (j.defaultCase.isEmpty) {
                  val sn = next(l,pst, pSig, callerContext)
                  if (esl.isDefined) eslb.exitSet(None, r)
                  update(confluence(r, getEntrySet(sn)), sn) || updated
                } else {
                  val switchDefault = j.defaultCase.get
                  val switchDefaultContext = callerContext.copy
                  switchDefaultContext.setContext(pSig, switchDefault.target.uri)
                  val switchDefaultLoc = pst.location(switchDefault.target.uri)
                  val sn = node(switchDefaultLoc, switchDefaultContext)
                  if (esl.isDefined) {
                    eslb.switchDefault(switchDefault, r)
                    eslb.exitSet(Some(switchDefault), r)
                  }
                  update(confluence(r, getEntrySet(sn)), sn) || updated
                }
              case j : GotoJump =>
                val gotoContext = callerContext.copy
                gotoContext.setContext(pSig, j.target.uri)
                val gotoLoc = pst.location(j.target.uri)
                val sn = node(gotoLoc, gotoContext)
                if (esl.isDefined) {
                  eslb.gotoJump(j, s)
                  eslb.exitSet(Some(j), s)
                }
                update(confluence(s, getEntrySet(sn)), sn)
              case j : ReturnJump =>
                val exitContext = callerContext.copy
                exitContext.setContext(pSig, pSig)
                val sn = cg.getCGExitNode(exitContext)
                val r = if (j.exp.isEmpty) s else fE(j.exp.get, s, currentContext)
                if (esl.isDefined) {
                  eslb.returnJump(j, r)
                  eslb.exitSet(Some(j), r)
                }
                update(confluence(r, getEntrySet(sn)), sn)
              case j : CallJump =>
                if (esl.isDefined) eslb.callJump(j, s)
                if (j.jump.isEmpty) {
                  val sn = next(l, pst, pSig, callerContext)
                  val r = fA(j, s, currentContext)
                  if (esl.isDefined) eslb.exitSet(None, r)
                  update(confluence(r, getEntrySet(sn)), sn)
                } else
                  jumpF(fA(j, s, currentContext), j.jump.get)
            }

        val ln = node(l, currentContext)
        var s = getEntrySet(ln)
        l match {
          case l : ComplexLocation =>
            val bs = l.transformations.map { t =>
              var r = s
              t.actions.foreach { a =>
                if (esl.isDefined) eslb.action(a, r)
                r = actionF(r, a, currentContext)
              }
              if (t.jump.isDefined)
                jumpF(r, t.jump.get)
              else {
                val sn = next(l, pst, pSig, callerContext)
                if (esl.isDefined) eslb.exitSet(None, r)
                update(confluence(r, getEntrySet(sn)), sn)
              }
            }
            bs.exists(_ == true)
//          case l : ActionLocation =>
//            val sn = next(l)
//            if (esl.isDefined) eslb.action(l.action, s)
//            val r = actionF(s, l.action)
//            if (esl.isDefined) eslb.exitSet(None, r)
//            update(confluence(r, getEntrySet(sn)), sn)
          case l : ActionLocation =>
             if(esl.isDefined) eslb.action(l.action, s)
             val r = actionF(s, l.action, currentContext)
             if(esl.isDefined) eslb.exitSet(None, r)
             if(l.index < pst.locations.size - 1){
               val sn =next(l, pst, pSig, callerContext)
               update(confluence(r, getEntrySet(sn)), sn)
             } else {
               true
             }
          case l : JumpLocation =>
            jumpF(s, l.jump)
          case l : EmptyLocation =>
            if (esl.isDefined)
              eslb.exitSet(None, s)
            val r = s
            val sn = next(l, pst, pSig, callerContext)
            update(confluence(r, getEntrySet(sn)), sn)
            true
        }
      }

      def visit(l : LocationDecl,
          			pst : ProcedureSymbolTable,
          			callerContext : Context,
                esl : Option[EntrySetListener[LatticeElement]] = None) : Boolean =
//        if (forward) visitForward(l, esl)
//        else visitBackward(l, esl)
        visitForward(l, pst, callerContext, esl)
      
      def resolveCall(cn : CGCallNode, cj : CallJump) : (ISet[LatticeElement], ISet[LatticeElement]) = {
        val locUri = cn.getLocUri
        val callerContext = cn.getContext
        val s = getEntrySet(cn)
        val calleeSet = callr.getCalleeSet(s, cj)
        calleeSet.foreach{
          case callee =>
            cg.collectCfgToBaseGraph(callee, callerContext, false)
            cg.extendGraph(callee.getSignature, callerContext)
        }
        callr.getFactsForCalleeAndReturn(s, cj)
      }

    }
    
    val imdaf = new IMdaf(getEntrySet _, initial)
    var pst = entryPoint.getProcedureBody
    val initContext = new Context(cg.K_CONTEXT)
    cg.collectCfgToBaseGraph(entryPoint, initContext, true)
    val workList = mlistEmpty[N]

    workList += flow.entryNode
    while (!workList.isEmpty) {
      val n = workList.remove(0)
      val callerContext = n.getContext.copy
      callerContext.removeTopContext
      n match {
        case en : CGEntryNode =>
          for (succ <- cg.successors(n)) {
            imdaf.update(getEntrySet(n), succ)
            workList += succ
          }
        case xn : CGExitNode =>
          for (succ <- cg.successors(xn)){
            require(succ.isInstanceOf[CGReturnNode])
            val l = succ.getOwnerPST.location(succ.asInstanceOf[CGReturnNode].getLocUri)
	          require(cg.isCall(l))
	          val cj = l.asInstanceOf[JumpLocation].jump.asInstanceOf[CallJump]
            val factsForCaller = callr.getAndMapFactsForCaller(getEntrySet(xn), cj, xn.getOwnerPST.procedure)
            if (imdaf.update(factsForCaller, succ))
            	workList += succ
          }
        case cn : CGCallNode =>
          val l = cn.getOwnerPST.location(cn.getLocUri)
          require(cg.isCall(l))
          val cj = l.asInstanceOf[JumpLocation].jump.asInstanceOf[CallJump]
          val (factsForCallee, factsForReturn) = imdaf.resolveCall(cn, cj)
          for (succ <- cg.successors(n)) {
            succ match{
              case n : CGEntryNode =>
                val newCalleeFacts = callr.mapFactsToCallee(factsForCallee, cj, n.getOwnerPST.procedure)
                if(imdaf.update(newCalleeFacts, n))
                	workList += succ
              case n : CGReturnNode =>
                if(imdaf.update(factsForReturn, n))
                	workList += succ
              case a => throw new RuntimeException("unexpected node type: " + a)
            }
          }
        case rn : CGReturnNode =>
          for (succ <- cg.successors(n)) {
            imdaf.update(getEntrySet(n), succ)
            workList += succ
          }
        case nn : CGNormalNode =>
          if (imdaf.visit(nn.getOwnerPST.location(nn.getLocUri), nn.getOwnerPST, callerContext))
            workList ++= cg.successors(n)
        case a => throw new RuntimeException("unexpected node type: " + a)
      }
    }

    imdaf
    
	}
}