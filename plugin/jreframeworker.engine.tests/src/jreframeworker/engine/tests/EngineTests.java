package jreframeworker.engine.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.Test;

import jreframeworker.engine.Engine;
import junit.framework.TestCase;

public class EngineTests extends TestCase {

	private String projectSource = new File("src" + File.separator + "jreframeworker" + File.separator + "engine" + File.separator + "tests").getAbsolutePath();
	private File workingDirectory = null;
	
	private void delete(File f) throws IOException {
		if (f.isDirectory()){
			for (File c : f.listFiles()){
				delete(c);
			}
		}
		if (!f.delete()){
			throw new FileNotFoundException("Failed to delete file: " + f);
		}
	}
	
	private List<File> gatherTestSources(File testSourceDirectory){
		ArrayList<File> sourceFiles = new ArrayList<File>();
		for(File sourceFile : testSourceDirectory.listFiles()){
			if(sourceFile.getName().endsWith(".java")){
				sourceFiles.add(sourceFile);
			}
		}
		return sourceFiles;
	}

	private List<File> compileSources(List<File> sourceFiles) throws IOException {
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
			classFiles.add(new File(workingDirectory.getAbsolutePath() + File.separator + sourceFile.getName().replace(".java", ".class")));
		}
		return classFiles;
	}
	
	public File getClassFile(String className, List<File> classFiles){
		for(File classFile : classFiles){
			if(classFile.getName().endsWith(className + ".class")){
				return classFile;
			}
		}
		return null;
	}
	
	@Override
	protected void setUp() throws Exception {
		workingDirectory = Files.createTempDirectory("working-directory").toFile();
	}

	@Override
	protected void tearDown() throws Exception {
		delete(workingDirectory);
		workingDirectory = null;
	}
	
	@Test
	public void testMergeMethodReplacesOriginalMethod() {
		// gather sources
		File testSourceDirectory = new File(projectSource + File.separator + "inputs" + File.separator + "a");
		List<File> sourceFiles = gatherTestSources(testSourceDirectory);
		
		// compile sources
		List<File> classFiles = new LinkedList<File>();
		try {
			classFiles.addAll(compileSources(sourceFiles));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		File baseClass = getClassFile("BaseClass", classFiles);
		assertNotNull(baseClass);
		
		File mergeClass = getClassFile("MergeClass", classFiles);
		assertNotNull(mergeClass);
		
		// TODO: jar base class
		
		// TODO: merge class into base class
//		Engine engine = new Engine();
		
		fail("Not yet implemented");
	}
	
	@Test
	public void testMergeMethodPreservesOriginalMethod() {
		fail("Not yet implemented");
	}

}
