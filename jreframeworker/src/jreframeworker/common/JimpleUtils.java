package jreframeworker.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

	public static void disassemble(IJavaProject project, IFile input) throws JavaModelException, IOException, SootConversionException {
		
		IFolder jimpleDirectory = project.getProject().getFolder("jimple");
		if(!jimpleDirectory.exists()){
			jimpleDirectory.getLocation().toFile().mkdirs();
		}
		
		G.reset();
		
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
		String[] args = argList.toArray(new String[argList.size()]);
		
		try {
			soot.Main.main(args);

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
		} catch (Throwable t) {
			throw new SootConversionException(t);
		}
	}
	
	public static void assemble(IJavaProject project, IFile output){
		
	}
	
	// Throwable exception wrapper to make a runtime soot conversion exception checked
	public static class SootConversionException extends Exception {
		private static final long serialVersionUID = 1L;
		public SootConversionException(Throwable t) {
			super(t);
		}
	}
}
