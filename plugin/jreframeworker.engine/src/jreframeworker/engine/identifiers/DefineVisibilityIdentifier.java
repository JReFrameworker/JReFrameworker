package jreframeworker.engine.identifiers;
import java.util.LinkedList;

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
	
	private static final String TYPE = "type";
	private static final String FIELD = "field";
	private static final String METHOD = "method";
	private static final String VISIBILITY = "visibility";
	
	public static class DefineTypeVisibilityAnnotation {
		private String className;
		private Visibility visibility;
		
		public DefineTypeVisibilityAnnotation(String className, Visibility visibility) {
			this.className = className;
			this.visibility = visibility;
		}
		
		public String getClassName(){
			return className;
		}
		
		public Visibility getVisibility(){
			return visibility;
		}
	}
	
	public static class DefineMethodVisibilityAnnotation {
		private String className;
		private String methodName;
		private Visibility visibility;
		
		public DefineMethodVisibilityAnnotation(String className, String methodName, Visibility visibility) {
			this.className = className;
			this.methodName = methodName;
			this.visibility = visibility;
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
		private String className;
		private String fieldName;
		private Visibility visibility;
		
		public DefineFieldVisibilityAnnotation(String className, String fieldName, Visibility visibility) {
			this.className = className;
			this.fieldName = fieldName;
			this.visibility = visibility;
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

	public DefineVisibilityIdentifier(ClassNode classNode) {
		if (classNode.invisibleAnnotations != null) {
			for (Object annotationObject : classNode.invisibleAnnotations) {
				AnnotationNode annotation = (AnnotationNode) annotationObject;
				JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
				checker.visitAnnotation(annotation.desc, false);
				if(checker.isDefineTypeVisibilityAnnotation()){
					String typeValue = null;
					Visibility visibilityValue = null;
					if (annotation.values != null) {
				        for (int i = 0; i < annotation.values.size(); i += 2) {
				            String name = (String) annotation.values.get(i);
				            Object value = annotation.values.get(i + 1);
				            if(name.equals(TYPE)){
				            	typeValue = ((String)value).replaceAll("\\.", "/");
				            } else if(name.equals(VISIBILITY)){
				            	String valueString = (String) value;
				            	visibilityValue = Visibility.getVisibilityFromString(valueString);
				            }
				        }
				        if(typeValue != null && visibilityValue != null){
				        	String className = typeValue;
				        	if(className.equals("")){
				        		 classNode.superName.replaceAll("\\.", "/");
				        	}
				        	targetTypes.add(new DefineTypeVisibilityAnnotation(className, visibilityValue));
				        }
				    }
				} else if(checker.isDefineMethodVisibilityAnnotation()){
					String typeValue = null;
					String methodValue = null;
					Visibility visibilityValue = null;
					if (annotation.values != null) {
				        for (int i = 0; i < annotation.values.size(); i += 2) {
				            String name = (String) annotation.values.get(i);
				            Object value = annotation.values.get(i + 1);
				            if(name.equals(TYPE)){
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
				        		 classNode.superName.replaceAll("\\.", "/");
				        	}
				        	targetMethods.add(new DefineMethodVisibilityAnnotation(className, methodValue, visibilityValue));
				        }
				    }
				} else if(checker.isDefineFieldVisibilityAnnotation()){
					String typeValue = null;
					String fieldValue = null;
					Visibility visibilityValue = null;
					if (annotation.values != null) {
				        for (int i = 0; i < annotation.values.size(); i += 2) {
				            String name = (String) annotation.values.get(i);
				            Object value = annotation.values.get(i + 1);
				            if(name.equals(TYPE)){
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
				        		 classNode.superName.replaceAll("\\.", "/");
				        	}
				        	targetFields.add(new DefineFieldVisibilityAnnotation(className, fieldValue, visibilityValue));
				        }
				    }
				} 	
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
