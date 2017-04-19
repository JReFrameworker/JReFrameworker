package jreframeworker.engine.identifiers;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class JREFAnnotationIdentifier extends ClassVisitor {

	private static final String SUPERTYPE = "supertype";
	
	private static final String DEFINE_TYPE_ANNOTATION = "Ljreframeworker/annotations/types/DefineType;";
	private static final String DEFINE_TYPE_FINALITY_ANNOTATION = "Ljreframeworker/annotations/types/DefineTypeFinality;";
	private static final String DEFINE_TYPE_VISIBILITY_ANNOTATION = "Ljreframeworker/annotations/types/DefineTypeVisibility;";
	private static final String MERGE_TYPE_ANNOTATION = "Ljreframeworker/annotations/types/MergeType;";
	
	private static final String DEFINE_FIELD_ANNOTATION = "Ljreframeworker/annotations/fields/DefineField;";
	private static final String DEFINE_FIELD_FINALITY_ANNOTATION = "Ljreframeworker/annotations/fields/DefineFieldFinality;";
	private static final String DEFINE_FIELD_VISIBILITY_ANNOTATION = "Ljreframeworker/annotations/fields/DefineFieldVisibility;";
	
	private static final String DEFINE_METHOD_ANNOTATION = "Ljreframeworker/annotations/methods/DefineMethod;";
	private static final String DEFINE_METHOD_FINALITY_ANNOTATION = "Ljreframeworker/annotations/methods/DefineMethodFinality;";
	private static final String DEFINE_METHOD_VISIBILITY_ANNOTATION = "Ljreframeworker/annotations/methods/DefineMethodVisibility;";
	private static final String MERGE_METHOD_ANNOTATION = "Ljreframeworker/annotations/methods/MergeMethod;";
	
	private boolean isDefineTypeAnnotation = false;
	private boolean isDefineFieldAnnotation = false;
	private boolean isDefineMethodAnnotation = false;
	
	private boolean isDefineTypeFinalityAnnotation = false;
	private boolean isDefineFieldFinalityAnnotation = false;
	private boolean isDefineMethodFinalityAnnotation = false;
	
	private boolean isDefineTypeVisibilityAnnotation = false;
	private boolean isDefineFieldVisibilityAnnotation = false;
	private boolean isDefineMethodVisibilityAnnotation = false;
	
	private boolean isMergeTypeAnnotation = false;
	private boolean isMergeMethodAnnotation = false;

	public JREFAnnotationIdentifier() {
		super(Opcodes.ASM5);
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String name, boolean visible) {
		// System.out.println("Annotation: " + name);
		if (name.equals(DEFINE_TYPE_ANNOTATION)) {
			isDefineTypeAnnotation = true;
		} else if (name.equals(DEFINE_FIELD_ANNOTATION)) {
			isDefineFieldAnnotation = true;
		} else if (name.equals(DEFINE_METHOD_ANNOTATION)) {
			isDefineMethodAnnotation = true;
		}
		
		else if (name.equals(DEFINE_TYPE_FINALITY_ANNOTATION)) {
			isDefineTypeFinalityAnnotation = true;
		} else if (name.equals(DEFINE_FIELD_FINALITY_ANNOTATION)) {
			isDefineFieldFinalityAnnotation = true;
		} else if (name.equals(DEFINE_METHOD_FINALITY_ANNOTATION)) {
			isDefineMethodFinalityAnnotation = true;
		}
		
		else if (name.equals(DEFINE_TYPE_VISIBILITY_ANNOTATION)) {
			isDefineTypeVisibilityAnnotation = true;
		} else if (name.equals(DEFINE_FIELD_VISIBILITY_ANNOTATION)) {
			isDefineFieldVisibilityAnnotation = true;
		} else if (name.equals(DEFINE_METHOD_VISIBILITY_ANNOTATION)) {
			isDefineMethodVisibilityAnnotation = true;
		}
		
		else if (name.equals(MERGE_TYPE_ANNOTATION)) {
			isMergeTypeAnnotation = true;
		} else if (name.equals(MERGE_METHOD_ANNOTATION)) {
			isMergeMethodAnnotation = true;
		}
		return null;
	}
	
	public boolean isDefineTypeAnnotation() {
		return isDefineTypeAnnotation;
	}

	public boolean isDefineFieldAnnotation() {
		return isDefineFieldAnnotation;
	}

	public boolean isDefineMethodAnnotation() {
		return isDefineMethodAnnotation;
	}
	
	public boolean isDefineTypeFinalityAnnotation() {
		return isDefineTypeFinalityAnnotation;
	}

	public boolean isDefineFieldFinalityAnnotation() {
		return isDefineFieldFinalityAnnotation;
	}

	public boolean isDefineMethodFinalityAnnotation() {
		return isDefineMethodFinalityAnnotation;
	}
	
	public boolean isDefineTypeVisibilityAnnotation() {
		return isDefineTypeVisibilityAnnotation;
	}

	public boolean isDefineFieldVisibilityAnnotation() {
		return isDefineFieldVisibilityAnnotation;
	}

	public boolean isDefineMethodVisibilityAnnotation() {
		return isDefineMethodVisibilityAnnotation;
	}

	public boolean isMergeTypeAnnotation() {
		return isMergeTypeAnnotation;
	}

	public boolean isMergeMethodAnnotation() {
		return isMergeMethodAnnotation;
	}
	
	public boolean isDefineVisibilityAnnotation(){
		return isDefineTypeVisibilityAnnotation
				|| isDefineFieldVisibilityAnnotation
				|| isDefineMethodVisibilityAnnotation;
	}
	
	public boolean isDefineFinalityAnnotation(){
		return isDefineTypeFinalityAnnotation
				|| isDefineFieldFinalityAnnotation
				|| isDefineMethodFinalityAnnotation;
	}
	
	public boolean isJREFAnnotation(){
		return isDefineTypeAnnotation 
			|| isDefineFieldAnnotation 
			|| isDefineMethodAnnotation 
			
			|| isDefineTypeFinalityAnnotation
			|| isDefineFieldFinalityAnnotation
			|| isDefineMethodFinalityAnnotation
			
			|| isDefineTypeVisibilityAnnotation
			|| isDefineFieldVisibilityAnnotation
			|| isDefineMethodVisibilityAnnotation
			
			|| isMergeTypeAnnotation 
			|| isMergeMethodAnnotation;
	}
	
	public static class MergeTypeAnnotation {
		private String supertype;
		
		public MergeTypeAnnotation(String supertype) {
			this.supertype = supertype;
		}
		
		public String getSupertype(){
			return supertype;
		}
	}
	
	public static MergeTypeAnnotation getMergeTypeAnnotation(ClassNode classNode, AnnotationNode annotation){
		String superTypeValue = null;
		if(annotation.values != null){
	        for (int i = 0; i < annotation.values.size(); i += 2) {
	            String name = (String) annotation.values.get(i);
	            Object value = annotation.values.get(i + 1);
	            if(name.equals(SUPERTYPE)){
	            	superTypeValue = ((String)value).replaceAll("\\.", "/");
	            }
	        }
	    }
		if(superTypeValue == null || superTypeValue.equals("")){
			superTypeValue = classNode.superName;
        }
		return new MergeTypeAnnotation(superTypeValue);
	}
	
}
