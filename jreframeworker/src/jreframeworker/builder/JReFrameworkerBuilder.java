package jreframeworker.builder;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

import jreframeworker.common.JarModifier;
import jreframeworker.common.RuntimeUtils;
import jreframeworker.core.JReFrameworker;
import jreframeworker.core.bytecode.identifiers.JREFAnnotationIdentifier;
import jreframeworker.core.bytecode.operations.Merge;
import jreframeworker.core.bytecode.utils.BytecodeUtils;
import jreframeworker.log.Log;

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
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class JReFrameworkerBuilder extends IncrementalProjectBuilder {
	
	private static int buildNumber = 1;
//	private static HashMap<String,Long> bytecodeTimestamps = new HashMap<String,Long>();
	
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
				resetProjectRuntimes(jProject, true);
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

	// TODO: adding a progress monitor subtask here would be a nice feature
	private void resetProjectRuntimes(IJavaProject jProject, boolean restoreOriginalRuntimes) throws IOException, CoreException {
		
		// TODO: allow modifcations to all jars not just rt.jar
		
		// save and clear the classpath
//		IClasspathEntry[] classpath = jProject.getRawClasspath();
//		jProject.setRawClasspath(new IClasspathEntry[] {}, null);
		
//		// delete all the runtimes
//		for(File runtime : jProject.getProject().getFolder(JReFrameworker.RUNTIMES_DIRECTORY).getLocation().toFile().listFiles()){
//			if(runtime.isFile() && runtime.getName().endsWith(".jar")){
////				runtime.setWritable(true);
////				runtime.delete();
//				FileDeleteStrategy.FORCE.delete(runtime);
//				// cleaning the build could causing infinite rebuilds if we don't reset the build state
//			}
//		}
		
		File runtime = jProject.getProject().getFolder(JReFrameworker.RUNTIMES_DIRECTORY).getFile("rt.jar").getLocation().toFile();
		FileDeleteStrategy.FORCE.delete(runtime);
		
		// if requested restore the original runtimes
		if(restoreOriginalRuntimes){
			
			File originalRuntime = jProject.getProject().getFolder(JReFrameworker.ORIGINAL_RUNTIMES_DIRECTORY).getFile("rt.jar").getLocation().toFile();
			RuntimeUtils.copyFile(originalRuntime, runtime);
			
//			// restore each runtime
//			for(File originalRuntime : jProject.getProject().getFolder(JReFrameworker.ORIGINAL_RUNTIMES_DIRECTORY).getLocation().toFile().listFiles()){
//				if(originalRuntime.isFile() && originalRuntime.getName().endsWith(".jar")){
//					File runtime = new File(jProject.getProject().getFolder(JReFrameworker.RUNTIMES_DIRECTORY).getLocation().toFile().getCanonicalPath() 
//							+ File.separatorChar + originalRuntime.getName());
//					RuntimeUtils.copyFile(originalRuntime, runtime);
//					// cleaning the build could causing infinite rebuilds if we don't reset the build state
//				}
//			}
		}
		
		// restore the classpath
//		jProject.setRawClasspath(classpath, null);
	}
	
	protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
		
		Log.info("Build Number: " + buildNumber++);
		
		IJavaProject jProject = getJReFrameworkerProject();
		if(jProject != null){
			// first clean out the modified runtimes
			try {
				resetProjectRuntimes(jProject, false);
			} catch (IOException e) {
				Log.error("Error building " + jProject.getProject().getName() + ", could not purge runtimes.  Project may be in an invalid state.", e);
				return;
			}
			
			// build each jref project fresh
			monitor.beginTask("Building JReFrameworker project: " + jProject.getProject().getName(), 1);
			Log.info("Building JReFrameworker project: " + jProject.getProject().getName());

			File binDirectory = jProject.getProject().getFolder(JReFrameworker.BINARY_DIRECTORY).getLocation().toFile();
			try {
				// for each runtime, make modifications that are found in the compiled bytecode
				for(File originalRuntime : getOriginalRuntimes(jProject)){
					JarModifier runtimeModifications = new JarModifier(originalRuntime);
					buildProject(binDirectory, jProject, runtimeModifications);
					File modifiedRuntime = getCorrespondingModifiedRuntime(jProject, originalRuntime);
					runtimeModifications.save(modifiedRuntime);
				}
			} catch (IOException e) {
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

	// TODO: adding a progress monitor subtask here would be a nice feature
	private void buildProject(File binDirectory, IJavaProject jProject, JarModifier runtimeModifications) throws IOException {
		// make changes for each annotated class file in current directory
		File[] files = binDirectory.listFiles();
		for(File file : files){
			if(file.isFile()){
				if(file.getName().endsWith(".class")){
					// check to see if the class is annotated with 
					ClassNode classNode = BytecodeUtils.getClassNode(file);
					// TODO: address innerclasses, classNode.innerClasses, could these even be found from class files? they would be different files...
					if(classNode.invisibleAnnotations != null){
						for(Object o : classNode.invisibleAnnotations){
							AnnotationNode annotationNode = (AnnotationNode) o;
							JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
							checker.visitAnnotation(annotationNode.desc, false);
							if(checker.isJREFAnnotation()){
								String qualifiedClassName = classNode.name + ".class";
								if(checker.isDefineTypeAnnotation()){
									if(runtimeModifications.getJarEntrySet().contains(qualifiedClassName)){
										runtimeModifications.add(qualifiedClassName, file, true);
										Log.info("Replaced: " + qualifiedClassName + " in " + runtimeModifications.getJarFile().getName());
									} else {
										runtimeModifications.add(qualifiedClassName, file, false);
										Log.info("Inserted: " + qualifiedClassName + " into " + runtimeModifications.getJarFile().getName());
									}
								} else if(checker.isMergeTypeAnnotation()){
									File outputClass = File.createTempFile(classNode.name, ".class");
									File baseClass = File.createTempFile(classNode.name, ".class");
									String qualifiedParentClassName = classNode.superName + ".class";
									runtimeModifications.extractEntry(qualifiedParentClassName, baseClass);
									Merge.mergeClasses(baseClass, file, outputClass);
									baseClass.delete();
									runtimeModifications.add(qualifiedParentClassName, outputClass, true);
									// TODO: clean up outputClass somehow,
									// probably need to make a local temp
									// directory which gets deleted at the end
									// of the build
									Log.info("Merged: " + qualifiedClassName + " into " + qualifiedParentClassName + " in " + runtimeModifications.getJarFile().getName());
								}
							}
						}
					}
				}
			} else if(file.isDirectory()){
				buildProject(file, jProject, runtimeModifications);
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
	 * Helper method for getting a list of the original runtimes
	 * @param jProject
	 * @return
	 * @throws IOException
	 */
	private LinkedList<File> getOriginalRuntimes(IJavaProject jProject) throws IOException {
		LinkedList<File> runtimes = new LinkedList<File>();
		for(File runtime : jProject.getProject().getFolder(JReFrameworker.ORIGINAL_RUNTIMES_DIRECTORY).getLocation().toFile().listFiles()){
			if(runtime.isFile() && runtime.getName().endsWith(".jar")){
				
				if(runtime.getName().equals("rt.jar")) // TODO: remove this conditional to consider all jars, not just rt.jar
					runtimes.add(runtime.getCanonicalFile());
			}
		}
		return runtimes;
	}
	
	/**
	 * Helper method for getting a File object that points to the corresponding modified runtime location
	 * Note: The file target may not exist yet
	 * @param jProject
	 * @param originalRuntime
	 * @return
	 * @throws IOException
	 */
	private File getCorrespondingModifiedRuntime(IJavaProject jProject, File originalRuntime) throws IOException {
		File runtimesDirectory = jProject.getProject().getFolder(JReFrameworker.RUNTIMES_DIRECTORY).getLocation().toFile();
		return new File(runtimesDirectory.getCanonicalPath() + File.separatorChar + originalRuntime.getName());
	}
	
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
	
}
