package org.sireum.amandroid.test.framework.security

import org.sireum.amandroid.test.framework.TestFramework
import org.sireum.util._
import org.sireum.jawa.MessageCenter._
import org.sireum.amandroid.alir.AndroidGlobalConfig
import java.io.File
import java.net.URI
import org.sireum.jawa.util.APKFileResolver
import org.sireum.amandroid.android.decompile.Dex2PilarConverter
import org.sireum.amandroid.alir.interProcedural.reachingFactsAnalysis.AndroidRFAConfig
import org.sireum.jawa.JawaCodeSource
import org.sireum.amandroid.android.util.AndroidLibraryAPISummary
import org.sireum.amandroid.alir.interProcedural.reachingFactsAnalysis.AndroidReachingFactsAnalysisConfig
import org.sireum.jawa.util.Timer
import org.sireum.jawa.Center
import org.sireum.amandroid.alir.dataRecorder.DataCollector
import org.sireum.amandroid.alir.dataRecorder.MetricRepo
import java.io.PrintWriter
import org.sireum.amandroid.alir.AppCenter
import org.sireum.jawa.util.IgnoreException
import org.sireum.amandroid.alir.AndroidConstants
import org.sireum.amandroid.alir.interProcedural.reachingFactsAnalysis.AndroidReachingFactsAnalysis
import org.sireum.jawa.ClassLoadManager
import org.sireum.jawa.util.TimeOutException
import org.sireum.amandroid.security.dataInjection.IntentInjectionCollector
import org.sireum.jawa.alir.interProcedural.dataDependenceAnalysis.InterproceduralDataDependenceAnalysis
import org.sireum.amandroid.alir.interProcedural.taintAnalysis.AndroidDataDependentTaintAnalysis
import org.sireum.amandroid.security.dataInjection.IntentInjectionSourceAndSinkManager

object IntentInjectionCounter {
  var total = 0
  var oversize = 0
  var haveresult = 0
  
  override def toString : String = "total: " + total + ", oversize: " + oversize + ", haveResult: " + haveresult
}

class IntentInjectionTestFramework extends TestFramework {
  def Analyzing : this.type = this

  def title(s : String) : this.type = {
    _title = caseString + s
    this
  }

  def file(fileRes : FileResourceUri) =
    InterProceduralConfiguration(title, fileRes)
/**
 * does inter procedural analysis of an app
 * @param src is the uri of the apk file
 */
  case class InterProceduralConfiguration //
  (title : String,
   srcRes : FileResourceUri) {

    test(title) {
    	msg_critical("####" + title + "#####")
    	IntentInjectionCounter.total += 1
    	// before starting the analysis of the current app, first reset the Center which may still hold info (of the resolved records) from the previous analysis
    	AndroidGlobalConfig.initJawaAlirInfoProvider
    	
    	val srcFile = new File(new URI(srcRes))
    	val dexFile = APKFileResolver.getDexFile(srcRes, FileUtil.toUri(srcFile.getParentFile()))
    	
    	// convert the dex file to the "pilar" form
    	val pilarFileUri = Dex2PilarConverter.convert(dexFile)
    	val pilarFile = new File(new URI(pilarFileUri))
    	if(pilarFile.length() <= (100 * 1024 * 1024)){
    		AndroidRFAConfig.setupCenter
	    	//store the app's pilar code in AmandroidCodeSource which is organized record by record.
	    	JawaCodeSource.load(pilarFileUri, AndroidLibraryAPISummary)
	    	try{
		    	val pre = new IntentInjectionCollector(srcRes)
				  pre.collectInfo
				  val ssm = new IntentInjectionSourceAndSinkManager(pre.getPackageName, pre.getLayoutControls, pre.getCallbackMethods, AndroidGlobalConfig.SourceAndSinkFilePath)
				  var entryPoints = Center.getEntryPoints(AndroidConstants.MAINCOMP_ENV)
//		    	entryPoints ++= Center.getEntryPoints(AndroidConstants.COMP_ENV)
		    	val iacs = pre.getInterestingContainers(ssm.getSinkSigs ++ AndroidConstants.getIccMethods)
		    	entryPoints = entryPoints.filter(e=>iacs.contains(e.getDeclaringRecord))
		    	println(entryPoints)
				  AndroidReachingFactsAnalysisConfig.k_context = 1
			    AndroidReachingFactsAnalysisConfig.resolve_icc = false
			    AndroidReachingFactsAnalysisConfig.resolve_static_init = false
			    AndroidReachingFactsAnalysisConfig.timerOpt = Some(new Timer(5))
		    	entryPoints.par.foreach{
		    	  ep =>
		    	    try{
			    	    msg_critical("--------------Component " + ep + "--------------")
			    	    val initialfacts = AndroidRFAConfig.getInitialFactsForMainEnvironment(ep)
			    	    val (icfg, irfaResult) = AndroidReachingFactsAnalysis(ep, initialfacts, new ClassLoadManager)
			    	    AppCenter.addInterproceduralReachingFactsAnalysisResult(ep.getDeclaringRecord, icfg, irfaResult)
			    	    msg_critical("processed-->" + icfg.getProcessed.size)
			    	    val iddResult = InterproceduralDataDependenceAnalysis(icfg, irfaResult)
			    	    AppCenter.addInterproceduralDataDependenceAnalysisResult(ep.getDeclaringRecord, iddResult)
			    	    val tar = AndroidDataDependentTaintAnalysis(iddResult, irfaResult, ssm)    
			    	    AppCenter.addTaintAnalysisResult(ep.getDeclaringRecord, tar)
				    	} catch {
		    	      case te : TimeOutException => System.err.println("Timeout!")
		    	    }
    	    } 
				  
		    	val appData = DataCollector.collect
		    	MetricRepo.collect(appData)
		    	val outputDir = System.getenv(AndroidGlobalConfig.ANDROID_OUTPUT_DIR)
		    	if(outputDir == null) throw new RuntimeException("Does not have env var: " + AndroidGlobalConfig.ANDROID_OUTPUT_DIR)
		    	val apkName = title.substring(0, title.lastIndexOf("."))
		    	val appDataDirFile = new File(outputDir + "/" + apkName)
		    	if(!appDataDirFile.exists()) appDataDirFile.mkdirs()
		    	val out = new PrintWriter(appDataDirFile + "/AppData.txt")
			    out.print(appData.toString)
			    out.close()
			    val mr = new PrintWriter(outputDir + "/MetricInfo.txt")
				  mr.print(MetricRepo.toString)
				  mr.close()
				  IntentInjectionCounter.haveresult += 1
	    	} catch {
	    	  case ie : IgnoreException =>
	    	    err_msg_critical("Ignored!")
	    	  case re : RuntimeException => 
	    	    re.printStackTrace()
	    	  case e : Exception =>
	    	    e.printStackTrace()
	    	} finally {
	    	}
    	} else {
    	  IntentInjectionCounter.oversize += 1
    	  err_msg_critical("Pilar file size is too large:" + pilarFile.length()/1024/1024 + "MB")
    	}
    	
    	Center.reset
    	AppCenter.reset
    	// before starting the analysis of the current app, first clear the previous app's records' code from the AmandroidCodeSource
    	JawaCodeSource.clearAppRecordsCodes
    	System.gc()
		  System.gc()
    	msg_critical(IntentInjectionCounter.toString)
    	msg_critical("************************************\n")
    }
  }

  protected var _title : String = null
  protected var num = 0
  protected def title() = if (_title == null) {
    num += 1
    "Analysis #" + num
  } else _title
}