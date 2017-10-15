package com.jreframeworker.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

import com.jreframeworker.engine.identifiers.DefineFinalityIdentifier;
import com.jreframeworker.engine.identifiers.DefineIdentifier;
import com.jreframeworker.engine.identifiers.DefineVisibilityIdentifier;
import com.jreframeworker.engine.identifiers.JREFAnnotationIdentifier;
import com.jreframeworker.engine.identifiers.MergeIdentifier;
import com.jreframeworker.engine.identifiers.PurgeIdentifier;
import com.jreframeworker.engine.identifiers.DefineFinalityIdentifier.DefineFieldFinalityAnnotation;
import com.jreframeworker.engine.identifiers.DefineFinalityIdentifier.DefineMethodFinalityAnnotation;
import com.jreframeworker.engine.identifiers.DefineFinalityIdentifier.DefineTypeFinalityAnnotation;
import com.jreframeworker.engine.identifiers.DefineIdentifier.DefineTypeAnnotation;
import com.jreframeworker.engine.identifiers.DefineVisibilityIdentifier.DefineFieldVisibilityAnnotation;
import com.jreframeworker.engine.identifiers.DefineVisibilityIdentifier.DefineMethodVisibilityAnnotation;
import com.jreframeworker.engine.identifiers.DefineVisibilityIdentifier.DefineTypeVisibilityAnnotation;
import com.jreframeworker.engine.identifiers.MergeIdentifier.MergeTypeAnnotation;
import com.jreframeworker.engine.identifiers.PurgeIdentifier.PurgeFieldAnnotation;
import com.jreframeworker.engine.identifiers.PurgeIdentifier.PurgeMethodAnnotation;
import com.jreframeworker.engine.identifiers.PurgeIdentifier.PurgeTypeAnnotation;
import com.jreframeworker.log.Log;

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
	 * Returns the corresponding source file for the given class files of a project
	 * @param jrefProject
	 * @param compilationUnits
	 * @return
	 * @throws JavaModelException
	 * @throws IOException
	 */
	public static final File getCorrespondingSourceFile(JReFrameworkerProject jrefProject, File classFile) throws JavaModelException, IOException {
		String binaryDirectoryPath = jrefProject.getBinaryDirectory().getCanonicalPath();
		String classFilePath = classFile.getCanonicalPath();
		
		String relativeClassFileDirectoryPath = classFilePath.substring(binaryDirectoryPath.length());
		if(!relativeClassFileDirectoryPath.isEmpty() && relativeClassFileDirectoryPath.charAt(0) == File.separatorChar){
			relativeClassFileDirectoryPath = relativeClassFileDirectoryPath.substring(1);
		}
		
		String sourceFileName = classFile.getName();
		relativeClassFileDirectoryPath = relativeClassFileDirectoryPath.substring(0, relativeClassFileDirectoryPath.indexOf(sourceFileName));
		sourceFileName = sourceFileName.replace(".class", ".java");
		File sourceFile = new File(jrefProject.getSourceDirectory().getCanonicalPath() + File.separator + relativeClassFileDirectoryPath + File.separator + sourceFileName);
		return sourceFile;
	}
	
	/**
	 * Returns the corresponding class file for the given compilation units of the project
	 * @param jrefProject
	 * @param compilationUnits
	 * @return
	 * @throws JavaModelException
	 * @throws IOException
	 */
	public static final File getCorrespondingClassFile(JReFrameworkerProject jrefProject, File sourceFile) throws JavaModelException, IOException {
		String sourceDirectory = jrefProject.getSourceDirectory().getCanonicalPath();
		String relativeSourceFileDirectoryPath = sourceFile.getParentFile().getCanonicalPath().substring(sourceDirectory.length());
		if(!relativeSourceFileDirectoryPath.isEmpty() && relativeSourceFileDirectoryPath.charAt(0) == File.separatorChar){
			relativeSourceFileDirectoryPath = relativeSourceFileDirectoryPath.substring(1);
		}
		String classFileName = sourceFile.getName().replace(".java", ".class");
		File classFile = new File(jrefProject.getBinaryDirectory().getCanonicalPath() + File.separator + relativeSourceFileDirectoryPath + File.separator + classFileName);
		return classFile;
	}
	
	public static int getLastBuildPhase(JReFrameworkerProject jrefProject) throws IOException {
		int phase=1;
		while(getBuildPhaseDirectory(jrefProject, phase).exists()){
			phase++;
		}
		if(phase != 1){
			return phase-1;
		} else {
			return 1;
		}
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
			if(defineTypeAnnotation != null) {
				// TODO: investigate NPE here, defineTypeAnnotation is null somehow...
				phases.add(defineTypeAnnotation.getPhase()); 
			} else {
				Log.error("DefineTypeAnnotation is null", new RuntimeException("DefineTypeAnnotation is null"));
			}
			// define field, define method must have the same phase as the define type annotation
			// so we can't discover new phases by looking at the body
		}
		
		// if no phases were detected add the default build phase
		if(phases.isEmpty()){
			phases.add(1);
		}
		
		ArrayList<Integer> phasesSorted = new ArrayList<Integer>(phases);
		Collections.sort(phasesSorted);
		return phasesSorted;
	}
	
	public static final boolean hasTopLevelAnnotation(ClassNode classNode) throws IOException {
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
		
		return false;
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
