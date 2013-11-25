package org.sireum.amandroid.android.interProcedural.reachingFactsAnalysis

import org.sireum.util._
import org.sireum.amandroid.interProcedural.reachingFactsAnalysis.RFAFact
import org.sireum.amandroid.AmandroidProcedure
import org.sireum.amandroid.interProcedural.reachingFactsAnalysis.VarSlot
import org.sireum.amandroid.interProcedural.reachingFactsAnalysis.RFAInstance
import org.sireum.amandroid.android.AndroidConstants
import org.sireum.amandroid.NormalType
import org.sireum.amandroid.interProcedural.Context
import org.sireum.amandroid.interProcedural.reachingFactsAnalysis.FieldSlot
import org.sireum.amandroid.interProcedural.reachingFactsAnalysis.RFAConcreteStringInstance
import org.sireum.amandroid.GlobalConfig
import org.sireum.amandroid.AmandroidRecord
import org.sireum.amandroid.Center

object AndroidRFAConfig {
  /**
   * before starting the analysis, prepares the Center with some additional info
   * a record named "Unknown" with a procedure called "unknown()" is added to the Center
   * this special record is used to handle out-of-scope calls 
   */
  def setupCenter = {
    val rec = new AmandroidRecord
    rec.init(Center.UNKNOWN_RECORD)
    val p = new AmandroidProcedure
    p.init(Center.UNKNOWN_PROCEDURE_SIG)
    p.setPhantom
    rec.addProcedure(p)
    Center.addRecord(rec)
  }
  
  /**
   * generates and returns the initial facts corresponding to the "Intent" parameter of a dummyMain 
   * the generated fact says that the param Intent is generated at the Center.
   */
	def getInitialFactsForMainEnvironment(dm : AmandroidProcedure) : ISet[RFAFact] = {
	  require(dm.getShortName == AndroidConstants.MAINCOMP_ENV)
	  var result = isetEmpty[RFAFact]
	  val intentSlot = VarSlot(dm.getParamName(0))
	  val context : Context = new Context(GlobalConfig.CG_CONTEXT_K)
	  context.setContext("Center", "L0000")
	  val intentValue = RFAInstance(NormalType(AndroidConstants.INTENT, 0), context.copy)
	  result += RFAFact(intentSlot, intentValue)
//	  val mActionSlot = FieldSlot(intentValue, AndroidConstants.INTENT_ACTION)
//	  val mActionValue = RFAConcreteStringInstance(AndroidConstants.ACTION_MAIN, context.copy)
//	  result += RFAFact(mActionSlot, mActionValue)
//	  val mCategoriesSlot = FieldSlot(intentValue, AndroidConstants.INTENT_CATEGORIES)
//	  val mCategoriesValue = RFAConcreteStringInstance(AndroidConstants.CATEGORY_LAUNCHER, context.copy)
//	  result += RFAFact(mCategoriesSlot, mCategoriesValue)
	  result
	}
}