import identifiers.BaseMethodsIdentifier;
import identifiers.MergeMethodsIdentifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import utils.AnnotationUtils;
import utils.BytecodeUtils;

public class Merge {

	private static final String METHOD_RENAME_PREFIX = "jref_";
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {

		// get input stream handles to each bytecode file
		File baseClass = new File("/Users/benjholla/Desktop/Evil/runtimes/rt/java/io/File.class");
		File classToMerge = new File("/Users/benjholla/Desktop/Evil/bin/java/io/HiddenFile.class");
		
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
		BytecodeUtils.writeClass(baseClassNode, modifiedBaseClassFile);
		
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

}
