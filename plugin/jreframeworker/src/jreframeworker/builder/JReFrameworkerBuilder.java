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

import jreframeworker.core.BuildFile;
import jreframeworker.core.BuilderUtils;
import jreframeworker.core.IncrementalBuilder;
import jreframeworker.core.IncrementalBuilder.DeltaSource;
import jreframeworker.core.IncrementalBuilder.DeltaSource.Delta;
import jreframeworker.core.IncrementalBuilder.IncrementalBuilderException;
import jreframeworker.core.JReFrameworkerProject;
import jreframeworker.engine.utils.BytecodeUtils;
import jreframeworker.log.Log;

public class JReFrameworkerBuilder extends IncrementalProjectBuilder {
	
	public static final String BUILDER_ID = "jreframeworker.JReFrameworkerBuilder";

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
			Log.info("Cleaning: " + jrefProject.getProject().getName());
			
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
		Log.info("Full Build: " + jrefProject.getProject().getName());
		
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
						ClassNode classNode = BytecodeUtils.getClassNode(classFile);
						if(BuilderUtils.hasTopLevelAnnotation(classNode)){
							// in a full build all sources are added deltas
							DeltaSource source = new DeltaSource(sourceFile, sourceFile, classNode, Delta.ADDED);
							sourcesToProcess.add(source);
						}
					}
				}
			} catch (IOException e) {
				Log.error("Error resolving compilation units", e);
				return;
			}
		}
		
		try {
			// build the project
			if(!sourcesToProcess.isEmpty()){
				incrementalBuilder.build(sourcesToProcess, monitor);
				updateBuildClasspath(jrefProject);
			}
		} catch (IncrementalBuilderException e) {
			Log.error("Error Building JReFrameworker Project", e);
		}
		
		// OLD CODE....just keeping for posterity...until cleaned up
		
//		// run the java builder
//		jrefProject.getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
//		jrefProject.refresh();
//
//
//
//
//		// make sure the build directory exists
//		File projectBuildDirectory = jrefProject.getBinaryDirectory();
//		if (!projectBuildDirectory.exists()) {
//			projectBuildDirectory.mkdirs();
//		}
//
//
//
//		// add each class from classes in jars in raw directory
//		// this happens before any build phases
//		File rawDirectory = jrefProject.getProject().getFolder(JReFrameworker.RAW_DIRECTORY).getLocation().toFile();
//		if(rawDirectory.exists()){
//			for(File jarFile : rawDirectory.listFiles()){
//				if(jarFile.getName().endsWith(".jar")){
//					for(Engine engine : allEngines){
//						Log.info("Embedding raw resource: " + jarFile.getName() + " in " + engine.getJarName());
//						
//						// TODO: unjar to temp directory instead?
//						File outputDirectory = new File(jarFile.getParentFile().getAbsolutePath() + File.separatorChar + "." + jarFile.getName().replace(".jar", ""));
//						JarModifier.unjar(jarFile, outputDirectory);
//						
//						// add raw class files
//						addClassFiles(engine, outputDirectory);
//						
//						// cleanup temporary directory
//						delete(outputDirectory);
//					}
//				}
//			}
//		}
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
		Log.info("Incremental Build");
		boolean incrementalBuildChangesMade = false;
		JReFrameworkerProject jrefProject = incrementalBuilder.getJReFrameworkerProject();
		BuildDeltaVisitor deltaVisitor = new BuildDeltaVisitor(jrefProject);
		delta.accept(deltaVisitor);
		if(!deltaVisitor.getDeltaBuildFilesToProcess().isEmpty()){
			// TODO: this could be improved to just detect changes to individual libraries...but too much work for now...
			// changes to the build file require a full build
			clean(monitor);
			fullBuild(monitor);
			return;
		} else {
			// process incremental changes
			// changes could be to source code, which will change the class files
			// changes could also be to class files only (if a build error was resolved but the source was not touched)
			Set<DeltaSource> sourceDeltas = deltaVisitor.getDeltaSourcesToProcess();
			if(!sourceDeltas.isEmpty()){
				try {
					incrementalBuildChangesMade = incrementalBuilder.build(sourceDeltas, monitor);
				} catch (IncrementalBuilderException e) {
					Log.error("Error incrementally building JReFrameworker project", e);
				}
			}
		}
		
		if(incrementalBuildChangesMade){
			updateBuildClasspath(jrefProject);
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
							ClassNode classNode = BytecodeUtils.getClassNode(classFile);
							if(BuilderUtils.hasTopLevelAnnotation(classNode)){
								resolvedFiles.add(sourceFile);
								resolvedFiles.add(classFile);
							}
						}
					}
				} catch (IOException | CoreException e) {
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
					if(resource.exists() && resource.isFile()){
						
						String changeType = "Added: ";
						switch (delta.getKind()) {
						case IResourceDelta.ADDED:
							changeType = "Added: ";
							break;
						case IResourceDelta.CHANGED:
							changeType = "Modified: ";
							break;
						case IResourceDelta.REMOVED:
							changeType = "Removed: ";
							break;
						}
						Log.info(changeType + resource.getName());
						
						if(resource.getName().equals(BuildFile.XML_BUILD_FILENAME)){
							buildFilesToProcess.add(new DeltaBuildFile(resource, delta));
						} else if(resource.getName().endsWith(".java") || resource.getName().endsWith(".class")){
							if(resolvedFiles.contains(resource)){
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
								switch (delta.getKind()) {
								case IResourceDelta.ADDED:
								case IResourceDelta.CHANGED:
									if(resource.getName().endsWith(".java")){
										try {
											File sourceFile = resource;
											File classFile = BuilderUtils.getCorrespondingClassFile(jrefProject, sourceFile);
											ClassNode classNode = BytecodeUtils.getClassNode(classFile);
											deltaSourcesToProcess.add(new IncrementalBuilder.DeltaSource(resource, sourceFile, classNode, sourceDeltaType));
										} catch (Exception e){
											throw new IllegalArgumentException("Unable to process source: " + resource.getName(), e);
										}
									} else if(resource.getName().endsWith(".class")){
										try {
											File classFile = resource;
											ClassNode classNode = BytecodeUtils.getClassNode(classFile);
											File sourceFile = BuilderUtils.getCorrespondingSourceFile(jrefProject, classFile);
											deltaSourcesToProcess.add(new IncrementalBuilder.DeltaSource(resource,sourceFile, classNode, sourceDeltaType));
										} catch (Exception e){
											throw new IllegalArgumentException("Unable to process source: " + resource.getName(), e);
										}
									}
									break;
								case IResourceDelta.REMOVED:
									// a removed source won't have a corresponding class file
									if(resource.getName().endsWith(".java")){
										try {
											File sourceFile = resource;
											deltaSourcesToProcess.add(new IncrementalBuilder.DeltaSource(resource, sourceFile, sourceDeltaType));
										} catch (Exception e){
											throw new IllegalArgumentException("Unable to process source: " + resource.getName(), e);
										}
									}
									break;
								}
							}
						}
					}
				}
			} catch (Exception e){
				// not a valid file, skip
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
