package jreframeworker.engine;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarException;

import org.objectweb.asm.ClassLoaders;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

import jreframeworker.engine.identifiers.BaseMethodsIdentifier;
import jreframeworker.engine.identifiers.DefineFinalityIdentifier;
import jreframeworker.engine.identifiers.DefineFinalityIdentifier.DefineFieldFinalityAnnotation;
import jreframeworker.engine.identifiers.DefineFinalityIdentifier.DefineMethodFinalityAnnotation;
import jreframeworker.engine.identifiers.DefineFinalityIdentifier.DefineTypeFinalityAnnotation;
import jreframeworker.engine.identifiers.DefineIdentifier;
import jreframeworker.engine.identifiers.DefineIdentifier.DefineMethodAnnotation;
import jreframeworker.engine.identifiers.DefineVisibilityIdentifier;
import jreframeworker.engine.identifiers.DefineVisibilityIdentifier.DefineFieldVisibilityAnnotation;
import jreframeworker.engine.identifiers.DefineVisibilityIdentifier.DefineMethodVisibilityAnnotation;
import jreframeworker.engine.identifiers.DefineVisibilityIdentifier.DefineTypeVisibilityAnnotation;
import jreframeworker.engine.identifiers.DefineVisibilityIdentifier.Visibility;
import jreframeworker.engine.identifiers.JREFAnnotationIdentifier;
import jreframeworker.engine.identifiers.MergeIdentifier;
import jreframeworker.engine.identifiers.MergeIdentifier.MergeMethodAnnotation;
import jreframeworker.engine.identifiers.MergeIdentifier.MergeTypeAnnotation;
import jreframeworker.engine.identifiers.PurgeIdentifier;
import jreframeworker.engine.identifiers.PurgeIdentifier.PurgeFieldAnnotation;
import jreframeworker.engine.identifiers.PurgeIdentifier.PurgeMethodAnnotation;
import jreframeworker.engine.identifiers.PurgeIdentifier.PurgeTypeAnnotation;
import jreframeworker.engine.log.Log;
import jreframeworker.engine.utils.AnnotationUtils;
import jreframeworker.engine.utils.BytecodeUtils;
import jreframeworker.engine.utils.JarModifier;

public class Engine {

	private String jarName;
	private Set<String> originalEntries;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jarName == null) ? 0 : jarName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Engine other = (Engine) obj;
		if (jarName == null) {
			if (other.jarName != null)
				return false;
		} else if (!jarName.equals(other.jarName))
			return false;
		return true;
	}

	private String mergeRenamePrefix;
	private JarModifier jarModifier;
	private ClassLoader[] classLoaders = new ClassLoader[]{ getClass().getClassLoader() };
	
	private static class Bytecode {
		private byte[] bytecode;
		
		public Bytecode(byte[] bytecode){
			this.bytecode = bytecode;
		}
		
		public byte[] getBytecode(){
			return bytecode;
		}
	}
	
	private HashMap<String,Bytecode> bytecodeCache = new HashMap<String,Bytecode>();
	private Set<String> purgedEntries = new HashSet<String>();

	public String getJarName(){
		return jarName;
	}
	
	public File getOriginalJar(){
		return jarModifier.getJarFile();
	}
	
	public Set<String> getOriginalEntries(){
		return new HashSet<String>(originalEntries);
	}
	
	public Set<String> getModificationEntries(){
		return new HashSet<String>(bytecodeCache.keySet());
	}
	
	public Engine(File jar, String mergeRenamePrefix) throws JarException, IOException {
		this.mergeRenamePrefix = mergeRenamePrefix;
		this.jarModifier = new JarModifier(jar);
		this.jarName = jar.getName();
		this.originalEntries = new HashSet<String>(jarModifier.getJarEntrySet());
	}
	
	public Engine(File jar, String mergeRenamePrefix, ClassLoader[] classLoaders) throws JarException, IOException {
		this(jar, mergeRenamePrefix);
		this.classLoaders = classLoaders;
	}
	
	public void setClassLoaders(ClassLoader... classLoaders){
		this.classLoaders = classLoaders;
	}
	
	private ClassNode getBytecode(String entry) throws IOException {
		return BytecodeUtils.getClassNode(getRawBytecode(entry));
	}
	
	private byte[] getRawBytecode(String entry) throws IOException {
		if(bytecodeCache.containsKey(entry)){
			return bytecodeCache.get(entry).getBytecode();
		} else {
			String qualifiedClassFilename = entry + ".class";
			byte[] bytecode = jarModifier.extractEntry(qualifiedClassFilename);
			bytecodeCache.put(entry, new Bytecode(bytecode));
			return bytecode;
		}
	}
	
	private void purgeBytecode(String entry){
		bytecodeCache.remove(entry);
		purgedEntries.add(entry);
	}
	
	private void updateBytecode(String entry, ClassNode classNode) throws IOException {
		updateBytecode(entry, BytecodeUtils.writeClass(classNode));
	}
	
	private void updateBytecode(String entry, byte[] bytecode) throws IOException {
		bytecodeCache.put(entry, new Bytecode(bytecode));
	}
	
//	public void addUnprocessed(byte[] inputClass) throws IOException {
//		ClassNode classNode = BytecodeUtils.getClassNode(inputClass);
//		updateBytecode(classNode.name, inputClass);
//	}
	
	public void addUnprocessed(byte[] inputClass, boolean overwrite) throws IOException {
		ClassNode classNode = BytecodeUtils.getClassNode(inputClass);
		String qualifiedClassName = classNode.name + ".class";
		jarModifier.add(qualifiedClassName, inputClass, overwrite);
	}
	
	public boolean process(byte[] inputClass) throws IOException {
		
		// set the ASM class loaders to be used to process this input
		ClassLoaders.setClassLoaders(classLoaders);
		
		boolean processed = false;
		ClassNode classNode = BytecodeUtils.getClassNode(inputClass);
		Log.info("Processing input class: " + classNode.name + "...");
		
		// make requested method and field purges
		PurgeIdentifier purgeIdentifier = new PurgeIdentifier(classNode);
		processed |= purge(purgeIdentifier);
		
		// set finality
		DefineFinalityIdentifier defineFinalityIdentifier = new DefineFinalityIdentifier(classNode);
		processed |= setFinality(defineFinalityIdentifier);
		
		// set visibility modifiers
		DefineVisibilityIdentifier defineVisibilityIdentifier = new DefineVisibilityIdentifier(classNode);
		processed |= setVisibility(defineVisibilityIdentifier);
		
		// TODO: address innerclasses, classNode.innerClasses, could these even be found from class files? they would be different files...
		if(classNode.invisibleAnnotations != null){
			for(Object annotationObject : classNode.invisibleAnnotations){
				AnnotationNode annotationNode = (AnnotationNode) annotationObject;
				JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
				checker.visitAnnotation(annotationNode.desc, false);
				String qualifiedClassName = classNode.name;
				if(checker.isDefineTypeAnnotation()){
					String qualifiedClassFilename = qualifiedClassName + ".class";
					if(jarModifier.getJarEntrySet().contains(qualifiedClassFilename)){
						updateBytecode(classNode.name, inputClass);
						Log.info("Replaced: " + qualifiedClassName + " in " + jarModifier.getJarFile().getName());
					} else {
						updateBytecode(classNode.name, inputClass);
						Log.info("Inserted: " + qualifiedClassName + " into " + jarModifier.getJarFile().getName());
					}
					processed = true;
				} else if(checker.isMergeTypeAnnotation()){
					MergeIdentifier mergeIdentifier = new MergeIdentifier(classNode);
					
					MergeTypeAnnotation mergeTypeAnnotation = mergeIdentifier.getMergeTypeAnnotation();
					String qualifiedParentClassName = mergeTypeAnnotation.getSupertype();
					byte[] baseClass = getRawBytecode(qualifiedParentClassName);
					byte[] mergedClass = mergeClasses(baseClass, inputClass);
					updateBytecode(qualifiedParentClassName, mergedClass);
					Log.info("Merged: " + qualifiedClassName + " into " + qualifiedParentClassName + " in " + jarModifier.getJarFile().getName());
					processed = true;
				}
			}
		}
		
		return processed;
	}
	
	private boolean purge(PurgeIdentifier purgeIdentifier) throws IOException {
		boolean processed = false;
		// purge types
		for(PurgeTypeAnnotation purgeTypeAnnotation : purgeIdentifier.getPurgeTypeAnnotations()){
			String className = purgeTypeAnnotation.getClassName();
			if(className.contains("$")){
				// deal with outer class references to inner class files first
				String baseClassName = className.substring(0, className.lastIndexOf("$"));
				ClassNode baseClassNode = getBytecode(baseClassName);
				List<InnerClassNode> innerClassNodesToRemove = new LinkedList<InnerClassNode>();
				for(InnerClassNode innerClassNode : baseClassNode.innerClasses){
					if(innerClassNode.name.equals(className)){
						innerClassNodesToRemove.add(innerClassNode);
					}
				}
				for(InnerClassNode innerClassNodeToRemove : innerClassNodesToRemove){
					baseClassNode.innerClasses.remove(innerClassNodeToRemove);
					Log.info("Purged " + baseClassName + " reference to " + innerClassNodeToRemove.name + " inner class.");
				}
				updateBytecode(baseClassName, BytecodeUtils.writeClass(baseClassNode));

				// deal with the inner class file directly
				String innerClassName = className;
				purgeBytecode(innerClassName);
				Log.info("Purged " + innerClassName + " inner class.");
				processed = true;
			} else {
				// simple case no inner classes
				ClassNode baseClassNode = getBytecode(className);
				if(baseClassNode != null){
					Log.info("Purged " + baseClassNode.name + " class.");
					purgeBytecode(className);
					processed = true;
				} else {
					Log.warning("Could not locate base class.", new RuntimeException("Missing base class"));
				}
			}
		}
		// purge methods
		for(PurgeMethodAnnotation purgeMethodAnnotation : purgeIdentifier.getPurgeMethodAnnotations()){
			// final is not a valid modifier for initializers so no need to consider that case
			String className = purgeMethodAnnotation.getClassName();
			ClassNode classNode = getBytecode(className);
			for (Object o : classNode.methods) {
				MethodNode methodNode = (MethodNode) o;
				if(methodNode.name.equals(purgeMethodAnnotation.getMethodName())){
					ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
					PurgeAdapter purgeAdapter = new PurgeAdapter(classWriter, methodNode);
					ClassReader purgedBaseClassReader = new ClassReader(BytecodeUtils.writeClass(classNode));
					purgedBaseClassReader.accept(purgeAdapter, ClassReader.EXPAND_FRAMES);
					byte[] purgedClassBytes = classWriter.toByteArray();
					classNode = BytecodeUtils.getClassNode(purgedClassBytes);
					updateBytecode(classNode.name, purgedClassBytes);
					processed = true;
					
					updateBytecode(className, classNode);
					processed = true;
					
					Log.info("Purged " + classNode.name + "." + methodNode.name + " method.");
					processed = true;
				}
			}
		}
		// purge fields
		for(PurgeFieldAnnotation purgeFieldAnnotation : purgeIdentifier.getPurgeFieldAnnotations()){
			String className = purgeFieldAnnotation.getClassName();
			ClassNode classNode = getBytecode(className);
			for (Object o : classNode.fields) {
				FieldNode fieldNode = (FieldNode) o;
				if(fieldNode.name.equals(purgeFieldAnnotation.getFieldName())){
					ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
					PurgeAdapter purgeAdapter = new PurgeAdapter(classWriter, fieldNode);
					ClassReader purgedBaseClassReader = new ClassReader(BytecodeUtils.writeClass(classNode));
					purgedBaseClassReader.accept(purgeAdapter, ClassReader.EXPAND_FRAMES);
					byte[] purgedClassBytes = classWriter.toByteArray();
					classNode = BytecodeUtils.getClassNode(purgedClassBytes);
					updateBytecode(classNode.name, purgedClassBytes);
					processed = true;
					
					updateBytecode(className, classNode);
					processed = true;
					
					Log.info("Purged " + classNode.name + "." + fieldNode.name + " field.");
					break; // should only be one match
				}
			}
		}
		return processed;
	}
	
	@SuppressWarnings("unused")
	private static String getAccessModifiers(int access){
		LinkedList<String> modifiers = new LinkedList<String>();
		if((Opcodes.ACC_ABSTRACT & access) == Opcodes.ACC_ABSTRACT){
			modifiers.add("abstract");
		}
		if((Opcodes.ACC_ANNOTATION & access) == Opcodes.ACC_ANNOTATION){
			modifiers.add("annotation");
		}
		if((Opcodes.ACC_BRIDGE & access) == Opcodes.ACC_BRIDGE){
			modifiers.add("bridge");
		}
		if((Opcodes.ACC_DEPRECATED & access) == Opcodes.ACC_DEPRECATED){
			modifiers.add("deprecated");
		}
		if((Opcodes.ACC_ENUM & access) == Opcodes.ACC_ENUM){
			modifiers.add("enum");
		}
		if((Opcodes.ACC_FINAL & access) == Opcodes.ACC_FINAL){
			modifiers.add("final");
		}
		if((Opcodes.ACC_INTERFACE & access) == Opcodes.ACC_INTERFACE){
			modifiers.add("interface");
		}
		if((Opcodes.ACC_MANDATED & access) == Opcodes.ACC_MANDATED){
			modifiers.add("mandated");
		}
		if((Opcodes.ACC_NATIVE & access) == Opcodes.ACC_NATIVE){
			modifiers.add("native");
		}
		if((Opcodes.ACC_PRIVATE & access) == Opcodes.ACC_PRIVATE){
			modifiers.add("private");
		}
		if((Opcodes.ACC_PROTECTED & access) == Opcodes.ACC_PROTECTED){
			modifiers.add("protected");
		}
		if((Opcodes.ACC_PUBLIC & access) == Opcodes.ACC_PUBLIC){
			modifiers.add("public");
		}
		if((Opcodes.ACC_STATIC & access) == Opcodes.ACC_STATIC){
			modifiers.add("static");
		}
		if((Opcodes.ACC_STRICT & access) == Opcodes.ACC_STRICT){
			modifiers.add("strict");
		}
		if((Opcodes.ACC_SUPER & access) == Opcodes.ACC_SUPER){
			modifiers.add("super");
		}
		if((Opcodes.ACC_SYNCHRONIZED & access) == Opcodes.ACC_SYNCHRONIZED){
			modifiers.add("synchronized");
		}
		if((Opcodes.ACC_SYNTHETIC & access) == Opcodes.ACC_SYNTHETIC){
			modifiers.add("synthetic");
		}
		if((Opcodes.ACC_TRANSIENT & access) == Opcodes.ACC_TRANSIENT){
			modifiers.add("transient");
		}
		if((Opcodes.ACC_VARARGS & access) == Opcodes.ACC_VARARGS){
			modifiers.add("varargs");
		}
		if((Opcodes.ACC_VOLATILE & access) == Opcodes.ACC_VOLATILE){
			modifiers.add("volatile");
		}
		return modifiers.toString();
	}
	
	/**
	 * Sets the access (visibility) modifiers for types, methods, and fields as defined by the annotation system
	 * @param defineVisibilityIdentifier
	 * @param runtimeModifications
	 * @throws IOException 
	 */
	private boolean setVisibility(DefineVisibilityIdentifier defineVisibilityIdentifier) throws IOException {
		boolean processed = false;
		// update types
		for(DefineTypeVisibilityAnnotation defineTypeVisibilityAnnotation : defineVisibilityIdentifier.getTargetTypes()){
			String className = defineTypeVisibilityAnnotation.getClassName();
			if(className.contains("$")){
				// deal with outer class references to inner class files first
				String baseClassName = className.substring(0, className.lastIndexOf("$"));
				ClassNode baseClassNode = getBytecode(baseClassName);
				for(InnerClassNode innerClassNode : baseClassNode.innerClasses){
					if(innerClassNode.name.equals(className)){
//						Log.info("Pre Access Modifiers: " + getAccessModifiers(innerClassNode.access));
						innerClassNode.access = innerClassNode.access & (~Opcodes.ACC_PUBLIC & ~Opcodes.ACC_PROTECTED & ~Opcodes.ACC_PRIVATE);
						if(defineTypeVisibilityAnnotation.getVisibility() == Visibility.PUBLIC){
							innerClassNode.access = innerClassNode.access | Opcodes.ACC_PUBLIC;
							Log.info("Set outer class attributes for " + innerClassNode.name + " class to be public.");
						} else if(defineTypeVisibilityAnnotation.getVisibility() == Visibility.PROTECTED){
							innerClassNode.access = innerClassNode.access | Opcodes.ACC_PROTECTED;
							Log.info("Set outer class attributes for " + innerClassNode.name + " class to be protected.");
						} else if(defineTypeVisibilityAnnotation.getVisibility() == Visibility.PRIVATE){
							innerClassNode.access = innerClassNode.access | Opcodes.ACC_PRIVATE;
							Log.info("Set outer class attributes for " + innerClassNode.name + " class to be private.");
						} else {
							// should never happen
							throw new RuntimeException("Missing visibility modifier");
						}
//						Log.info("Post Access Modifiers: " + getAccessModifiers(innerClassNode.access));
					}
				}
				updateBytecode(baseClassName, baseClassNode);
				
				// deal with the inner class file directly
				String innerClassName = className;
				baseClassNode = getBytecode(innerClassName);
				for(InnerClassNode innerClassNode : baseClassNode.innerClasses){
					if(innerClassNode.name.equals(className)){
//						Log.info("Pre Access Modifiers: " + getAccessModifiers(innerClassNode.access));
						innerClassNode.access = innerClassNode.access & (~Opcodes.ACC_PUBLIC & ~Opcodes.ACC_PROTECTED & ~Opcodes.ACC_PRIVATE);
						if(defineTypeVisibilityAnnotation.getVisibility() == Visibility.PUBLIC){
							innerClassNode.access = innerClassNode.access | Opcodes.ACC_PUBLIC;
							Log.info("Set " + innerClassNode.name + " inner class to be public.");
						} else if(defineTypeVisibilityAnnotation.getVisibility() == Visibility.PROTECTED){
							innerClassNode.access = innerClassNode.access | Opcodes.ACC_PROTECTED;
							Log.info("Set " + innerClassNode.name + " inner class to be protected.");
						} else if(defineTypeVisibilityAnnotation.getVisibility() == Visibility.PRIVATE){
							innerClassNode.access = innerClassNode.access | Opcodes.ACC_PRIVATE;
							Log.info("Set " + innerClassNode.name + " inner class to be private.");
						} else {
							// should never happen
							throw new RuntimeException("Missing visibility modifier");
						}
//						Log.info("Post Access Modifiers: " + getAccessModifiers(innerClassNode.access));
					}
				}
				updateBytecode(innerClassName, baseClassNode);
			} else {
				// simple case no inner classes
				ClassNode baseClassNode = getBytecode(className);
//				Log.info("Pre Access Modifiers: " + getAccessModifiers(baseClassNode.access));
				baseClassNode.access = baseClassNode.access & (~Opcodes.ACC_PUBLIC & ~Opcodes.ACC_PROTECTED & ~Opcodes.ACC_PRIVATE);
				if(defineTypeVisibilityAnnotation.getVisibility() == Visibility.PUBLIC){
					baseClassNode.access = baseClassNode.access | Opcodes.ACC_PUBLIC;
					Log.info("Set " + baseClassNode.name + " class to be public.");
				} else if(defineTypeVisibilityAnnotation.getVisibility() == Visibility.PROTECTED){
					baseClassNode.access = baseClassNode.access | Opcodes.ACC_PROTECTED;
					Log.info("Set " + baseClassNode.name + " class to be protected.");
				} else if(defineTypeVisibilityAnnotation.getVisibility() == Visibility.PRIVATE){
					baseClassNode.access = baseClassNode.access | Opcodes.ACC_PRIVATE;
					Log.info("Set " + baseClassNode.name + " class to be private.");
				} else {
					// should never happen
					throw new RuntimeException("Missing visibility modifier");
				}
//				Log.info("Post Access Modifiers: " + getAccessModifiers(baseClassNode.access));
				updateBytecode(className, baseClassNode);
			}
			
			processed = true;
		}
		// update methods
		for(DefineMethodVisibilityAnnotation defineMethodVisibilityAnnotation : defineVisibilityIdentifier.getTargetMethods()){
			String qualifiedClassName = defineMethodVisibilityAnnotation.getClassName();
			String[] simpleClassNameParts = qualifiedClassName.split("/");
			ClassNode baseClassNode = getBytecode(qualifiedClassName);
			String simpleClassName = simpleClassNameParts[simpleClassNameParts.length-1];
			if(simpleClassName.contains("$")){
				simpleClassName = simpleClassName.substring(simpleClassName.indexOf("$")+1,simpleClassName.length());
			}
			for (Object o : baseClassNode.methods) {
				MethodNode methodNode = (MethodNode) o;
				if(defineMethodVisibilityAnnotation.getMethodName().equals(simpleClassName)){
					if(methodNode.name.equals("<init>")){
//						Log.info("Pre Access Modifiers: " + getAccessModifiers(methodNode.access));
						methodNode.access = methodNode.access & (~Opcodes.ACC_PUBLIC & ~Opcodes.ACC_PROTECTED & ~Opcodes.ACC_PRIVATE);
						if(defineMethodVisibilityAnnotation.getVisibility() == Visibility.PUBLIC){
							methodNode.access = methodNode.access | Opcodes.ACC_PUBLIC;
							Log.info("Set " + methodNode.name + " initializer to be public.");
						} else if(defineMethodVisibilityAnnotation.getVisibility() == Visibility.PROTECTED){
							methodNode.access = methodNode.access | Opcodes.ACC_PROTECTED;
							Log.info("Set " + methodNode.name + " initializer to be protected.");
						} else if(defineMethodVisibilityAnnotation.getVisibility() == Visibility.PRIVATE){
							methodNode.access = methodNode.access | Opcodes.ACC_PRIVATE;
							Log.info("Set " + methodNode.name + " initializer to be private.");
						} else {
							// should never happen
							throw new RuntimeException("Missing visibility modifier");
						}
//						Log.info("Post Access Modifiers: " + getAccessModifiers(methodNode.access));
					} else if(methodNode.name.equals("<clinit>")){
//						Log.info("Pre Access Modifiers: " + getAccessModifiers(methodNode.access));
						methodNode.access = methodNode.access & (~Opcodes.ACC_PUBLIC & ~Opcodes.ACC_PROTECTED & ~Opcodes.ACC_PRIVATE);
						if(defineMethodVisibilityAnnotation.getVisibility() == Visibility.PUBLIC){
							methodNode.access = methodNode.access | Opcodes.ACC_PUBLIC;
							Log.info("Set " + methodNode.name + " static initializer to be public.");
						} else if(defineMethodVisibilityAnnotation.getVisibility() == Visibility.PROTECTED){
							methodNode.access = methodNode.access | Opcodes.ACC_PROTECTED;
							Log.info("Set " + methodNode.name + " static initializer to be protected.");
						} else if(defineMethodVisibilityAnnotation.getVisibility() == Visibility.PRIVATE){
							methodNode.access = methodNode.access | Opcodes.ACC_PRIVATE;
							Log.info("Set " + methodNode.name + " static initializer to be private.");
						} else {
							// should never happen
							throw new RuntimeException("Missing visibility modifier");
						}
//						Log.info("Post Access Modifiers: " + getAccessModifiers(methodNode.access));
					}
					updateBytecode(qualifiedClassName, baseClassNode);
					processed = true;
				} else if(methodNode.name.equals(defineMethodVisibilityAnnotation.getMethodName())){
//					Log.info("Pre Access Modifiers: " + getAccessModifiers(methodNode.access));
					methodNode.access = methodNode.access & (~Opcodes.ACC_PUBLIC & ~Opcodes.ACC_PROTECTED & ~Opcodes.ACC_PRIVATE);
					if(defineMethodVisibilityAnnotation.getVisibility() == Visibility.PUBLIC){
						methodNode.access = methodNode.access | Opcodes.ACC_PUBLIC;
						Log.info("Set " + methodNode.name + " method to be public.");
					} else if(defineMethodVisibilityAnnotation.getVisibility() == Visibility.PROTECTED){
						methodNode.access = methodNode.access | Opcodes.ACC_PROTECTED;
						Log.info("Set " + methodNode.name + " method to be protected.");
					} else if(defineMethodVisibilityAnnotation.getVisibility() == Visibility.PRIVATE){
						methodNode.access = methodNode.access | Opcodes.ACC_PRIVATE;
						Log.info("Set " + methodNode.name + " method to be private.");
					} else {
						// should never happen
						throw new RuntimeException("Missing visibility modifier");
					}
//					Log.info("Post Access Modifiers: " + getAccessModifiers(methodNode.access));
					updateBytecode(qualifiedClassName, baseClassNode);
					processed = true;
//					break; // should only be one match?
					// TODO: is above true? need to do better signature matching I assume? for now just blast em all...
				}
			}
		}
		// update fields
		for(DefineFieldVisibilityAnnotation defineFieldVisibilityAnnotation : defineVisibilityIdentifier.getTargetFields()){
			String className = defineFieldVisibilityAnnotation.getClassName();
			ClassNode baseClassNode = getBytecode(className);
			for (Object o : baseClassNode.fields) {
				FieldNode fieldNode = (FieldNode) o;
				if(fieldNode.name.equals(defineFieldVisibilityAnnotation.getFieldName())){
//					Log.info("Pre Access Modifiers: " + getAccessModifiers(fieldNode.access));
					fieldNode.access = fieldNode.access & (~Opcodes.ACC_PUBLIC & ~Opcodes.ACC_PROTECTED & ~Opcodes.ACC_PRIVATE);
					if(defineFieldVisibilityAnnotation.getVisibility() == Visibility.PUBLIC){
						fieldNode.access = fieldNode.access | Opcodes.ACC_PUBLIC;
						Log.info("Set " + fieldNode.name + " field to be public.");
					} else if(defineFieldVisibilityAnnotation.getVisibility() == Visibility.PROTECTED){
						fieldNode.access = fieldNode.access | Opcodes.ACC_PROTECTED;
						Log.info("Set " + fieldNode.name + " field to be protected.");
					} else if(defineFieldVisibilityAnnotation.getVisibility() == Visibility.PRIVATE){
						fieldNode.access = fieldNode.access | Opcodes.ACC_PRIVATE;
						Log.info("Set " + fieldNode.name + " field to be private.");
					} else {
						// should never happen
						throw new RuntimeException("Missing visibility modifier");
					}
//					Log.info("Post Access Modifiers: " + getAccessModifiers(fieldNode.access));
					updateBytecode(className, baseClassNode);
					processed = true;
					break; // should only be one match
				}
			}
		}
		return processed;
	}

	/**
	 * Sets the finality bit for for types, methods, and fields as defined by the annotation system
	 * @param defineFinalityIdentifier
	 * @param runtimeModifications
	 * @throws IOException
	 */
	private boolean setFinality(DefineFinalityIdentifier defineFinalityIdentifier) throws IOException {
		boolean processed = false;
		// update types
		for(DefineTypeFinalityAnnotation defineTypeFinalityAnnotation : defineFinalityIdentifier.getTargetTypes()){
			String className = defineTypeFinalityAnnotation.getClassName();
			if(className.contains("$")){
				// deal with outer class references to inner class files first
				String baseClassName = className.substring(0, className.lastIndexOf("$"));
				ClassNode baseClassNode = getBytecode(baseClassName);
				for(InnerClassNode innerClassNode : baseClassNode.innerClasses){
					if(innerClassNode.name.equals(className)){
//						Log.info("Pre Access Modifiers: " + getAccessModifiers(innerClassNode.access));
						if(defineTypeFinalityAnnotation.getFinality()){
							innerClassNode.access = innerClassNode.access | Opcodes.ACC_FINAL;
							Log.info("Set " + innerClassNode.name + " class to be final.");
						} else {
							innerClassNode.access = innerClassNode.access & (~Opcodes.ACC_FINAL);
							Log.info("Set " + innerClassNode.name + " class to be non-final.");
						}
//						Log.info("Post Access Modifiers: " + getAccessModifiers(innerClassNode.access));
					}
				}
				updateBytecode(baseClassName, baseClassNode);
				
				// deal with the inner class file directly
				String innerClassName = className;
				baseClassNode = getBytecode(innerClassName);
				for(InnerClassNode innerClassNode : baseClassNode.innerClasses){
					if(innerClassNode.name.equals(className)){
//						Log.info("Pre Access Modifiers: " + getAccessModifiers(innerClassNode.access));
						if(defineTypeFinalityAnnotation.getFinality()){
							innerClassNode.access = innerClassNode.access | Opcodes.ACC_FINAL;
							Log.info("Set " + innerClassNode.name + " class to be final.");
						} else {
							innerClassNode.access = innerClassNode.access & (~Opcodes.ACC_FINAL);
							Log.info("Set " + innerClassNode.name + " class to be non-final.");
						}
//						Log.info("Post Access Modifiers: " + getAccessModifiers(innerClassNode.access));
					}
				}
				updateBytecode(innerClassName, baseClassNode);
				processed = true;
			} else {
				// simple case no inner classes
				ClassNode baseClassNode = getBytecode(className);
				if(baseClassNode != null){
//					Log.info("Pre Access Modifiers: " + getAccessModifiers(baseClassNode.access));
					if(defineTypeFinalityAnnotation.getFinality()){
						baseClassNode.access = baseClassNode.access | Opcodes.ACC_FINAL;
						Log.info("Set " + baseClassNode.name + " class to be final.");
					} else {
						baseClassNode.access = baseClassNode.access & (~Opcodes.ACC_FINAL);
						Log.info("Set " + baseClassNode.name + " class to be non-final.");
					}
//					Log.info("Post Access Modifiers: " + getAccessModifiers(baseClassNode.access));
					updateBytecode(className, baseClassNode);
					processed = true;
				} else {
					Log.warning("Could not locate base class.", new RuntimeException("Missing base class"));
				}
			}
		}
		// update methods
		for(DefineMethodFinalityAnnotation defineMethodFinalityAnnotation : defineFinalityIdentifier.getTargetMethods()){
			// final is not a valid modifier for initializers so no need to consider that case
			String className = defineMethodFinalityAnnotation.getClassName();
			ClassNode baseClassNode = getBytecode(className);
			for (Object o : baseClassNode.methods) {
				MethodNode methodNode = (MethodNode) o;
				if(methodNode.name.equals(defineMethodFinalityAnnotation.getMethodName())){
//					Log.info("Pre Access Modifiers: " + getAccessModifiers(methodNode.access));
					if(defineMethodFinalityAnnotation.getFinality()){
						methodNode.access = methodNode.access | Opcodes.ACC_FINAL;
						Log.info("Set " + methodNode.name + " method to be final.");
					} else {
						methodNode.access = methodNode.access & (~Opcodes.ACC_FINAL);
						Log.info("Set " + methodNode.name + " method to be non-final.");
					}
//					Log.info("Post Access Modifiers: " + getAccessModifiers(methodNode.access));
					updateBytecode(className, baseClassNode);
					processed = true;
				}
			}
		}
		// update fields
		for(DefineFieldFinalityAnnotation defineFieldFinalityAnnotation : defineFinalityIdentifier.getTargetFields()){
			String className = defineFieldFinalityAnnotation.getClassName();
			ClassNode baseClassNode = getBytecode(className);
			for (Object o : baseClassNode.fields) {
				FieldNode fieldNode = (FieldNode) o;
				if(fieldNode.name.equals(defineFieldFinalityAnnotation.getFieldName())){
//					Log.info("Pre Access Modifiers: " + getAccessModifiers(fieldNode.access));
					if(defineFieldFinalityAnnotation.getFinality()){
						fieldNode.access = fieldNode.access | Opcodes.ACC_FINAL;
						Log.info("Set " + fieldNode.name + " field to be final.");
					} else {
						fieldNode.access = fieldNode.access & (~Opcodes.ACC_FINAL);
						Log.info("Set " + fieldNode.name + " field to be non-final.");
					}
//					Log.info("Post Access Modifiers: " + getAccessModifiers(fieldNode.access));
					updateBytecode(className, baseClassNode);
					processed = true;
					break; // should only be one match
				}
			}
		}
		return processed;
	}

	public void save(File outputFile) throws IOException {
		for(String entry : purgedEntries){
			jarModifier.remove(entry + ".class");
		}
		for(Entry<String,Bytecode> entry : bytecodeCache.entrySet()){
			jarModifier.add(entry.getKey() + ".class", entry.getValue().getBytecode(), true);
		}
		jarModifier.save(outputFile);
	}
	
	private byte[] mergeClasses(byte[] baseClass, byte[] classToMerge) throws IOException {
		// read the classes into ClassNode objects
		ClassNode baseClassNode = BytecodeUtils.getClassNode(baseClass);
		ClassNode classToMergeClassNode = BytecodeUtils.getClassNode(classToMerge);

		// get a list of base methods conflicting with methods to merge
		BaseMethodsIdentifier baseMethodsIdentifier = new BaseMethodsIdentifier(baseClassNode);
		LinkedList<MethodNode> baseMethods = baseMethodsIdentifier.getBaseMethods();
		
		// identify methods to insert or replace
		DefineIdentifier defineMethodsIdentifier = new DefineIdentifier(classToMergeClassNode);
		LinkedList<DefineMethodAnnotation> methodsToDefine = defineMethodsIdentifier.getDefineMethodAnnotations();
		
		// identify methods to merge
		MergeIdentifier mergeIdentifier = new MergeIdentifier(classToMergeClassNode);
		LinkedList<MergeMethodAnnotation> methodToMergeAnnotations = mergeIdentifier.getMergeMethodAnnotations();
		
		// rename base methods that should be preserved
		LinkedList<String> renamedMethods = new LinkedList<String>();
		for(MergeMethodAnnotation methodToMergeAnnotation : methodToMergeAnnotations){
			MethodNode methodToMerge = methodToMergeAnnotation.getMethodNode();
			boolean foundTargetMethod = false;
			for(MethodNode baseMethod : baseMethods){
				if(methodToMerge.signature != null && baseMethod.signature != null){
					if(methodToMerge.signature.equals(baseMethod.signature)){
						if(methodToMerge.name.equals(baseMethod.name) && methodToMerge.desc.equals(baseMethod.desc)){
							renamedMethods.add(baseClassNode.name + "." + renameMethod(baseMethod));
							foundTargetMethod = true;
							continue;
						}
					}
				} else {
					// signature was null, fall back to name and description only
					if(methodToMerge.name.equals(baseMethod.name) && methodToMerge.desc.equals(baseMethod.desc)){
						renamedMethods.add(baseClassNode.name + "." + renameMethod(baseMethod));
						foundTargetMethod = true;
						continue;
					}
				}
			}
			if(!foundTargetMethod){
				Log.warning("Target method " + methodToMerge.desc.toString() + " does not exist! Runtime behavior may not be correct.");
			}
		}
		
		// purge defined methods that were already there
		// adapt a ClassWriter with the PurgeAdapter
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		Set<MethodNode> methodsToPurge = new HashSet<MethodNode>();
		for(DefineMethodAnnotation methodToDefine : methodsToDefine){
			methodsToPurge.add(methodToDefine.getMethodNode());
		}
		Set<FieldNode> fieldsToPurge = new HashSet<FieldNode>();
		PurgeAdapter purgeAdapter = new PurgeAdapter(classWriter, methodsToPurge, fieldsToPurge);
		ClassReader purgedBaseClassReader = new ClassReader(BytecodeUtils.writeClass(baseClassNode));
		purgedBaseClassReader.accept(purgeAdapter, ClassReader.EXPAND_FRAMES);
		baseClassNode = BytecodeUtils.getClassNode(classWriter.toByteArray());

		// merge the classes
		// adapt a ClassWriter with the MergeAdapter
		// modifiedBaseClass, classToMerge -> MergeAdapter -> ClassWriter
		classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		MergeAdapter mergeAdapter = new MergeAdapter(classWriter, classToMergeClassNode, mergeRenamePrefix, renamedMethods);
		ClassReader modifiedBaseClassReader = new ClassReader(BytecodeUtils.writeClass(baseClassNode));
		modifiedBaseClassReader.accept(mergeAdapter, ClassReader.EXPAND_FRAMES);
		return classWriter.toByteArray();
	}

	private String renameMethod(MethodNode methodToRename) {
		// first remove any annotations from renamed base methods
		AnnotationUtils.clearMethodAnnotations(methodToRename);
		
		// rename the method
		String originalMethodName = methodToRename.name;
		String renamedMethodName = mergeRenamePrefix + methodToRename.name;
		methodToRename.name = renamedMethodName;
		
		// make the method private to hide it from the end user
		methodToRename.access = methodToRename.access & (~Opcodes.ACC_PUBLIC & ~Opcodes.ACC_PROTECTED & ~Opcodes.ACC_PRIVATE);
		methodToRename.access = methodToRename.access | Opcodes.ACC_PRIVATE;

		Log.info("Renamed " + originalMethodName + " to " + renamedMethodName);
		
		return originalMethodName; // return the original name
	}
	
}
