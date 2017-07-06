package jreframeworker.builder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import jreframeworker.core.JReFrameworker;
import jreframeworker.core.JReFrameworkerProject;
import jreframeworker.engine.identifiers.DefineFinalityIdentifier;
import jreframeworker.engine.identifiers.DefineIdentifier;
import jreframeworker.engine.identifiers.DefineVisibilityIdentifier;
import jreframeworker.engine.identifiers.JREFAnnotationIdentifier;
import jreframeworker.engine.identifiers.MergeIdentifier;
import jreframeworker.engine.identifiers.PurgeIdentifier;
import jreframeworker.engine.identifiers.DefineFinalityIdentifier.DefineFieldFinalityAnnotation;
import jreframeworker.engine.identifiers.DefineFinalityIdentifier.DefineMethodFinalityAnnotation;
import jreframeworker.engine.identifiers.DefineFinalityIdentifier.DefineTypeFinalityAnnotation;
import jreframeworker.engine.identifiers.DefineIdentifier.DefineTypeAnnotation;
import jreframeworker.engine.identifiers.DefineVisibilityIdentifier.DefineFieldVisibilityAnnotation;
import jreframeworker.engine.identifiers.DefineVisibilityIdentifier.DefineMethodVisibilityAnnotation;
import jreframeworker.engine.identifiers.DefineVisibilityIdentifier.DefineTypeVisibilityAnnotation;
import jreframeworker.engine.identifiers.MergeIdentifier.MergeTypeAnnotation;
import jreframeworker.engine.identifiers.PurgeIdentifier.PurgeFieldAnnotation;
import jreframeworker.engine.identifiers.PurgeIdentifier.PurgeMethodAnnotation;
import jreframeworker.engine.identifiers.PurgeIdentifier.PurgeTypeAnnotation;
import jreframeworker.engine.utils.BytecodeUtils;
import jreframeworker.log.Log;

public class BuilderUtils {

	/**
	 * Returns a collection of K_SOURCE Compilation units in the project's package fragments
	 * Reference: https://www.eclipse.org/forums/index.php/t/68072/
	 * @param javaproject
	 * @return
	 */
	public static final ICompilationUnit[] getSourceCompilationUnits(IJavaProject jProject) {
		ArrayList<ICompilationUnit> sourceCompilationUnits = new ArrayList<ICompilationUnit>();
		try {
			IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				IPackageFragmentRoot root = roots[i];
				if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
					IJavaElement[] javaElements = root.getChildren();
					for (int j = 0; j < javaElements.length; j++) {
						IJavaElement javaElement = javaElements[j];
						if (javaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
							IPackageFragment pf = (IPackageFragment) javaElement;
							ICompilationUnit[] compilationUnits = pf.getCompilationUnits();
							for (int k = 0; k < compilationUnits.length; k++) {
								ICompilationUnit unit = compilationUnits[k];
								if (unit.isStructureKnown()) {
									sourceCompilationUnits.add(unit);
								}
							}
						}
					}
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		ICompilationUnit[] sourceCompilationUnitsArray = new ICompilationUnit[sourceCompilationUnits.size()];
		sourceCompilationUnits.toArray(sourceCompilationUnitsArray);
		return sourceCompilationUnitsArray;
	}
	
	/**
	 * Returns true if the compilation unit has severe problem markers
	 * 
	 * Reference: https://www.ibm.com/support/knowledgecenter/en/SS4JCV_7.5.5/org.eclipse.jdt.doc.isv/guide/jdt_api_compile.htm
	 * @param compilationUnit
	 * @return
	 * @throws CoreException
	 */
	public static final boolean hasSevereProblems(ICompilationUnit compilationUnit) throws CoreException {
		IResource javaSourceFile = compilationUnit.getUnderlyingResource();
		IMarker[] markers = javaSourceFile.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
		ArrayList<IMarker> severeErrorMarkers = new ArrayList<IMarker>();
		for (IMarker marker : markers) {
			Integer severityType = (Integer) marker.getAttribute(IMarker.SEVERITY);
			if (severityType.intValue() == IMarker.SEVERITY_ERROR){
				severeErrorMarkers.add(marker);
			}
		}
		return !severeErrorMarkers.isEmpty();
	}
	
	/**
	 * Returns the the corresponding class file for the given compilation units of the project
	 * @param jrefProject
	 * @param compilationUnits
	 * @return
	 * @throws JavaModelException
	 * @throws IOException
	 */
	public static final File getCorrespondingClassFile(JReFrameworkerProject jrefProject, ICompilationUnit compilationUnit) throws JavaModelException, IOException {
		File sourceFile = compilationUnit.getUnderlyingResource().getLocation().toFile().getCanonicalFile();
		String sourceDirectory = jrefProject.getSourceDirectory().getCanonicalPath();
		String relativeSourceFileDirectoryPath = sourceFile.getParentFile().getCanonicalPath().substring(sourceDirectory.length());
		if(relativeSourceFileDirectoryPath.charAt(0) == File.separatorChar){
			relativeSourceFileDirectoryPath = relativeSourceFileDirectoryPath.substring(1);
		}
		String classFileName = sourceFile.getName().replace(".java", ".class");
		File classFile = new File(jrefProject.getBinaryDirectory().getCanonicalPath() + File.separator + relativeSourceFileDirectoryPath + File.separator + classFileName);
		return classFile;
	}
	
	public static final File getBuildPhaseDirectory(JReFrameworkerProject jrefProject, int buildPhase) throws IOException {
		File projectBuildDirectory = jrefProject.getProject().getFolder(JReFrameworker.BUILD_DIRECTORY).getLocation().toFile();
		String buildPhaseDirectoryName = JReFrameworker.BUILD_PHASE_DIRECTORY_PREFIX + "-" + buildPhase;
		return new File(projectBuildDirectory.getCanonicalPath() + File.separatorChar + buildPhaseDirectoryName);
	}
	
	public static final File getBuildPhaseJar(String targetJar, JReFrameworkerProject jrefProject, int buildPhase) throws IOException {
		return new File(getBuildPhaseDirectory(jrefProject, buildPhase).getCanonicalPath() + File.separatorChar + targetJar);
	}

	public static final List<Integer> getSortedBuildPhases(ClassNode classNode) throws IOException {
		Set<Integer> phases = new HashSet<Integer>();
		try {
			boolean purgeModification = hasPurgeModification(classNode);
			if(purgeModification){
				PurgeIdentifier purgeIdentifier = new PurgeIdentifier(classNode);
				for(PurgeTypeAnnotation purgeTypeAnnotation : purgeIdentifier.getPurgeTypeAnnotations()){
					phases.add(purgeTypeAnnotation.getPhase());
				}
				for(PurgeFieldAnnotation purgeFieldAnnotation : purgeIdentifier.getPurgeFieldAnnotations()){
					phases.add(purgeFieldAnnotation.getPhase());
				}
				for(PurgeMethodAnnotation purgeMethodAnnotation : purgeIdentifier.getPurgeMethodAnnotations()){
					phases.add(purgeMethodAnnotation.getPhase());
				}
			}
			
			boolean finalityModification = hasFinalityModification(classNode);
			if(finalityModification){
				DefineFinalityIdentifier defineFinalityIdentifier = new DefineFinalityIdentifier(classNode);
				for(DefineTypeFinalityAnnotation defineTypeFinalityAnnotation : defineFinalityIdentifier.getTargetTypes()){
					phases.add(defineTypeFinalityAnnotation.getPhase());
				}
				for(DefineFieldFinalityAnnotation defineFieldFinalityAnnotation : defineFinalityIdentifier.getTargetFields()){
					phases.add(defineFieldFinalityAnnotation.getPhase());
				}
				for(DefineMethodFinalityAnnotation defineMethodFinalityAnnotation : defineFinalityIdentifier.getTargetMethods()){
					phases.add(defineMethodFinalityAnnotation.getPhase());
				}
			}
			
			boolean visibilityModification = hasVisibilityModification(classNode);
			if(visibilityModification){
				DefineVisibilityIdentifier defineVisibilityIdentifier = new DefineVisibilityIdentifier(classNode);
				for(DefineTypeVisibilityAnnotation defineTypeVisibilityAnnotation : defineVisibilityIdentifier.getTargetTypes()){
					phases.add(defineTypeVisibilityAnnotation.getPhase());
				}
				for(DefineFieldVisibilityAnnotation defineFieldVisibilityAnnotation : defineVisibilityIdentifier.getTargetFields()){
					phases.add(defineFieldVisibilityAnnotation.getPhase());
				}
				for(DefineMethodVisibilityAnnotation defineMethodVisibilityAnnotation : defineVisibilityIdentifier.getTargetMethods()){
					phases.add(defineMethodVisibilityAnnotation.getPhase());
				}
			}
			
			boolean mergeModification = hasMergeTypeModification(classNode);
			if(mergeModification){
				MergeIdentifier mergeIdentifier = new MergeIdentifier(classNode);
				MergeTypeAnnotation mergeTypeAnnotation = mergeIdentifier.getMergeTypeAnnotation();
				phases.add(mergeTypeAnnotation.getPhase());
				// no such thing as merge field, so skipping fields
				// define field, define method, and merge method all must have the same phase as the merge type annotation
				// so we can't discover new phases by looking at the body
			}
			
			boolean defineModification = hasDefineTypeModification(classNode);
			if(defineModification){
				DefineIdentifier defineIdentifier = new DefineIdentifier(classNode);
				DefineTypeAnnotation defineTypeAnnotation = defineIdentifier.getDefineTypeAnnotation();
				phases.add(defineTypeAnnotation.getPhase());
				// define field, define method must have the same phase as the define type annotation
				// so we can't discover new phases by looking at the body
			}
		} catch (RuntimeException e){
			Log.error("Error discovering build phases...", e);
		}
		
		// if no phases were detected add the default build phase
		if(phases.isEmpty()){
			phases.add(1); 
		}
		
		ArrayList<Integer> phasesSorted = new ArrayList<Integer>(phases);
		Collections.sort(phasesSorted);
		return phasesSorted;
	}
	
	public static final boolean hasTypeModification(File classFile) {
		try {
			byte[] classBytes = Files.readAllBytes(classFile.toPath());
			if (classBytes.length > 0) {
				ClassNode classNode = BytecodeUtils.getClassNode(classBytes);

				boolean purgeModification = hasPurgeModification(classNode);
				if (purgeModification) {
					return true;
				}

				boolean finalityModification = hasFinalityModification(classNode);
				if (finalityModification) {
					return true;
				}

				boolean visibilityModification = hasVisibilityModification(classNode);
				if (visibilityModification) {
					return true;
				}

				boolean mergeModification = hasMergeTypeModification(classNode);
				if (mergeModification) {
					return true;
				}

				boolean defineModification = hasDefineTypeModification(classNode);
				if (defineModification) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}
	
	public static final boolean hasMergeTypeModification(ClassNode classNode) throws IOException {
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
	
	public static final boolean hasDefineTypeModification(ClassNode classNode) throws IOException {
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
	
	public static final boolean hasPurgeModification(ClassNode classNode) throws IOException {
		// TODO: address innerclasses, classNode.innerClasses, could these even be found from class files? they would be different files...
		if(classNode.invisibleAnnotations != null){
			for(Object annotationObject : classNode.invisibleAnnotations){
				AnnotationNode annotationNode = (AnnotationNode) annotationObject;
				JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
				checker.visitAnnotation(annotationNode.desc, false);
				if(checker.isPurgeAnnotation()){
					return true;
				}
			}
		}
		return false;
	}
	
	public static final boolean hasFinalityModification(ClassNode classNode) throws IOException {
		// TODO: address innerclasses, classNode.innerClasses, could these even be found from class files? they would be different files...
		if(classNode.invisibleAnnotations != null){
			for(Object annotationObject : classNode.invisibleAnnotations){
				AnnotationNode annotationNode = (AnnotationNode) annotationObject;
				JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
				checker.visitAnnotation(annotationNode.desc, false);
				if(checker.isFinalityAnnotation()){
					return true;
				}
			}
		}
		return false;
	}
	
	public static final boolean hasVisibilityModification(ClassNode classNode) throws IOException {
		// TODO: address innerclasses, classNode.innerClasses, could these even be found from class files? they would be different files...
		if(classNode.invisibleAnnotations != null){
			for(Object annotationObject : classNode.invisibleAnnotations){
				AnnotationNode annotationNode = (AnnotationNode) annotationObject;
				JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
				checker.visitAnnotation(annotationNode.desc, false);
				if(checker.isVisibilityAnnotation()){
					return true;
				}
			}
		}
		return false;
	}
	
}
