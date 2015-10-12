import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class Merge {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {

		// get input stream handles to each bytecode file
		FileInputStream originalClass = new FileInputStream("/Users/benjholla/Desktop/HiddenFile/rt/java/io/File.class");
		FileInputStream classToMerge = new FileInputStream("/Users/benjholla/Desktop/HiddenFile/java/io/HiddenFile.class");

		// create a class reader for each bytecode stream
		ClassReader classToMergeReader = new ClassReader(classToMerge);
		ClassReader originalClassReader = new ClassReader(originalClass);

		// read the classToMerge into a ClassNode object
		ClassNode classToMergeClassNode = new ClassNode();
		classToMergeReader.accept(classToMergeClassNode, ClassReader.EXPAND_FRAMES);

		// strip the jref interfaces from classToMergeClassNode
		LinkedList<String> interfacesToRemove = new LinkedList<String>();
		for (Object o : classToMergeClassNode.interfaces) {
			// example: jreframeworker/operations/interfaces/JREF_Merge
			if (o.toString().startsWith("jreframeworker")) {
				interfacesToRemove.add(o.toString());
			}
		}
		classToMergeClassNode.interfaces.removeAll(interfacesToRemove);

		// TODO: 1) get a list of toMerge methods that will overwrite target methods
		
		// TODO: 2) preprocess original class in a ClassNode or something to
		// rename overwritten methods

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
