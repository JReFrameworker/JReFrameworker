package com.jreframeworker.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarException;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.objectweb.asm.tree.ClassNode;
import org.xml.sax.SAXException;

import com.jreframeworker.common.RuntimeUtils;
import com.jreframeworker.engine.Engine;
import com.jreframeworker.engine.identifiers.DefineFinalityIdentifier;
import com.jreframeworker.engine.identifiers.DefineIdentifier;
import com.jreframeworker.engine.identifiers.DefineVisibilityIdentifier;
import com.jreframeworker.engine.identifiers.MergeIdentifier;
import com.jreframeworker.engine.identifiers.PurgeIdentifier;
import com.jreframeworker.engine.identifiers.DefineIdentifier.DefineTypeAnnotation;
import com.jreframeworker.engine.identifiers.MergeIdentifier.MergeTypeAnnotation;
import com.jreframeworker.log.Log;
import com.jreframeworker.preferences.JReFrameworkerPreferences;

public class IncrementalBuilder {
	
	public static final int DEFAULT_BUILD_PHASE = 1;
	
	public static abstract class Source {
		protected File resourceFile;
		protected File sourceFile;
		protected ClassNode classNode;
		
		public Source(File resourceFile, File sourceFile, ClassNode classNode){
			try {
				if(resourceFile != null && resourceFile.exists()){
					this.resourceFile = resourceFile.getCanonicalFile();
				} else {
					this.resourceFile = resourceFile;
				}
			} catch (Exception e){
				throw new IllegalArgumentException(e);
			}
			try {
				if(sourceFile != null && sourceFile.exists()){
					this.sourceFile = sourceFile.getCanonicalFile();
				} else {
					this.sourceFile = sourceFile;
				}
			} catch (Exception e){
				throw new IllegalArgumentException(e);
			}
			this.classNode = classNode;
		}
		
		public File getResourceFile(){
			return resourceFile;
		}
		
		public File getSourceFile() {
			return sourceFile;
		}

		public ClassNode getClassNode(){
			return classNode;
		}
		
		public abstract List<Integer> getSortedPhases();
		
		/**
		 * A source object is equivalent if it shares the same resource file
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((resourceFile == null) ? 0 : resourceFile.hashCode());
			return result;
		}

		/**
		 * A source object is equivalent if it shares the same resource file
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			try {
				Source other = (Source) obj;
				if (resourceFile == null) {
					if (other.resourceFile != null)
						return false;
				} else if (!resourceFile.equals(other.resourceFile))
					return false;
				return true;
			} catch (ClassCastException e){
				return false;
			}
		}
		
	}
	
	public static class ProcessedSource extends Source {
		
		private List<Integer> phases = new LinkedList<Integer>();
		
		public ProcessedSource(File resourceFile, File sourceFile, ClassNode classNode, List<Integer> phases) {
			super(resourceFile, sourceFile, classNode);
			this.phases = phases;
		}

		@Override
		public List<Integer> getSortedPhases() {
			return new LinkedList<Integer>(phases);
		}
		
		@Override
		public String toString() {
			return "[ProcessedSource (" + sourceFile.getName() + ")]";
		}
	}
	
	public static class DeltaSource extends Source {
		
		public static enum Delta {
			ADDED, MODIFIED, REMOVED;
		}
		
		private Delta delta;
		private List<Integer> phases = new LinkedList<Integer>();
		
		public DeltaSource(File resource, File sourceFile, Delta delta){
			this(resource, sourceFile, null, delta);
		}
		
		public DeltaSource(File resourceFile, File sourceFile, ClassNode classNode, Delta delta){
			super(resourceFile, sourceFile, classNode);
			this.delta = delta;
			if(delta == Delta.REMOVED && classNode != null){
				throw new IllegalArgumentException("Removed source should not contain class nodes.");
			} else if(delta != Delta.REMOVED && classNode == null){
				throw new IllegalArgumentException("Added or Modified sources must contain class nodes.");
			}
			if(delta != Delta.REMOVED){
				try {
					this.phases = BuilderUtils.getSortedBuildPhases(classNode);
				} catch (IOException e) {
					throw new IllegalArgumentException("Unable to recover build phases.");
				}
			}
		}

		public Delta getDelta() {
			return delta;
		}
		
		public ProcessedSource getProcessedSource(){
			return new ProcessedSource(getResourceFile(), getSourceFile(), getClassNode(), getSortedPhases());
		}

		@Override
		public List<Integer> getSortedPhases() {
			List<Integer> result = new LinkedList<Integer>(phases);
			if(result.isEmpty()){
				// TODO: if we can make better guarantees about the correctness of this result,
				// it may be better to return empty phase results so that we can identify deleted
				// sources that had no phases
				
				// for now assume phase one is always there (even if the source has no phases)
				result.add(1); 
			}
			return result;
		}
		
		@Override
		public String toString() {
			String change = "";
			if(delta == Delta.ADDED){
				change = "Added: ";
			} else if(delta == Delta.MODIFIED){
				change = "Modified: ";
			} else if(delta == Delta.REMOVED){
				change = "Removed: ";
			}
			return "[DeltaSource (" + change + resourceFile.getName() + ")]";
		}
		
	}
	
	public static class IncrementalBuilderException extends Exception {
		
		private static final long serialVersionUID = 1L;

		public IncrementalBuilderException(String message){
			super(message);
		}
		
		public IncrementalBuilderException(String message, Throwable t){
			super(message, t);
		}
	}
	
	private JReFrameworkerProject jrefProject;
	private int currentPhase = DEFAULT_BUILD_PHASE;
	private Set<ProcessedSource> processedSources = new HashSet<ProcessedSource>();
	
	public IncrementalBuilder(JReFrameworkerProject jrefProject){
		this.jrefProject = jrefProject;
	}
	
	public JReFrameworkerProject getJReFrameworkerProject(){
		return jrefProject;
	}
	
	public static enum PostBuildAction {
		UPDATE_CLASSPATH, CLEAN_REBUILD, NONE
	}

	public PostBuildAction build(Set<DeltaSource> sourceDeltas, IProgressMonitor monitor) throws IncrementalBuilderException {
		if(sourceDeltas.isEmpty()){
			// nothing to do
			return PostBuildAction.NONE;
		}
		try {
			// first separate the java and class source delta resources
			Set<DeltaSource> javaSourceDeltas = new HashSet<DeltaSource>();
			Set<DeltaSource> classSourceDeltas = new HashSet<DeltaSource>();
			for(DeltaSource sourceDelta : sourceDeltas){
				if(sourceDelta.getResourceFile().getName().endsWith(".java")){
					javaSourceDeltas.add(sourceDelta);
				} else if(sourceDelta.getResourceFile().getName().endsWith(".class")){
					classSourceDeltas.add(sourceDelta);
				}
			}
			
			// then map java class deltas to java file source deltas
			// class file edits may be made without edits to the source when the classpath has been updated
			Map<DeltaSource,DeltaSource> classToJavaDeltaSources = new HashMap<DeltaSource,DeltaSource>();
			for(DeltaSource classSourceDelta : classSourceDeltas){
				boolean mapped = false;
				for(DeltaSource javaSourceDelta : javaSourceDeltas){
					if(javaSourceDelta.getResourceFile().equals(classSourceDelta.getSourceFile())){
						classToJavaDeltaSources.put(classSourceDelta, javaSourceDelta);
						mapped = true;
						break;
					}
				}
				if(!mapped){
					classToJavaDeltaSources.put(classSourceDelta, null);
				}
			}
			
			if(JReFrameworkerPreferences.isVerboseLoggingEnabled()){
				Log.info("Incremental changes: " + classToJavaDeltaSources.toString());
			}
			
			// next figure out if we need to revert to a previous build phase
			// reverts can occur when a class file is modified or removed or added
			// added case: an earlier phase is added
			// modified case: a phase is changed or the class is changed and the phase needs to be reprocessed
			// removed case: a phase should not have been run
			Set<Source> staleSources = new HashSet<Source>();
			for(DeltaSource source : javaSourceDeltas){
				// first consider modified or removed sources
				if(source.getDelta() == DeltaSource.Delta.MODIFIED || source.getDelta() == DeltaSource.Delta.REMOVED){
					
					// if the source was removed and it has no phases then nothing to do
					if(source.getDelta() == DeltaSource.Delta.REMOVED){
						// revert to the earliest phase in the removed source
						if(!source.getSortedPhases().isEmpty()){
							currentPhase = source.getSortedPhases().get(0);
							if(currentPhase == DEFAULT_BUILD_PHASE){
								return PostBuildAction.CLEAN_REBUILD;
							}
						}
						break;
					}

					// note modified and removed sources that are no longer valid
					staleSources.add(source);
					
					// for modified and removed sources revert back to
					// the min(the source's original phase, lowest modified phase value)
					boolean sourceFound = false;
					for(ProcessedSource processedSource : processedSources){
						if(processedSource.equals(source)){
							sourceFound = true;
							// current phase should be the earliest phase of the previously processed source
							currentPhase = Math.min(currentPhase, processedSource.phases.get(0));
							// if the source is modified also consider the earlier phase in the modification
							if(source.getDelta() == DeltaSource.Delta.MODIFIED){
								int earliestModificationPhase = source.getSortedPhases().get(0);
								currentPhase = Math.min(currentPhase, earliestModificationPhase);
							}
							break;
						}
					}
					if(!sourceFound){
//						throw new IncrementalBuilderException("Unable to locate previous source reference");
						// a source which was previously uncompilable just became compilable, so we need to revert back to it's earliest phase
						currentPhase = source.getSortedPhases().get(0);
					}
				}
				
				// consider phase reverts due to added sources
				// example a new source was added with an earlier phase
				if(source.getDelta() == DeltaSource.Delta.ADDED){
					int earliestPhase = source.getSortedPhases().get(0);
					currentPhase = Math.min(currentPhase, earliestPhase);
				}
			}
			
			// remove stale sources
			for(Source source : staleSources){
				processedSources.remove(source);
			}
			
			// figure out which sources need to reprocessed
			Set<Source> sourcesToProcess = new HashSet<Source>();
			for(ProcessedSource source : processedSources){
				for(int phase : source.getSortedPhases()){
					if(currentPhase <= phase){
						if(phase == DEFAULT_BUILD_PHASE){
							return PostBuildAction.CLEAN_REBUILD;
						} else {
							sourcesToProcess.add(source);
							break;
						}
					}
				}
			}
			
			// add any new or modified sources to the list of sources to be processed
			// updating the class path causes the compiler to change the class
			// files even though the corresponding source files did not change.
			// If the resource modified was a class file and not a source file
			// then we should ignore this change to prevent infinite build loops
			for(DeltaSource source : classSourceDeltas){
				boolean classFileOnlyModification = classToJavaDeltaSources.get(source) == null;
				if(classFileOnlyModification){
					// a class file only modification is important if its a class file with a phase we haven't seen yet
					// otherwise its just a result of classpath updating
					for(int phase : source.getSortedPhases()){
						if(currentPhase <= phase){
							sourcesToProcess.add(source);
						}
					}
				} else {
					if(source.getDelta() == DeltaSource.Delta.ADDED || source.getDelta() == DeltaSource.Delta.MODIFIED){
						sourcesToProcess.add(source);
					}
				}
			}
			
			// assert build phases are contiguous
			Set<Integer> phases = new HashSet<Integer>();
			for(Source source : sourcesToProcess){
				phases.addAll(source.getSortedPhases());
			}
			
			LinkedList<Integer> sortedPhases = new LinkedList<Integer>(phases);
			Collections.sort(sortedPhases);
			if(sortedPhases.isEmpty()){
				// no phases found
				return PostBuildAction.NONE;
			} else {
				for(int i=sortedPhases.getFirst(); i<=sortedPhases.getLast(); i++){
					if(!sortedPhases.contains(i)){
						throw new IncrementalBuilderException("Phases are not contiguous. Phase " + i + " is missing.");
					}
				}
				
				// starting from the current phase process every phase in the set of sources to process
				int lastPhase = sortedPhases.getLast();
				while(currentPhase <= lastPhase){
					boolean isFirstPhase = (currentPhase == DEFAULT_BUILD_PHASE);
					boolean isLastPhase = (currentPhase == lastPhase);
					
					// gather the sources that are relevant to the current phase
					Set<Source> phaseSources = new HashSet<Source>();
					for(Source source : sourcesToProcess){
						if(source.getSortedPhases().contains(currentPhase)){
							phaseSources.add(source);
						}
					}
					
					// build the phase targets
					buildPhase(phaseSources, currentPhase, isFirstPhase, isLastPhase, monitor);
					
					currentPhase++;
				}
				
				// record the processed phases for the next incremental build
				for(Source source : sourcesToProcess){
					// processed sources are already recorded as processed
					// just need to add the delta sources as processed sources
					if(source instanceof DeltaSource){
						processedSources.add(((DeltaSource) source).getProcessedSource());
					}
				}
				
				// changes were made, need to update the classpath
				return PostBuildAction.UPDATE_CLASSPATH;
			}
		} catch (Throwable t){
			throw new IncrementalBuilderException("Error building sources", t);
		}
	}

	private void buildPhase(Set<Source> phaseSources, int currentPhase, boolean isFirstPhase, boolean isLastPhase, IProgressMonitor monitor) throws JarException, SAXException, IOException, ParserConfigurationException, CoreException, IncrementalBuilderException {
		// map class entries to and initial modification engine sets
		Map<String, Set<Engine>> engineMap = new HashMap<String, Set<Engine>>();
		Set<Engine> allEngines = new HashSet<Engine>();

		// initialize the modification engines
		// if its the first phase then we are just initializing with the original jars
		// if its after the first phase then we are initializing with the last build phase jars
		BuildFile buildFile = jrefProject.getBuildFile();
		if(isFirstPhase){
			for(BuildFile.Target target : buildFile.getTargets()) {
				// classpath has been restored, these are all the original jars
				File originalJar = RuntimeUtils.getClasspathJar(target.getName(), jrefProject);
				if (originalJar != null && originalJar.exists()) {
					Engine engine = new Engine(originalJar, JReFrameworkerPreferences.getMergeRenamingPrefix());
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
					Log.warning("Original Jar not found: " + target.getName());
				}
			}
		} else {
			for(BuildFile.Target target : buildFile.getTargets()) {
				File phaseJar = BuilderUtils.getBuildPhaseJar(target.getName(), jrefProject, currentPhase-1);
				if(!phaseJar.exists()){
					phaseJar = RuntimeUtils.getClasspathJar(target.getName(), jrefProject);
				}
				if (phaseJar != null && phaseJar.exists()) {
					Engine engine = new Engine(phaseJar, JReFrameworkerPreferences.getMergeRenamingPrefix());
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
					Log.warning("Phase Jar not found: " + target.getName());
				}
			}
		}
		
		// make library modifications
		modifyTarget(phaseSources, currentPhase, engineMap, allEngines, monitor);
		
		// make sure the build directory exists
		File projectBuildDirectory = jrefProject.getBuildDirectory();
		if (!projectBuildDirectory.exists()) {
			projectBuildDirectory.mkdirs();
		}
		
		// write out the modified jars
		for(Engine engine : allEngines){
			File modifiedLibrary = BuilderUtils.getBuildPhaseJar(engine.getJarName(), jrefProject, currentPhase);
			modifiedLibrary.getParentFile().mkdirs();
			engine.save(modifiedLibrary);

			if(isLastPhase){
				File finalModifiedLibrary = new File(projectBuildDirectory.getCanonicalPath() + File.separatorChar + engine.getJarName());
				if(finalModifiedLibrary.exists()){
					// replace the library
					finalModifiedLibrary.delete();
				}
				RuntimeUtils.copyFile(modifiedLibrary, finalModifiedLibrary);
			}
			
			// log the modified runtime
			String base = jrefProject.getProject().getLocation().toFile().getCanonicalPath();
			String relativeFilePath = modifiedLibrary.getCanonicalPath().substring(base.length());
			if(relativeFilePath.charAt(0) == File.separatorChar){
				relativeFilePath = relativeFilePath.substring(1);
			}
			if(JReFrameworkerPreferences.isVerboseLoggingEnabled()) Log.info("Modified: " + relativeFilePath);
		}
		
		jrefProject.refresh();
	}
	
	private void modifyTarget(Set<Source> sources, int phase, Map<String, Set<Engine>> engineMap, Set<Engine> allEngines, IProgressMonitor monitor) throws IOException, IncrementalBuilderException {
		SubMonitor modificationMonitor = SubMonitor.convert(monitor, sources.size());
		monitor.subTask("Modifying targets of " + sources.size() + " phase " + phase + " source" + (sources.size() > 1 ? "s" : ""));
		for(Source source : sources){
			
			// check if a cancellation was requested
			if(modificationMonitor.isCanceled()){
				throw new IncrementalBuilderException("Modification process was cancelled.");
			}
			
			ClassNode classNode = source.getClassNode();
			// TODO: refactor this bit to just save the parsed annotation requests instead of true/false
			boolean purgeModification = BuilderUtils.hasPurgeModification(classNode);
			boolean finalityModification = BuilderUtils.hasFinalityModification(classNode);
			boolean visibilityModification = BuilderUtils.hasVisibilityModification(classNode);
			boolean mergeModification = BuilderUtils.hasMergeTypeModification(classNode);
			boolean defineModification = BuilderUtils.hasDefineTypeModification(classNode);
			
			if(purgeModification || finalityModification || visibilityModification || mergeModification || defineModification){
				// get the qualified modification class name
				String base = jrefProject.getBinaryDirectory().getCanonicalPath();
				String modificationClassName = source.getSourceFile().getCanonicalPath().substring(base.length());
				if(modificationClassName.charAt(0) == File.separatorChar){
					modificationClassName = modificationClassName.substring(1);
				}
				modificationClassName = modificationClassName.replace(".java", "");
			
				if(purgeModification){
					Set<String> targets = PurgeIdentifier.getPurgeTargets(classNode, phase);
					for(String target : targets){
						// purge target from each jar that contains the purge target
						if(engineMap.containsKey(target)){
							for(Engine engine : engineMap.get(target)){
								if(RuntimeUtils.isRuntimeJar(engine.getOriginalJar())){
									engine.setClassLoaders(new ClassLoader[]{ getClass().getClassLoader() });
								} else {
									URL[] jarURL = { new URL("jar:file:" + engine.getOriginalJar().getCanonicalPath() + "!/") };
									engine.setClassLoaders(new ClassLoader[]{ getClass().getClassLoader(), URLClassLoader.newInstance(jarURL) });
								}
								engine.process(classNode, phase);
							}
						} else {
							Log.warning("Class entry [" + target + "] specified by [" + classNode.name + "] could not be found in any of the target jars.");
						}
					}
				} 
				
				if(finalityModification){
					Set<String> targets = DefineFinalityIdentifier.getFinalityTargets(classNode, phase);
					for(String target : targets){
						// merge into each target jar that contains the merge target
						if(engineMap.containsKey(target)){
							for(Engine engine : engineMap.get(target)){
								if(RuntimeUtils.isRuntimeJar(engine.getOriginalJar())){
									engine.setClassLoaders(new ClassLoader[]{ getClass().getClassLoader() });
								} else {
									URL[] jarURL = { new URL("jar:file:" + engine.getOriginalJar().getCanonicalPath() + "!/") };
									engine.setClassLoaders(new ClassLoader[]{ getClass().getClassLoader(), URLClassLoader.newInstance(jarURL) });
								}
								engine.process(classNode, phase);
							}
						} else {
							Log.warning("Class entry [" + target + "] specified by [" + classNode.name + "] could not be found in any of the target jars.");
						}
					}
				} 
				
				if(visibilityModification){
					Set<String> targets = DefineVisibilityIdentifier.getVisibilityTargets(classNode, phase);
					for(String target : targets){
						// merge into each target jar that contains the merge target
						if(engineMap.containsKey(target)){
							for(Engine engine : engineMap.get(target)){
								if(RuntimeUtils.isRuntimeJar(engine.getOriginalJar())){
									engine.setClassLoaders(new ClassLoader[]{ getClass().getClassLoader() });
								} else {
									URL[] jarURL = { new URL("jar:file:" + engine.getOriginalJar().getCanonicalPath() + "!/") };
									engine.setClassLoaders(new ClassLoader[]{ getClass().getClassLoader(), URLClassLoader.newInstance(jarURL) });
								}
								engine.process(classNode, phase);
							}
						} else {
							Log.warning("Class entry [" + target + "] specified by [" + classNode.name + "] could not be found in any of the target jars.");
						}
					}
				}
				
				if(mergeModification){
					MergeIdentifier mergeIdentifier = new MergeIdentifier(classNode);
					MergeTypeAnnotation mergeTypeAnnotation = mergeIdentifier.getMergeTypeAnnotation();
					if(mergeTypeAnnotation.getPhase() == phase){
						String target = mergeTypeAnnotation.getSupertype();
						// merge into each target jar that contains the merge target
						if(engineMap.containsKey(target)){
							for(Engine engine : engineMap.get(target)){
								if(RuntimeUtils.isRuntimeJar(engine.getOriginalJar())){
									engine.setClassLoaders(new ClassLoader[]{ getClass().getClassLoader() });
								} else {
									URL[] jarURL = { new URL("jar:file:" + engine.getOriginalJar().getCanonicalPath() + "!/") };
									engine.setClassLoaders(new ClassLoader[]{ getClass().getClassLoader(), URLClassLoader.newInstance(jarURL) });
								}
								engine.process(classNode, phase);
							}
						} else {
							Log.warning("Class entry [" + target + "] specified by [" + classNode.name + "] could not be found in any of the target jars.");
						}
					}
				} 
				
				if(defineModification){
					DefineIdentifier defineIdentifier = new DefineIdentifier(classNode);
					DefineTypeAnnotation defineTypeAnnotation = defineIdentifier.getDefineTypeAnnotation();
					if(defineTypeAnnotation.getPhase() == phase){
						// define or replace in every target jar
						for(Engine engine : allEngines){
							if(RuntimeUtils.isRuntimeJar(engine.getOriginalJar())){
								engine.setClassLoaders(new ClassLoader[]{ getClass().getClassLoader() });
							} else {
								URL[] jarURL = { new URL("jar:file:" + engine.getOriginalJar().getCanonicalPath() + "!/") };
								engine.setClassLoaders(new ClassLoader[]{ getClass().getClassLoader(), URLClassLoader.newInstance(jarURL) });
							}
							engine.process(classNode, phase);
						}
					}
				}
			}
			modificationMonitor.worked(1);
		}
		
	}
	
}
