package jreframeworker.atlas.projects;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.runtime.CoreException;
import org.xml.sax.SAXException;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.log.Log;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import jreframeworker.annotations.types.DefineType;
import jreframeworker.annotations.types.DefineTypeFinality;
import jreframeworker.annotations.types.DefineTypeVisibility;
import jreframeworker.annotations.types.MergeType;
import jreframeworker.atlas.analysis.ClassAnalysis;
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
	
	public void refresh() throws CoreException {
		project.refresh();
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
	 * Creates logic to define a new class with the specified javadoc comment
	 * 
	 * Example: defineType("com.test", "HelloWorld")
	 * @param packageName
	 * @param className
	 * @throws IOException  
	 * @throws CoreException 
	 */
	public void defineType(String packageName, String className, String javadoc) {
		packageName = packageName.trim();
		checkPackageName(packageName);
		
		try {
			TypeSpec type = TypeSpec.classBuilder(className)
				    .addModifiers(Modifier.PUBLIC)
				    .addAnnotation(DefineType.class)
				    .addJavadoc(javadoc)
				    .build();
			
			JavaFile javaFile = JavaFile.builder(packageName, type)
					.build();
	
			writeSourceFile(packageName, className, javaFile);
		} catch (Throwable t){
			Log.error("Error creating define type logic", t);
		}
	}
	
	/**
	 * Creates logic to define a new class
	 * 
	 * Example: defineType("com.test", "HelloWorld")
	 * @param packageName
	 * @param className
	 * @throws IOException  
	 * @throws CoreException 
	 */
	public void defineType(String packageName, String className) {
		String javadoc = "TODO: Implement class body"
				  + "\n\nThe entire contents of this class's bytecode will"
				  + "\nbe injected into the target's \"" + packageName + "\" package.\n";
		defineType(packageName, className, javadoc);
	}
	
	/**
	 * Creates logic to replace a class in the given class target
	 * @param targetClass
	 */
	public void replaceType(String packageName, String className){
		String javadoc = "TODO: Implement class body"
				  + "\n\nThe entire contents of this class's bytecode will"
				  + "\nbe used to replace " + packageName + "." + className + " in the target.\n";
		defineType(packageName, className, javadoc);
	}
	
	/**
	 * Creates logic to replace a class in the given class targets
	 * @param targetClass
	 */
	public void replaceTypes(Q targetClasses){
		replaceTypes(targetClasses.nodes(XCSG.Java.Class).eval().nodes());
	}
	
	/**
	 * Creates logic to replace a class in the given class targets
	 * @param targetClass
	 */
	public void replaceTypes(AtlasSet<Node> targetClasses){
		for(Node targetClass : targetClasses){
			replaceType(targetClass);
		}
	}
	
	/**
	 * Creates logic to replace a class in the given class target
	 * @param targetClass
	 */
	public void replaceType(Node targetClass){
		if(targetClass.taggedWith(XCSG.Java.Class)){
			String className = ClassAnalysis.getName(targetClass);
			String packageName = ClassAnalysis.getPackage(targetClass);
			replaceType(packageName, className);
		}
	}
	
	/**
	 * Creates a new class with that contains a DefineTypeFinality annotation
	 * set to the type specified by the given packageName and className values
	 * with the given finality
	 * 
	 * @param packageName
	 * @param className
	 * @param finality
	 */
	public void setTypeFinality(String packageName, String className, String targetClassPackageName, String targetClassName, boolean finality){
		packageName = packageName.trim();
		checkPackageName(packageName);
		checkPackageName(targetClassPackageName);
		try {
			String javadoc = "Runs as a first pass to set type finality.\n";
			
			TypeSpec type = TypeSpec.classBuilder(className)
				    .addModifiers(Modifier.PUBLIC)
				    .addAnnotation(AnnotationSpec.builder(DefineTypeFinality.class)
		                    .addMember("type", ("\"" + targetClassPackageName + "." + targetClassName + "\""))
		                    .addMember("finality", new Boolean(finality).toString())
		                    .build())
				    .addJavadoc(javadoc)
				    .build();
			
			JavaFile javaFile = JavaFile.builder(packageName, type)
					.build();
	
			writeSourceFile(packageName, className, javaFile);
		} catch (Throwable t){
			Log.error("Error creating define type finality logic", t);
		}
	}
	
	/**
	 * Creates a new class with that contains a DefineTypeVisibility annotation
	 * set to the type specified by the given packageName and className values
	 * with the given finality
	 * 
	 * @param packageName
	 * @param className
	 * @param finality
	 */
	public void setTypeVisibility(String packageName, String className, String targetClassPackageName, String targetClassName, String visibility){
		packageName = packageName.trim();
		checkPackageName(packageName);
		checkPackageName(targetClassPackageName);
		try {
			String javadoc = "Runs as a first pass to set type visibility.\n";
			
			TypeSpec type = TypeSpec.classBuilder(className)
				    .addModifiers(Modifier.PUBLIC)
				    .addAnnotation(AnnotationSpec.builder(DefineTypeVisibility.class)
		                    .addMember("type", ("\"" + targetClassPackageName + "." + targetClassName + "\""))
		                    .addMember("visibility", ("\"" + visibility + "\""))
		                    .build())
				    .addJavadoc(javadoc)
				    .build();
			
			JavaFile javaFile = JavaFile.builder(packageName, type)
					.build();
	
			writeSourceFile(packageName, className, javaFile);
		} catch (Throwable t){
			Log.error("Error creating define type visibility logic", t);
		}
	}
	
	/**
	 * Creates logic to merge code into the given class target
	 * Note: Does not consider prebuild options
	 * @param targetClass
	 */
	public void mergeType(String packageName, String className, String targetClassPackageName, String targetClassName){
		packageName = packageName.trim();
		checkPackageName(packageName);
		
		try {
			String qualifiedTargetClass = targetClassPackageName + "." + targetClassName;
			
			String javadoc = "TODO: Implement class body"
					  + "\n\nThe contents of this class's bytecode will be used to define, replace, and/or"
					  + "\nmerge (preserve and replace) with " + packageName + "." + className + " in the target.\n"
					  + "\nUse the @DefineField and @DefineMethod annotations to insert or replace\n"
					  + "\nfields and methods. Use the @MergeMethod annotation to preserve and hide the\n"
					  + "\noriginal method and replace the accessible method.\n";

			TypeSpec type = TypeSpec.classBuilder(className).superclass(Class.forName(qualifiedTargetClass))
				    .addModifiers(Modifier.PUBLIC)
				    .addAnnotation(MergeType.class)
				    .addJavadoc(javadoc)
				    .build();
			
			JavaFile javaFile = JavaFile.builder(packageName, type)
					.build();
	
			writeSourceFile(packageName, className, javaFile);
		} catch (Throwable t){
			Log.error("Error creating merge type logic", t);
		}
	}
	
	/**
	 * Creates logic to merge code into the given class target
	 * @param targetClass
	 */
	public void mergeType(Node targetClass){
		if(targetClass.taggedWith(XCSG.Java.Class)){
			String targetClassPackage = ClassAnalysis.getPackage(targetClass);
			String targetClassName = ClassAnalysis.getName(targetClass);
			if(ClassAnalysis.isFinal(targetClass)){
				setTypeFinality(targetClassPackage, (targetClassName + "FinalityPrebuild"), targetClassPackage, targetClassName, false);
			}
			if(ClassAnalysis.isFinal(targetClass)){
				setTypeVisibility(targetClassPackage, (targetClassName + "VisibilityPrebuild"), targetClassPackage, targetClassName, "public");
			}
			mergeType(targetClassPackage, "Merge" + targetClassName , targetClassPackage, targetClassName);
		}
	}
	
	/**
	 * Creates logic to replace a class in the given class targets
	 * @param targetClass
	 */
	public void mergeTypes(Q targetClasses){
		mergeTypes(targetClasses.nodes(XCSG.Java.Class).eval().nodes());
	}
	
	/**
	 * Creates logic to replace a class in the given class targets
	 * @param targetClass
	 */
	public void mergeTypes(AtlasSet<Node> targetClasses){
		for(Node targetClass : targetClasses){
			mergeType(targetClass);
		}
	}
	
	/**
	 * Creates logic to define a new field in the given target classes
	 * @param fields
	 */
	public void defineFields(Q targetClasses){
		defineFields(targetClasses.nodes(XCSG.Java.Class).eval().nodes());
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
		replaceFields(fields.nodes(XCSG.Field).eval().nodes());
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
		defineMethods(targetClasses.nodes(XCSG.Java.Class).eval().nodes());
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
		replaceMethods(methods.nodes(XCSG.Method).eval().nodes());
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
		mergeMethods(methods.nodes(XCSG.Method).eval().nodes());
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
		addPreExecutionMethodHooks(methods.nodes(XCSG.Method).eval().nodes());
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
		addPostExecutionMethodHooks(methods.nodes(XCSG.Method).eval().nodes());
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
	
	/**
	 * Just some really weak input sanitization to catch potentially common mistakes
	 * @param packageName
	 */
	private void checkPackageName(String packageName) {
		if(packageName.contains("/") 
			|| packageName.contains("\\") 
			|| packageName.contains(" ") 
			|| packageName.endsWith(".")){
			throw new IllegalArgumentException("Invalid package name.");
		}
	}
	
	/**
	 * Creates the new source file in the project
	 * @param packageName
	 * @param className
	 * @param javaFile
	 * @throws IOException
	 * @throws CoreException
	 */
	private void writeSourceFile(String packageName, String className, JavaFile javaFile) throws IOException, CoreException {
		// figure out where to put the source file
		String relativePackageDirectory = packageName.replace(".", "/");
		File sourceFile = new File(project.getProject().getFolder("/src/" + relativePackageDirectory).getLocation().toFile().getAbsolutePath() 
								+ File.separator + className +  ".java");
		
		// make the package directory if its not there already
		sourceFile.getParentFile().mkdirs();
		
		// write source file out to src folder
		FileWriter fileWriter = new FileWriter(sourceFile);
		javaFile.writeTo(fileWriter);
		fileWriter.close();
		
		// refresh the project
		refresh();
	}
}
