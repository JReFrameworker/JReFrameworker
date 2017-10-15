package com.jreframeworker.core;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.jreframeworker.log.Log;
import com.jreframeworker.preferences.JReFrameworkerPreferences;

public class BuildFile {

	public static final String XML_BUILD_FILENAME = "jreframeworker.xml";
	
	private File jrefXMLFile;

	private BuildFile(File jrefXMLFile){
		this.jrefXMLFile = jrefXMLFile;
	}
	
	/**
	 * A build file is equivalent to an object if it is representing the same file
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jrefXMLFile == null) ? 0 : jrefXMLFile.hashCode());
		return result;
	}

	/**
	 * A build file is equivalent to an object if it is representing the same file
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BuildFile other = (BuildFile) obj;
		if (jrefXMLFile == null) {
			if (other.jrefXMLFile != null)
				return false;
		} else if (!jrefXMLFile.equals(other.jrefXMLFile))
			return false;
		return true;
	}
	
	/**
	 * A target is a jar that we want to modify
	 */
	public static abstract class Target {
		private String name;
		
		public Target(String name){
			this.name = name;
		}
		
		public String getName(){
			return name;
		}
		
		public abstract boolean isRuntime();
	}
	
	/**
	 * For all practical purposes, we say a runtime target has a path
	 * and we know where it is absolutely or relative to the project
	 */
	public static class LibraryTarget extends Target {
		
		private String path;
		
		public LibraryTarget(String name, String path){
			super(name);
			this.path = path;
		}
		
		public String getLibraryPath(){
			return path;
		}

		@Override
		public boolean isRuntime() {
			return false;
		}
	}
	
	/**
	 * For all practical purposes, we say a runtime target does not have a path
	 * since it is located depending on the current project runtime.
	 */
	public static class RuntimeTarget extends Target {

		public RuntimeTarget(String name) {
			super(name);
		}
		
		@Override
		public boolean isRuntime() {
			return true;
		}
		
	}
	
	/**
	 * Returns a set of all the target jars in the xml file
	 * @param buildFile
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public Set<Target> getTargets() throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(jrefXMLFile);
		doc.getDocumentElement().normalize();
		NodeList targets = doc.getElementsByTagName("target");
		Set<Target> results = new HashSet<Target>();
		for (int i = 0; i < targets.getLength(); i++) {
			Element target = (Element) targets.item(i);
			String name = target.getAttribute("name");
			if(!name.isEmpty()){
				Boolean runtime = false;
				if(target.hasAttribute("runtime")){
					runtime = Boolean.parseBoolean(target.getAttribute("runtime"));
				}
				if(runtime){
					results.add(new RuntimeTarget(name));
				} else {
					String path = target.getAttribute("path");
					path = path.replace(File.separatorChar, '/');
					results.add(new LibraryTarget(name, path));
				}
			}
		}
		return results;
	}
	
	/**
	 * Adds a target jar to the build file
	 * @param buildFile
	 * @param targetJar
	 * @throws TransformerException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public void addRuntimeTarget(String targetJarName) throws TransformerException, ParserConfigurationException, SAXException, IOException {
		addTarget(new RuntimeTarget(targetJarName));
	}
	
	/**
	 * Adds a target jar to the build file
	 * @param buildFile
	 * @param targetJar
	 * @throws TransformerException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public void addLibraryTarget(String targetJarName, String targetJarPath) throws TransformerException, ParserConfigurationException, SAXException, IOException {
		addTarget(new LibraryTarget(targetJarName, targetJarPath));
	}
	
	/**
	 * Adds a target jar to the build file
	 * @param buildFile
	 * @param targetJar
	 * @throws TransformerException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public void addTarget(Target targetToAdd) throws TransformerException, ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(jrefXMLFile);
		doc.getDocumentElement().normalize();
		
		// check if the target already exists
		for(Target target : getTargets()) {
			if(target.getName().equalsIgnoreCase(targetToAdd.getName())) {
				return;
			}
		}
		
		// add target
		Element rootElement = doc.getDocumentElement();
		Element target = doc.createElement("target");
		rootElement.appendChild(target);
		
		target.setAttribute("name", targetToAdd.getName());
		
		if(targetToAdd instanceof RuntimeTarget){
			target.setAttribute("runtime", "true");
		} else if(targetToAdd instanceof LibraryTarget){
			target.setAttribute("runtime", "false");
			target.setAttribute("path", ((LibraryTarget)targetToAdd).getLibraryPath());
		}

		// write the content into xml file
		writeBuildFile(jrefXMLFile, doc);
	}
	
	/**
	 * Removes a target jar from the build file
	 * @param project
	 * @param targetJar
	 */
	public void removeTarget(String targetJarName) throws TransformerException, ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(jrefXMLFile);
		doc.getDocumentElement().normalize();
		
		// remove target
		NodeList targets = doc.getElementsByTagName("target");
		for (int i = 0; i < targets.getLength(); i++) {
			Element target = (Element) targets.item(i);
			if(target.getAttribute("name").equals(targetJarName)){
				target.getParentNode().removeChild(target);
			}
		}

		// write the content into xml file
		writeBuildFile(jrefXMLFile, doc);
	}
	
	/**
	 * Returns the existing build file or creates one if one does not exist
	 * @param project
	 * @return
	 * @throws JavaModelException 
	 */
	public static BuildFile getOrCreateBuildFile(IJavaProject jProject) {
		File buildXMLFile = new File(jProject.getProject().getLocation().toFile().getAbsolutePath() + File.separator + XML_BUILD_FILENAME);
		if(buildXMLFile.exists()){
			return new BuildFile(buildXMLFile);
		} else {
			return createBuildFile(jProject);
		}
	}
	
	/**
	 * Creates a new build file
	 * @param project
	 * @return
	 * @throws JavaModelException 
	 */
	public static BuildFile createBuildFile(IJavaProject jProject) {
		try {
			File buildXMLFile = new File(jProject.getProject().getLocation().toFile().getAbsolutePath() + File.separator + XML_BUILD_FILENAME);
			String base = jProject.getProject().getLocation().toFile().getCanonicalPath();
			String relativeBuildFilePath = buildXMLFile.getCanonicalPath().substring(base.length());
			if(relativeBuildFilePath.charAt(0) == File.separatorChar){
				relativeBuildFilePath = relativeBuildFilePath.substring(1);
			}

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root elements
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("build");
			doc.appendChild(rootElement);
			
			// save the original classpath
			Element targets = doc.createElement("targets");
			rootElement.appendChild(targets);

			// write the content into xml file
			writeBuildFile(buildXMLFile, doc);
			
			if(JReFrameworkerPreferences.isVerboseLoggingEnabled()) {
				Log.info("Created Build XML File: " + relativeBuildFilePath);
			}
			
			return new BuildFile(buildXMLFile);
		} catch (ParserConfigurationException pce) {
			Log.error("ParserConfigurationException", pce);
		} catch (TransformerException tfe) {
			Log.error("TransformerException", tfe);
		} catch (IOException ioe) {
			Log.error("IOException", ioe);
		} catch (DOMException dome) {
			Log.error("DOMException", dome);
		}
		throw new RuntimeException("Unable to create build file.");
	}

	/**
	 * Helper method to write a pretty-printed build xml file
	 * @param buildXMLFile
	 * @param doc
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 */
	private static void writeBuildFile(File buildXMLFile, Document doc) throws TransformerFactoryConfigurationError, TransformerConfigurationException, TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(buildXMLFile);
		transformer.transform(source, result);
	}
	
}
