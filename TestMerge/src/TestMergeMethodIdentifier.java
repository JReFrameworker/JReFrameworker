import identifiers.MergeMethodIdentifier;

import java.io.FileInputStream;
import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;


public class TestMergeMethodIdentifier {

	public static void main(String[] args) throws IOException {
		FileInputStream classToMerge = new FileInputStream("/Users/benjholla/Desktop/HiddenFile/java/io/HiddenFile.class");
		ClassReader classToMergeReader = new ClassReader(classToMerge);
		ClassNode classToMergeClassNode = new ClassNode();
		classToMergeReader.accept(classToMergeClassNode, ClassReader.EXPAND_FRAMES);
		
		MergeMethodIdentifier identifier = new MergeMethodIdentifier(classToMergeClassNode);
		System.out.println(identifier.getMergeMethods());
	}

}
