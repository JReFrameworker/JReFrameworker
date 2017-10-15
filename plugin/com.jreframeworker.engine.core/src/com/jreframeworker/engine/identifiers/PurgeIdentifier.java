package com.jreframeworker.engine.identifiers;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class PurgeIdentifier {
	
	/**
	 * Returns a collection of qualified classes that are marked to be purged or contain
	 * classes, fields, or methods marked to be purged
	 * @param classNode
	 * @return
	 * @throws IOException
	 */
	public static Set<String> getPurgeTargets(ClassNode classNode) throws IOException {
		PurgeIdentifier purgeIdentifier = new PurgeIdentifier(classNode);
		Set<String> targets = new HashSet<String>();
		for(PurgeTypeAnnotation annotation : purgeIdentifier.getPurgeTypeAnnotations()){
			targets.add(annotation.getClassName());
		}
		for(PurgeMethodAnnotation annotation : purgeIdentifier.getPurgeMethodAnnotations()){
			targets.add(annotation.getClassName());
		}
		for(PurgeFieldAnnotation annotation : purgeIdentifier.getPurgeFieldAnnotations()){
			targets.add(annotation.getClassName());
		}
		return targets;
	}
	
	/**
	 * Returns a collection of qualified classes that are marked to be purged or contain
	 * classes, fields, or methods marked to be purged
	 * @param classNode
	 * @return
	 * @throws IOException
	 */
	public static Set<String> getPurgeTargets(ClassNode classNode, int phase) throws IOException {
		PurgeIdentifier purgeIdentifier = new PurgeIdentifier(classNode);
		Set<String> targets = new HashSet<String>();
		for(PurgeTypeAnnotation annotation : purgeIdentifier.getPurgeTypeAnnotations()){
			if(annotation.getPhase() == phase){
				targets.add(annotation.getClassName());
			}
		}
		for(PurgeMethodAnnotation annotation : purgeIdentifier.getPurgeMethodAnnotations()){
			if(annotation.getPhase() == phase){
				targets.add(annotation.getClassName());
			}
		}
		for(PurgeFieldAnnotation annotation : purgeIdentifier.getPurgeFieldAnnotations()){
			if(annotation.getPhase() == phase){
				targets.add(annotation.getClassName());
			}
		}
		return targets;
	}
	
	private static final String PHASE = "phase";
	private static final String TYPE = "type";
	private static final String FIELD = "field";
	private static final String METHOD = "method";
	
	public static class PurgeTypeAnnotation {
		private int phase;
		private String className;
		
		public PurgeTypeAnnotation(int phase, String className) {
			this.phase = phase;
			this.className = className;
		}
		
		public int getPhase(){
			return phase;
		}
		
		public String getClassName(){
			return className;
		}
	}
	
	public static class PurgeMethodAnnotation {
		private int phase;
		private String className;
		private String methodName;
		
		public PurgeMethodAnnotation(int phase, String className, String methodName) {
			this.phase = phase;
			this.className = className;
			this.methodName = methodName;
		}
		
		public int getPhase(){
			return phase;
		}
		
		public String getClassName(){
			return className;
		}
		
		public String getMethodName(){
			return methodName;
		}
	}
	
	public static class PurgeFieldAnnotation {
		private int phase;
		private String className;
		private String fieldName;
		
		public PurgeFieldAnnotation(int phase, String className, String fieldName) {
			this.phase = phase;
			this.className = className;
			this.fieldName = fieldName;
		}
		
		public int getPhase(){
			return phase;
		}
		
		public String getClassName(){
			return className;
		}
		
		public String getFieldName(){
			return fieldName;
		}
	}
	
	private LinkedList<PurgeTypeAnnotation> purgeTypeAnnotations = new LinkedList<PurgeTypeAnnotation>();
	private LinkedList<PurgeMethodAnnotation> purgeMethodAnnotations = new LinkedList<PurgeMethodAnnotation>();
	private LinkedList<PurgeFieldAnnotation> purgeFieldAnnotations = new LinkedList<PurgeFieldAnnotation>();

	@SuppressWarnings("rawtypes")
	public PurgeIdentifier(ClassNode classNode) {
		// a purge annotation must be on a type, putting it on a field or a
		// method is silly since you created it just to purge it
		if (classNode.invisibleAnnotations != null) {
			for (Object annotationObject : classNode.invisibleAnnotations) {
				AnnotationNode annotation = (AnnotationNode) annotationObject;
				JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
				checker.visitAnnotation(annotation.desc, false);
				
				// types
				if(checker.isPurgeTypesAnnotation()){
					for(Object value : annotation.values){
						if(value instanceof List){
							for(Object valueObject : (List) value){
								if(valueObject instanceof AnnotationNode){
									AnnotationNode annotationValue = (AnnotationNode) valueObject;
									extractPurgeTypeAnnotationValues(classNode, annotationValue);
								}
							}
						}
						
					}
				} else if(checker.isPurgeTypeAnnotation()){
					extractPurgeTypeAnnotationValues(classNode, annotation);
				} 
				
				// methods
				else if(checker.isPurgeMethodsAnnotation()){
					for(Object value : annotation.values){
						if(value instanceof List){
							for(Object valueObject : (List) value){
								if(valueObject instanceof AnnotationNode){
									AnnotationNode annotationValue = (AnnotationNode) valueObject;
									extractPurgeMethodValues(classNode, annotationValue);
								}
							}
						}
					}
				} else if(checker.isPurgeMethodAnnotation()){
					extractPurgeMethodValues(classNode, annotation);
				}  
				
				// fields
				else if(checker.isPurgeFieldsAnnotation()){
					for(Object value : annotation.values){
						if(value instanceof List){
							for(Object valueObject : (List) value){
								if(valueObject instanceof AnnotationNode){
									AnnotationNode annotationValue = (AnnotationNode) valueObject;
									extractPurgeFieldValues(classNode, annotationValue);
								}
							}
						}
					}
				}  else if(checker.isPurgeFieldAnnotation()){
					extractPurgeFieldValues(classNode, annotation);
				} 	
			}
		}
    }

	private void extractPurgeFieldValues(ClassNode classNode, AnnotationNode annotation) {
		int phaseValue = 1; // default to 1
		String typeValue = null;
		String fieldValue = null;
		
		if (annotation.values != null) {
		    for (int i = 0; i < annotation.values.size(); i += 2) {
		        String name = (String) annotation.values.get(i);
		        Object value = annotation.values.get(i + 1);
		        if(name.equals(PHASE)){
		        	phaseValue = (int) value;
		        } else if(name.equals(TYPE)){
		        	typeValue = ((String)value).replaceAll("\\.", "/");
		        } else if(name.equals(FIELD)){
		        	fieldValue = (String) value;
		        }
		    }
		    if(typeValue != null && fieldValue != null){
		    	String className = typeValue;
		    	if(className.equals("")){
		    		className = classNode.superName;
		    	}
		    	purgeFieldAnnotations.add(new PurgeFieldAnnotation(phaseValue, className, fieldValue));
		    }
		}
	}

	private void extractPurgeMethodValues(ClassNode classNode, AnnotationNode annotation) {
		int phaseValue = 1; // default to 1
		String typeValue = null;
		String methodValue = null;
		
		if (annotation.values != null) {
		    for (int i = 0; i < annotation.values.size(); i += 2) {
		        String name = (String) annotation.values.get(i);
		        Object value = annotation.values.get(i + 1);
		        if(name.equals(PHASE)){
		        	phaseValue = (int) value;
		        } else if(name.equals(TYPE)){
		        	typeValue = ((String)value).replaceAll("\\.", "/");
		        } else if(name.equals(METHOD)){
		        	methodValue = (String) value;
		        }
		    }
		    if(typeValue != null && methodValue != null){
		    	String className = typeValue;
		    	if(className.equals("")){
		    		className = classNode.superName;
		    	}
		    	purgeMethodAnnotations.add(new PurgeMethodAnnotation(phaseValue, className, methodValue));
		    }
		}
	}

	private void extractPurgeTypeAnnotationValues(ClassNode classNode, AnnotationNode annotation) {
		int phaseValue = 1; // default to 1
		String typeValue = null;
		
		if (annotation.values != null) {
		    for (int i = 0; i < annotation.values.size(); i += 2) {
		        String name = (String) annotation.values.get(i);
		        Object value = annotation.values.get(i + 1);
		        if(name.equals(PHASE)){
		        	phaseValue = (int) value;
		        } else if(name.equals(TYPE)){
		        	typeValue = ((String)value).replaceAll("\\.", "/");
		        }
		    }
		    if(typeValue != null){
		    	String className = typeValue;
		    	if(className.equals("")){
		    		className = classNode.superName;
		    	}
		    	purgeTypeAnnotations.add(new PurgeTypeAnnotation(phaseValue, className));
		    }
		}
	}
	
	public LinkedList<PurgeTypeAnnotation> getPurgeTypeAnnotations() {
		return purgeTypeAnnotations;
	}
	
    public LinkedList<PurgeMethodAnnotation> getPurgeMethodAnnotations() {
		return purgeMethodAnnotations;
	}
    
    public LinkedList<PurgeFieldAnnotation> getPurgeFieldAnnotations() {
		return purgeFieldAnnotations;
	}
    
}
