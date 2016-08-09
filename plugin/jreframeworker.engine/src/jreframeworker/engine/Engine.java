package jreframeworker.engine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.jar.JarException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
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

public class Engine {

	private String mergeRenamePrefix;
	private JarModifier runtimeModifications;
	
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
	
	public Engine(File runtime, String mergeRenamePrefix) throws JarException, IOException {
		this.mergeRenamePrefix = mergeRenamePrefix;
		this.runtimeModifications = new JarModifier(runtime);
	}
	
	private ClassNode getBytecode(String entry) throws IOException {
		return BytecodeUtils.getClassNode(getRawBytecode(entry));
	}
	
	private byte[] getRawBytecode(String entry) throws IOException {
		if(bytecodeCache.containsKey(entry)){
			return bytecodeCache.get(entry).getBytecode();
		} else {
			String qualifiedClassFilename = entry + ".class";
			byte[] bytecode = runtimeModifications.extractEntry(qualifiedClassFilename);
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
		runtimeModifications.add(qualifiedClassName, inputClass, overwrite);
	}
	
	public boolean process(byte[] inputClass) throws IOException {
		boolean processed = false;
		// check to see if the class is annotated with 
		ClassNode classNode = BytecodeUtils.getClassNode(inputClass);
		Log.info("Processing Input Class: " + classNode.name + "...");
		
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
					if(runtimeModifications.getJarEntrySet().contains(qualifiedClassFilename)){
						updateBytecode(classNode.name, inputClass);
						Log.info("Replaced: " + qualifiedClassName + " in " + runtimeModifications.getJarFile().getName());
					} else {
						updateBytecode(classNode.name, inputClass);
						Log.info("Inserted: " + qualifiedClassName + " into " + runtimeModifications.getJarFile().getName());
					}
					processed = true;
				} else if(checker.isMergeTypeAnnotation()){
					MergeTypeAnnotation mergeTypeAnnotation = JREFAnnotationIdentifier.getMergeTypeAnnotation(classNode, annotationNode);
					String qualifiedParentClassName = mergeTypeAnnotation.getSupertype();
					byte[] baseClass = getRawBytecode(qualifiedParentClassName);
					byte[] mergedClass = mergeClasses(baseClass, inputClass);
					updateBytecode(qualifiedParentClassName, mergedClass);
					Log.info("Merged: " + qualifiedClassName + " into " + qualifiedParentClassName + " in " + runtimeModifications.getJarFile().getName());
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
			runtimeModifications.add(entry.getKey() + ".class", entry.getValue().getBytecode(), true);
		}
		runtimeModifications.save(outputFile);
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
		
		// purge defined methods that are already there
		for(MethodNode methodToPurge : methodsToDefine){
			for(MethodNode baseMethod : baseMethods){
				if(methodToPurge.signature != null && baseMethod.signature != null){
					if(methodToPurge.signature.equals(baseMethod.signature)){
						if(methodToPurge.name.equals(baseMethod.name) && methodToPurge.desc.equals(baseMethod.desc)){
							purgeMethod(baseMethod);
							continue;
						}
					}
				} else {
					// signature was null, fall back to name and description only
					if(methodToPurge.name.equals(baseMethod.name) && methodToPurge.desc.equals(baseMethod.desc)){
						purgeMethod(baseMethod);
						continue;
					}
				}
			}
		}
		
		// identify methods to merge
		MergeMethodsIdentifier mergeMethodsIdentifier = new MergeMethodsIdentifier(classToMergeClassNode);
		LinkedList<MethodNode> methodsToMerge = mergeMethodsIdentifier.getMergeMethods();
		
		// rename conflicting base methods
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

		// write out the modified base class
		byte[] modifiedBaseClass = BytecodeUtils.writeClass(baseClassNode);

		// adapt a ClassWriter with the MergeAdapter
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		MergeAdapter mergeAdapter = new MergeAdapter(classWriter, classToMergeClassNode, mergeRenamePrefix, renamedMethods);

		// merge the classes
		// modifiedBaseClass, classToMerge -> MergeAdapter -> ClassWriter
		ClassReader modifiedBaseClassReader = new ClassReader(modifiedBaseClass);
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
	
	// TODO: refactor to remove this hack
	// currently just renaming and hiding methods to "purge" them, 
	// but really we should just remove the bytecode entirely
	private String purgeMethod(MethodNode methodToPurge) {
		// first remove any annotations from renamed base methods
		AnnotationUtils.clearMethodAnnotations(methodToPurge);
		
		// rename the method
		String originalMethodName = methodToPurge.name;
		String renamedMethodName = mergeRenamePrefix + "purged_" + methodToPurge.name;
		methodToPurge.name = renamedMethodName;
		
		// make the method private to hide it from the end user
		methodToPurge.access = Opcodes.ACC_PRIVATE;
		
		Log.info("Renamed " + originalMethodName + " to " + renamedMethodName);
		
		return originalMethodName; // return the original name
	}
	
	/**
	 * This class is responsible for merging two class files based on 
	 * the merging strategies in the JReFrameworker framework.
	 * 
	 * References: http://www.jroller.com/eu/entry/merging_class_methods_with_asm
	 * 
	 * @author Ben Holland
	 */
	private static class MergeAdapter extends ClassVisitor {
		private ClassNode classToMerge;
		private String baseClassName;
		private String mergeRenamePrefix;
		private LinkedList<String> renamedMethods;

		public MergeAdapter(ClassVisitor baseClassVisitor, ClassNode classToMerge, String mergeReamePrefix, LinkedList<String> renamedMethods) {
			super(Opcodes.ASM5, baseClassVisitor);
			this.classToMerge = classToMerge;
			this.mergeRenamePrefix = mergeReamePrefix;
			this.renamedMethods = renamedMethods;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);
			baseClassName = name;
		}

		public void visitEnd() {
			// copy each field of the class to merge in to the original class
			for (Object fieldObject : classToMerge.fields) {
				FieldNode fieldNode = (FieldNode) fieldObject;
				// only insert the field if it is annotated
				if(fieldNode.invisibleAnnotations != null){
					boolean addField = false;
					for(Object annotationObject : fieldNode.invisibleAnnotations){
						AnnotationNode annotationNode = (AnnotationNode) annotationObject;
						JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
						checker.visitAnnotation(annotationNode.desc, false);
						if(checker.isDefineFieldAnnotation()){
							addField = true;
							break;
						}
					}
					if(addField){
						// clear field annotations and insert the field
						AnnotationUtils.clearFieldAnnotations(fieldNode);
						fieldNode.accept(this);
						Log.info("Added Field: " + fieldNode.name);
					}
				}
			}

			// copy each method of the class to merge that is annotated
			// with a jref annotation to the original class
			for (Object methodObject : classToMerge.methods) {
				MethodNode methodNode = (MethodNode) methodObject;

				// static initializers need to be handled specially
				if(methodNode.name.equals("<clinit>")){
					// TODO: merge static initializers
				} else if(methodNode.name.equals("<init>")){
					// TODO: merge initializers
				} else {
					boolean define = false;
					boolean merge = false;
					// check if method is annotated with a jref annotation
					LinkedList<AnnotationNode> jrefAnnotations = new LinkedList<AnnotationNode>();
					if (methodNode.invisibleAnnotations != null) {
						for (Object annotationObject : methodNode.invisibleAnnotations) {
							AnnotationNode annotation = (AnnotationNode) annotationObject;
							// check if the annotation is a jref annotation
							JREFAnnotationIdentifier jrefChecker = new JREFAnnotationIdentifier();
							jrefChecker.visitAnnotation(annotation.desc, false);
							if(jrefChecker.isJREFAnnotation()){
								jrefAnnotations.add(annotation);
								if(jrefChecker.isDefineMethodAnnotation()){
									define = true;
								}
								if(jrefChecker.isMergeMethodAnnotation()){
									merge = true;
								}
							}
						}
					}
					
					// if the method is annotated with @DefineMethod or @MergeMethod, add the method
					if(define || merge){
						// in any case, strip the jref annotations from the method
						methodNode.invisibleAnnotations.removeAll(jrefAnnotations);
						if(merge){
							mergeMethod(methodNode, renamedMethods);
							Log.info("Merged Method: " + methodNode.name);
						} else {
							addMethod(methodNode);
							Log.info("Added Method: " + methodNode.name);
						}
					}
				}
			}

			super.visitEnd();
		}
		
		/**
		 * Adds the method to the base class
		 * @param methodNode
		 */
		private void addMethod(MethodNode methodNode){
			String[] exceptions = new String[methodNode.exceptions.size()];
			methodNode.exceptions.toArray(exceptions);
			MethodVisitor mv = cv.visitMethod(methodNode.access, methodNode.name, methodNode.desc, methodNode.signature, exceptions);

			methodNode.instructions.resetLabels();
			// SimpleRemapper -> maps old name to new name
			// updates owners and descriptions appropriately
			methodNode.accept(new RemappingMethodAdapter(methodNode.access, methodNode.desc, mv, new SimpleRemapper(classToMerge.name, baseClassName)));
		}

		/**
		 * Performs some merge changes to the method instructions then adds the method
		 * @param methodNode
		 * @param renamedMethods
		 */
		@SuppressWarnings("unused")
		private void mergeMethod(MethodNode methodNode, LinkedList<String> renamedMethods) {
			// clean up method instructions
        	InsnList instructions = methodNode.instructions;
			Iterator<AbstractInsnNode> instructionIterator = instructions.iterator();
			while (instructionIterator.hasNext()) {
				AbstractInsnNode abstractInstruction = instructionIterator.next();
				if (abstractInstruction instanceof FieldInsnNode) {
					FieldInsnNode instruction = (FieldInsnNode) abstractInstruction;
				} else if (abstractInstruction instanceof FrameNode) {
					FrameNode instruction = (FrameNode) abstractInstruction;
				} else if (abstractInstruction instanceof IincInsnNode) {
					IincInsnNode instruction = (IincInsnNode) abstractInstruction;
				} else if (abstractInstruction instanceof InsnNode) {
					InsnNode instruction = (InsnNode) abstractInstruction;
				} else if (abstractInstruction instanceof IntInsnNode) {
					IntInsnNode instruction = (IntInsnNode) abstractInstruction;
				} else if (abstractInstruction instanceof InvokeDynamicInsnNode) {
					InvokeDynamicInsnNode instruction = (InvokeDynamicInsnNode) abstractInstruction;
				} else if (abstractInstruction instanceof JumpInsnNode) {
					JumpInsnNode instruction = (JumpInsnNode) abstractInstruction;
				} else if (abstractInstruction instanceof LabelNode) {
					LabelNode instruction = (LabelNode) abstractInstruction;
				} else if (abstractInstruction instanceof LdcInsnNode) {
					LdcInsnNode instruction = (LdcInsnNode) abstractInstruction;
				} else if (abstractInstruction instanceof LineNumberNode) {
					LineNumberNode instruction = (LineNumberNode) abstractInstruction;
				} else if (abstractInstruction instanceof LookupSwitchInsnNode) {
					LookupSwitchInsnNode instruction = (LookupSwitchInsnNode) abstractInstruction;
				} else if (abstractInstruction instanceof MethodInsnNode) {
					MethodInsnNode instruction = (MethodInsnNode) abstractInstruction;
					// check if the method call needs to be changed to a renamed method name
					// replace calls to super.x methods with prefix+x calls in the class to merge
					// TODO: should check more than just the name, need to check whole method signature
					for (String renamedMethod : renamedMethods) {
						if (instruction.name.equals(renamedMethod)) {
							// this method has been renamed, we need to rename the call as well
							instruction.name = mergeRenamePrefix + instruction.name;
							// if we renamed it, this call used super.x, so make
							// it a virtual invocation instead of special invocation
							if (instruction.getOpcode() == Opcodes.INVOKESPECIAL) {
								instruction.setOpcode(Opcodes.INVOKEVIRTUAL);
							}
						}
					}
				} else if (abstractInstruction instanceof MultiANewArrayInsnNode) {
					MultiANewArrayInsnNode instruction = (MultiANewArrayInsnNode) abstractInstruction;
				} else if (abstractInstruction instanceof TableSwitchInsnNode) {
					TableSwitchInsnNode instruction = (TableSwitchInsnNode) abstractInstruction;
				} else if (abstractInstruction instanceof TypeInsnNode) {
					TypeInsnNode instruction = (TypeInsnNode) abstractInstruction;
				} else if (abstractInstruction instanceof VarInsnNode) {
					VarInsnNode instruction = (VarInsnNode) abstractInstruction;
				}
			}
			
			// finally insert the method
			addMethod(methodNode);
		}
	}

}
