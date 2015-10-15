package jreframeworker.core.bytecode.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class IOUtils {

	public static void writeClass(ClassNode classNode, File classFile) throws IOException {
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
	public static ClassNode getClassNode(File classFile) throws IOException {
		FileInputStream fis = new FileInputStream(classFile);
		ClassReader classReader = new ClassReader(fis);
		ClassNode classNode = new ClassNode();
		classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
		fis.close();
		return classNode;
	}
	
}
