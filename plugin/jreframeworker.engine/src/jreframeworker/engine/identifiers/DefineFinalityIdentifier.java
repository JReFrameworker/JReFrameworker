package jreframeworker.engine.identifiers;
import java.util.LinkedList;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class DefineFinalityIdentifier {

	public static final String TARGET = "target";
	public static final String FINALITY = "finality";
	
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
				if(checker.isDefineTypeFinalityAnnotation()){
					String targetValue = null;
					Boolean finalityValue = null;
					if (annotation.values != null) {
				        for (int i = 0; i < annotation.values.size(); i += 2) {
				            String name = (String) annotation.values.get(i);
				            Object value = annotation.values.get(i + 1);
				            if(name.equals(TARGET)){
				            	targetValue = ((String)value).replaceAll("\\.", "/");
				            } else if(name.equals(FINALITY)){
				            	finalityValue = (boolean) value;
				            }
				        }
				        if(targetValue != null && finalityValue != null){
				        	targetTypes.add(new DefineTypeFinalityAnnotation(targetValue, finalityValue));
				        }
				    }
				} else if(checker.isDefineMethodFinalityAnnotation()){
					String targetValue = null;
					Boolean finalityValue = null;
					if (annotation.values != null) {
				        for (int i = 0; i < annotation.values.size(); i += 2) {
				            String name = (String) annotation.values.get(i);
				            Object value = annotation.values.get(i + 1);
				            if(name.equals(TARGET)){
				            	targetValue = ((String)value).replaceAll("\\.", "/");
				            } else if(name.equals(FINALITY)){
				            	finalityValue = (boolean) value;
				            }
				        }
				        if(targetValue != null && finalityValue != null){
				        	String className = classNode.superName.replaceAll("\\.", "/");
				        	targetMethods.add(new DefineMethodFinalityAnnotation(className, targetValue, finalityValue));
				        }
				    }
				} else if(checker.isDefineFieldFinalityAnnotation()){
					String targetValue = null;
					Boolean finalityValue = null;
					if (annotation.values != null) {
				        for (int i = 0; i < annotation.values.size(); i += 2) {
				            String name = (String) annotation.values.get(i);
				            Object value = annotation.values.get(i + 1);
				            if(name.equals(TARGET)){
				            	targetValue = ((String)value).replaceAll("\\.", "/");
				            } else if(name.equals(FINALITY)){
				            	finalityValue = (boolean) value;
				            }
				        }
				        if(targetValue != null && finalityValue != null){
				        	String className = classNode.superName.replaceAll("\\.", "/");
				        	targetFields.add(new DefineFieldFinalityAnnotation(className, targetValue, finalityValue));
				        }
				    }
				} 	
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
