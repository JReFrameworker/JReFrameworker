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
	 * Creates logic to define a new class
	 * @param packageName
	 * @param className
	 */
	public void defineType(String packageName, String className){
		// TODO: implement
	}
	
	/**
	 * Creates logic to replace a class in the given class target
	 * @param targetClass
	 */
	public void replaceType(Node targetClass){
		// TODO: implement
	}
	
	/**
	 * Creates logic to merge code into the given class target
	 * @param targetClass
	 */
	public void mergeType(Node targetClass){
		// TODO: implement
	}
	
	/**
	 * Creates logic to define a new field in the given target classes
	 * @param fields
	 */
	public void defineFields(Q targetClasses){
		defineFields(targetClasses.nodesTaggedWithAny(XCSG.Java.Class).eval().nodes());
	}
	
	/**
	 * Creates logic to define a new field in the given target classes
	 * @param field
	 */
	public void defineFields(AtlasSet<Node> targetClasses){
		for(Node field : targetClasses){
			defineField(field);
		}
	}
	
	/**
	 * Creates logic to define a new field in the given target class
	 * @param field
	 */
	public void defineField(Node targetClass){
		if(targetClass.taggedWith(XCSG.Java.Class)){
			// TODO: implement
		}
	}
	
	/**
	 * Creates logic to replace code in the given fields
	 * @param fields
	 */
	public void replaceFields(Q fields){
		replaceFields(fields.nodesTaggedWithAny(XCSG.Field).eval().nodes());
	}
	
	/**
	 * Creates logic to replace code in the given fields
	 * @param fields
	 */
	public void replaceFields(AtlasSet<Node> fields){
		for(Node field : fields){
			replaceField(field);
		}
	}
	
	/**
	 * Creates logic to replace code in the given field
	 * @param field
	 */
	public void replaceField(Node field){
		if(field.taggedWith(XCSG.Field)){
			// TODO: implement
		}
	}
	
	/**
	 * Creates logic to define a new method in the given target classes
	 * @param methods
	 */
	public void defineMethods(Q targetClasses){
		defineMethods(targetClasses.nodesTaggedWithAny(XCSG.Java.Class).eval().nodes());
	}
	
	/**
	 * Creates logic to define a new method in the given target classes
	 * @param method
	 */
	public void defineMethods(AtlasSet<Node> targetClasses){
		for(Node method : targetClasses){
			defineMethod(method);
		}
	}
	
	/**
	 * Creates logic to define a new method in the given target class
	 * @param method
	 */
	public void defineMethod(Node targetClass){
		if(targetClass.taggedWith(XCSG.Java.Class)){
			// TODO: implement
		}
	}
	
	/**
	 * Creates logic to replace code in the given methods
	 * @param methods
	 */
	public void replaceMethods(Q methods){
		replaceMethods(methods.nodesTaggedWithAny(XCSG.Method).eval().nodes());
	}
	
	/**
	 * Creates logic to replace code in the given methods
	 * @param methods
	 */
	public void replaceMethods(AtlasSet<Node> methods){
		for(Node method : methods){
			replaceMethod(method);
		}
	}
	
	/**
	 * Creates logic to replace code in the given method
	 * @param method
	 */
	public void replaceMethod(Node method){
		if(method.taggedWith(XCSG.Method)){
			// TODO: implement
		}
	}
	
	/**
	 * Creates logic to preserve and replace accessible code in the given methods
	 * @param methods
	 */
	public void mergeMethods(Q methods){
		mergeMethods(methods.nodesTaggedWithAny(XCSG.Method).eval().nodes());
	}
	
	/**
	 * Creates logic to preserve and replace accessible code in the given methods
	 * @param method
	 */
	public void mergeMethods(AtlasSet<Node> methods){
		for(Node method : methods){
			mergeMethod(method);
		}
	}
	
	/**
	 * Creates logic to preserve and replace accessible code in the given method
	 * @param method
	 */
	public void mergeMethod(Node method){
		if(method.taggedWith(XCSG.Method)){
			// TODO: implement
		}
	}
	
	/**
	 * Creates logic to inject code before the given methods are executed
	 * @param method
	 */
	public void addPreExecutionMethodHooks(Q methods){
		addPreExecutionMethodHooks(methods.nodesTaggedWithAny(XCSG.Method).eval().nodes());
	}
	
	/**
	 * Creates logic to inject code before the given methods are executed
	 * @param method
	 */
	public void addPreExecutionMethodHooks(AtlasSet<Node> methods){
		for(Node method : methods){
			addPreExecutionMethodHook(method);
		}
	}
	
	/**
	 * Creates logic to inject code before the given method is executed
	 * @param method
	 */
	public void addPreExecutionMethodHook(Node method){
		if(method.taggedWith(XCSG.Method)){
			// TODO: implement
		}
	}
	
	/**
	 * Creates logic to inject code after the given methods are executed
	 * @param method
	 */
	public void addPostExecutionMethodHooks(Q methods){
		addPostExecutionMethodHooks(methods.nodesTaggedWithAny(XCSG.Method).eval().nodes());
	}
	
	/**
	 * Creates logic to inject code after the given methods are executed
	 * @param method
	 */
	public void addPostExecutionMethodHooks(AtlasSet<Node> methods){
		for(Node method : methods){
			addPostExecutionMethodHook(method);
		}
	}
	/**
	 * Creates logic to inject code after the given method is executed
	 * @param method
	 */
	public void addPostExecutionMethodHook(Node method){
		if(method.taggedWith(XCSG.Method)){
			// TODO: implement
		}
	}
}
