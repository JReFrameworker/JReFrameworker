package jreframeworker.builder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

	public static File getBuildPhaseDirectory(JReFrameworkerProject jrefProject, int buildPhase, int namedBuildPhase) throws IOException {
		File projectBuildDirectory = jrefProject.getProject().getFolder(JReFrameworker.BUILD_DIRECTORY).getLocation().toFile();
		String buildPhaseDirectoryName = JReFrameworker.BUILD_PHASE_DIRECTORY_PREFIX + "-" + buildPhase + "-" + namedBuildPhase;
		return new File(projectBuildDirectory.getCanonicalPath() + File.separatorChar + buildPhaseDirectoryName);
	}
	
	public static File getBuildPhaseJar(String targetJar, JReFrameworkerProject jrefProject, int buildPhase, int namedBuildPhase) throws IOException {
		return new File(getBuildPhaseDirectory(jrefProject, buildPhase, namedBuildPhase).getCanonicalPath() + File.separatorChar + targetJar);
	}

	public static Map<Integer,Integer> getNormalizedBuildPhases(Set<File> classFiles) throws IOException {
		Integer[] phases = getBuildPhases(classFiles);
		Map<Integer,Integer> normalizedPhases = new HashMap<Integer,Integer>();
		Integer normalizedPhase = 1;
		for(Integer phase : phases){
			normalizedPhases.put(normalizedPhase++, phase);
		}
		return normalizedPhases;
	}

	public static Integer[] getBuildPhases(Set<File> classFiles) throws IOException {
		Set<Integer> phases = new HashSet<Integer>();
		for(File classFile : classFiles){
			byte[] classBytes = Files.readAllBytes(classFile.toPath());
			if(classBytes.length > 0){
				try {
					ClassNode classNode = BytecodeUtils.getClassNode(classBytes);
					
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
			}
		}
		
		ArrayList<Integer> phasesSorted = new ArrayList<Integer>(phases);
		Collections.sort(phasesSorted);
		Integer[] result = new Integer[phasesSorted.size()];
		phases.toArray(result);
		return result;
	}
	
	public static boolean hasTypeModification(File classFile) {
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
	
	public static boolean hasMergeTypeModification(ClassNode classNode) throws IOException {
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
	
	public static boolean hasDefineTypeModification(ClassNode classNode) throws IOException {
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
	
	public static boolean hasPurgeModification(ClassNode classNode) throws IOException {
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
	
	public static boolean hasFinalityModification(ClassNode classNode) throws IOException {
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
	
	public static boolean hasVisibilityModification(ClassNode classNode) throws IOException {
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
