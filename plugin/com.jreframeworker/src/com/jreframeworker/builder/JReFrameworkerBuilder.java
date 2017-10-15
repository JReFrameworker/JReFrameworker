package com.jreframeworker.builder;

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

import com.jreframeworker.core.BuildFile;
import com.jreframeworker.core.BuilderUtils;
import com.jreframeworker.core.IncrementalBuilder;
import com.jreframeworker.core.JReFrameworkerProject;
import com.jreframeworker.core.IncrementalBuilder.DeltaSource;
import com.jreframeworker.core.IncrementalBuilder.IncrementalBuilderException;
import com.jreframeworker.core.IncrementalBuilder.DeltaSource.Delta;
import com.jreframeworker.engine.utils.BytecodeUtils;
import com.jreframeworker.log.Log;
import com.jreframeworker.preferences.JReFrameworkerPreferences;

public class JReFrameworkerBuilder extends IncrementalProjectBuilder {
	
	public static final String BUILDER_ID = "com.jreframeworker.JReFrameworkerBuilder";

	private IncrementalBuilder incrementalBuilder;
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@SuppressWarnings("rawtypes")
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		if(incrementalBuilder == null){
			JReFrameworkerProject jrefProject = getJReFrameworkerProject();
			if(jrefProject != null){
				incrementalBuilder = new IncrementalBuilder(getJReFrameworkerProject());
			} else {
				Log.warning(getProject().getName() + " is not a valid JReFrameworker project!");
				return null;
			}
		}
		
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
		// reset the incremental builder and purge files and build state from the project
		JReFrameworkerProject jrefProject = getJReFrameworkerProject();
		if(jrefProject != null){
			monitor.beginTask("Cleaning: " + jrefProject.getProject().getName(), 1);
			if(JReFrameworkerPreferences.isVerboseLoggingEnabled()) Log.info("Cleaning: " + jrefProject.getProject().getName());
			
			incrementalBuilder = new IncrementalBuilder(jrefProject);
			
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
		if(JReFrameworkerPreferences.isVerboseLoggingEnabled()) Log.info("Full Build: " + jrefProject.getProject().getName());

		try {
			// discover class files to process and filter out
			// the compilation units with build errors
			Set<DeltaSource> sourcesToProcess = new HashSet<DeltaSource>();
			ICompilationUnit[] compilationUnits = BuilderUtils.getSourceCompilationUnits(jrefProject.getJavaProject());
			for(ICompilationUnit compilationUnit : compilationUnits){
				try {
					File sourceFile = compilationUnit.getCorrespondingResource().getLocation().toFile().getCanonicalFile();
					File classFile = BuilderUtils.getCorrespondingClassFile(jrefProject, sourceFile);
					if(classFile.exists()){
						if(!BuilderUtils.hasSevereProblems(compilationUnit)){
							try {
								ClassNode classNode = BytecodeUtils.getClassNode(classFile);
								if(BuilderUtils.hasTopLevelAnnotation(classNode)){
									// in a full build all sources are added deltas
									DeltaSource javaSource = new DeltaSource(sourceFile, sourceFile, classNode, Delta.ADDED);
									sourcesToProcess.add(javaSource);
									DeltaSource classSource = new DeltaSource(classFile, sourceFile, classNode, Delta.ADDED);
									sourcesToProcess.add(classSource);
								}
							} catch (Exception e){
								if(e.getMessage() != null && (e.getMessage().toUpperCase().contains("VIRUS") || e.getMessage().toUpperCase().contains("MALWARE"))){
									Log.warning("Ignoring class [" + classFile.getName() + "] because it was detected as malware by host machine and could not be accessed.");
								} else {
									throw e;
								}
							}
						}
					}
				} catch (IOException e) {
					throw new RuntimeException("Error resolving compilation units", e);
				}
			}
			
			// build the project
			if(!sourcesToProcess.isEmpty()){
				incrementalBuilder.build(sourcesToProcess, monitor);
				updateBuildClasspath(jrefProject);
			}
		} catch (Exception e) {
			Log.error("Error Building JReFrameworker Project", e);
		}
	}
	
	/**
	 * Incrementally builds the project given a set of file changes
	 * 
	 * Reference: http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2FresAdv_builders.htm
	 * 
	 * @param delta
	 * @param monitor
	 * @throws CoreException
	 */
	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		if(JReFrameworkerPreferences.isVerboseLoggingEnabled()) Log.info("Incremental Build");
		JReFrameworkerProject jrefProject = incrementalBuilder.getJReFrameworkerProject();
		BuildDeltaVisitor deltaVisitor = new BuildDeltaVisitor(jrefProject);
		delta.accept(deltaVisitor);
		if(!deltaVisitor.getDeltaBuildFilesToProcess().isEmpty()){
			// TODO: this could be improved to just detect changes to individual libraries...but too much work for now...
			// changes to the build file require a full build
			clean(monitor);
		} else {
			// process incremental changes
			// changes could be to source code, which will change the class files
			// changes could also be to class files only (if a build error was resolved but the source was not touched)
			Set<DeltaSource> sourceDeltas = deltaVisitor.getDeltaSourcesToProcess();
			if(!sourceDeltas.isEmpty()){
				try {
					IncrementalBuilder.PostBuildAction postBuildAction = incrementalBuilder.build(sourceDeltas, monitor);
					if(postBuildAction == IncrementalBuilder.PostBuildAction.UPDATE_CLASSPATH){
						updateBuildClasspath(jrefProject);
					} else if(postBuildAction == IncrementalBuilder.PostBuildAction.CLEAN_REBUILD) {
						if(JReFrameworkerPreferences.isVerboseLoggingEnabled()){
							Log.info("Rebuilding");
						}
						clean(monitor);
					}
				} catch (IncrementalBuilderException e) {
					Log.error("Error incrementally building JReFrameworker project", e);
				}
			}
		}
		if(JReFrameworkerPreferences.isVerboseLoggingEnabled()){
			Log.info("Incremental build complete");
		}
	}

	private void updateBuildClasspath(JReFrameworkerProject jrefProject) throws CoreException {
		// make sure we are working with the latest files
		jrefProject.refresh();
		
		// remove the java nature to prevent the Java builder from running until we are ready
		// if the build phase directory is null or does not exist then nothing was done during the phase
		File buildDirectory = jrefProject.getBuildDirectory();
		if(buildDirectory.exists()){
			jrefProject.disableJavaBuilder();
			for(File file : buildDirectory.listFiles()){
				if(file.getName().endsWith(".jar")){
					File modifiedLibrary = file;
					try {
						jrefProject.updateProjectLibrary(modifiedLibrary.getName(), modifiedLibrary);
					} catch (IOException e) {
						Log.warning("Unable to update project classpath", e);
					}
				}
			}
			// restore the java nature
			jrefProject.enableJavaBuilder();
			jrefProject.refresh();
		}
		
		if(JReFrameworkerPreferences.isVerboseLoggingEnabled()) {
			Log.info("Updated classpath");
		}
	}
	
	private static class BuildDeltaVisitor implements IResourceDeltaVisitor {
		
		private static class DeltaBuildFile {
			private File buildFile;
			private IResourceDelta delta;

			public DeltaBuildFile(File buildFile, IResourceDelta delta) {
				this.buildFile = buildFile;
				this.delta = delta;
			}
			
			@SuppressWarnings("unused")
			public File getBuildFile(){
				return buildFile;
			}
			
			@SuppressWarnings("unused")
			public IResourceDelta getDelta(){
				return delta;
			}
		}
		
		private JReFrameworkerProject jrefProject;
		private Set<DeltaSource> deltaSourcesToProcess = new HashSet<DeltaSource>();
		private Set<DeltaBuildFile> buildFilesToProcess = new HashSet<DeltaBuildFile>();
		private Set<File> resolvedFiles = new HashSet<File>();
		
		public BuildDeltaVisitor(JReFrameworkerProject jrefProject){
			this.jrefProject = jrefProject;
			ICompilationUnit[] compilationUnits = BuilderUtils.getSourceCompilationUnits(jrefProject.getJavaProject());
			for(ICompilationUnit compilationUnit : compilationUnits){
				try {
					File sourceFile = compilationUnit.getCorrespondingResource().getLocation().toFile().getCanonicalFile();
					File classFile = BuilderUtils.getCorrespondingClassFile(jrefProject, sourceFile);
					if(classFile.exists()){
						if(!BuilderUtils.hasSevereProblems(compilationUnit)){
							try {
								ClassNode classNode = BytecodeUtils.getClassNode(classFile);
								if(BuilderUtils.hasTopLevelAnnotation(classNode)){
									resolvedFiles.add(sourceFile);
									resolvedFiles.add(classFile);
								}
							} catch (Exception e){
								checkAntivirusInterference(classFile, e);
							}
						}
					}
				} catch (Exception e) {
					throw new IllegalArgumentException("Error resolving compilation units", e);
				}
			}
		}
		
		public Set<DeltaSource> getDeltaSourcesToProcess(){
			return deltaSourcesToProcess;
		}

		public Set<DeltaBuildFile> getDeltaBuildFilesToProcess(){
			return buildFilesToProcess;
		}
		
		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			try {
				String relativeResourcePath = delta.getResource().getProjectRelativePath().toOSString();
				// note: the relative project path itself would be an empty string relative to the project
				if(!relativeResourcePath.isEmpty()){ 
					String resourcePath = jrefProject.getProject().getLocation().toFile().getCanonicalPath() + File.separator + relativeResourcePath;
					File resource = new File(resourcePath);
					if((resource.exists() && resource.isFile()) || delta.getKind() == IResourceDelta.REMOVED){
						if(resource.getName().equals(BuildFile.XML_BUILD_FILENAME)){
							buildFilesToProcess.add(new DeltaBuildFile(resource, delta));
						} else if(resource.getName().endsWith(".java") || resource.getName().endsWith(".class")){
							// convert IResourceDelta to SourceDelta.Delta types
							Delta sourceDeltaType = Delta.ADDED;
							switch (delta.getKind()) {
							case IResourceDelta.ADDED:
								sourceDeltaType = Delta.ADDED;
								break;
							case IResourceDelta.CHANGED:
								sourceDeltaType = Delta.MODIFIED;
								break;
							case IResourceDelta.REMOVED:
								sourceDeltaType = Delta.REMOVED;
								break;
							}
							
							// construct DeltaSource objects for each case
							switch (sourceDeltaType) {
							case ADDED:
							case MODIFIED:
								if(resolvedFiles.contains(resource)){
									try {
										if (resource.getName().endsWith(".java")) {
											File sourceFile = resource;
											File classFile = BuilderUtils.getCorrespondingClassFile(jrefProject, sourceFile);
											ClassNode classNode = BytecodeUtils.getClassNode(classFile);
											deltaSourcesToProcess.add(new IncrementalBuilder.DeltaSource(resource, sourceFile, classNode, sourceDeltaType));
										} else if (resource.getName().endsWith(".class")) {
											File classFile = resource;
											ClassNode classNode = BytecodeUtils.getClassNode(classFile);
											File sourceFile = BuilderUtils.getCorrespondingSourceFile(jrefProject, classFile);
											deltaSourcesToProcess.add(new IncrementalBuilder.DeltaSource(resource, sourceFile, classNode, sourceDeltaType));
										}
									} catch (Exception e) {
										throw new IllegalArgumentException("Unable to process source: " + resource.getName(), e);
									}
								}
								break;
							case REMOVED:
								// a removed source won't have a source file or class file, so it won't be in the resolved files
								try {
									if (resource.getName().endsWith(".java")) {
										File sourceFile = resource;
										deltaSourcesToProcess.add(new IncrementalBuilder.DeltaSource(resource, sourceFile, sourceDeltaType));
									} else if (resource.getName().endsWith(".class")) {
										File classFile = resource;
										File sourceFile = BuilderUtils.getCorrespondingSourceFile(jrefProject, classFile);
										deltaSourcesToProcess.add(new IncrementalBuilder.DeltaSource(resource, sourceFile, sourceDeltaType));
									}
								} catch (Exception e){
									throw new IllegalArgumentException("Unable to process source: " + resource.getName(), e);
								}
								break;
							}
						}
					}
				}
			} catch (Exception e){
				// not a valid file, skip
				Log.warning("Unable to process resource: " + delta.getResource().getName(), e);
			}

			// always returns true so the resource delta's children should be visited
			// returning false skips the resource's children
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
	
	private static void checkAntivirusInterference(File classFile, Exception e) throws Exception {
		if(e.getMessage() != null && (e.getMessage().toUpperCase().contains("VIRUS") || e.getMessage().toUpperCase().contains("MALWARE"))){
			Log.warning("Ignoring class [" + classFile.getName() + "] because it was detected as malware by host machine and could not be accessed.");
		} else {
			throw e;
		}
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
