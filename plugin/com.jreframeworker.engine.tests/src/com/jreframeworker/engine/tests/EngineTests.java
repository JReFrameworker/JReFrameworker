package com.jreframeworker.engine.tests;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;

import com.jreframeworker.engine.Engine;
import com.jreframeworker.engine.tests.utils.TestUtilities;
import com.jreframeworker.engine.utils.BytecodeUtils;

import junit.framework.TestCase;

public class EngineTests extends TestCase {

	private String packagePrefix = "com.jreframeworker.engine.tests";
	private String projectSource = new File("src" + File.separator + packagePrefix.replace(".", File.separator)).getAbsolutePath();
	private File workingDirectory = null;

	@Override
	protected void setUp() throws Exception {
		workingDirectory = Files.createTempDirectory("working-directory").toFile();
	}

	@Override
	protected void tearDown() throws Exception {
		TestUtilities.delete(workingDirectory);
		workingDirectory = null;
	}
	
	@Test
	@SuppressWarnings({"resource", "unchecked", "rawtypes"})
	public void testMergeMethodReplacesOriginalMethod() throws Exception {
		// gather sources
		String pkg = "inputs.a";
		File testSourceDirectory = new File(projectSource + File.separator + pkg.replace(".", File.separator));
		List<File> sourceFiles = TestUtilities.gatherTestSources(testSourceDirectory);
		
		// compile sources
		List<File> classFiles = TestUtilities.compileSources(sourceFiles, workingDirectory);
		
		// assert all class files were compiled successfully
		File baseClass = TestUtilities.getClassFile("BaseClass", classFiles);
		assertNotNull(baseClass);
		assertTrue(baseClass.exists());
		File mergeClass = TestUtilities.getClassFile("MergeClass", classFiles);
		assertNotNull(mergeClass);
		assertTrue(mergeClass.exists());
		
		// jar base class
		File originalJar = new File(workingDirectory.getAbsolutePath() + File.separator + "original.jar");
		TestUtilities.jarFiles(workingDirectory, originalJar, (packagePrefix + "." + pkg), baseClass);
		
		// merge class into base class
		String renamePrefix = "jref_";
		Engine engine = new Engine(originalJar, renamePrefix);
		File modifiedJar = new File(workingDirectory.getAbsolutePath() + File.separator + "modified.jar");
		engine.process(BytecodeUtils.writeClass(BytecodeUtils.getClassNode(mergeClass)));
		engine.save(modifiedJar);
		
		// execute the modified base class method
		URL[] jarURL = { new URL("jar:file:" + modifiedJar.getCanonicalPath() + "!/") };
		ClassLoader classLoader = new URLClassLoader(jarURL, null); // important: set parent class loader to null!
		Class modifiedBaseClass = classLoader.loadClass(packagePrefix + "." + pkg + "." + "BaseClass");
		Method modifiedBaseClassMethod = modifiedBaseClass.getDeclaredMethod("method");
		Object modifiedBaseClassInstance = modifiedBaseClass.newInstance();
		Object result = modifiedBaseClassMethod.invoke(modifiedBaseClassInstance);
		
		// assert expected behavior is observed
		// the replaced method should return merge-method
		assertEquals("merge-method", result);
	}
	
	@Test
	@SuppressWarnings({"resource", "unchecked", "rawtypes"})
	public void testMergeMethodPreservesOriginalMethod() throws Exception {
		// gather sources
		String pkg = "inputs.b";
		File testSourceDirectory = new File(projectSource + File.separator + pkg.replace(".", File.separator));
		List<File> sourceFiles = TestUtilities.gatherTestSources(testSourceDirectory);
		
		// compile sources
		List<File> classFiles = TestUtilities.compileSources(sourceFiles, workingDirectory);
		
		// assert all class files were compiled successfully
		File baseClass = TestUtilities.getClassFile("BaseClass", classFiles);
		assertNotNull(baseClass);
		assertTrue(baseClass.exists());
		File mergeClass = TestUtilities.getClassFile("MergeClass", classFiles);
		assertNotNull(mergeClass);
		assertTrue(mergeClass.exists());
		
		// jar base class
		File originalJar = new File(workingDirectory.getAbsolutePath() + File.separator + "original.jar");
		TestUtilities.jarFiles(workingDirectory, originalJar, (packagePrefix + "." + pkg), baseClass);
		
		// merge class into base class
		String renamePrefix = "jref_";
		Engine engine = new Engine(originalJar, renamePrefix);
		File modifiedJar = new File(workingDirectory.getAbsolutePath() + File.separator + "modified.jar");
		engine.process(BytecodeUtils.writeClass(BytecodeUtils.getClassNode(mergeClass)));
		engine.save(modifiedJar);
		
		// execute the modified base class method
		URL[] jarURL = { new URL("jar:file:" + modifiedJar.getCanonicalPath() + "!/") };
		ClassLoader classLoader = new URLClassLoader(jarURL, null); // important: set parent class loader to null!
		Class modifiedBaseClass = classLoader.loadClass(packagePrefix + "." + pkg + "." + "BaseClass");
		Method modifiedBaseClassMethod = modifiedBaseClass.getDeclaredMethod("method");
		Object modifiedBaseClassInstance = modifiedBaseClass.newInstance();
		Object result = modifiedBaseClassMethod.invoke(modifiedBaseClassInstance);	
		
		// assert expected behavior is observed
		// the replaced method should return merge-method
		assertEquals("merged-original-method", result);
	}

}
