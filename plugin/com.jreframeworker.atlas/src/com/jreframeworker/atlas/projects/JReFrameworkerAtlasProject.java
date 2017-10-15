package com.jreframeworker.atlas.projects;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.xml.sax.SAXException;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.log.Log;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.jreframeworker.annotations.methods.MergeMethod;
import com.jreframeworker.annotations.types.DefineType;
import com.jreframeworker.annotations.types.DefineTypeFinality;
import com.jreframeworker.annotations.types.DefineTypeVisibility;
import com.jreframeworker.annotations.types.MergeType;
import com.jreframeworker.atlas.analysis.ClassAnalysis;
import com.jreframeworker.atlas.analysis.MethodAnalysis;
import com.jreframeworker.atlas.analysis.MethodAnalysis.Return;
import com.jreframeworker.common.RuntimeUtils;
import com.jreframeworker.core.JReFrameworkerProject;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

public class JReFrameworkerAtlasProject {

	@SuppressWarnings("rawtypes")
	private Class searchClasspathForClass(String className) throws SAXException, IOException, ParserConfigurationException, JavaModelException {
		ArrayList<ClassLoader> classLoaders = new ArrayList<ClassLoader>();
		classLoaders.add(getClass().getClassLoader());
		for (String targetJar : listTargets()) {
			File originalJar = RuntimeUtils.getClasspathJar(targetJar, project);
			URL[] jarURL = { new URL("jar:file:" + originalJar.getCanonicalPath() + "!/") };
			classLoaders.add(URLClassLoader.newInstance(jarURL));
		}
		for(ClassLoader classLoader : classLoaders){
			try{
				return classLoader.loadClass(className);
			} catch (Exception e){
				continue;
			}
		}
		throw new RuntimeException("Class not found");
	}
	
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
	public Set<File> defineType(String sourcePackageName, String sourceClassName, String javadoc) {
		Set<File> sourceFiles = new HashSet<File>();
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
	
			sourceFiles.add(writeSourceFile(sourcePackageName, sourceClassName, javaFile));
		} catch (Throwable t){
			Log.error("Error creating define type logic", t);
		}
		
		return sourceFiles;
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
	public Set<File> defineType(String sourcePackageName, String sourceClassName) {
		String javadoc = "TODO: Implement class body"
				  + "\n\nThe entire contents of this class's bytecode will"
				  + "\nbe injected into the target's \"" + sourcePackageName + "\" package.\n";
		return defineType(sourcePackageName, sourceClassName, javadoc);
	}
	
	/**
	 * Creates logic to replace a class in the given class target
	 * @param targetClass
	 */
	public Set<File> replaceType(String sourcePackageName, String sourceClassName){
		String javadoc = "TODO: Implement class body"
				  + "\n\nThe entire contents of this class's bytecode will"
				  + "\nbe used to replace " + sourcePackageName + "." + sourceClassName + " in the target.\n";
		return defineType(sourcePackageName, sourceClassName, javadoc);
	}
	
	/**
	 * Creates logic to replace a class in the given class targets
	 * @param targetClass
	 */
	public Set<File> replaceTypes(Q targetClasses){
		return replaceTypes(targetClasses.nodes(XCSG.Java.Class).eval().nodes());
	}
	
	/**
	 * Creates logic to replace a class in the given class targets
	 * @param targetClass
	 */
	public Set<File> replaceTypes(AtlasSet<Node> targetClasses){
		Set<File> sourceFiles = new HashSet<File>();
		for(Node targetClass : targetClasses){
			sourceFiles.addAll(replaceType(targetClass));
		}
		return sourceFiles;
	}
	
	/**
	 * Creates logic to replace a class in the given class target
	 * @param targetClass
	 */
	public Set<File> replaceType(Node targetClass){
		Set<File> sourceFiles = new HashSet<File>();
		if(targetClass.taggedWith(XCSG.Java.Class)){
			String sourceClassName = ClassAnalysis.getName(targetClass);
			String sourcePackageName = ClassAnalysis.getPackage(targetClass);
			sourceFiles.addAll(replaceType(sourcePackageName, sourceClassName));
		}
		return sourceFiles;
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
	public Set<File> setTypeFinality(String sourcePackageName, String sourceClassName, String targetClassPackageName, String targetClassName, boolean finality){
		Set<File> sourceFiles = new HashSet<File>();
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
	
			sourceFiles.add(writeSourceFile(sourcePackageName, sourceClassName, javaFile));
		} catch (Throwable t){
			Log.error("Error creating define type finality logic", t);
		}
		
		return sourceFiles;
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
	public Set<File> setTypeVisibility(String sourcePackageName, String sourceClassName, String targetClassPackageName, String targetClassName, String visibility){
		Set<File> sourceFiles = new HashSet<File>();
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
	
			sourceFiles.add(writeSourceFile(sourcePackageName, sourceClassName, javaFile));
		} catch (Throwable t){
			Log.error("Error creating define type visibility logic", t);
		}
		return sourceFiles;
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
	public Set<File> setMethodFinality(String sourceClassPackageName, String sourceClassName, String targetClassPackageName, String targetClassName, Node targetMethod, boolean finality) {
		// TODO: implement
		return new HashSet<File>();
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
	public Set<File> setMethodVisibility(String sourceClassPackageName, String sourceClassName, String targetClassPackageName, String targetClassName, Node targetMethod, String visibility) {
		// TODO: implement
		return new HashSet<File>();
	}

	/**
	 * Creates logic to merge code into the given class target
	 * Note: Does not consider prebuild options
	 * @param targetClass
	 */
	public Set<File> mergeType(String sourcePackageName, String sourceClassName, String targetClassPackageName, String targetClassName){
		Set<File> sourceFiles = new HashSet<File>();
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

			TypeSpec type = TypeSpec.classBuilder(sourceClassName).superclass(searchClasspathForClass(qualifiedTargetClass))
				    .addModifiers(Modifier.PUBLIC)
				    .addAnnotation(MergeType.class)
				    .addJavadoc(javadoc)
				    .build();
			
			JavaFile javaFile = JavaFile.builder(sourcePackageName, type)
					.build();
	
			sourceFiles.add(writeSourceFile(sourcePackageName, sourceClassName, javaFile));
		} catch (Throwable t){
			Log.error("Error creating merge type logic", t);
		}
		return sourceFiles;
	}
	
	/**
	 * Creates logic to merge code into the given class target
	 * @param targetClass
	 */
	public Set<File> mergeType(Node targetClass){
		Set<File> sourceFiles = new HashSet<File>();
		if(targetClass.taggedWith(XCSG.Java.Class)){
			String targetClassPackageName = ClassAnalysis.getPackage(targetClass);
			String targetClassName = ClassAnalysis.getName(targetClass);
			if(ClassAnalysis.isFinal(targetClass)){
				sourceFiles.addAll(setTypeFinality(targetClassPackageName, (targetClassName + "FinalityPrebuild"), targetClassPackageName, targetClassName, false));
			}
			if(ClassAnalysis.isFinal(targetClass)){
				sourceFiles.addAll(setTypeVisibility(targetClassPackageName, (targetClassName + "VisibilityPrebuild"), targetClassPackageName, targetClassName, "public"));
			}
			sourceFiles.addAll(mergeType(targetClassPackageName, "Merge" + targetClassName , targetClassPackageName, targetClassName));
		}
		return sourceFiles;
	}
	
	/**
	 * Creates logic to replace a class in the given class targets
	 * @param targetClass
	 */
	public Set<File> mergeTypes(Q targetClasses){
		return mergeTypes(targetClasses.nodes(XCSG.Java.Class).eval().nodes());
	}
	
	/**
	 * Creates logic to replace a class in the given class targets
	 * @param targetClass
	 */
	public Set<File> mergeTypes(AtlasSet<Node> targetClasses){
		Set<File> sourceFiles = new HashSet<File>();
		for(Node targetClass : targetClasses){
			sourceFiles.addAll(mergeType(targetClass));
		}
		return sourceFiles;
	}
	
	/**
	 * Creates logic to define a new field in the given target classes
	 * @param fields
	 */
	public Set<File> defineFields(Q targetClasses){
		return defineFields(targetClasses.nodes(XCSG.Java.Class).eval().nodes());
	}
	
	/**
	 * Creates logic to define a new field in the given target classes
	 * @param field
	 */
	public Set<File> defineFields(AtlasSet<Node> targetClasses){
		Set<File> sourceFiles = new HashSet<File>();
		for(Node field : targetClasses){
			sourceFiles.addAll(defineField(field));
		}
		return sourceFiles;
	}
	
	/**
	 * Creates logic to define a new field in the given target class
	 * @param field
	 */
	public Set<File> defineField(Node targetClass){
		Set<File> sourceFiles = new HashSet<File>();
		if(targetClass.taggedWith(XCSG.Java.Class)){
			// TODO: implement
		}
		return sourceFiles;
	}
	
	/**
	 * Creates logic to replace code in the given fields
	 * @param fields
	 */
	public Set<File> replaceFields(Q fields){
		return replaceFields(fields.nodes(XCSG.Field).eval().nodes());
	}
	
	/**
	 * Creates logic to replace code in the given fields
	 * @param fields
	 */
	public Set<File> replaceFields(AtlasSet<Node> fields){
		Set<File> sourceFiles = new HashSet<File>();
		for(Node field : fields){
			sourceFiles.addAll(replaceField(field));
		}
		return sourceFiles;
	}
	
	/**
	 * Creates logic to replace code in the given field
	 * @param field
	 */
	public Set<File> replaceField(Node field){
		Set<File> sourceFiles = new HashSet<File>();
		if(field.taggedWith(XCSG.Field)){
			// TODO: implement
		}
		return sourceFiles;
	}
	
	/**
	 * Creates logic to define a new method in the given target classes
	 * @param methods
	 */
	public Set<File> defineMethods(Q targetClasses){
		return defineMethods(targetClasses.nodes(XCSG.Java.Class).eval().nodes());
	}
	
	/**
	 * Creates logic to define a new method in the given target classes
	 * @param method
	 */
	public Set<File> defineMethods(AtlasSet<Node> targetClasses){
		Set<File> sourceFiles = new HashSet<File>();
		for(Node method : targetClasses){
			sourceFiles.addAll(defineMethod(method));
		}
		return sourceFiles;
	}
	
	/**
	 * Creates logic to define a new method in the given target class
	 * @param method
	 */
	public Set<File> defineMethod(Node targetClass){
		Set<File> sourceFiles = new HashSet<File>();
		if(targetClass.taggedWith(XCSG.Java.Class)){
			// TODO: implement
		}
		return sourceFiles;
	}
	
	/**
	 * Creates logic to replace code in the given methods
	 * @param methods
	 */
	public Set<File> replaceMethods(Q methods){
		return replaceMethods(methods.nodes(XCSG.Method).eval().nodes());
	}
	
	/**
	 * Creates logic to replace code in the given methods
	 * @param methods
	 */
	public Set<File> replaceMethods(AtlasSet<Node> methods){
		Set<File> sourceFiles = new HashSet<File>();
		for(Node method : methods){
			sourceFiles.addAll(replaceMethod(method));
		}
		return sourceFiles;
	}
	
	/**
	 * Creates logic to replace code in the given method
	 * @param method
	 */
	public Set<File> replaceMethod(Node method){
		Set<File> sourceFiles = new HashSet<File>();
		if(method.taggedWith(XCSG.Method)){
			// TODO: implement
		}
		return sourceFiles;
	}
	
	
	public Set<File> mergeMethod(String sourcePackageName, String sourceClassName, Node targetMethod){
		Set<File> sourceFiles = new HashSet<File>();
		Node targetClass = MethodAnalysis.getOwnerClass(targetMethod);
		sourcePackageName = sourcePackageName.trim();
		checkPackageName(sourcePackageName);
		if(targetClass.taggedWith(XCSG.Java.Class)){
			String targetClassPackageName = ClassAnalysis.getPackage(targetClass);
			String targetClassName = ClassAnalysis.getName(targetClass);
			if(ClassAnalysis.isFinal(targetClass)){
				sourceFiles.addAll(setTypeFinality(targetClassPackageName, (targetClassName + "TypeFinalityPrebuild"), targetClassPackageName, targetClassName, false));
			}
			if(!ClassAnalysis.isPublic(targetClass)){
				sourceFiles.addAll(setTypeVisibility(targetClassPackageName, (targetClassName + "TypeVisibilityPrebuild"), targetClassPackageName, targetClassName, "public"));
			}
			
			String methodName = MethodAnalysis.getName(targetMethod);
			String capitalizedMethodName = methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
			if(MethodAnalysis.isFinal(targetMethod)){
				sourceFiles.addAll(setMethodFinality(targetClassPackageName, (targetClassName + capitalizedMethodName + "MethodFinalityPrebuild"), targetClassPackageName, targetClassName, targetMethod, false));
			}
			if(!ClassAnalysis.isPublic(targetMethod)){
				sourceFiles.addAll(setMethodVisibility(targetClassPackageName, (targetClassName + capitalizedMethodName + "MethodVisibilityPrebuild"), targetClassPackageName, targetClassName, targetMethod, "public"));
			}
			
			try {
				String qualifiedTargetClass = targetClassPackageName + "." + targetClassName;

				// figure out the merge method modifiers
				ArrayList<Modifier> modifierSet = new ArrayList<Modifier>();
				modifierSet.add(Modifier.PUBLIC); // it will be public after it gets set as public...
				if (MethodAnalysis.isStatic(targetMethod)) {
					modifierSet.add(Modifier.STATIC);
				}
				Modifier[] modifiers = new Modifier[modifierSet.size()];
				modifierSet.toArray(modifiers);

				List<ParameterSpec> parameters = new LinkedList<ParameterSpec>();
				for (MethodAnalysis.Parameter parameter : MethodAnalysis.getParameters(targetMethod)) {
					@SuppressWarnings("rawtypes")
					Class parameterClassType;
					if (parameter.isPrimitive()) {
						parameterClassType = parameter.getPrimitive();
					} else {
						parameterClassType = searchClasspathForClass(parameter.getType());
						if (parameter.isArray()) {
							// TODO: how to consider dimension
							ArrayTypeName array = ArrayTypeName.of(parameterClassType);
							parameters.add(ParameterSpec.builder(array, parameter.getName(), parameter.getModifiers())
									.build());
						} else {
							parameters.add(ParameterSpec
									.builder(parameterClassType, parameter.getName(), parameter.getModifiers())
									.build());
						}
					}
				}

				Return ret = MethodAnalysis.getReturnType(targetMethod);
				TypeName returnTypeName;
				@SuppressWarnings("rawtypes")
				Class returnClassType;
				if (ret.isPrimitive()) {
					returnClassType = ret.getPrimitive();
				} else {
					returnClassType = searchClasspathForClass(ret.getType());
				}
				if (ret.isArray()) {
					// TODO: how to consider dimension
					returnTypeName = ArrayTypeName.of(returnClassType); 
				} else {
					returnTypeName = TypeName.get(returnClassType);
				}

				MethodSpec method;
				if (modifierSet.contains(Modifier.STATIC)) {
					method = MethodSpec.methodBuilder(methodName).addModifiers(modifiers).returns(returnTypeName)
							.addParameters(parameters).addAnnotation(MergeMethod.class)
							.addJavadoc("Use " + targetClassName + "." + methodName
									+ " to access the preserved original " + methodName + " implementation.\n" + "Use "
									+ sourceClassName + "." + methodName + " to access this modified " + methodName
									+ " implementation.\n")
							.addComment("TODO: Implement").build();
				} else {
					// only add override annotation if the target method is not static
					method = MethodSpec.methodBuilder(methodName).addModifiers(modifiers).returns(returnTypeName)
							.addParameters(parameters).addAnnotation(MergeMethod.class).addAnnotation(Override.class)
							.addJavadoc("Use super." + methodName + " to access the preserved original " + methodName
									+ " implementation.\n" + "Use this." + methodName + " to access this modified "
									+ methodName + " implementation.\n")
							.addComment("TODO: Implement").build();
				}

				// TODO: consider using addStatement to add a return
				// statement of the original implementation result if not
				// void return

				TypeSpec type = TypeSpec.classBuilder(sourceClassName)
						.superclass(searchClasspathForClass(qualifiedTargetClass)).addModifiers(Modifier.PUBLIC)
						.addAnnotation(MergeType.class).addMethod(method).build();

				JavaFile javaFile = JavaFile.builder(sourcePackageName, type).build();

				sourceFiles.add(writeSourceFile(sourcePackageName, sourceClassName, javaFile));
			} catch (Throwable t) {
				Log.error("Error creating merge method logic", t);
			}
		}
		return sourceFiles;
	}

	/**
	 * Creates logic to preserve and replace accessible code in the given methods
	 * @param methods
	 */
	public Set<File> mergeMethods(Q methods){
		return mergeMethods(methods.nodes(XCSG.Method).eval().nodes());
	}
	
	/**
	 * Creates logic to preserve and replace accessible code in the given methods
	 * @param method
	 */
	public Set<File> mergeMethods(AtlasSet<Node> methods){
		Set<File> sourceFiles = new HashSet<File>();
		for(Node method : methods){
			sourceFiles.addAll(mergeMethod(method));
		}
		return sourceFiles;
	}
	
	/**
	 * Creates logic to preserve and replace accessible code in the given method
	 * @param method
	 */
	public Set<File> mergeMethod(Node method){
		Set<File> sourceFiles = new HashSet<File>();
		if(method.taggedWith(XCSG.Method)){
			Node targetClass = MethodAnalysis.getOwnerClass(method);
			String packageName = ClassAnalysis.getPackage(targetClass);
			String className = ClassAnalysis.getName(targetClass);
			sourceFiles.addAll(mergeMethod(packageName, ("MergeMethod" + className), method));
		}
		return sourceFiles;
	}
	
	/**
	 * Creates logic to inject code before the given methods are executed
	 * @param method
	 */
	public Set<File> addPreExecutionMethodHooks(Q methods){
		return addPreExecutionMethodHooks(methods.nodes(XCSG.Method).eval().nodes());
	}
	
	/**
	 * Creates logic to inject code before the given methods are executed
	 * @param method
	 */
	public Set<File> addPreExecutionMethodHooks(AtlasSet<Node> methods){
		Set<File> sourceFiles = new HashSet<File>();
		for(Node method : methods){
			sourceFiles.addAll(addPreExecutionMethodHook(method));
		}
		return sourceFiles;
	}
	
	/**
	 * Creates logic to inject code before the given method is executed
	 * @param method
	 */
	public Set<File> addPreExecutionMethodHook(Node method){
		Set<File> sourceFiles = new HashSet<File>();
		if(method.taggedWith(XCSG.Method)){
			// TODO: implement
		}
		return sourceFiles;
	}
	
	/**
	 * Creates logic to inject code after the given methods are executed
	 * @param method
	 */
	public Set<File> addPostExecutionMethodHooks(Q methods){
		return addPostExecutionMethodHooks(methods.nodes(XCSG.Method).eval().nodes());
	}
	
	/**
	 * Creates logic to inject code after the given methods are executed
	 * @param method
	 */
	public Set<File> addPostExecutionMethodHooks(AtlasSet<Node> methods){
		Set<File> sourceFiles = new HashSet<File>();
		for(Node method : methods){
			sourceFiles.addAll(addPostExecutionMethodHook(method));
		}
		return sourceFiles;
	}
	
	/**
	 * Creates logic to inject code after the given method is executed
	 * @param method
	 */
	public Set<File> addPostExecutionMethodHook(Node method){
		Set<File> sourceFiles = new HashSet<File>();
		if(method.taggedWith(XCSG.Method)){
			// TODO: implement
		}
		return sourceFiles;
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
	private File writeSourceFile(String sourcePackageName, String sourceClassName, JavaFile javaFile) throws IOException, CoreException {
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
		
		return sourceFile;
	}
}
