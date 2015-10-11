package jreframeworker.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

import jreframeworker.Activator;
import jreframeworker.log.Log;
import jreframeworker.ui.PreferencesPage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import soot.G;
import soot.SootClass;
import soot.util.Chain;

public class JimpleUtils {

	public static void disassemble(IJavaProject project, IFile input) throws JavaModelException, IOException {
		
		IFolder jimpleDirectory = project.getProject().getFolder("jimple");
		if(!jimpleDirectory.exists()){
			jimpleDirectory.getLocation().toFile().mkdirs();
		}
		
		StringBuilder classpath = new StringBuilder();
		for(IClasspathEntry entry: project.getRawClasspath()){
			// add all libraries to the classpath except the runtime to dissassemble
			if(!entry.getPath().toFile().getName().equals(input.getLocation().toFile().getName())){
				classpath.append(entry.getPath().toFile().getCanonicalPath());
				classpath.append(File.pathSeparator);
			}
		}

		ArrayList<String> argList = new ArrayList<String>();
		argList.add("-src-prec"); argList.add("class");
		argList.add("--xml-attributes");
		argList.add("-f"); argList.add("jimple");
		argList.add("-cp"); argList.add(classpath.toString());
		if(Activator.getDefault().getPreferenceStore().getBoolean(PreferencesPage.ALLOW_PHANTOM_REFERENCES_BOOLEAN)){
			argList.add("-allow-phantom-refs");
		}
		argList.add("-output-dir"); argList.add(jimpleDirectory.getLocation().toFile().getAbsolutePath());
		argList.add("-process-dir"); argList.add(input.getLocation().toFile().getAbsolutePath());
		argList.add("-include-all");
		String[] sootArgs = argList.toArray(new String[argList.size()]);
		
		try {
			G.reset();
			soot.Main.main(sootArgs);
		} catch (Throwable t) {
			Log.error("An error occurred processing bytecode.\n\nSoot Classpath: " + Arrays.toString(sootArgs), t);
		}
		
		// warn about any phantom references
		Chain<SootClass> phantomClasses = soot.Scene.v().getPhantomClasses();
        if (!phantomClasses.isEmpty()) {
                TreeSet<String> missingClasses = new TreeSet<String>();
                for (SootClass sootClass : phantomClasses) {
                        missingClasses.add(sootClass.toString());
                }
                StringBuilder message = new StringBuilder();
                message.append("Some classes were referenced, but could not be found.\n\n");
                for (String sootClass : missingClasses) {
                        message.append(sootClass);
                        message.append("\n");
                }
                Log.warning(message.toString());
        }
	}
	
	public static void assemble(IJavaProject project, IFile output) throws IOException, JavaModelException{
		
		IFolder jimpleDirectory = project.getProject().getFolder("jimple");
		if(!jimpleDirectory.exists()){
			jimpleDirectory.getLocation().toFile().mkdirs();
		}
		
		StringBuilder classpath = new StringBuilder();
		for(IClasspathEntry entry: project.getRawClasspath()){
			// add all libraries to the classpath except the runtime that was disassembled
			if(!entry.getPath().toFile().getName().equals(output.getLocation().toFile().getName())){
				classpath.append(entry.getPath().toFile().getCanonicalPath());
				classpath.append(File.pathSeparator);
			}
		}
		
		// configure soot arguments
		ArrayList<String> argList = new ArrayList<String>();
		argList.add("-src-prec"); argList.add("jimple");
//		argList.add("--xml-attributes");
		argList.add("-f"); argList.add("class");
		argList.add("-cp"); argList.add(classpath.toString());
		if(Activator.getDefault().getPreferenceStore().getBoolean(PreferencesPage.ALLOW_PHANTOM_REFERENCES_BOOLEAN)){
			argList.add("-allow-phantom-refs");
		}
		argList.add("-output-dir"); argList.add(output.getLocation().toFile().getCanonicalPath()); argList.add("-output-jar");
		argList.add("-process-dir"); argList.add(jimpleDirectory.getLocation().toFile().getCanonicalPath());
		argList.add("-include-all");
		String[] sootArgs = argList.toArray(new String[argList.size()]);

		// run soot to compile jimple
		try {
			G.reset();
			soot.Main.v().run(sootArgs);
		} catch (Throwable t){
			Log.error("An error occurred processing Jimple.\n\nSoot Classpath: " + Arrays.toString(sootArgs), t);
		}

		// warn about any phantom references
		Chain<SootClass> phantomClasses = soot.Scene.v().getPhantomClasses();
        if (!phantomClasses.isEmpty()) {
            TreeSet<String> missingClasses = new TreeSet<String>();
            for (SootClass sootClass : phantomClasses) {
                    missingClasses.add(sootClass.toString());
            }
            StringBuilder message = new StringBuilder();
            message.append("When compiling Jimple, some classes were referenced that could not be found.\n\n");
            for (String sootClass : missingClasses) {
                    message.append(sootClass);
                    message.append("\n");
            }
            Log.warning(message.toString());
        }
	}
	
	// Throwable exception wrapper to make a runtime soot conversion exception checked
	public static class SootConversionException extends Exception {
		private static final long serialVersionUID = 1L;
		public SootConversionException(Throwable t) {
			super(t);
		}
	}
}
