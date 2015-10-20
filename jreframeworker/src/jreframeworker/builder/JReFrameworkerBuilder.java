package jreframeworker.builder;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import jreframeworker.common.RuntimeUtils;
import jreframeworker.core.JReFrameworker;
import jreframeworker.core.bytecode.identifiers.JREFAnnotationIdentifier;
import jreframeworker.core.bytecode.utils.BytecodeUtils;
import jreframeworker.log.Log;

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
				resetProjectRuntimes(jProject);
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
	private void resetProjectRuntimes(IJavaProject jProject) throws IOException {
		// delete all the runtimes
		for(File runtime : jProject.getProject().getFolder(JReFrameworker.RUNTIMES_DIRECTORY).getLocation().toFile().listFiles()){
			if(runtime.isFile() && runtime.getName().endsWith(".jar")){
				runtime.delete();
				if(runtime.exists()){
					throw new IOException("Could not delete: " + runtime.getAbsolutePath());
				}
			}
		}
		// restore the original runtimes
		for(File originalRuntime : jProject.getProject().getFolder(JReFrameworker.ORIGINAL_RUNTIMES_DIRECTORY).getLocation().toFile().listFiles()){
			if(originalRuntime.isFile() && originalRuntime.getName().endsWith(".jar")){
				File runtime = new File(jProject.getProject().getFolder(JReFrameworker.RUNTIMES_DIRECTORY).getLocation().toFile().getCanonicalPath() 
						+ File.separatorChar + originalRuntime.getName());
				RuntimeUtils.copyFile(originalRuntime, runtime);
			}
		}
	}
	
	protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
		
		IJavaProject jProject = getJReFrameworkerProject();

		if(jProject != null){
			// first clean out the modified runtimes
			try {
				resetProjectRuntimes(jProject);
				// cleaning the build could causing infinite rebuilds if we don't reset the build state
				this.forgetLastBuiltState(); 
			} catch (IOException e) {
				Log.error("Error building " + jProject.getProject().getName() + ", could not purge runtimes.  Project may be in an invalid state.", e);
				return;
			}
			
			// build each jref project fresh
			monitor.beginTask("Building JReFrameworker project: " + jProject.getProject().getName(), 1);
			Log.info("Building JReFrameworker project: " + jProject.getProject().getName());

			File binDirectory = jProject.getProject().getFolder(JReFrameworker.BINARY_DIRECTORY).getLocation().toFile();
			try {
				buildProject(binDirectory, jProject);
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
	private void buildProject(File root, IJavaProject jProject) throws IOException {
		File[] files = root.listFiles();
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
								if(checker.isDefineTypeAnnotation()){
									// TODO: determine if its an insert or a replace
									Log.info("INSERT or REPLACE " + classNode.name + " in project " + jProject.getProject().getName());
								} else if(checker.isMergeTypeAnnotation()){
									// TODO: execute merge
//									Merge.mergeClasses(baseClass, classToMerge, outputClass);
									Log.info("MERGE " + classNode.name + " in project " + jProject.getProject().getName());
								}
							}
						}
					}
				}
			} else if(file.isDirectory()){
				buildProject(file, jProject);
			}
		}
	}

	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		// TODO: implement, how do we get just the changed files? file timestamps maybe?
//		Log.info("Incremental Building...");
		fullBuild(monitor); // for now we are lazy..and just rebuild every time
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
