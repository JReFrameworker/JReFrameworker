package com.jreframeworker.engine.identifiers;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class DefineVisibilityIdentifier {

	public static enum Visibility {
		PRIVATE, PROTECTED, PUBLIC;
		
		public static Visibility getVisibilityFromString(String valueString) {
			if(valueString.equalsIgnoreCase("private")){
				return Visibility.PRIVATE;
			} else if(valueString.equalsIgnoreCase("protected")){
				return Visibility.PROTECTED;
			} else if(valueString.equalsIgnoreCase("public")){
				return Visibility.PUBLIC;
			} else {
				throw new RuntimeException("Invalid visibility modifier");
			}
		}
	}
	
	public static Set<String> getVisibilityTargets(ClassNode classNode) throws IOException {
		DefineVisibilityIdentifier visibilityIdentifier = new DefineVisibilityIdentifier(classNode);
		Set<String> targets = new HashSet<String>();
		for(DefineTypeVisibilityAnnotation annotation : visibilityIdentifier.getTargetTypes()){
			targets.add(annotation.getClassName());
		}
		for(DefineMethodVisibilityAnnotation annotation : visibilityIdentifier.getTargetMethods()){
			targets.add(annotation.getClassName());
		}
		for(DefineFieldVisibilityAnnotation annotation : visibilityIdentifier.getTargetFields()){
			targets.add(annotation.getClassName());
		}
		return targets;
	}
	
	public static Set<String> getVisibilityTargets(ClassNode classNode, int phase) throws IOException {
		DefineVisibilityIdentifier visibilityIdentifier = new DefineVisibilityIdentifier(classNode);
		Set<String> targets = new HashSet<String>();
		for(DefineTypeVisibilityAnnotation annotation : visibilityIdentifier.getTargetTypes()){
			if(annotation.getPhase() == phase){
				targets.add(annotation.getClassName());
			}
		}
		for(DefineMethodVisibilityAnnotation annotation : visibilityIdentifier.getTargetMethods()){
			if(annotation.getPhase() == phase){
				targets.add(annotation.getClassName());
			}
		}
		for(DefineFieldVisibilityAnnotation annotation : visibilityIdentifier.getTargetFields()){
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
	private static final String VISIBILITY = "visibility";
	
	public static class DefineTypeVisibilityAnnotation {
		private int phase;
		private String className;
		private Visibility visibility;
		
		public DefineTypeVisibilityAnnotation(int phase, String className, Visibility visibility) {
			this.phase = phase;
			this.className = className;
			this.visibility = visibility;
		}
		
		public int getPhase(){
			return phase;
		}
		
		public String getClassName(){
			return className;
		}
		
		public Visibility getVisibility(){
			return visibility;
		}
	}
	
	public static class DefineMethodVisibilityAnnotation {
		private int phase;
		private String className;
		private String methodName;
		private Visibility visibility;
		
		public DefineMethodVisibilityAnnotation(int phase, String className, String methodName, Visibility visibility) {
			this.phase = phase;
			this.className = className;
			this.methodName = methodName;
			this.visibility = visibility;
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
		
		public Visibility getVisibility(){
			return visibility;
		}
	}
	
	public static class DefineFieldVisibilityAnnotation {
		private int phase;
		private String className;
		private String fieldName;
		private Visibility visibility;
		
		public DefineFieldVisibilityAnnotation(int phase, String className, String fieldName, Visibility visibility) {
			this.phase = phase;
			this.className = className;
			this.fieldName = fieldName;
			this.visibility = visibility;
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
		
		public Visibility getVisibility(){
			return visibility;
		}
	}
	
	private LinkedList<DefineTypeVisibilityAnnotation> targetTypes = new LinkedList<DefineTypeVisibilityAnnotation>();
	private LinkedList<DefineMethodVisibilityAnnotation> targetMethods = new LinkedList<DefineMethodVisibilityAnnotation>();
	private LinkedList<DefineFieldVisibilityAnnotation> targetFields = new LinkedList<DefineFieldVisibilityAnnotation>();

	@SuppressWarnings("rawtypes")
	public DefineVisibilityIdentifier(ClassNode classNode) {
		if (classNode.invisibleAnnotations != null) {
			for (Object annotationObject : classNode.invisibleAnnotations) {
				AnnotationNode annotation = (AnnotationNode) annotationObject;
				JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
				checker.visitAnnotation(annotation.desc, false);
				
				// type visibilities
				if(checker.isDefineTypeVisibilitiesAnnotation()){
					for(Object value : annotation.values){
						if(value instanceof List){
							for(Object valueObject : (List) value){
								if(valueObject instanceof AnnotationNode){
									AnnotationNode annotationValue = (AnnotationNode) valueObject;
									extractDefineTypeVisibilityAnnotationValues(classNode, annotationValue);
								}
							}
						}
						
					}
				} else if(checker.isDefineTypeVisibilityAnnotation()){
					extractDefineTypeVisibilityAnnotationValues(classNode, annotation);
				} 
				
				// method visibilities
				else if(checker.isDefineMethodVisibilitiesAnnotation()){
					for(Object value : annotation.values){
						if(value instanceof List){
							for(Object valueObject : (List) value){
								if(valueObject instanceof AnnotationNode){
									AnnotationNode annotationValue = (AnnotationNode) valueObject;
									extractDefineMethodVisibilityAnnotationValues(classNode, annotationValue);
								}
							}
						}
						
					}
				} else if(checker.isDefineMethodVisibilityAnnotation()){
					extractDefineMethodVisibilityAnnotationValues(classNode, annotation);
				} 
				
				// field visibilities
				else if(checker.isDefineFieldVisibilitiesAnnotation()){
					for(Object value : annotation.values){
						if(value instanceof List){
							for(Object valueObject : (List) value){
								if(valueObject instanceof AnnotationNode){
									AnnotationNode annotationValue = (AnnotationNode) valueObject;
									extractDefineFieldVisibilityAnnotationValues(classNode, annotationValue);
								}
							}
						}
						
					}
				} else if(checker.isDefineFieldVisibilityAnnotation()){
					extractDefineFieldVisibilityAnnotationValues(classNode, annotation);
				} 	
			}
		}
    }

	private void extractDefineFieldVisibilityAnnotationValues(ClassNode classNode, AnnotationNode annotation) {
		int phaseValue = 1; // default to 1
		String typeValue = null;
		String fieldValue = null;
		Visibility visibilityValue = null;
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
		        } else if(name.equals(VISIBILITY)){
		        	String valueString = (String) value;
		        	visibilityValue = Visibility.getVisibilityFromString(valueString);
		        }
		    }
		    if(typeValue != null && fieldValue != null && visibilityValue != null){
		    	String className = typeValue;
		    	if(className.equals("")){
		    		className = classNode.superName;
		    	}
		    	targetFields.add(new DefineFieldVisibilityAnnotation(phaseValue, className, fieldValue, visibilityValue));
		    }
		}
	}

	private void extractDefineMethodVisibilityAnnotationValues(ClassNode classNode, AnnotationNode annotation) {
		int phaseValue = 1; // default to 1
		String typeValue = null;
		String methodValue = null;
		Visibility visibilityValue = null;
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
		        } else if(name.equals(VISIBILITY)){
		        	String valueString = (String) value;
		        	visibilityValue = Visibility.getVisibilityFromString(valueString);
		        }
		    }
		    if(typeValue != null && methodValue != null && visibilityValue != null){
		    	String className = typeValue;
		    	if(className.equals("")){
		    		className = classNode.superName;
		    	}
		    	targetMethods.add(new DefineMethodVisibilityAnnotation(phaseValue, className, methodValue, visibilityValue));
		    }
		}
	}

	private void extractDefineTypeVisibilityAnnotationValues(ClassNode classNode, AnnotationNode annotation) {
		int phaseValue = 1; // default to 1
		String typeValue = null;
		Visibility visibilityValue = null;
		if (annotation.values != null) {
		    for (int i = 0; i < annotation.values.size(); i += 2) {
		        String name = (String) annotation.values.get(i);
		        Object value = annotation.values.get(i + 1);
		        if(name.equals(PHASE)){
		        	phaseValue = (int) value;
		        } else if(name.equals(TYPE)){
		        	typeValue = ((String)value).replaceAll("\\.", "/");
		        } else if(name.equals(VISIBILITY)){
		        	String valueString = (String) value;
		        	visibilityValue = Visibility.getVisibilityFromString(valueString);
		        }
		    }
		    if(typeValue != null && visibilityValue != null){
		    	String className = typeValue;
		    	if(className.equals("")){
		    		className = classNode.superName;
		    	}
		    	targetTypes.add(new DefineTypeVisibilityAnnotation(phaseValue, className, visibilityValue));
		    }
		}
	}
	
	public LinkedList<DefineTypeVisibilityAnnotation> getTargetTypes() {
		return targetTypes;
	}
	
    public LinkedList<DefineMethodVisibilityAnnotation> getTargetMethods() {
		return targetMethods;
	}
    
    public LinkedList<DefineFieldVisibilityAnnotation> getTargetFields() {
		return targetFields;
	}
}
