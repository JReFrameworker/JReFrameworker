package jreframeworker.engine.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.objectweb.asm.ClassLoaders;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class BytecodeUtils {

	/**
	 * Writes a class to a byte array
	 * @param classNode
	 * @param classFile
	 * @throws IOException
	 */
	public static byte[] writeClass(ClassNode classNode) throws IOException {
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(classWriter);
        return classWriter.toByteArray();
	}

	/**
	 * Reads a bytecode class file into a ClassNode object
	 * @param classFile
	 * @return
	 * @throws IOException
	 */
	public static ClassNode getClassNode(File classFile) throws IOException {
		byte[] bytes = Files.readAllBytes(classFile.toPath());
		return getClassNode(bytes);
	}

	/**
	 * Reads a bytecode class file into a ClassNode object
	 * @param classFile
	 * @return
	 * @throws IOException
	 */
	public static ClassNode getClassNode(byte[] bytes) {
		ClassReader classReader = new ClassReader(bytes);
		ClassNode classNode = new ClassNode();
		classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
		return classNode;
	}
	
}
