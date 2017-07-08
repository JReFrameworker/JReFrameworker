package jreframeworker.builder;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.JavaCore;
import org.objectweb.asm.tree.ClassNode;

import jreframeworker.core.BuilderUtils;
import jreframeworker.core.IncrementalBuilder;
import jreframeworker.core.IncrementalBuilder.DeltaSource;
import jreframeworker.core.IncrementalBuilder.DeltaSource.Delta;
import jreframeworker.core.JReFrameworkerProject;
import jreframeworker.engine.utils.BytecodeUtils;
import jreframeworker.log.Log;

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
		JReFrameworkerProject jrefProject = getJReFrameworkerProject();
		if(jrefProject != null){
			monitor.beginTask("Cleaning: " + jrefProject.getProject().getName(), 1);
			Log.info("Cleaning: " + jrefProject.getProject().getName());
			
			// clear the Java compiler error markers (these will be fixed and restored if they remain after building phases)
//			jrefProject.getProject().deleteMarkers(JavaCore.ERROR, true, IProject.DEPTH_INFINITE);
			jrefProject.getProject().deleteMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IProject.DEPTH_INFINITE);

			jrefProject.disableJavaBuilder();
			try {
				jrefProject.clean();
				jrefProject.restoreOriginalClasspathEntries(); 
			} catch (Exception e) {
				Log.error("Error cleaning " + jrefProject.getProject().getName(), e);
			}
			jrefProject.enableJavaBuilder();
			
			this.forgetLastBuiltState();
			jrefProject.refresh();
			
			monitor.worked(1);
		} else {
			Log.warning(getProject().getName() + " is not a valid JReFrameworker project!");
		}
	}
	
	protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
		JReFrameworkerProject jrefProject = getJReFrameworkerProject();
		monitor.beginTask("Full Build: " + jrefProject.getProject().getName(), 1);
		Log.info("Full Build: " + jrefProject.getProject().getName());
		
		IncrementalBuilder incrementalBuilder = new IncrementalBuilder(jrefProject);
		
		try {
			boolean compilationRequired = true;
			while(compilationRequired){
				compilationRequired = false;
				
				// run the java builder
				jrefProject.getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
				jrefProject.refresh();
				
				// make sure the build directory exists
				File projectBuildDirectory = jrefProject.getBinaryDirectory();
				if (!projectBuildDirectory.exists()) {
					projectBuildDirectory.mkdirs();
				}

//				// add each class from classes in jars in raw directory
//				// this happens before any build phases
//				File rawDirectory = jrefProject.getProject().getFolder(JReFrameworker.RAW_DIRECTORY).getLocation().toFile();
//				if(rawDirectory.exists()){
//					for(File jarFile : rawDirectory.listFiles()){
//						if(jarFile.getName().endsWith(".jar")){
//							for(Engine engine : allEngines){
//								Log.info("Embedding raw resource: " + jarFile.getName() + " in " + engine.getJarName());
//								
//								// TODO: unjar to temp directory instead?
//								File outputDirectory = new File(jarFile.getParentFile().getAbsolutePath() + File.separatorChar + "." + jarFile.getName().replace(".jar", ""));
//								JarModifier.unjar(jarFile, outputDirectory);
//								
//								// add raw class files
//								addClassFiles(engine, outputDirectory);
//								
//								// cleanup temporary directory
//								delete(outputDirectory);
//							}
//						}
//					}
//				}
				
				// discover class files to process and filter out
				// the compilation units with build errors
				Set<DeltaSource> sourcesToProcess = new HashSet<DeltaSource>();
				ICompilationUnit[] compilationUnits = BuilderUtils.getSourceCompilationUnits(jrefProject.getJavaProject());
				for(ICompilationUnit compilationUnit : compilationUnits){
					try {
						File sourceFile = compilationUnit.getCorrespondingResource().getLocation().toFile().getCanonicalFile();
						File classFile = BuilderUtils.getCorrespondingClassFile(jrefProject, compilationUnit);
						if(classFile.exists()){
							if(BuilderUtils.hasSevereProblems(compilationUnit)){
								compilationRequired = true;
							} else {
								ClassNode classNode = BytecodeUtils.getClassNode(classFile);
								if(BuilderUtils.hasTopLevelAnnotation(classNode)){
									// in a full build all sources are added deltas
									DeltaSource source = new DeltaSource(sourceFile, classNode, Delta.ADDED);
									sourcesToProcess.add(source);
								}
							}
						}
					} catch (IOException e) {
						Log.error("Error resolving compilation units", e);
						return;
					}
				}
				
				// run the JREF incremental builder
				incrementalBuilder.build(sourcesToProcess, monitor);
				jrefProject.refresh();
				
				// remove the java nature to prevent the Java builder from running until we are ready
				// if the build phase directory is null or does not exist then nothing was done during the phase
				int lastBuildPhase = BuilderUtils.getLastBuildPhase(jrefProject);
				File buildPhaseDirectory = BuilderUtils.getBuildPhaseDirectory(jrefProject, lastBuildPhase);
				if(buildPhaseDirectory != null && buildPhaseDirectory.exists()){
					jrefProject.disableJavaBuilder();
					for(File file : buildPhaseDirectory.listFiles()){
						if(file.getName().endsWith(".jar")){
							File modifiedLibrary = file;
							jrefProject.updateProjectLibrary(modifiedLibrary.getName(), modifiedLibrary);
						}
					}
					// restore the java nature
					jrefProject.enableJavaBuilder();
					jrefProject.refresh();
				}
			}
			
			jrefProject.refresh();
		} catch (Throwable t){
			Log.error("Error Building JReFrameworker Project", t);
		}
	}
	
	// TODO: see http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2FresAdv_builders.htm
	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		Log.info("Incremental Build");
		delta.accept(new BuildDeltaVisitor());
	}
	
	private static class BuildDeltaVisitor implements IResourceDeltaVisitor {
		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			File sourceFile = new File(delta.getFullPath().toOSString());
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				System.out.println("Added: " + sourceFile.getName());
				break;
			case IResourceDelta.REMOVED:
				System.out.println("Removed: " + sourceFile.getName());
				break;
			case IResourceDelta.CHANGED:
				System.out.println("Changed: " + sourceFile.getName());
				break;
			}
			return true;
		}
	}
	
	/**
	 * Returns the JReFrameworker project to build or clean, if the project is invalid returns null
	 * @return
	 */
	private JReFrameworkerProject getJReFrameworkerProject(){
		IProject project = getProject();
		try {
			if(project.isOpen() && project.exists() && project.hasNature(JavaCore.NATURE_ID) && project.hasNature(JReFrameworkerNature.NATURE_ID)){
				return new JReFrameworkerProject(project);
			}
		} catch (CoreException e) {}
		return null;
	}
	
//	private void addClassFiles(Engine engine, File f) throws IOException {
//		if (f.isDirectory()){
//			for (File f2 : f.listFiles()){
//				addClassFiles(engine, f2);
//			}
//		} else if(f.getName().endsWith(".class")){
//			engine.addUnprocessed(Files.readAllBytes(f.toPath()), true);
//		}
//	}
	
}
