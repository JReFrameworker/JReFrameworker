package com.jreframeworker.engine.identifiers;
import java.util.LinkedList;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MergeIdentifier {
	
	private static final String PHASE = "phase";
	private static final String SUPERTYPE = "supertype";
	
	private MergeTypeAnnotation mergeTypeAnnotation = null;
	private LinkedList<MergeMethodAnnotation> mergeMethodAnnotations = new LinkedList<MergeMethodAnnotation>();
	
	public MergeIdentifier(ClassNode classNode) {
		// types
		if (classNode.invisibleAnnotations != null) {
			for (Object annotationObject : classNode.invisibleAnnotations) {
				AnnotationNode annotation = (AnnotationNode) annotationObject;
				JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
				checker.visitAnnotation(annotation.desc, false);
				if(checker.isMergeTypeAnnotation()){
					extractMergeTypeAnnotationValues(classNode, annotation);
				}	
			}
		}
		
		// methods
    	for (Object o : classNode.methods) {
			MethodNode methodNode = (MethodNode) o;
			if (methodNode.invisibleAnnotations != null) {
				for (Object annotationObject : methodNode.invisibleAnnotations) {
					AnnotationNode annotation = (AnnotationNode) annotationObject;
					JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
					checker.visitAnnotation(annotation.desc, false);
					if(checker.isMergeMethodAnnotation()){
						extractMergeMethodAnnotationValues(methodNode, annotation);
					}
				}
			}
    	}
    }
    
    public static class MergeTypeAnnotation {
    	private int phase;
		private String supertype;
		
		public MergeTypeAnnotation(int phase, String supertype) {
			this.phase = phase;
			this.supertype = supertype;
		}
		
		public int getPhase(){
			return phase;
		}
		
		public String getSupertype(){
			return supertype;
		}
	}
	
    public static class MergeMethodAnnotation {
    	private MethodNode methodNode;
		
		public MergeMethodAnnotation(MethodNode methodNode) {
			this.methodNode = methodNode;
		}
		
		public MethodNode getMethodNode(){
			return methodNode;
		}
	}
    
    private void extractMergeMethodAnnotationValues(MethodNode methodNode, AnnotationNode annotation){
		if(methodNode != null){
			mergeMethodAnnotations.add(new MergeMethodAnnotation(methodNode));
        }
	}
    
	private void extractMergeTypeAnnotationValues(ClassNode classNode, AnnotationNode annotation){
		if(classNode != null){
			int phaseValue = 1; // default to 1
			String superTypeValue = null;
			if(annotation.values != null){
		        for (int i = 0; i < annotation.values.size(); i += 2) {
		            String name = (String) annotation.values.get(i);
		            Object value = annotation.values.get(i + 1);
		            if(name.equals(PHASE)){
			        	phaseValue = (int) value;
			        } else if(name.equals(SUPERTYPE)){
		            	superTypeValue = ((String)value).replaceAll("\\.", "/");
		            }
		        }
		    }
			if(superTypeValue == null || superTypeValue.equals("")){
				superTypeValue = classNode.superName;
	        }
			mergeTypeAnnotation = new MergeTypeAnnotation(phaseValue, superTypeValue);
		}
	}
	
	public MergeTypeAnnotation getMergeTypeAnnotation() {
		return mergeTypeAnnotation;
	}
	
    public LinkedList<MergeMethodAnnotation> getMergeMethodAnnotations() {
		return mergeMethodAnnotations;
	}
    
}
