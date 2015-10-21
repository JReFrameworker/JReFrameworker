package jreframeworker.core.bytecode.operations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import jreframeworker.core.bytecode.identifiers.BaseMethodsIdentifier;
import jreframeworker.core.bytecode.identifiers.JREFAnnotationIdentifier;
import jreframeworker.core.bytecode.identifiers.MergeMethodsIdentifier;
import jreframeworker.core.bytecode.utils.AnnotationUtils;
import jreframeworker.core.bytecode.utils.BytecodeUtils;
import jreframeworker.ui.PreferencesPage;

import org.objectweb.asm.Attribute;
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

public class Merge {
	
	public static void mergeClasses(File baseClass, File classToMerge, File outputClass) throws IOException {
		
		final String MERGE_RENAME_PREFIX = PreferencesPage.getMergeRenamingPrefix();
		
		// read the classes into ClassNode objects
		ClassNode baseClassNode = BytecodeUtils.getClassNode(baseClass);
		ClassNode classToMergeClassNode = BytecodeUtils.getClassNode(classToMerge);

		// TODO: check that the type is annotated with @MergeType
		
		// identify methods to merge
		MergeMethodsIdentifier mergeMethodsIdentifier = new MergeMethodsIdentifier(classToMergeClassNode);
		LinkedList<MethodNode> methodsToMerge = mergeMethodsIdentifier.getMergeMethods();
		
		// get a list of base methods conflicting with methods to merge
		BaseMethodsIdentifier baseMethodsIdentifier = new BaseMethodsIdentifier(baseClassNode);
		LinkedList<MethodNode> baseMethods = baseMethodsIdentifier.getBaseMethods();
		
		// create a list of methods to rename
		LinkedList<MethodNode> methodsToRename = new LinkedList<MethodNode>();
		for(MethodNode methodToMerge : methodsToMerge){
			for(MethodNode baseMethod : baseMethods){
				if(methodToMerge.name.equals(baseMethod.name)){
					methodsToRename.add(baseMethod);
					continue;
				}
			}
		}
		
		// rename conflicting base methods
		LinkedList<String> renamedMethods = new LinkedList<String>();
		for(MethodNode methodToRename : methodsToRename){
			// first remove any annotations from renamed base methods
			// TODO: consider adding these annotations to the method to merge 
			// to maintain the cover of the original method annotations
			AnnotationUtils.clearMethodAnnotations(methodToRename);
			
			// rename the method
			renamedMethods.add(methodToRename.name); // save the original name
			String renamedMethodName = MERGE_RENAME_PREFIX + methodToRename.name;
			methodToRename.name = renamedMethodName;
			
			// make the method private to hide it from the end user
			methodToRename.access = Opcodes.ACC_PRIVATE;
		}

		// write out the modified base class to a temporary class file
		String baseClassName = baseClassNode.name.replace('/', '.');
		File modifiedBaseClassFile = File.createTempFile(baseClassName, ".class");
		BytecodeUtils.writeClass(baseClassNode, modifiedBaseClassFile);

		// adapt a ClassWriter with the MergeAdapter
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		MergeAdapter mergeAdapter = new MergeAdapter(classWriter, classToMergeClassNode, MERGE_RENAME_PREFIX, renamedMethods);

		// merge the classes
		// modifiedBaseClass, classToMerge -> MergeAdapter -> ClassWriter
		FileInputStream modifiedBaseClassFileInputStream = new FileInputStream(modifiedBaseClassFile);
		ClassReader modifiedBaseClassReader = new ClassReader(modifiedBaseClassFileInputStream);
		modifiedBaseClassReader.accept(mergeAdapter, ClassReader.EXPAND_FRAMES);
		modifiedBaseClassFileInputStream.close();

		// write the output file
		FileOutputStream fos = new FileOutputStream(outputClass);
		fos.write(classWriter.toByteArray());
		fos.close();
		modifiedBaseClassFile.delete();
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
			for (Object o : classToMerge.fields) {
				FieldNode fieldNode = (FieldNode) o;
				// only insert the field if it is annotated
				if(fieldNode.invisibleAnnotations != null){
					for(Object o2 : fieldNode.invisibleAnnotations){
						AnnotationNode annotationNode = (AnnotationNode) o2;
						JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
						checker.visitAnnotation(annotationNode.desc, false);
						if(checker.isDefineFieldAnnotation()){
							// insert the field
							fieldNode.accept(this);
						}
					}
				}
			}

			// copy each method of the class to merge that is annotated
			// with a jref annotation to the original class
			for (Object o : classToMerge.methods) {
				MethodNode methodNode = (MethodNode) o;
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
					// then add the method, the renaming or deletion of any existing methods 
					// is done earlier so its safe to just add it now
					addMethod(methodNode, renamedMethods);
				}
			}

			super.visitEnd();
		}

		@SuppressWarnings("unused")
		private void addMethod(MethodNode methodNode, LinkedList<String> renamedMethods) {

			// clean up method instructions
			if (methodNode.attrs != null) {
				for (Attribute attribute : methodNode.attrs) {
					System.out.println("Method Attribute: " + attribute.type);
				}
			}
        	InsnList instructions = methodNode.instructions;
			Iterator<AbstractInsnNode> instructionIterator = instructions.iterator();
			while (instructionIterator.hasNext()) {
				AbstractInsnNode abstractInstruction = instructionIterator.next();
				// for each instruction type change the owner if there is one to
				// the base class
				// and perform any other merge operations needed
				if (abstractInstruction instanceof FieldInsnNode) {
					FieldInsnNode instruction = (FieldInsnNode) abstractInstruction;
					
					System.out.println("\nFieldInsnNode Name: " + instruction.name);
					System.out.println("FieldInsnNode Owner: " + instruction.owner);
					System.out.println("FieldInsnNode Description: " + instruction.desc);
					
					if (instruction.owner != null && instruction.owner.equals(classToMerge.name)) {
						instruction.owner = baseClassName;
					}
					if (instruction.desc != null) {
						if (instruction.desc.contains(classToMerge.name)) {
							instruction.desc = instruction.desc.replace(classToMerge.name, baseClassName);
						}
					}
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
					
					System.out.println("\nInvokeDynamicInsnNode Name: " + instruction.name);
					System.out.println("InvokeDynamicInsnNode Description: " + instruction.desc);
					
					if (instruction.desc != null) {
						if (instruction.desc.contains(classToMerge.name)) {
							instruction.desc = instruction.desc.replace(classToMerge.name, baseClassName);
						}
					}
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
					
					System.out.println("\nMethodInsnNode Name: " + instruction.name);
					System.out.println("MethodInsnNode Owner: " + instruction.owner);
					System.out.println("MethodInsnNode Description: " + instruction.desc);
					
					// change the owner of the method to the base class
					if (instruction.owner != null && instruction.owner.equals(classToMerge.name)) {
						instruction.owner = baseClassName;
					}
					if (instruction.desc.contains(classToMerge.name)) {
						instruction.desc = instruction.desc.replace(classToMerge.name, baseClassName);
					}
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
					
					System.out.println("Modified MethodInsnNode Name: " + instruction.name);
					System.out.println("Modified MethodInsnNode Owner: " + instruction.owner);
					System.out.println("Modified MethodInsnNode Description: " + instruction.desc);
				} else if (abstractInstruction instanceof MultiANewArrayInsnNode) {
					MultiANewArrayInsnNode instruction = (MultiANewArrayInsnNode) abstractInstruction;
					

					System.out.println("\nMultiANewArrayInsnNode Description: " + instruction.desc);
					
					if (instruction.desc != null) {
						if (instruction.desc.contains(classToMerge.name)) {
							instruction.desc = instruction.desc.replace(classToMerge.name, baseClassName);
						}
					}
				} else if (abstractInstruction instanceof TableSwitchInsnNode) {
					TableSwitchInsnNode instruction = (TableSwitchInsnNode) abstractInstruction;
				} else if (abstractInstruction instanceof TypeInsnNode) {
					TypeInsnNode instruction = (TypeInsnNode) abstractInstruction;
					
					
					System.out.println("\nTypeInsnNode Description: " + instruction.desc);
					
					if (instruction.desc != null) {
						if (instruction.desc.contains(classToMerge.name)) {
							instruction.desc = instruction.desc.replace(classToMerge.name, baseClassName);
						}
					}
				} else if (abstractInstruction instanceof VarInsnNode) {
					VarInsnNode instruction = (VarInsnNode) abstractInstruction;
				}
			}
			
			String[] exceptions = new String[methodNode.exceptions.size()];
			methodNode.exceptions.toArray(exceptions);
			MethodVisitor mv = cv.visitMethod(methodNode.access, methodNode.name, methodNode.desc, methodNode.signature, exceptions);
			
			System.out.println("Adding: " + methodNode.name + ", " + methodNode.desc + ", " + methodNode.signature);
			
			methodNode.instructions.resetLabels();
			methodNode.accept(new RemappingMethodAdapter(methodNode.access, methodNode.desc, mv, new SimpleRemapper(baseClassName, classToMerge.name)));
		}
	}

}
