package jreframeworker.core;

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

import org.eclipse.core.resources.IProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import jreframeworker.log.Log;

public class BuildFile {

	public static final String XML_BUILD_FILENAME = "jreframeworker.xml";
	
	private File jrefXMLFile;
	
	private BuildFile(File jrefXMLFile){
		this.jrefXMLFile = jrefXMLFile;
	}
	
	/**
	 * Returns a set of all the target jars in the xml file
	 * @param buildFile
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public Set<String> getTargets() throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(jrefXMLFile);
		doc.getDocumentElement().normalize();
		NodeList targets = doc.getElementsByTagName("target");
		Set<String> results = new HashSet<String>();
		for (int i = 0; i < targets.getLength(); i++) {
			Element target = (Element) targets.item(i);
			results.add(target.getAttribute("name"));
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
	public void addTarget(String targetJarName) throws TransformerException, ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(jrefXMLFile);
		doc.getDocumentElement().normalize();
		
		// add target
		Element rootElement = doc.getDocumentElement();
		Element target = doc.createElement("target");
		rootElement.appendChild(target);
		target.setAttribute("name", targetJarName);

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
	 */
	public static BuildFile getOrCreateBuildFile(IProject project) {
		File buildXMLFile = new File(project.getLocation().toFile().getAbsolutePath() + File.separator + XML_BUILD_FILENAME);
		if(buildXMLFile.exists()){
			return new BuildFile(buildXMLFile);
		} else {
			return createBuildFile(project);
		}
	}
	
	/**
	 * Creates a new build file
	 * @param project
	 * @return
	 */
	public static BuildFile createBuildFile(IProject project) {
		try {
			File buildXMLFile = new File(project.getLocation().toFile().getAbsolutePath() + File.separator + XML_BUILD_FILENAME);
			Log.info("Created Build XML File: " + buildXMLFile.getAbsolutePath());
			
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root elements
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("build");
			doc.appendChild(rootElement);

			// write the content into xml file
			writeBuildFile(buildXMLFile, doc);
			
			return new BuildFile(buildXMLFile);
		} catch (ParserConfigurationException pce) {
			Log.error("ParserConfigurationException", pce);
		} catch (TransformerException tfe) {
			Log.error("TransformerException", tfe);
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
