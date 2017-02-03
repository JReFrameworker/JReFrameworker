package jreframeworker.builder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileDeleteStrategy;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.xml.sax.SAXException;

import jreframeworker.core.JReFrameworker;
import jreframeworker.engine.Engine;
import jreframeworker.engine.utils.JarModifier;
import jreframeworker.log.Log;
import jreframeworker.ui.PreferencesPage;

public class JReFrameworkerBuilder extends IncrementalProjectBuilder {
	
	private static int buildNumber = 1;
	
	public static final String BUILDER_ID = "jreframeworker.JReFrameworkerBuilder";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@SuppressWarnings("rawtypes")
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	protected void clean(IProgressMonitor monitor) throws CoreException {
		IJavaProject jProject = getJReFrameworkerProject();
		if(jProject != null){
			monitor.beginTask("Cleaning JReFrameworker project: " + jProject.getProject().getName(), 1);
			Log.info("Cleaning JReFrameworker project: " + jProject.getProject().getName());
			try {
				File runtimesDirectory = jProject.getProject().getFolder(JReFrameworker.RUNTIMES_DIRECTORY).getLocation().toFile();
				File configFile = jProject.getProject().getFile(JReFrameworker.RUNTIMES_CONFIG).getLocation().toFile();
				configFile.delete();
				clearProjectRuntimes(jProject, runtimesDirectory);
				this.forgetLastBuiltState(); 
			} catch (IOException e) {
				Log.error("Error cleaning " + jProject.getProject().getName(), e);
			}
			jProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			monitor.worked(1);
			
			Log.info("Finished cleaning JReFrameworker project: " + jProject.getProject().getName());
		} else {
			Log.warning(getProject().getName() + " is not a valid JReFrameworker project!");
		}
	}

	private void clearProjectRuntimes(IJavaProject jProject, File runtimesDirectory) throws IOException {
		for(File runtime : runtimesDirectory.listFiles()){
			FileDeleteStrategy.FORCE.delete(runtime);
		}
	}
	
	protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
		
		Log.info("JReFrameworker Build Number: " + buildNumber++);
		
		IJavaProject jProject = getJReFrameworkerProject();	
		if(jProject != null){
			// make sure the runtimes directory exists
			File runtimesDirectory = jProject.getProject().getFolder(JReFrameworker.RUNTIMES_DIRECTORY).getLocation().toFile();
			if(!runtimesDirectory.exists()){
				runtimesDirectory.mkdirs();
			}
			// first delete the modified runtime
			try {
				clearProjectRuntimes(jProject, runtimesDirectory);
			} catch (IOException e) {
				Log.error("Error building " + jProject.getProject().getName() + ", could not purge runtimes.  Project may be in an invalid state.", e);
				return;
			}
			
			// build each jref project fresh
			monitor.beginTask("Building JReFrameworker project: " + jProject.getProject().getName(), 1);
			Log.info("Building JReFrameworker project: " + jProject.getProject().getName());

			// write config file
			File configFile = jProject.getProject().getFile(JReFrameworker.RUNTIMES_CONFIG).getLocation().toFile();
			FileWriter config;
			try {
				config = new FileWriter(configFile);
				config.write("merge-rename-prefix," + PreferencesPage.getMergeRenamingPrefix());
			} catch (IOException e) {
				Log.error("Could not write to config file.", e);
				return;
			}
			
			File applicationDirectory = new File(jProject.getProject().getLocation().toFile().getAbsolutePath() + File.separator + JReFrameworker.APPLICATION_DIRECTORY);
			applicationDirectory.mkdirs();
			File binDirectory = jProject.getProject().getFolder(JReFrameworker.BINARY_DIRECTORY).getLocation().toFile();
			try {
				String targetJar = JReFrameworker.getTargetJar(jProject.getProject());
				boolean isTargetJarRuntime = JReFrameworker.isTargetJarRuntime(jProject.getProject());
				
				// make modifications that are defined in the project to the compiled bytecode
				if(isTargetJarRuntime){
					// runtime project
					File originalJar = getOriginalRuntime(targetJar);
					if(originalJar.exists()){
						Engine engine = new Engine(originalJar, PreferencesPage.getMergeRenamingPrefix());
						buildProject(binDirectory, jProject, engine, config);
						config.close();
						File modifiedRuntime = new File(runtimesDirectory.getCanonicalPath() + File.separatorChar + targetJar);

						// add the class files from libraries
						// TODO: cross-reference with jar dependencies (don't add unreferenced libraries)
						File libDirectory = jProject.getProject().getFolder(JReFrameworker.LIBRARY_DIRECTORY).getLocation().toFile();
						if(libDirectory.exists()){
							for(File jarFile : libDirectory.listFiles()){
								if(jarFile.getName().endsWith(".jar")){
									Log.info("Embedding library: " + jarFile.getName());
									
									File outputDirectory = new File(jarFile.getParentFile().getAbsolutePath() + File.separatorChar + "." + jarFile.getName().replace(".jar", ""));
									JarModifier.unjar(jarFile, outputDirectory);
									
									addClassFiles(engine, outputDirectory);
									
									// cleanup
									delete(outputDirectory);
								}
							}
						}
						
						engine.save(modifiedRuntime);
						Log.info("Modified Runtime: " + modifiedRuntime.getCanonicalPath());
					} else {
						Log.warning("Jar not found: " + originalJar.getAbsolutePath());
					}
				} else {
					// application project
					File originalJar = jProject.getProject().getFile(targetJar).getLocation().toFile();
					if(originalJar.exists()){
						// TODO: add custom class loader for application jars
//						String pathToJar = "/Users/benjholla/Desktop/JReFrameworker/neon/runtime-EclipseApplication/AirPlan1Logger/airplan_1.jar";
//						URL[] urls = { new URL("jar:file:" + pathToJar+"!/") };
//						classLoader = URLClassLoader.newInstance(urls);
						Engine engine = new Engine(originalJar, PreferencesPage.getMergeRenamingPrefix());
						buildProject(binDirectory, jProject, engine, config);
						config.close();
						File modifiedApplication = new File(applicationDirectory.getCanonicalPath() + File.separatorChar + targetJar);
						engine.save(modifiedApplication);
						Log.info("Modified Application: " + modifiedApplication.getCanonicalPath());
					} else {
						Log.warning("Jar not found: " + originalJar.getAbsolutePath());
					}
				}
			} catch (IOException | SAXException | ParserConfigurationException e) {
				Log.error("Error building " + jProject.getProject().getName(), e);
				return;
			}
			
			jProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			monitor.worked(1);
			
			Log.info("Finished building JReFrameworker project: " + jProject.getProject().getName());
		} else {
			Log.warning(getProject().getName() + " is not a valid JReFrameworker project!");
		}
	}
	
	private void addClassFiles(Engine engine, File f) throws IOException {
		if (f.isDirectory()){
			for (File f2 : f.listFiles()){
				addClassFiles(engine, f2);
			}
		} else if(f.getName().endsWith(".class")){
			engine.addUnprocessed(Files.readAllBytes(f.toPath()), true);
		}
	}

	private File getOriginalRuntime(String targetJar) throws IOException {
		IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
		LibraryLocation[] locations = JavaRuntime.getLibraryLocations(vmInstall);
		for (LibraryLocation library : locations) {
			File runtime = JavaCore.newLibraryEntry(library.getSystemLibraryPath(), null, null).getPath().toFile().getCanonicalFile();
			if(runtime.getName().equals(targetJar)){
				return runtime;
			}
		}
		return null;
	}

	// TODO: adding a progress monitor subtask here would be a nice feature
	private void buildProject(File binDirectory, IJavaProject jProject, Engine engine, FileWriter config) throws IOException {
		// make changes for each annotated class file in current directory
		File[] files = binDirectory.listFiles();
		for(File file : files){
			if(file.isFile()){
				if(file.getName().endsWith(".class")){
					byte[] bytes = Files.readAllBytes(file.toPath());
					if(bytes.length > 0 && engine.process(bytes)){
						String base = jProject.getProject().getFolder(JReFrameworker.BINARY_DIRECTORY).getLocation().toFile().getCanonicalPath();
						String entry = file.getCanonicalPath().substring(base.length());
						if(entry.charAt(0) == File.separatorChar){
							entry = entry.substring(1);
						}
						entry = entry.replace(".class", "");
						config.write("\nclass," + entry);
						config.flush();
					}
				}
			} else if(file.isDirectory()){
				buildProject(file, jProject, engine, config);
			}
		}
	}

	// TODO: see http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2FresAdv_builders.htm
	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {

		// for now just doing nothing, incremental build is fighting with full build and can't delete rt.jar for some reason
		
//		Log.info("Incremental Build");
		
		// TODO: implement an incremental build
//		delta.accept(new BuildDeltaVisitor());
		
//		// triggers a full rebuild
//		this.forgetLastBuiltState();
//		this.needRebuild();
		
		// directly triggers a full rebuild
//		fullBuild(monitor);
	}
	
//	private static class BuildVisitor implements IResourceVisitor {
//		@Override
//		public boolean visit(IResource resource) {
//			System.out.println(resource.getLocation().toFile().getAbsolutePath());
//			return true;
//		}
//	}
//	
//	private static class BuildDeltaVisitor implements IResourceDeltaVisitor {
//		@Override
//		public boolean visit(IResourceDelta delta) throws CoreException {
//			switch (delta.getKind()) {
//			case IResourceDelta.ADDED:
//				System.out.print("Added: ");
//				break;
//			case IResourceDelta.REMOVED:
//				System.out.print("Removed: ");
//				break;
//			case IResourceDelta.CHANGED:
//				System.out.print("Changed: ");
//				break;
//			}
//			System.out.println(delta.getFullPath().toOSString());
//			return true;
//		}
//	}
	
	/**
	 * Returns the JReFrameworker project to build or clean, if the project is invalid returns null
	 * @return
	 */
	private IJavaProject getJReFrameworkerProject(){
		IProject project = getProject();
		try {
			if(project.isOpen() && project.hasNature(JavaCore.NATURE_ID) && project.hasNature(JReFrameworkerNature.NATURE_ID)){
				IJavaProject jProject = JavaCore.create(project);
				if(jProject.exists()){
					return jProject;
				}
			}
		} catch (CoreException e) {}
		return null;
	}
	
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
	
}
