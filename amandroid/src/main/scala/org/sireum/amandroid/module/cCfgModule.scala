// Do not edit this file. It is auto-generated from org.sireum.amandroid.module.cCfg
// by org.sireum.pipeline.gen.ModuleGenerator

package org.sireum.amandroid.module

import org.sireum.util._
import org.sireum.pipeline._
import java.lang.String
import org.sireum.alir.AlirIntraProceduralNode
import org.sireum.alir.ControlFlowGraph
import org.sireum.alir.DefRef
import org.sireum.alir.MonotoneDataFlowAnalysisResult
import org.sireum.amandroid.android.interProcedural.objectFlowAnalysis.AndroidObjectFlowGraph
import org.sireum.amandroid.android.interProcedural.objectFlowAnalysis.AndroidValueSet
import org.sireum.amandroid.interProcedural.objectFlowAnalysis.OfaNode
import org.sireum.amandroid.intraProcedural.compressedControlFlowGraph.CompressedControlFlowGraph
import org.sireum.amandroid.interProcedural.pointsToAnalysis.PointerAssignmentGraph
import org.sireum.amandroid.interProcedural.pointsToAnalysis.PtaNode
import org.sireum.amandroid.module.AndroidIntraProcedural.AndroidIntraAnalysisResult
import org.sireum.pilar.ast.LocationDecl
import org.sireum.pilar.symbol.ProcedureSymbolTable
import org.sireum.pilar.symbol.SymbolTable
import scala.Function1
import scala.Function2
import scala.Option
import scala.collection.mutable.Map

object cCfgModule extends PipelineModule {
  def title = "Compressed Control Flow Graph Builder"
  def origin = classOf[cCfg]

  val poolKey = "cCfg.pool"
  val globalProcedureSymbolTableKey = "Global.procedureSymbolTable"
  val cCfgKey = "cCfg.cCfg"
  val globalPoolKey = "Global.pool"
  val cfgKey = "cCfg.cfg"
  val globalCfgKey = "Global.cfg"
  val globalCCfgKey = "Global.cCfg"

  def compute(job : PipelineJob, info : PipelineJobModuleInfo) : MBuffer[Tag] = {
    val tags = marrayEmpty[Tag]
    try {
      val module = Class.forName("org.sireum.amandroid.module.cCfgModuleDef")
      val cons = module.getConstructors()(0)
      val params = Array[AnyRef](job, info)
      val inst = cons.newInstance(params : _*)
    } catch {
      case e : Throwable =>
        e.printStackTrace
        tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker, e.getMessage);
    }
    return tags
  }

  override def initialize(job : PipelineJob) {
  }

  override def validPipeline(stage : PipelineStage, job : PipelineJob) : MBuffer[Tag] = {
    val tags = marrayEmpty[Tag]
    val deps = ilist[PipelineModule](CfgModule, CfgModule)
    deps.foreach(d =>
      if(stage.modules.contains(d)){
        tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
            "'" + this.title + "' depends on '" + d.title + "' yet both were found in stage '" + stage.title + "'"
        )
      }
    )
    return tags
  }

  def inputDefined (job : PipelineJob) : MBuffer[Tag] = {
    val tags = marrayEmpty[Tag]
    var _pool : scala.Option[AnyRef] = None
    var _poolKey : scala.Option[String] = None

    val keylistpool = List(cCfgModule.globalPoolKey, CfgModule.poolKey)
    keylistpool.foreach(key => 
      if(job ? key) { 
        if(_pool.isEmpty) {
          _pool = Some(job(key))
          _poolKey = Some(key)
        }
        if(!(job(key).asInstanceOf[AnyRef] eq _pool.get)) {
          tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
            "Input error for '" + this.title + "': 'pool' keys '" + _poolKey.get + " and '" + key + "' point to different objects.")
        }
      }
    )

    _pool match{
      case Some(x) =>
        if(!x.isInstanceOf[scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode]]){
          tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
            "Input error for '" + this.title + "': Wrong type found for 'pool'.  Expecting 'scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode]' but found '" + x.getClass.toString + "'")
        }
      case None =>
        tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
          "Input error for '" + this.title + "': No value found for 'pool'")       
    }
    var _procedureSymbolTable : scala.Option[AnyRef] = None
    var _procedureSymbolTableKey : scala.Option[String] = None

    val keylistprocedureSymbolTable = List(cCfgModule.globalProcedureSymbolTableKey)
    keylistprocedureSymbolTable.foreach(key => 
      if(job ? key) { 
        if(_procedureSymbolTable.isEmpty) {
          _procedureSymbolTable = Some(job(key))
          _procedureSymbolTableKey = Some(key)
        }
        if(!(job(key).asInstanceOf[AnyRef] eq _procedureSymbolTable.get)) {
          tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
            "Input error for '" + this.title + "': 'procedureSymbolTable' keys '" + _procedureSymbolTableKey.get + " and '" + key + "' point to different objects.")
        }
      }
    )

    _procedureSymbolTable match{
      case Some(x) =>
        if(!x.isInstanceOf[org.sireum.pilar.symbol.ProcedureSymbolTable]){
          tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
            "Input error for '" + this.title + "': Wrong type found for 'procedureSymbolTable'.  Expecting 'org.sireum.pilar.symbol.ProcedureSymbolTable' but found '" + x.getClass.toString + "'")
        }
      case None =>
        tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
          "Input error for '" + this.title + "': No value found for 'procedureSymbolTable'")       
    }
    var _cfg : scala.Option[AnyRef] = None
    var _cfgKey : scala.Option[String] = None

    val keylistcfg = List(cCfgModule.globalCfgKey, CfgModule.cfgKey)
    keylistcfg.foreach(key => 
      if(job ? key) { 
        if(_cfg.isEmpty) {
          _cfg = Some(job(key))
          _cfgKey = Some(key)
        }
        if(!(job(key).asInstanceOf[AnyRef] eq _cfg.get)) {
          tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
            "Input error for '" + this.title + "': 'cfg' keys '" + _cfgKey.get + " and '" + key + "' point to different objects.")
        }
      }
    )

    _cfg match{
      case Some(x) =>
        if(!x.isInstanceOf[org.sireum.alir.ControlFlowGraph[java.lang.String]]){
          tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
            "Input error for '" + this.title + "': Wrong type found for 'cfg'.  Expecting 'org.sireum.alir.ControlFlowGraph[java.lang.String]' but found '" + x.getClass.toString + "'")
        }
      case None =>
        tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
          "Input error for '" + this.title + "': No value found for 'cfg'")       
    }
    return tags
  }

  def outputDefined (job : PipelineJob) : MBuffer[Tag] = {
    val tags = marrayEmpty[Tag]
    if(!(job ? cCfgModule.cCfgKey) && !(job ? cCfgModule.globalCCfgKey)) {
      tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
        "Output error for '" + this.title + "': No entry found for 'cCfg'. Expecting (cCfgModule.cCfgKey or cCfgModule.globalCCfgKey)") 
    }

    if(job ? cCfgModule.cCfgKey && !job(cCfgModule.cCfgKey).isInstanceOf[org.sireum.amandroid.intraProcedural.compressedControlFlowGraph.CompressedControlFlowGraph[java.lang.String]]) {
      tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker, 
        "Output error for '" + this.title + "': Wrong type found for cCfgModule.cCfgKey.  Expecting 'org.sireum.amandroid.intraProcedural.compressedControlFlowGraph.CompressedControlFlowGraph[java.lang.String]' but found '" + 
        job(cCfgModule.cCfgKey).getClass.toString + "'")
    } 

    if(job ? cCfgModule.globalCCfgKey && !job(cCfgModule.globalCCfgKey).isInstanceOf[org.sireum.amandroid.intraProcedural.compressedControlFlowGraph.CompressedControlFlowGraph[java.lang.String]]) {
      tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker, 
        "Output error for '" + this.title + "': Wrong type found for cCfgModule.globalCCfgKey.  Expecting 'org.sireum.amandroid.intraProcedural.compressedControlFlowGraph.CompressedControlFlowGraph[java.lang.String]' but found '" + 
        job(cCfgModule.globalCCfgKey).getClass.toString + "'")
    } 
    return tags
  }

  def getPool (options : scala.collection.Map[Property.Key, Any]) : scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode] = {
    if (options.contains(cCfgModule.globalPoolKey)) {
       return options(cCfgModule.globalPoolKey).asInstanceOf[scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode]]
    }
    if (options.contains(cCfgModule.poolKey)) {
       return options(cCfgModule.poolKey).asInstanceOf[scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode]]
    }
    if (options.contains(CfgModule.poolKey)) {
       return options(CfgModule.poolKey).asInstanceOf[scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode]]
    }

    throw new Exception("Pipeline checker should guarantee we never reach here")
  }

  def setPool (options : MMap[Property.Key, Any], pool : scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode]) : MMap[Property.Key, Any] = {

    options(cCfgModule.globalPoolKey) = pool
    options(poolKey) = pool

    return options
  }

  def getProcedureSymbolTable (options : scala.collection.Map[Property.Key, Any]) : org.sireum.pilar.symbol.ProcedureSymbolTable = {
    if (options.contains(cCfgModule.globalProcedureSymbolTableKey)) {
       return options(cCfgModule.globalProcedureSymbolTableKey).asInstanceOf[org.sireum.pilar.symbol.ProcedureSymbolTable]
    }

    throw new Exception("Pipeline checker should guarantee we never reach here")
  }

  def setProcedureSymbolTable (options : MMap[Property.Key, Any], procedureSymbolTable : org.sireum.pilar.symbol.ProcedureSymbolTable) : MMap[Property.Key, Any] = {

    options(cCfgModule.globalProcedureSymbolTableKey) = procedureSymbolTable

    return options
  }

  def getCfg (options : scala.collection.Map[Property.Key, Any]) : org.sireum.alir.ControlFlowGraph[java.lang.String] = {
    if (options.contains(cCfgModule.globalCfgKey)) {
       return options(cCfgModule.globalCfgKey).asInstanceOf[org.sireum.alir.ControlFlowGraph[java.lang.String]]
    }
    if (options.contains(cCfgModule.cfgKey)) {
       return options(cCfgModule.cfgKey).asInstanceOf[org.sireum.alir.ControlFlowGraph[java.lang.String]]
    }
    if (options.contains(CfgModule.cfgKey)) {
       return options(CfgModule.cfgKey).asInstanceOf[org.sireum.alir.ControlFlowGraph[java.lang.String]]
    }

    throw new Exception("Pipeline checker should guarantee we never reach here")
  }

  def setCfg (options : MMap[Property.Key, Any], cfg : org.sireum.alir.ControlFlowGraph[java.lang.String]) : MMap[Property.Key, Any] = {

    options(cCfgModule.globalCfgKey) = cfg
    options(cfgKey) = cfg

    return options
  }

  def getCCfg (options : scala.collection.Map[Property.Key, Any]) : org.sireum.amandroid.intraProcedural.compressedControlFlowGraph.CompressedControlFlowGraph[java.lang.String] = {
    if (options.contains(cCfgModule.globalCCfgKey)) {
       return options(cCfgModule.globalCCfgKey).asInstanceOf[org.sireum.amandroid.intraProcedural.compressedControlFlowGraph.CompressedControlFlowGraph[java.lang.String]]
    }
    if (options.contains(cCfgModule.cCfgKey)) {
       return options(cCfgModule.cCfgKey).asInstanceOf[org.sireum.amandroid.intraProcedural.compressedControlFlowGraph.CompressedControlFlowGraph[java.lang.String]]
    }

    throw new Exception("Pipeline checker should guarantee we never reach here")
  }

  def setCCfg (options : MMap[Property.Key, Any], cCfg : org.sireum.amandroid.intraProcedural.compressedControlFlowGraph.CompressedControlFlowGraph[java.lang.String]) : MMap[Property.Key, Any] = {

    options(cCfgModule.globalCCfgKey) = cCfg
    options(cCfgKey) = cCfg

    return options
  }

  object ConsumerView {
    implicit class cCfgModuleConsumerView (val job : PropertyProvider) extends AnyVal {
      def pool : scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode] = cCfgModule.getPool(job.propertyMap)
      def procedureSymbolTable : org.sireum.pilar.symbol.ProcedureSymbolTable = cCfgModule.getProcedureSymbolTable(job.propertyMap)
      def cfg : org.sireum.alir.ControlFlowGraph[java.lang.String] = cCfgModule.getCfg(job.propertyMap)
      def cCfg : org.sireum.amandroid.intraProcedural.compressedControlFlowGraph.CompressedControlFlowGraph[java.lang.String] = cCfgModule.getCCfg(job.propertyMap)
    }
  }

  object ProducerView {
    implicit class cCfgModuleProducerView (val job : PropertyProvider) extends AnyVal {

      def pool_=(pool : scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode]) { cCfgModule.setPool(job.propertyMap, pool) }
      def pool : scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode] = cCfgModule.getPool(job.propertyMap)

      def procedureSymbolTable_=(procedureSymbolTable : org.sireum.pilar.symbol.ProcedureSymbolTable) { cCfgModule.setProcedureSymbolTable(job.propertyMap, procedureSymbolTable) }
      def procedureSymbolTable : org.sireum.pilar.symbol.ProcedureSymbolTable = cCfgModule.getProcedureSymbolTable(job.propertyMap)

      def cfg_=(cfg : org.sireum.alir.ControlFlowGraph[java.lang.String]) { cCfgModule.setCfg(job.propertyMap, cfg) }
      def cfg : org.sireum.alir.ControlFlowGraph[java.lang.String] = cCfgModule.getCfg(job.propertyMap)

      def cCfg_=(cCfg : org.sireum.amandroid.intraProcedural.compressedControlFlowGraph.CompressedControlFlowGraph[java.lang.String]) { cCfgModule.setCCfg(job.propertyMap, cCfg) }
      def cCfg : org.sireum.amandroid.intraProcedural.compressedControlFlowGraph.CompressedControlFlowGraph[java.lang.String] = cCfgModule.getCCfg(job.propertyMap)
    }
  }
}

trait cCfgModule {
  def job : PipelineJob

  def pool : scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode] = cCfgModule.getPool(job.propertyMap)

  def procedureSymbolTable : org.sireum.pilar.symbol.ProcedureSymbolTable = cCfgModule.getProcedureSymbolTable(job.propertyMap)

  def cfg : org.sireum.alir.ControlFlowGraph[java.lang.String] = cCfgModule.getCfg(job.propertyMap)


  def cCfg_=(cCfg : org.sireum.amandroid.intraProcedural.compressedControlFlowGraph.CompressedControlFlowGraph[java.lang.String]) { cCfgModule.setCCfg(job.propertyMap, cCfg) }
  def cCfg : org.sireum.amandroid.intraProcedural.compressedControlFlowGraph.CompressedControlFlowGraph[java.lang.String] = cCfgModule.getCCfg(job.propertyMap)
}