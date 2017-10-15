package com.jreframeworker.engine.tests.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class TestUtilities {
	
	public static void jarFiles(File workingDirectory, File outputJar, String pkg, File... files) throws IOException {
		File extractedJar = new File(workingDirectory.getAbsolutePath() + File.separator + "jar");
		File extractedJarPackage = new File(extractedJar.getAbsolutePath() + File.separator + pkg.replace(".", File.separator));
		extractedJarPackage.mkdirs();
		for(File file : files){
			copyFile(file, new File(extractedJarPackage.getAbsolutePath() + File.separator + file.getName()));
		}
		TestUtilities.jar(extractedJar, outputJar, TestUtilities.generateEmptyManifest());
	}

	public static final String META_INF = "META-INF";

	/**
	 * Generates a Jar Manifest based on the given parameters
	 * @return
	 */
	public static Manifest generateEmptyManifest(){
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		return manifest;
	}
	
	/**
	 * Creates a Jar file from a directory
	 * 
	 * @param extractedJarPath
	 * @param outputJar
	 * @throws IOException
	 */
	public static void jar(File extractedJarPath, File outputJar, Manifest manifest) throws IOException {
		JarOutputStream target = new JarOutputStream(new FileOutputStream(outputJar), manifest);
		addFileToJar(extractedJarPath, extractedJarPath, target);
		target.close();
	}

	// note this current implementation also updates the timestamps of Jar contents
	public static void addFileToJar(File fileOrDirectoryToAdd, File extractedJarPath, JarOutputStream target) throws IOException {
		String relPath = "";
		if(!fileOrDirectoryToAdd.getAbsolutePath().equals(extractedJarPath.getAbsolutePath())){
			String fileToAddCanonicalPath = fileOrDirectoryToAdd.getCanonicalPath();
			int relStart = extractedJarPath.getCanonicalPath().length() + 1;
			int relEnd = fileOrDirectoryToAdd.getCanonicalPath().length();
			String d = fileToAddCanonicalPath.substring(relStart,relEnd);
			relPath = d.replace("\\", "/");
		}
		BufferedInputStream in = null;
		try {
			if (fileOrDirectoryToAdd.isDirectory()) {
				if (!relPath.isEmpty()) {
					if (!relPath.endsWith("/")){
						relPath += "/";
					}
					JarEntry entry = new JarEntry(relPath);
					entry.setTime(fileOrDirectoryToAdd.lastModified());
					target.putNextEntry(entry);
					target.closeEntry();
				}
				for (File nestedFile : fileOrDirectoryToAdd.listFiles()){
					addFileToJar(nestedFile, extractedJarPath, target);
				}
				return;
			}

			JarEntry entry = new JarEntry(relPath);
			entry.setTime(fileOrDirectoryToAdd.lastModified());
			target.putNextEntry(entry);
			in = new BufferedInputStream(new FileInputStream(fileOrDirectoryToAdd));

			byte[] buffer = new byte[1024];
			while (true) {
				int count = in.read(buffer);
				if (count == -1){
					break;
				}
				target.write(buffer, 0, count);
			}
			target.closeEntry();
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}
	
	public static void delete(File f) throws IOException {
		if (f.isDirectory()){
			for (File c : f.listFiles()){
				delete(c);
			}
		}
		if (!f.delete()){
			throw new FileNotFoundException("Failed to delete file: " + f);
		}
	}
	
	public static List<File> gatherTestSources(File testSourceDirectory){
		ArrayList<File> sourceFiles = new ArrayList<File>();
		for(File sourceFile : testSourceDirectory.listFiles()){
			if(sourceFile.getName().endsWith(".java")){
				sourceFiles.add(sourceFile);
			}
		}
		return sourceFiles;
	}

	public static List<File> compileSources(List<File> sourceFiles, File workingDirectory) throws IOException {
		JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
		
		if(javaCompiler == null){
			throw new RuntimeException("Could not find Java compiler.");
		}
		
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(diagnostics, Locale.ENGLISH, Charset.forName("UTF-8"));
		Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
		javaCompiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits).call();
		    
		for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
			throw new RuntimeException(String.format("Error on line %d in %d%n", diagnostic.getLineNumber(), diagnostic.getSource().toString()));
		}        
		 
		fileManager.close();
		
		List<File> classFiles = new LinkedList<File>();
		for(File sourceFile : sourceFiles){
			File outputFile = new File(workingDirectory.getAbsolutePath() + File.separator + sourceFile.getName().replace(".java", ".class"));
			new File(sourceFile.getAbsolutePath().replace(".java", ".class")).renameTo(outputFile);
			classFiles.add(outputFile);
		}
		return classFiles;
	}
	
	public static File getClassFile(String className, List<File> classFiles){
		for(File classFile : classFiles){
			if(classFile.getName().endsWith(className + ".class")){
				return classFile;
			}
		}
		return null;
	}
	
	public static void copyFile(File from, File to) throws IOException {
		Files.copy(from.toPath(), to.toPath());
	}
	
}
