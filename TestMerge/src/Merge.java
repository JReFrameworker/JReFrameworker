import identifiers.BaseMethodsIdentifier;
import identifiers.MergeMethodsIdentifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.SystemMenuBar;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class Merge {

	private static final String METHOD_RENAME_PREFIX = "jref_";
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {

		// get input stream handles to each bytecode file
		File baseClass = new File("/Users/benjholla/Desktop/HiddenFile/rt/java/io/File.class");
		File classToMerge = new File("/Users/benjholla/Desktop/HiddenFile/java/io/HiddenFile.class");
		
		// read the classes into ClassNode objects
		ClassNode baseClassNode = getClassNode(baseClass);
		ClassNode classToMergeClassNode = getClassNode(classToMerge);

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
			clearMethodAnnotations(methodToRename);
			
			// rename the method
			String renamedMethodName = METHOD_RENAME_PREFIX + methodToRename.name;
			methodToRename.name = renamedMethodName;
		}

		// write out the modified base class to a temporary class file
		String baseClassName = baseClassNode.name.replace('/', '.').replace('\\', '.');
		File modifiedBaseClassFile = File.createTempFile(baseClassName, ".class");
        writeClass(baseClassNode, modifiedBaseClassFile);
		
		// TODO: replace calls to super.x methods with prefix+x calls in the class to merge
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
		
		// strip the jref interfaces from the class to merge
		LinkedList<String> interfacesToRemove = new LinkedList<String>();
		for (Object o : classToMergeClassNode.interfaces) {
			// example: jreframeworker/operations/interfaces/JREF_Merge
			if (o.toString().startsWith("jreframeworker")) {
				interfacesToRemove.add(o.toString());
			}
		}
		classToMergeClassNode.interfaces.removeAll(interfacesToRemove);

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
		FileOutputStream fos = new FileOutputStream("/Users/benjholla/Desktop/test/FileMerged.class");
		fos.write(classWriter.toByteArray());
		fos.close();
	}

	private static void clearMethodAnnotations(MethodNode method) {
		// clear visible annotations
		if(method.visibleAnnotations != null) method.visibleAnnotations.clear();
		if(method.visibleLocalVariableAnnotations != null) method.visibleLocalVariableAnnotations.clear();
		if(method.visibleTypeAnnotations != null) method.visibleTypeAnnotations.clear();
		if(method.visibleParameterAnnotations != null){
			for(@SuppressWarnings("rawtypes") List parameterAnnotations : method.visibleParameterAnnotations){
				parameterAnnotations.clear();
			}
		}
		
		// clear invisible annotations
		if(method.invisibleAnnotations != null) method.invisibleAnnotations.clear();
		if(method.invisibleLocalVariableAnnotations != null) method.invisibleLocalVariableAnnotations.clear();
		if(method.invisibleParameterAnnotations != null) method.invisibleParameterAnnotations.clone();
		if(method.invisibleParameterAnnotations != null){
			for(@SuppressWarnings("rawtypes") List parameterAnnotations : method.invisibleParameterAnnotations){
				parameterAnnotations.clear();
			}
		}
	}
	
	private static void writeClass(ClassNode classNode, File classFile) throws IOException {
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(classWriter);
        FileOutputStream fos = new FileOutputStream(classFile);
        fos.write(classWriter.toByteArray());
        fos.close();
	}

	/**
	 * Reads a bytecode class file into a ClassNode object
	 * @param classFile
	 * @return
	 * @throws IOException
	 */
	private static ClassNode getClassNode(File classFile) throws IOException {
		FileInputStream fis = new FileInputStream(classFile);
		ClassReader classReader = new ClassReader(fis);
		ClassNode classNode = new ClassNode();
		classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
		fis.close();
		return classNode;
	}

}
