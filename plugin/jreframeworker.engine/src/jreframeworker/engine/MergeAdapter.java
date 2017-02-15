package jreframeworker.engine;

import java.util.Iterator;
import java.util.LinkedList;

import org.objectweb.asm.ClassVisitor;
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

import jreframeworker.engine.identifiers.JREFAnnotationIdentifier;
import jreframeworker.engine.log.Log;
import jreframeworker.engine.utils.AnnotationUtils;

/**
 * This class is responsible for merging two class files based on 
 * the merging strategies in the JReFrameworker framework.
 * 
 * References: http://www.jroller.com/eu/entry/merging_class_methods_with_asm
 * 
 * @author Ben Holland
 */
@SuppressWarnings("deprecation")
public class MergeAdapter extends ClassVisitor {
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
