import identifiers.MergeMethodIdentifier;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class Merge {

	private static final String METHOD_RENAME_PREFIX = "jref_";
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {

		// get input stream handles to each bytecode file
		FileInputStream originalClass = new FileInputStream("/Users/benjholla/Desktop/HiddenFile/rt/java/io/File.class");
		FileInputStream classToMerge = new FileInputStream("/Users/benjholla/Desktop/HiddenFile/java/io/HiddenFile.class");

		// create a class reader for each bytecode stream
		ClassReader classToMergeReader = new ClassReader(classToMerge);
		ClassReader originalClassReader = new ClassReader(originalClass);

		// read the class to merge into a ClassNode object
		ClassNode classToMergeClassNode = new ClassNode();
		classToMergeReader.accept(classToMergeClassNode, ClassReader.EXPAND_FRAMES);

		// identify methods to merge
		MergeMethodIdentifier identifier = new MergeMethodIdentifier(classToMergeClassNode);
		LinkedList<MethodNode> methodsToMerge = identifier.getMergeMethods();
		
		// TODO: get a list of original methods conflicting with methods to merge
		
		// TODO: create a key for renaming base methods that will be replaced with conflicting methods to merge
		
		// TODO: rename conflicting base methods using renaming key
		
		// TODO: remove any annotations from renamed base methods and join them with the to merge methods
		
		// TODO: replace calls to super.x methods with prefix+x calls
		
		// strip the jref interfaces from the class to merge
		LinkedList<String> interfacesToRemove = new LinkedList<String>();
		for (Object o : classToMergeClassNode.interfaces) {
			// example: jreframeworker/operations/interfaces/JREF_Merge
			if (o.toString().startsWith("jreframeworker")) {
				interfacesToRemove.add(o.toString());
			}
		}
		classToMergeClassNode.interfaces.removeAll(interfacesToRemove);

		// adapt a class writer with the merge adapter
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		MergeAdapter mergeAdapter = new MergeAdapter(classWriter, classToMergeClassNode);

		// merge the classes
		originalClassReader.accept(mergeAdapter, ClassReader.EXPAND_FRAMES);

		// write the output file
		FileOutputStream fos = new FileOutputStream("/Users/benjholla/Desktop/test/FileMerged.class");
		fos.write(classWriter.toByteArray());
		fos.close();
	}

}
