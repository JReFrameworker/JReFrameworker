package jreframeworker.engine.identifiers;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class DefineFinalityIdentifier {

	private static final String TYPE = "type";
	private static final String FIELD = "field";
	private static final String METHOD = "method";
	private static final String FINALITY = "finality";
	
	public static class DefineTypeFinalityAnnotation {
		private String className;
		private boolean finality;
		
		public DefineTypeFinalityAnnotation(String className, boolean finality) {
			this.className = className;
			this.finality = finality;
		}
		
		public String getClassName(){
			return className;
		}
		
		public boolean getFinality(){
			return finality;
		}
	}
	
	public static class DefineMethodFinalityAnnotation {
		private String className;
		private String methodName;
		private boolean finality;
		
		public DefineMethodFinalityAnnotation(String className, String methodName, boolean finality) {
			this.className = className;
			this.methodName = methodName;
			this.finality = finality;
		}
		
		public String getClassName(){
			return className;
		}
		
		public String getMethodName(){
			return methodName;
		}
		
		public boolean getFinality(){
			return finality;
		}
	}
	
	public static class DefineFieldFinalityAnnotation {
		private String className;
		private String fieldName;
		private boolean finality;
		
		public DefineFieldFinalityAnnotation(String className, String fieldName, boolean finality) {
			this.className = className;
			this.fieldName = fieldName;
			this.finality = finality;
		}
		
		public String getClassName(){
			return className;
		}
		
		public String getFieldName(){
			return fieldName;
		}
		
		public boolean getFinality(){
			return finality;
		}
	}
	
	private LinkedList<DefineTypeFinalityAnnotation> targetTypes = new LinkedList<DefineTypeFinalityAnnotation>();
	private LinkedList<DefineMethodFinalityAnnotation> targetMethods = new LinkedList<DefineMethodFinalityAnnotation>();
	private LinkedList<DefineFieldFinalityAnnotation> targetFields = new LinkedList<DefineFieldFinalityAnnotation>();

	public DefineFinalityIdentifier(ClassNode classNode) {
		if (classNode.invisibleAnnotations != null) {
			for (Object annotationObject : classNode.invisibleAnnotations) {
				AnnotationNode annotation = (AnnotationNode) annotationObject;
				JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
				checker.visitAnnotation(annotation.desc, false);
				// type finalities
				if(checker.isDefineTypeFinalitiesAnnotation()){
					for(Object value : annotation.values){
						if(value instanceof List){
							for(Object valueObject : (List) value){
								if(valueObject instanceof AnnotationNode){
									AnnotationNode annotationValue = (AnnotationNode) valueObject;
									extractDefineTypeFinalityAnnotationValues(classNode, annotationValue);
								}
							}
						}
						
					}
				} else if(checker.isDefineTypeFinalityAnnotation()){
					extractDefineTypeFinalityAnnotationValues(classNode, annotation);
				} 
				
				// method finalities
				else if(checker.isDefineMethodFinalitiesAnnotation()){
					for(Object value : annotation.values){
						if(value instanceof List){
							for(Object valueObject : (List) value){
								if(valueObject instanceof AnnotationNode){
									AnnotationNode annotationValue = (AnnotationNode) valueObject;
									extractDefineMethodFinalityValues(classNode, annotationValue);
								}
							}
						}
					}
				} else if(checker.isDefineMethodFinalityAnnotation()){
					extractDefineMethodFinalityValues(classNode, annotation);
				}  
				
				// field finalities
				else if(checker.isDefineFieldFinalitiesAnnotation()){
					for(Object value : annotation.values){
						if(value instanceof List){
							for(Object valueObject : (List) value){
								if(valueObject instanceof AnnotationNode){
									AnnotationNode annotationValue = (AnnotationNode) valueObject;
									extractDefineFieldFinalityValues(classNode, annotationValue);
								}
							}
						}
					}
				}  else if(checker.isDefineFieldFinalityAnnotation()){
					extractDefineFieldFinalityValues(classNode, annotation);
				} 	
			}
		}
    }

	private void extractDefineFieldFinalityValues(ClassNode classNode, AnnotationNode annotation) {
		String typeValue = null;
		String fieldValue = null;
		Boolean finalityValue = null;
		if (annotation.values != null) {
		    for (int i = 0; i < annotation.values.size(); i += 2) {
		        String name = (String) annotation.values.get(i);
		        Object value = annotation.values.get(i + 1);
		        if(name.equals(TYPE)){
		        	typeValue = ((String)value).replaceAll("\\.", "/");
		        } else if(name.equals(FIELD)){
		        	fieldValue = (String) value;
		        } else if(name.equals(FINALITY)){
		        	finalityValue = (boolean) value;
		        }
		    }
		    if(typeValue != null && fieldValue != null && finalityValue != null){
		    	String className = typeValue;
		    	if(className.equals("")){
		    		className = classNode.superName;;
		    	}
		    	targetFields.add(new DefineFieldFinalityAnnotation(className, fieldValue, finalityValue));
		    }
		}
	}

	private void extractDefineMethodFinalityValues(ClassNode classNode, AnnotationNode annotation) {
		String typeValue = null;
		String methodValue = null;
		Boolean finalityValue = null;
		if (annotation.values != null) {
		    for (int i = 0; i < annotation.values.size(); i += 2) {
		        String name = (String) annotation.values.get(i);
		        Object value = annotation.values.get(i + 1);
		        if(name.equals(TYPE)){
		        	typeValue = ((String)value).replaceAll("\\.", "/");
		        } else if(name.equals(METHOD)){
		        	methodValue = (String) value;
		        } else if(name.equals(FINALITY)){
		        	finalityValue = (boolean) value;
		        }
		    }
		    if(typeValue != null && methodValue != null && finalityValue != null){
		    	String className = typeValue;
		    	if(className.equals("")){
		    		className = classNode.superName;;
		    	}
		    	targetMethods.add(new DefineMethodFinalityAnnotation(className, methodValue, finalityValue));
		    }
		}
	}

	private void extractDefineTypeFinalityAnnotationValues(ClassNode classNode, AnnotationNode annotation) {
		String typeValue = null;
		Boolean finalityValue = null;
		if (annotation.values != null) {
		    for (int i = 0; i < annotation.values.size(); i += 2) {
		        String name = (String) annotation.values.get(i);
		        Object value = annotation.values.get(i + 1);
		        if(name.equals(TYPE)){
		        	typeValue = ((String)value).replaceAll("\\.", "/");
		        } else if(name.equals(FINALITY)){
		        	finalityValue = (boolean) value;
		        }
		    }
		    if(typeValue != null && finalityValue != null){
		    	String className = typeValue;
		    	if(className.equals("")){
		    		className = classNode.superName;;
		    	}
		    	targetTypes.add(new DefineTypeFinalityAnnotation(className, finalityValue));
		    }
		}
	}
	
	public LinkedList<DefineTypeFinalityAnnotation> getTargetTypes() {
		return targetTypes;
	}
	
    public LinkedList<DefineMethodFinalityAnnotation> getTargetMethods() {
		return targetMethods;
	}
    
    public LinkedList<DefineFieldFinalityAnnotation> getTargetFields() {
		return targetFields;
	}
}
