package org.sireum.amandroid.android.parser

import pxb.android.axml.AxmlVisitor.NodeVisitor
import org.sireum.jawa.MessageCenter._
import pxb.android.axml.AxmlVisitor
import pxb.android.axml.AxmlReader
import java.io.ByteArrayOutputStream
import org.sireum.util._
import java.io.InputStream

/**
 * Parser for analyzing the resource XML files inside an android application
 * 
 * Author: Fengguo Wei
 */
class ResourceFileParser extends AbstractAndroidXMLParser {
	final val TITLE = "ResourceFileParser"
	private final val DEBUG = false
	
	private val strs : MSet[String] = msetEmpty
	
	def getAllStrings : Set[String] = this.strs.toSet
	
	private class ResourceParser(resFile : String) extends NodeVisitor {
	        	
  	override def attr(ns : String, name : String, resourceId : Int, typ : Int, obj : Object) : Unit = {
  		// Check that we're actually working on an android attribute
  		if (ns == null)
  			return
  	  var tempNS = ns
  		tempNS = tempNS.trim()
  		if (tempNS.startsWith("*"))
  			tempNS = tempNS.substring(1)
  		if (!tempNS.equals("http://schemas.android.com/apk/res/android"))
  			return

  		// Read out the field data
  		var tempName = name
  		tempName = tempName.trim()
  		if (typ == AxmlVisitor.TYPE_STRING && obj.isInstanceOf[String]) {
  			val strData = obj.asInstanceOf[String].trim();
  			strs += strData
  		}
  	}
	}
	
	/**
	 * Parses all resource XML files in the given APK file.
	 * @param fileName The APK file
	 */
	def parseResourceFile(apkUri : FileResourceUri) {
				handleAndroidXMLFiles(apkUri, null, new AndroidXMLHandler() {
					
					override def handleXMLFile(fileName : String, fileNameFilter : Set[String], stream : InputStream) : Unit = {
						// We only process valid layout XML files
						if (!fileName.startsWith("res/"))
							return
						if (!fileName.endsWith(".xml")) {
							err_msg_normal(TITLE, "Skipping file " + fileName + " in resource folder...")
							return
						}
						// Get the fully-qualified class name
						var entryClass = fileName.substring(0, fileName.lastIndexOf("."))
						try {
							val bos = new ByteArrayOutputStream();
							var in : Int = 0
							in = stream.read()
							while (in >= 0){
								bos.write(in)
								in = stream.read()
							}
							bos.flush()
							val data = bos.toByteArray()
							if (data == null || data.length == 0)	// File empty?
								return
							
							val rdr = new AxmlReader(data)
							rdr.accept(new AxmlVisitor() {
								
								override def first(ns : String, name : String) : NodeVisitor = {
									new ResourceParser(fileName)
								}
							})
						}
						catch {
						  case ex : Exception =>
							  err_msg_detail(TITLE, "Could not read binary XML file: " + ex.getMessage())
						}
					}
				})
	}
}