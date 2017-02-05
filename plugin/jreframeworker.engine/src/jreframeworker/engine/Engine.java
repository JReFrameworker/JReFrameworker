package jreframeworker.engine;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarException;

import org.objectweb.asm.ClassLoaders;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.RemappingMethodAdapter;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import jreframeworker.engine.identifiers.BaseMethodsIdentifier;
import jreframeworker.engine.identifiers.DefineFinalityIdentifier;
import jreframeworker.engine.identifiers.DefineFinalityIdentifier.DefineFieldFinalityAnnotation;
import jreframeworker.engine.identifiers.DefineFinalityIdentifier.DefineMethodFinalityAnnotation;
import jreframeworker.engine.identifiers.DefineFinalityIdentifier.DefineTypeFinalityAnnotation;
import jreframeworker.engine.identifiers.DefineMethodsIdentifier;
import jreframeworker.engine.identifiers.DefineVisibilityIdentifier;
import jreframeworker.engine.identifiers.DefineVisibilityIdentifier.DefineFieldVisibilityAnnotation;
import jreframeworker.engine.identifiers.DefineVisibilityIdentifier.DefineMethodVisibilityAnnotation;
import jreframeworker.engine.identifiers.DefineVisibilityIdentifier.DefineTypeVisibilityAnnotation;
import jreframeworker.engine.identifiers.DefineVisibilityIdentifier.Visibility;
import jreframeworker.engine.identifiers.JREFAnnotationIdentifier;
import jreframeworker.engine.identifiers.JREFAnnotationIdentifier.MergeTypeAnnotation;
import jreframeworker.engine.identifiers.MergeMethodsIdentifier;
import jreframeworker.engine.log.Log;
import jreframeworker.engine.utils.AnnotationUtils;
import jreframeworker.engine.utils.BytecodeUtils;
import jreframeworker.engine.utils.JarModifier;

@SuppressWarnings("deprecation")
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
		// check to see if the class is annotated with 
		ClassNode classNode = BytecodeUtils.getClassNode(inputClass);
		Log.info("Processing input class: " + classNode.name + "...");
		
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
					MergeTypeAnnotation mergeTypeAnnotation = JREFAnnotationIdentifier.getMergeTypeAnnotation(classNode, annotationNode);
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
	
	/**
	 * Sets the access (visibility) modifiers for types, methods, and fields as defined by the annotation system
	 * @param defineVisibilityIdentifier
	 * @param runtimeModifications
	 * @throws IOException 
	 */
	private boolean setVisibility(DefineVisibilityIdentifier defineVisibilityIdentifier) throws IOException {
		boolean processed = false;
		for(DefineTypeVisibilityAnnotation defineTypeVisibilityAnnotation : defineVisibilityIdentifier.getTargetTypes()){
			String className = defineTypeVisibilityAnnotation.getClassName();
			ClassNode baseClassNode = getBytecode(className);
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
			updateBytecode(className, baseClassNode);
			processed = true;
		}
		for(DefineMethodVisibilityAnnotation defineMethodVisibilityAnnotation : defineVisibilityIdentifier.getTargetMethods()){
			String qualifiedClassName = defineMethodVisibilityAnnotation.getClassName();
			String[] simpleClassNameParts = qualifiedClassName.split("/");
			ClassNode baseClassNode = getBytecode(qualifiedClassName);
			String simpleClassName = simpleClassNameParts[simpleClassNameParts.length-1];
			for (Object o : baseClassNode.methods) {
				MethodNode methodNode = (MethodNode) o;
				if(defineMethodVisibilityAnnotation.getMethodName().equals(simpleClassName)){
					if(methodNode.name.equals("<init>")){
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
						updateBytecode(qualifiedClassName, baseClassNode);
						processed = true;
//						break; // should only be one match?
						// TODO: is above true? need to do better signature matching I assume? for now just blast em all...
					} else if(methodNode.name.equals("<clinit>")){
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
						updateBytecode(qualifiedClassName, baseClassNode);
						processed = true;
//						break; // should only be one match?
						// TODO: is above true? need to do better signature matching I assume? for now just blast em all...
					}
				} else if(methodNode.name.equals(defineMethodVisibilityAnnotation.getMethodName())){
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
					updateBytecode(qualifiedClassName, baseClassNode);
					processed = true;
//					break; // should only be one match?
					// TODO: is above true? need to do better signature matching I assume? for now just blast em all...
				}
			}
		}
		for(DefineFieldVisibilityAnnotation defineFieldVisibilityAnnotation : defineVisibilityIdentifier.getTargetFields()){
			String className = defineFieldVisibilityAnnotation.getClassName();
			ClassNode baseClassNode = getBytecode(className);
			for (Object o : baseClassNode.fields) {
				FieldNode fieldNode = (FieldNode) o;
				if(fieldNode.name.equals(defineFieldVisibilityAnnotation.getFieldName())){
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
		for(DefineTypeFinalityAnnotation defineTypeFinalityAnnotation : defineFinalityIdentifier.getTargetTypes()){
			String className = defineTypeFinalityAnnotation.getClassName();
			ClassNode baseClassNode = getBytecode(className);
			if(baseClassNode != null){
				if(defineTypeFinalityAnnotation.getFinality()){
					baseClassNode.access = baseClassNode.access | Opcodes.ACC_FINAL;
					Log.info("Set " + baseClassNode.name + " class to be final.");
				} else {
					baseClassNode.access = baseClassNode.access & (~Opcodes.ACC_FINAL);
					Log.info("Set " + baseClassNode.name + " class to be non-final.");
				}
				updateBytecode(className, baseClassNode);
				processed = true;
			} else {
				Log.warning("Could not located base class.", new RuntimeException("Missing base class"));
			}
		}
		for(DefineMethodFinalityAnnotation defineMethodFinalityAnnotation : defineFinalityIdentifier.getTargetMethods()){
			String className = defineMethodFinalityAnnotation.getClassName();
			ClassNode baseClassNode = getBytecode(className);
			for (Object o : baseClassNode.methods) {
				MethodNode methodNode = (MethodNode) o;
				if(methodNode.name.equals(defineMethodFinalityAnnotation.getMethodName())){
					if(defineMethodFinalityAnnotation.getFinality()){
						methodNode.access = methodNode.access | Opcodes.ACC_FINAL;
						Log.info("Set " + methodNode.name + " method to be final.");
					} else {
						methodNode.access = methodNode.access & (~Opcodes.ACC_FINAL);
						Log.info("Set " + methodNode.name + " method to be non-final.");
					}
					updateBytecode(className, baseClassNode);
					processed = true;
//					break; // should only be one match?
					// TODO: is above true? need to do better signature matching I assume? for now just blast em all...
				}
			}
		}
		for(DefineFieldFinalityAnnotation defineFieldFinalityAnnotation : defineFinalityIdentifier.getTargetFields()){
			String className = defineFieldFinalityAnnotation.getClassName();
			ClassNode baseClassNode = getBytecode(className);
			for (Object o : baseClassNode.fields) {
				FieldNode fieldNode = (FieldNode) o;
				if(fieldNode.name.equals(defineFieldFinalityAnnotation.getFieldName())){
					if(defineFieldFinalityAnnotation.getFinality()){
						fieldNode.access = fieldNode.access | Opcodes.ACC_FINAL;
						Log.info("Set " + fieldNode.name + " field to be final.");
					} else {
						fieldNode.access = fieldNode.access & (~Opcodes.ACC_FINAL);
						Log.info("Set " + fieldNode.name + " field to be non-final.");
					}
					updateBytecode(className, baseClassNode);
					processed = true;
					break; // should only be one match
				}
			}
		}
		return processed;
	}

	public void save(File outputFile) throws IOException {
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
		DefineMethodsIdentifier defineMethodsIdentifier = new DefineMethodsIdentifier(classToMergeClassNode);
		LinkedList<MethodNode> methodsToDefine = defineMethodsIdentifier.getDefineMethods();
		
		// identify methods to merge
		MergeMethodsIdentifier mergeMethodsIdentifier = new MergeMethodsIdentifier(classToMergeClassNode);
		LinkedList<MethodNode> methodsToMerge = mergeMethodsIdentifier.getMergeMethods();
		
		// rename base methods that should be preserved
		LinkedList<String> renamedMethods = new LinkedList<String>();
		for(MethodNode methodToMerge : methodsToMerge){
			boolean foundTargetMethod = false;
			for(MethodNode baseMethod : baseMethods){
				if(methodToMerge.signature != null && baseMethod.signature != null){
					if(methodToMerge.signature.equals(baseMethod.signature)){
						if(methodToMerge.name.equals(baseMethod.name) && methodToMerge.desc.equals(baseMethod.desc)){
							renamedMethods.add(renameMethod(baseMethod));
							foundTargetMethod = true;
							continue;
						}
					}
				} else {
					// signature was null, fall back to name and description only
					if(methodToMerge.name.equals(baseMethod.name) && methodToMerge.desc.equals(baseMethod.desc)){
						renamedMethods.add(renameMethod(baseMethod));
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
		Set<MethodNode> methodsToPurge = new HashSet<MethodNode>(methodsToDefine);
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
		methodToRename.access = Opcodes.ACC_PRIVATE;
		
		Log.info("Renamed " + originalMethodName + " to " + renamedMethodName);
		
		return originalMethodName; // return the original name
	}
	
}
