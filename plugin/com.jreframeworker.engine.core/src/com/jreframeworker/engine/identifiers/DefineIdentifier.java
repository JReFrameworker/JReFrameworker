package com.jreframeworker.engine.identifiers;
import java.util.LinkedList;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.FieldNode;

public class DefineIdentifier {
	
	private static final String PHASE = "phase";
	
	public static class DefineTypeAnnotation {
		private int phase;
		private ClassNode classNode;
		
		public DefineTypeAnnotation(int phase, ClassNode classNode) {
			this.phase = phase;
			this.classNode = classNode;
		}
		
		public int getPhase(){
			return phase;
		}
		
		public ClassNode getClassNode(){
			return classNode;
		}
	}

	public static class DefineMethodAnnotation {
		private MethodNode methodNode;
		
		public DefineMethodAnnotation(MethodNode methodNode) {
			this.methodNode = methodNode;
		}
		
		public MethodNode getMethodNode(){
			return methodNode;
		}
	}

	public static class DefineFieldAnnotation {
		private FieldNode fieldNode;
		
		public DefineFieldAnnotation(FieldNode fieldNode) {
			this.fieldNode = fieldNode;
		}
		
		public FieldNode getFieldNode(){
			return fieldNode;
		}
	}
	
	private DefineTypeAnnotation targetType = null;
	private LinkedList<DefineMethodAnnotation> targetMethods = new LinkedList<DefineMethodAnnotation>();
	private LinkedList<DefineFieldAnnotation> targetFields = new LinkedList<DefineFieldAnnotation>();

	public DefineIdentifier(ClassNode classNode) {
		if (classNode.invisibleAnnotations != null) {
			for (Object annotationObject : classNode.invisibleAnnotations) {
				AnnotationNode annotation = (AnnotationNode) annotationObject;
				JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
				checker.visitAnnotation(annotation.desc, false);
				
				// types
				if(checker.isDefineTypeAnnotation()){
					extractDefineTypeAnnotationValues(classNode, annotation);
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
					if(checker.isDefineMethodAnnotation()){
						extractDefineMethodValues(methodNode, annotation);
					}
				}
			}
    	}
		
		// fields
		for (Object o : classNode.fields) {
			FieldNode fieldNode = (FieldNode) o;
			if (fieldNode.invisibleAnnotations != null) {
				for (Object annotationObject : fieldNode.invisibleAnnotations) {
					AnnotationNode annotation = (AnnotationNode) annotationObject;
					JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
					checker.visitAnnotation(annotation.desc, false);
					if(checker.isDefineFieldAnnotation()){
						extractDefineFieldValues(fieldNode, annotation);
					} 
				}
			}
    	}
	}

	private void extractDefineFieldValues(FieldNode fieldNode, AnnotationNode annotation) {
		if(fieldNode != null){
	    	targetFields.add(new DefineFieldAnnotation(fieldNode));
	    }
	}

	private void extractDefineMethodValues(MethodNode methodNode, AnnotationNode annotation) {
		if(methodNode != null){
	    	targetMethods.add(new DefineMethodAnnotation(methodNode));
	    }
	}

	private void extractDefineTypeAnnotationValues(ClassNode classNode, AnnotationNode annotation) {
		int phaseValue = 1; // default to 1
		if (annotation.values != null) {
		    for (int i = 0; i < annotation.values.size(); i += 2) {
		        String name = (String) annotation.values.get(i);
		        Object value = annotation.values.get(i + 1);
		        if(name.equals(PHASE)){
		        	phaseValue = (int) value;
		        }
		    }
		    if(classNode != null){
		    	targetType = new DefineTypeAnnotation(phaseValue, classNode);
		    }
		}
	}

	public DefineTypeAnnotation getDefineTypeAnnotation() {
		return targetType;
	}

	public LinkedList<DefineMethodAnnotation> getDefineMethodAnnotations() {
		return targetMethods;
	}

	public LinkedList<DefineFieldAnnotation> getDefineFieldAnnotations() {
		return targetFields;
	}
    
}
