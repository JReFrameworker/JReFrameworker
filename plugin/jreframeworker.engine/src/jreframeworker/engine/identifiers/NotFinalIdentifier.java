package jreframeworker.engine.identifiers;
import java.util.LinkedList;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class NotFinalIdentifier {

	public static final String TARGET = "target";
	
	public static class NotFinalTypeAnnotation {
		private String className;
		
		public NotFinalTypeAnnotation(String className) {
			this.className = className;
		}
		
		public String getClassName(){
			return className;
		}
	}
	
	public static class NotFinalMethodAnnotation {
		private String className;
		private String methodName;
		
		public NotFinalMethodAnnotation(String className, String methodName) {
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
	
	public static class NotFinalFieldAnnotation {
		private String className;
		private String fieldName;
		
		public NotFinalFieldAnnotation(String className, String fieldName) {
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
	
	private LinkedList<NotFinalTypeAnnotation> markedTypes = new LinkedList<NotFinalTypeAnnotation>();
	private LinkedList<NotFinalMethodAnnotation> markedMethods = new LinkedList<NotFinalMethodAnnotation>();
	private LinkedList<NotFinalFieldAnnotation> markedFields = new LinkedList<NotFinalFieldAnnotation>();

	public NotFinalIdentifier(ClassNode classNode) {
		if (classNode.invisibleAnnotations != null) {
			for (Object annotationObject : classNode.invisibleAnnotations) {
				AnnotationNode annotation = (AnnotationNode) annotationObject;
				JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
				checker.visitAnnotation(annotation.desc, false);
				if(checker.isNotFinalTypeAnnotation()){
					if (annotation.values != null) {
				        for (int i = 0; i < annotation.values.size(); i += 2) {
				            String name = (String) annotation.values.get(i);
				            Object value = annotation.values.get(i + 1);
				            if(name.equals(TARGET)){
				            	markedTypes.add(new NotFinalTypeAnnotation(value.toString().replaceAll("\\.", "/")));
				            }
				        }
				    }
				} else if(checker.isNotFinalMethodAnnotation()){
					if (annotation.values != null) {
				        for (int i = 0; i < annotation.values.size(); i += 2) {
				            String name = (String) annotation.values.get(i);
				            Object value = annotation.values.get(i + 1);
				            if(name.equals(TARGET)){
				            	markedMethods.add(new NotFinalMethodAnnotation(classNode.superName.replaceAll("\\.", "/"), value.toString()));
				            }
				        }
				    }
				} else if(checker.isNotFinalFieldAnnotation()){
					if (annotation.values != null) {
				        for (int i = 0; i < annotation.values.size(); i += 2) {
				            String name = (String) annotation.values.get(i);
				            Object value = annotation.values.get(i + 1);
				            if(name.equals(TARGET)){
				            	markedFields.add(new NotFinalFieldAnnotation(classNode.superName.replaceAll("\\.", "/"), value.toString()));
				            }
				        }
				    }
				} 	
			}
		}
    }
	
	public LinkedList<NotFinalTypeAnnotation> getMarkedTypes() {
		return markedTypes;
	}
	
    public LinkedList<NotFinalMethodAnnotation> getMarkedMethods() {
		return markedMethods;
	}
    
    public LinkedList<NotFinalFieldAnnotation> getMarkedFields() {
		return markedFields;
	}
}
