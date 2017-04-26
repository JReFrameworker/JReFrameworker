package jreframeworker.atlas.projects;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import jreframeworker.annotations.methods.MergeMethod;
import jreframeworker.annotations.types.DefineType;
import jreframeworker.annotations.types.DefineTypeFinality;
import jreframeworker.annotations.types.DefineTypeVisibility;
import jreframeworker.annotations.types.MergeType;
import jreframeworker.atlas.analysis.ClassAnalysis;
import jreframeworker.atlas.analysis.MethodAnalysis;
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
	 * @param sourcePackageName
	 * @param sourceClassName
	 * @throws IOException  
	 * @throws CoreException 
	 */
	public void defineType(String sourcePackageName, String sourceClassName, String javadoc) {
		sourcePackageName = sourcePackageName.trim();
		checkPackageName(sourcePackageName);
		
		try {
			TypeSpec type = TypeSpec.classBuilder(sourceClassName)
				    .addModifiers(Modifier.PUBLIC)
				    .addAnnotation(DefineType.class)
				    .addJavadoc(javadoc)
				    .build();
			
			JavaFile javaFile = JavaFile.builder(sourcePackageName, type)
					.build();
	
			writeSourceFile(sourcePackageName, sourceClassName, javaFile);
		} catch (Throwable t){
			Log.error("Error creating define type logic", t);
		}
	}
	
	/**
	 * Creates logic to define a new class
	 * 
	 * Example: defineType("com.test", "HelloWorld")
	 * @param packageName
	 * @param sourceClassName
	 * @throws IOException  
	 * @throws CoreException 
	 */
	public void defineType(String sourcePackageName, String sourceClassName) {
		String javadoc = "TODO: Implement class body"
				  + "\n\nThe entire contents of this class's bytecode will"
				  + "\nbe injected into the target's \"" + sourcePackageName + "\" package.\n";
		defineType(sourcePackageName, sourceClassName, javadoc);
	}
	
	/**
	 * Creates logic to replace a class in the given class target
	 * @param targetClass
	 */
	public void replaceType(String sourcePackageName, String sourceClassName){
		String javadoc = "TODO: Implement class body"
				  + "\n\nThe entire contents of this class's bytecode will"
				  + "\nbe used to replace " + sourcePackageName + "." + sourceClassName + " in the target.\n";
		defineType(sourcePackageName, sourceClassName, javadoc);
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
			String sourceClassName = ClassAnalysis.getName(targetClass);
			String sourcePackageName = ClassAnalysis.getPackage(targetClass);
			replaceType(sourcePackageName, sourceClassName);
		}
	}
	
	/**
	 * Creates a new class with that contains a DefineTypeFinality annotation
	 * set to the type specified by the given sourcePackageName and sourceClassName values
	 * with the given finality
	 * 
	 * @param sourcePackageName
	 * @param sourceClassName
	 * @param finality
	 */
	public void setTypeFinality(String sourcePackageName, String sourceClassName, String targetClassPackageName, String targetClassName, boolean finality){
		sourcePackageName = sourcePackageName.trim();
		checkPackageName(sourcePackageName);
		checkPackageName(targetClassPackageName);
		try {
			String javadoc = "Runs as a first pass to set type finality.\n";
			
			TypeSpec type = TypeSpec.classBuilder(sourceClassName)
				    .addModifiers(Modifier.PUBLIC)
				    .addAnnotation(AnnotationSpec.builder(DefineTypeFinality.class)
		                    .addMember("type", ("\"" + targetClassPackageName + "." + targetClassName + "\""))
		                    .addMember("finality", new Boolean(finality).toString())
		                    .build())
				    .addJavadoc(javadoc)
				    .build();
			
			JavaFile javaFile = JavaFile.builder(sourcePackageName, type)
					.build();
	
			writeSourceFile(sourcePackageName, sourceClassName, javaFile);
		} catch (Throwable t){
			Log.error("Error creating define type finality logic", t);
		}
	}
	
	/**
	 * Creates a new class with that contains a DefineTypeVisibility annotation
	 * set to the type specified by the given sourcePackageName and sourceClassName values
	 * with the given finality
	 * 
	 * @param sourcePackageName
	 * @param sourceClassName
	 * @param finality
	 */
	public void setTypeVisibility(String sourcePackageName, String sourceClassName, String targetClassPackageName, String targetClassName, String visibility){
		sourcePackageName = sourcePackageName.trim();
		checkPackageName(sourcePackageName);
		checkPackageName(targetClassPackageName);
		try {
			String javadoc = "Runs as a first pass to set type visibility.\n";
			
			TypeSpec type = TypeSpec.classBuilder(sourceClassName)
				    .addModifiers(Modifier.PUBLIC)
				    .addAnnotation(AnnotationSpec.builder(DefineTypeVisibility.class)
		                    .addMember("type", ("\"" + targetClassPackageName + "." + targetClassName + "\""))
		                    .addMember("visibility", ("\"" + visibility + "\""))
		                    .build())
				    .addJavadoc(javadoc)
				    .build();
			
			JavaFile javaFile = JavaFile.builder(sourcePackageName, type)
					.build();
	
			writeSourceFile(sourcePackageName, sourceClassName, javaFile);
		} catch (Throwable t){
			Log.error("Error creating define type visibility logic", t);
		}
	}
	
	/**
	 * Creates a new class with that contains a DefineMethodFinality annotation
	 * set to the type specified by the given sourcePackageName and sourceClassName values
	 * with the given target method overridden and finality set
	 * 
	 * @param sourceClassPackageName
	 * @param sourceClassName
	 * @param targetClassPackageName
	 * @param targetClassName
	 * @param targetClassName
	 * @param finality
	 */
	public void setMethodFinality(String sourceClassPackageName, String sourceClassName, String targetClassPackageName, String targetClassName, Node targetMethod, boolean finality) {
		// TODO: implement
	}
	
	/**
	 * Creates a new class with that contains a DefineMethodVisibility annotation
	 * set to the type specified by the given sourcePackageName and sourceClassName values
	 * with the given target method overridden and visibility set
	 * 
	 * @param sourceClassPackageName
	 * @param sourceClassName
	 * @param targetClassPackageName
	 * @param targetClassName
	 * @param targetClassName
	 * @param visibility
	 */
	public void setMethodVisibility(String sourceClassPackageName, String sourceClassName, String targetClassPackageName, String targetClassName, Node targetMethod, String visibility) {
		// TODO: implement
	}

	/**
	 * Creates logic to merge code into the given class target
	 * Note: Does not consider prebuild options
	 * @param targetClass
	 */
	public void mergeType(String sourcePackageName, String sourceClassName, String targetClassPackageName, String targetClassName){
		sourcePackageName = sourcePackageName.trim();
		checkPackageName(sourcePackageName);
		
		try {
			String qualifiedTargetClass = targetClassPackageName + "." + targetClassName;
			
			String javadoc = "TODO: Implement class body"
					  + "\n\nThe contents of this class's bytecode will be used to define, replace, and/or"
					  + "\nmerge (preserve and replace) with " + sourcePackageName + "." + sourceClassName + " in the target.\n"
					  + "\nUse the @DefineField and @DefineMethod annotations to insert or replace\n"
					  + "\nfields and methods. Use the @MergeMethod annotation to preserve and hide the\n"
					  + "\noriginal method and replace the accessible method.\n";

			TypeSpec type = TypeSpec.classBuilder(sourceClassName).superclass(Class.forName(qualifiedTargetClass))
				    .addModifiers(Modifier.PUBLIC)
				    .addAnnotation(MergeType.class)
				    .addJavadoc(javadoc)
				    .build();
			
			JavaFile javaFile = JavaFile.builder(sourcePackageName, type)
					.build();
	
			writeSourceFile(sourcePackageName, sourceClassName, javaFile);
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
			String targetClassPackageName = ClassAnalysis.getPackage(targetClass);
			String targetClassName = ClassAnalysis.getName(targetClass);
			if(ClassAnalysis.isFinal(targetClass)){
				setTypeFinality(targetClassPackageName, (targetClassName + "FinalityPrebuild"), targetClassPackageName, targetClassName, false);
			}
			if(ClassAnalysis.isFinal(targetClass)){
				setTypeVisibility(targetClassPackageName, (targetClassName + "VisibilityPrebuild"), targetClassPackageName, targetClassName, "public");
			}
			mergeType(targetClassPackageName, "Merge" + targetClassName , targetClassPackageName, targetClassName);
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
	
	
	public void mergeMethod(String sourcePackageName, String sourceClassName, Node targetMethod){
		Node targetClass = MethodAnalysis.getOwnerClass(targetMethod);
		sourcePackageName = sourcePackageName.trim();
		checkPackageName(sourcePackageName);
		if(targetClass.taggedWith(XCSG.Java.Class)){
			String targetClassPackageName = ClassAnalysis.getPackage(targetClass);
			String targetClassName = ClassAnalysis.getName(targetClass);
			if(ClassAnalysis.isFinal(targetClass)){
				setTypeFinality(targetClassPackageName, (targetClassName + "TypeFinalityPrebuild"), targetClassPackageName, targetClassName, false);
			}
			if(!ClassAnalysis.isPublic(targetClass)){
				setTypeVisibility(targetClassPackageName, (targetClassName + "TypeVisibilityPrebuild"), targetClassPackageName, targetClassName, "public");
			}
			
			String methodName = MethodAnalysis.getName(targetMethod);
			String capitalizedMethodName = methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
			if(MethodAnalysis.isFinal(targetMethod)){
				setMethodFinality(targetClassPackageName, (targetClassName + capitalizedMethodName + "MethodFinalityPrebuild"), targetClassPackageName, targetClassName, targetMethod, false);
			}
			if(!ClassAnalysis.isPublic(targetMethod)){
				setMethodVisibility(targetClassPackageName, (targetClassName + capitalizedMethodName + "MethodVisibilityPrebuild"), targetClassPackageName, targetClassName, targetMethod, "public");
			}
			
			try {
				String qualifiedTargetClass = targetClassPackageName + "." + targetClassName;

				// figure out the merge method modifiers
				ArrayList<Modifier> modifierSet = new ArrayList<Modifier>();
				modifierSet.add(Modifier.PUBLIC); // it will be public after it gets set as public...
				if(MethodAnalysis.isStatic(targetMethod)){
					modifierSet.add(Modifier.STATIC);
				}
				Modifier[] modifiers = new Modifier[modifierSet.size()];
				modifierSet.toArray(modifiers);
				
				List<ParameterSpec> parameters = new LinkedList<ParameterSpec>();
				for(MethodAnalysis.Parameter parameter : MethodAnalysis.getParameters(targetMethod)){
					parameters.add(ParameterSpec.builder(parameter.getType(), parameter.getName(), parameter.getModifiers()).build());
				}
				
				@SuppressWarnings("rawtypes")
				Class returnType = MethodAnalysis.getReturnType(targetMethod);
				
				MethodSpec method;
				if(modifierSet.contains(Modifier.STATIC)){
					method = MethodSpec.methodBuilder(methodName)
						    .addModifiers(modifiers)
						    .returns(returnType)
						    .addParameters(parameters)
						    .addAnnotation(MergeMethod.class)
						    .addJavadoc("Use " + targetClassName + "." + methodName + " to access the preserved original " + methodName + " implementation.\n"
						    		  + "Use " + sourceClassName + "." + methodName + " to access this modified " + methodName + " implementation.\n")
						    .addComment("TODO: Implement")
						    .build();
				} else {
					method = MethodSpec.methodBuilder(methodName)
						    .addModifiers(modifiers)
						    .returns(returnType)
						    .addParameters(parameters)
						    .addAnnotation(MergeMethod.class)
						    .addAnnotation(Override.class) // only add override annotation if the target method is not static
						    .addJavadoc("Use super." + methodName + " to access the preserved original " + methodName + " implementation.\n"
						    		  + "Use this." + methodName + " to access this modified " + methodName + " implementation.\n")
						    .addComment("TODO: Implement")
						    .build();
				}
				
				// TODO: consider using addStatement to add a return statement of the original implementation result if not void return
				
				TypeSpec type = TypeSpec.classBuilder(sourceClassName).superclass(Class.forName(qualifiedTargetClass))
					    .addModifiers(Modifier.PUBLIC)
					    .addAnnotation(MergeType.class)
					    .addMethod(method)
					    .build();
				
				JavaFile javaFile = JavaFile.builder(sourcePackageName, type)
						.build();
		
				writeSourceFile(sourcePackageName, sourceClassName, javaFile);
			} catch (Throwable t){
				Log.error("Error creating merge method logic", t);
			}
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
	 * @param sourceClassName
	 * @param javaFile
	 * @throws IOException
	 * @throws CoreException
	 */
	private void writeSourceFile(String sourcePackageName, String sourceClassName, JavaFile javaFile) throws IOException, CoreException {
		// figure out where to put the source file
		String relativePackageDirectory = sourcePackageName.replace(".", "/");
		File sourceFile = new File(project.getProject().getFolder("/src/" + relativePackageDirectory).getLocation().toFile().getAbsolutePath() 
								+ File.separator + sourceClassName +  ".java");
		
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
