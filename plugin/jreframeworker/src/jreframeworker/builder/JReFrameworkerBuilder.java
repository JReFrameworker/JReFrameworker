package jreframeworker.builder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileDeleteStrategy;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.xml.sax.SAXException;

import jreframeworker.core.JReFrameworker;
import jreframeworker.engine.Engine;
import jreframeworker.engine.identifiers.JREFAnnotationIdentifier;
import jreframeworker.engine.identifiers.JREFAnnotationIdentifier.MergeTypeAnnotation;
import jreframeworker.engine.utils.BytecodeUtils;
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
				File buildDirectory = jProject.getProject().getFolder(JReFrameworker.BUILD_DIRECTORY).getLocation().toFile();
				File configFile = jProject.getProject().getFile(JReFrameworker.BUILD_CONFIG).getLocation().toFile();
				configFile.delete();
				clearProjectBuildDirectory(jProject, buildDirectory);
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

	private void clearProjectBuildDirectory(IJavaProject jProject, File projectBuildDirectory) throws IOException {
		for(File runtime : projectBuildDirectory.listFiles()){
			FileDeleteStrategy.FORCE.delete(runtime);
		}
	}
	
	protected void fullBuild(final IProgressMonitor monitor) throws CoreException {

		Log.info("JReFrameworker Build Number: " + buildNumber++);

		IJavaProject jProject = getJReFrameworkerProject();
		if (jProject != null) {
			// make sure the build directory exists
			File projectBuildDirectory = jProject.getProject().getFolder(JReFrameworker.BUILD_DIRECTORY).getLocation().toFile();
			if (!projectBuildDirectory.exists()) {
				projectBuildDirectory.mkdirs();
			}
			// first delete the modified runtime
			try {
				clearProjectBuildDirectory(jProject, projectBuildDirectory);
			} catch (IOException e) {
				Log.error("Error building " + jProject.getProject().getName()
						+ ", could not purge runtimes.  Project may be in an invalid state.", e);
				return;
			}

			// build each jref project fresh
			monitor.beginTask("Building JReFrameworker project: " + jProject.getProject().getName(), 1);
			Log.info("Building JReFrameworker project [" + jProject.getProject().getName() + "]");

			
			File binDirectory = jProject.getProject().getFolder(JReFrameworker.BINARY_DIRECTORY).getLocation().toFile();
			try {
				// map class entries to and initial modification engine sets
				Map<String, Set<Engine>> engineMap = new HashMap<String, Set<Engine>>();
				Set<Engine> allEngines = new HashSet<Engine>();
				for (String targetJar : JReFrameworker.getTargetJars(jProject.getProject())) {
					File originalJar = getOriginalJar(targetJar, jProject);
					if (originalJar.exists()) {
						Engine engine = new Engine(originalJar, PreferencesPage.getMergeRenamingPrefix());
						allEngines.add(engine);
						for(String entry : engine.getOriginalEntries()){
							entry = entry.replace(".class", "");
							if(engineMap.containsKey(entry)){
								engineMap.get(entry).add(engine);
							} else {
								Set<Engine> engines = new HashSet<Engine>();
								engines.add(engine);
								engineMap.put(entry, engines);
							}
						}
					} else {
						Log.warning("Jar not found: " + originalJar.getAbsolutePath());
					}
				}
				
				// write config file
				File configFile = jProject.getProject().getFile(JReFrameworker.BUILD_CONFIG).getLocation().toFile();
				FileWriter config;
				try {
					config = new FileWriter(configFile);
					config.write("merge-rename-prefix," + PreferencesPage.getMergeRenamingPrefix());
				} catch (IOException e) {
					Log.error("Could not write to config file.", e);
					return;
				}
				
				// add each class from classes in jars in raw directory
				File rawDirectory = jProject.getProject().getFolder(JReFrameworker.RAW_DIRECTORY).getLocation().toFile();
				if(rawDirectory.exists()){
					for(File jarFile : rawDirectory.listFiles()){
						if(jarFile.getName().endsWith(".jar")){
							for(Engine engine : allEngines){
								Log.info("Embedding raw resource: " + jarFile.getName() + " in " + engine.getJarName());
								
								// TODO: unjar to temp directory instead?
								File outputDirectory = new File(jarFile.getParentFile().getAbsolutePath() + File.separatorChar + "." + jarFile.getName().replace(".jar", ""));
								JarModifier.unjar(jarFile, outputDirectory);
								
								// add raw class files
								addClassFiles(engine, outputDirectory);
								
								// cleanup temporary directory
								delete(outputDirectory);
							}
						}
					}
				}
				
				// compute the source based jar modifications
				buildProject(binDirectory, jProject, engineMap, allEngines, config);
				config.close();
				
				// write out the modified jars
				for(Engine engine : allEngines){
					File modifiedRuntime = new File(projectBuildDirectory.getCanonicalPath() + File.separatorChar + engine.getJarName());
					engine.save(modifiedRuntime);
					
					// log the modified runtime
					String base = jProject.getProject().getLocation().toFile().getCanonicalPath();
					String relativeFilePath = modifiedRuntime.getCanonicalPath().substring(base.length());
					if(relativeFilePath.charAt(0) == File.separatorChar){
						relativeFilePath = relativeFilePath.substring(1);
					}
					Log.info("Modified: " + relativeFilePath);
				}
				
			} catch (IOException | SAXException | ParserConfigurationException e) {
				Log.error("Error building " + jProject.getProject().getName(), e);
				return;
			}

			jProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			monitor.worked(1);

			Log.info("Finished building JReFrameworker project [" + jProject.getProject().getName() + "]");
		} else {
			Log.warning(getProject().getName() + " is not a valid JReFrameworker project!");
		}
	}

	// TODO: adding a progress monitor subtask here would be a nice feature
	private void buildProject(File binDirectory, IJavaProject jProject, Map<String, Set<Engine>> engineMap, Set<Engine> allEngines, FileWriter config) throws IOException {
		// make changes for each annotated class file in current directory
		File[] files = binDirectory.listFiles();
		for(File file : files){
			if(file.isFile()){
				if(file.getName().endsWith(".class")){
					byte[] classBytes = Files.readAllBytes(file.toPath());
					if(classBytes.length > 0){
						ClassNode classNode = BytecodeUtils.getClassNode(classBytes);
						boolean mergeModification = isMergeTypeModification(classNode);
						boolean defineModification = isDefineTypeModification(classNode);
						
						if(mergeModification || defineModification){
							// get the qualified modification class name
							String base = jProject.getProject().getFolder(JReFrameworker.BINARY_DIRECTORY).getLocation().toFile().getCanonicalPath();
							String modificationClassName = file.getCanonicalPath().substring(base.length());
							if(modificationClassName.charAt(0) == File.separatorChar){
								modificationClassName = modificationClassName.substring(1);
							}
							modificationClassName = modificationClassName.replace(".class", "");
							
							if(mergeModification){
								String mergeTarget = getMergeTarget(classNode);
								// merge into each target jar that contains the merge target
								boolean modified = false;
								if(engineMap.containsKey(mergeTarget)){
									for(Engine engine : engineMap.get(mergeTarget)){
										if(isRuntimeJar(engine.getJarName())){
											engine.setClassLoaders(new ClassLoader[]{ getClass().getClassLoader() });
										} else {
											URL[] jarURL = { new URL("jar:file:" + engine.getOriginalJar().getCanonicalPath() + "!/") };
											engine.setClassLoaders(new ClassLoader[]{ getClass().getClassLoader(), URLClassLoader.newInstance(jarURL) });
										}
										if(engine.process(classBytes)){
											modified = true;
										}
									}
									if(modified){
										config.write("\nclass," + modificationClassName);
										config.flush();
									}
								} else {
									Log.warning("Class entry [" + mergeTarget + "] could not be found in any of the target jars.");
								}
							} else if(defineModification){
								// define or replace in every target jar
								boolean modified = false;
								for(Engine engine : allEngines){
									if(isRuntimeJar(engine.getJarName())){
										engine.setClassLoaders(new ClassLoader[]{ getClass().getClassLoader() });
									} else {
										URL[] jarURL = { new URL("jar:file:" + engine.getOriginalJar().getCanonicalPath() + "!/") };
										engine.setClassLoaders(new ClassLoader[]{ getClass().getClassLoader(), URLClassLoader.newInstance(jarURL) });
									}
									if(engine.process(classBytes)){
										modified = true;
									}
								}
								if(modified){
									config.write("\nclass," + modificationClassName);
									config.flush();
								}
							}
						}
					}
				}
			} else if(file.isDirectory()){
				buildProject(file, jProject, engineMap, allEngines, config);
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
	
	private void addClassFiles(Engine engine, File f) throws IOException {
		if (f.isDirectory()){
			for (File f2 : f.listFiles()){
				addClassFiles(engine, f2);
			}
		} else if(f.getName().endsWith(".class")){
			engine.addUnprocessed(Files.readAllBytes(f.toPath()), true);
		}
	}
	
	private File getOriginalJar(String targetJar, IJavaProject jProject) throws IOException, JavaModelException {
		for(IClasspathEntry classpathEntry : jProject.getRawClasspath()){
			if(classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY){
				File jar = classpathEntry.getPath().toFile().getCanonicalFile();
				if(jar.getName().equals(targetJar)){
					return jar;
				}
			}
		}
		return getOriginalRuntimeJar(targetJar);
	}

	private boolean isRuntimeJar(String jar) throws IOException {
		IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
		LibraryLocation[] locations = JavaRuntime.getLibraryLocations(vmInstall);
		for (LibraryLocation library : locations) {
			File runtime = JavaCore.newLibraryEntry(library.getSystemLibraryPath(), null, null).getPath().toFile().getCanonicalFile();
			if(runtime.getName().equals(jar)){
				return true;
			}
		}
		return false;
	}
	
	private File getOriginalRuntimeJar(String targetJar) throws IOException {
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
	
	private static boolean isMergeTypeModification(ClassNode classNode) throws IOException {
		// TODO: address innerclasses, classNode.innerClasses, could these even be found from class files? they would be different files...
		if(classNode.invisibleAnnotations != null){
			for(Object annotationObject : classNode.invisibleAnnotations){
				AnnotationNode annotationNode = (AnnotationNode) annotationObject;
				JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
				checker.visitAnnotation(annotationNode.desc, false);
				if(checker.isMergeTypeAnnotation()){
					return true;
				}
			}
		}
		return false;
	}
	
	private static String getMergeTarget(ClassNode classNode) throws IOException {
		// TODO: address innerclasses, classNode.innerClasses, could these even be found from class files? they would be different files...
		if(classNode.invisibleAnnotations != null){
			for(Object annotationObject : classNode.invisibleAnnotations){
				AnnotationNode annotationNode = (AnnotationNode) annotationObject;
				MergeTypeAnnotation mergeTypeAnnotation = JREFAnnotationIdentifier.getMergeTypeAnnotation(classNode, annotationNode);
				return mergeTypeAnnotation.getSupertype();
			}
		}
		return null;
	}
	
	private static boolean isDefineTypeModification(ClassNode classNode) throws IOException {
		// TODO: address innerclasses, classNode.innerClasses, could these even be found from class files? they would be different files...
		if(classNode.invisibleAnnotations != null){
			for(Object annotationObject : classNode.invisibleAnnotations){
				AnnotationNode annotationNode = (AnnotationNode) annotationObject;
				JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
				checker.visitAnnotation(annotationNode.desc, false);
				if(checker.isDefineTypeAnnotation()){
					return true;
				}
			}
		}
		return false;
	}
}
