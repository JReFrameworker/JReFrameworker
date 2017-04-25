package jreframeworker.atlas;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.runtime.CoreException;
import org.xml.sax.SAXException;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.xcsg.XCSG;

import jreframeworker.core.JReFrameworkerProject;

public class JReFrameworkerAtlasProject {

	private JReFrameworkerProject project;
	
	public JReFrameworkerAtlasProject(JReFrameworkerProject project) {
		this.project = project;
	}
	
	/**
	 * Returns the Eclipse project resource
	 * @return
	 */
	public JReFrameworkerProject getProject(){
		return project;
	}
	
	/**
	 * Lists the JReFrameworker project targets
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public Set<String> listTargets() throws SAXException, IOException, ParserConfigurationException {
		return project.listTargets();
	}
	
	/**
	 * Adds a target from the JReFrameworker project
	 * @throws CoreException 
	 * @throws URISyntaxException 
	 */
	public void addTarget(File targetLibrary) throws TransformerException, ParserConfigurationException, SAXException, IOException, URISyntaxException, CoreException {
		project.addTarget(targetLibrary);
	}
	
	/**
	 * Adds a target with the given relative library directory
	 * @param targetLibrary
	 * @param relativeLibraryDirectory
	 * @throws TransformerException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws CoreException
	 */
	public void addTarget(File targetLibrary, String relativeLibraryDirectory) throws TransformerException, ParserConfigurationException, SAXException, IOException, URISyntaxException, CoreException {
		project.addTarget(targetLibrary, relativeLibraryDirectory);
	}
	
	/**
	 * Removes a target from the JReFrameworker project
	 */
	public void removeTarget(String target) throws TransformerException, ParserConfigurationException, SAXException, IOException {
		project.removeTarget(target);
	}
	
	/**
	 * Creates logic to inject code before the given methods are executed
	 * @param method
	 */
	public void addPreExecutionFunctionHooks(Q functions){
		addPreExecutionFunctionHooks(functions.nodesTaggedWithAny(XCSG.Function).eval().nodes());
	}
	
	/**
	 * Creates logic to inject code before the given methods are executed
	 * @param method
	 */
	public void addPreExecutionFunctionHooks(AtlasSet<Node> functions){
		for(Node function : functions){
			if(function.taggedWith(XCSG.Function)){
				addPreExecutionFunctionHook(function);
			}
		}
	}
	
	/**
	 * Creates logic to inject code before the given method is executed
	 * @param method
	 */
	public void addPreExecutionFunctionHook(Node function){
		// TODO: implement
	}
	
	/**
	 * Creates logic to inject code after the given methods are executed
	 * @param method
	 */
	public void addPostExecutionFunctionHooks(Q functions){
		addPostExecutionFunctionHooks(functions.nodesTaggedWithAny(XCSG.Function).eval().nodes());
	}
	
	/**
	 * Creates logic to inject code after the given methods are executed
	 * @param method
	 */
	public void addPostExecutionFunctionHooks(AtlasSet<Node> functions){
		for(Node function : functions){
			if(function.taggedWith(XCSG.Function)){
				addPostExecutionFunctionHook(function);
			}
		}
	}
	
	/**
	 * Creates logic to inject code after the given method is executed
	 * @param method
	 */
	public void addPostExecutionFunctionHook(Node method){
		// TODO: implement
	}
}
