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
import jreframeworker.core.bytecode.utils.IOUtils;

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
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class Merge {

	private static final String METHOD_RENAME_PREFIX = "jref_";
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {

		// get input stream handles to each bytecode file
		File baseClass = new File("/Users/benjholla/Desktop/Evil/runtimes/rt/java/io/File.class");
		File classToMerge = new File("/Users/benjholla/Desktop/Evil/bin/java/io/HiddenFile.class");
		
		// read the classes into ClassNode objects
		ClassNode baseClassNode = IOUtils.getClassNode(baseClass);
		ClassNode classToMergeClassNode = IOUtils.getClassNode(classToMerge);

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
		for(MethodNode methodToRename : methodsToRename){
			// first remove any annotations from renamed base methods
			// TODO: consider adding these annotations to the method to merge 
			// to maintain the cover of the original method annotations
			AnnotationUtils.clearMethodAnnotations(methodToRename);
			
			// rename the method
			String renamedMethodName = METHOD_RENAME_PREFIX + methodToRename.name;
			methodToRename.name = renamedMethodName;
			
			// make the method private to hide it from the end user
			methodToRename.access = Opcodes.ACC_PRIVATE;
		}

		// write out the modified base class to a temporary class file
		String baseClassName = baseClassNode.name.replace('/', '.').replace('\\', '.');
		File modifiedBaseClassFile = File.createTempFile(baseClassName, ".class");
		IOUtils.writeClass(baseClassNode, modifiedBaseClassFile);
		
		// replace calls to super.x methods with prefix+x calls in the class to merge
        for(MethodNode methodToMerge : methodsToMerge){
        	InsnList instructions = methodToMerge.instructions;
			Iterator<AbstractInsnNode> instructionIterator = instructions.iterator();
        	while(instructionIterator.hasNext()){
        		AbstractInsnNode instruction = instructionIterator.next();
        		
        		if(instruction instanceof MethodInsnNode){
        			MethodInsnNode methodInstruction = (MethodInsnNode) instruction;
        			// change the owner of the method to the base class
        			methodInstruction.owner = baseClassNode.name;
        			// check if the method call needs to be changed to a renamed method name
//        			System.out.println("Name: " + methodInstruction.name + ", Opcode: " + methodInstruction.getOpcode() 
//        					+ ", Type: " + methodInstruction.getType() + ", " + methodInstruction.toString());
        			for(MethodNode method : methodsToMerge){
        				if(methodInstruction.name.equals(method.name)){
        					// this method has been renamed, we need to rename the call as well
        					methodInstruction.name = METHOD_RENAME_PREFIX + methodInstruction.name;
        					// if we renamed it, this call used super.x, so make it a virtual 
        					// invocation instead of special invocation
        					if(methodInstruction.getOpcode()==Opcodes.INVOKESPECIAL){
        						methodInstruction.setOpcode(Opcodes.INVOKEVIRTUAL);
        					}
        				}
        			}
        		}
        	}
        }

		// adapt a ClassWriter with the MergeAdapter
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		MergeAdapter mergeAdapter = new MergeAdapter(classWriter, classToMergeClassNode);

		// merge the classes
		// modifiedBaseClass, classToMerge -> MergeAdapter -> ClassWriter
		FileInputStream modifiedBaseClassFileInputStream = new FileInputStream(modifiedBaseClassFile);
		ClassReader modifiedBaseClassReader = new ClassReader(modifiedBaseClassFileInputStream);
		modifiedBaseClassReader.accept(mergeAdapter, ClassReader.EXPAND_FRAMES);
		modifiedBaseClassFileInputStream.close();

		// write the output file
		File mergedClassFile = new File("/Users/benjholla/Desktop/test/FileMerged.class");
		FileOutputStream fos = new FileOutputStream(mergedClassFile);
		fos.write(classWriter.toByteArray());
		fos.close();
		modifiedBaseClassFile.delete();
		
		// TODO: perform verification on the generated bytecode
		// currently this just assumes the bytecode is on the classpath already, 
		// so its not detecting the generated bytecode (just the original)
//		ClassVerifier.verify(baseClassNode.name.replace('/', '.').replace('\\', '.'));
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

		public MergeAdapter(ClassVisitor baseClassVisitor, ClassNode classToMerge) {
			super(Opcodes.ASM5, baseClassVisitor);
			this.classToMerge = classToMerge;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);
			baseClassName = name;
		}

		@SuppressWarnings("unchecked")
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
					addMethod(methodNode);
				}
			}

			super.visitEnd();
		}

		@SuppressWarnings("unchecked")
		private void addMethod(MethodNode methodNode) {
			String[] exceptions = new String[methodNode.exceptions.size()];
			methodNode.exceptions.toArray(exceptions);
			MethodVisitor mv = cv.visitMethod(methodNode.access, methodNode.name, methodNode.desc, methodNode.signature, exceptions);
			methodNode.instructions.resetLabels();
			methodNode.accept(new RemappingMethodAdapter(methodNode.access, methodNode.desc, mv, new SimpleRemapper(baseClassName, classToMerge.name)));
		}
	}

}
