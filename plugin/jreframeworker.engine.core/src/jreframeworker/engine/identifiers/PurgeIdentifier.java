package jreframeworker.engine.identifiers;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class PurgeIdentifier {
	
	public static Set<String> getPurgeTargets(ClassNode classNode) throws IOException {
		PurgeIdentifier purgeIdentifier = new PurgeIdentifier(classNode);
		Set<String> targets = new HashSet<String>();
		for(PurgeTypeAnnotation annotation : purgeIdentifier.getTargetTypes()){
			targets.add(annotation.getClassName());
		}
		for(PurgeMethodAnnotation annotation : purgeIdentifier.getTargetMethods()){
			targets.add(annotation.getClassName());
		}
		for(PurgeFieldAnnotation annotation : purgeIdentifier.getTargetFields()){
			targets.add(annotation.getClassName());
		}
		return targets;
	}
	
	private static final String TYPE = "type";
	private static final String FIELD = "field";
	private static final String METHOD = "method";
	
	public static class PurgeTypeAnnotation {
		private String className;
		
		public PurgeTypeAnnotation(String className) {
			this.className = className;
		}
		
		public String getClassName(){
			return className;
		}
	}
	
	public static class PurgeMethodAnnotation {
		private String className;
		private String methodName;
		
		public PurgeMethodAnnotation(String className, String methodName) {
			this.className = className;
			this.methodName = methodName;
		}
		
		public String getClassName(){
			return className;
		}
		
		public String getMethodName(){
			return methodName;
		}
	}
	
	public static class PurgeFieldAnnotation {
		private String className;
		private String fieldName;
		
		public PurgeFieldAnnotation(String className, String fieldName) {
			this.className = className;
			this.fieldName = fieldName;
		}
		
		public String getClassName(){
			return className;
		}
		
		public String getFieldName(){
			return fieldName;
		}
	}
	
	private LinkedList<PurgeTypeAnnotation> targetTypes = new LinkedList<PurgeTypeAnnotation>();
	private LinkedList<PurgeMethodAnnotation> targetMethods = new LinkedList<PurgeMethodAnnotation>();
	private LinkedList<PurgeFieldAnnotation> targetFields = new LinkedList<PurgeFieldAnnotation>();

	@SuppressWarnings("rawtypes")
	public PurgeIdentifier(ClassNode classNode) {
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
		String typeValue = null;
		String fieldValue = null;
		
		if (annotation.values != null) {
		    for (int i = 0; i < annotation.values.size(); i += 2) {
		        String name = (String) annotation.values.get(i);
		        Object value = annotation.values.get(i + 1);
		        if(name.equals(TYPE)){
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
		    	targetFields.add(new PurgeFieldAnnotation(className, fieldValue));
		    }
		}
	}

	private void extractPurgeMethodValues(ClassNode classNode, AnnotationNode annotation) {
		String typeValue = null;
		String methodValue = null;
		
		if (annotation.values != null) {
		    for (int i = 0; i < annotation.values.size(); i += 2) {
		        String name = (String) annotation.values.get(i);
		        Object value = annotation.values.get(i + 1);
		        if(name.equals(TYPE)){
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
		    	targetMethods.add(new PurgeMethodAnnotation(className, methodValue));
		    }
		}
	}

	private void extractPurgeTypeAnnotationValues(ClassNode classNode, AnnotationNode annotation) {
		String typeValue = null;
		
		if (annotation.values != null) {
		    for (int i = 0; i < annotation.values.size(); i += 2) {
		        String name = (String) annotation.values.get(i);
		        Object value = annotation.values.get(i + 1);
		        if(name.equals(TYPE)){
		        	typeValue = ((String)value).replaceAll("\\.", "/");
		        }
		    }
		    if(typeValue != null){
		    	String className = typeValue;
		    	if(className.equals("")){
		    		className = classNode.superName;
		    	}
		    	targetTypes.add(new PurgeTypeAnnotation(className));
		    }
		}
	}
	
	public LinkedList<PurgeTypeAnnotation> getTargetTypes() {
		return targetTypes;
	}
	
    public LinkedList<PurgeMethodAnnotation> getTargetMethods() {
		return targetMethods;
	}
    
    public LinkedList<PurgeFieldAnnotation> getTargetFields() {
		return targetFields;
	}
    
}
