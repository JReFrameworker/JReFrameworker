package checkers;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class JREFAnnotationChecker extends ClassVisitor {

	private static final String DEFINE_TYPE_ANNOTATION = "Ljreframeworker/annotations/types/DefineType;";
	private static final String DEFINE_FIELD_ANNOTATION = "Ljreframeworker/annotations/fields/DefineField;";
	private static final String DEFINE_METHOD_ANNOTATION = "Ljreframeworker/annotations/methods/DefineMethod;";
	private static final String MERGE_TYPE_ANNOTATION = "Ljreframeworker/annotations/types/MergeType;";
	private static final String MERGE_METHOD_ANNOTATION = "Ljreframeworker/annotations/methods/MergeMethod;";
	
	private boolean isDefineTypeAnnotation = false;
	private boolean isDefineFieldAnnotation = false;
	private boolean isDefineMethodAnnotation = false;
	private boolean isMergeTypeAnnotation = false;
	private boolean isMergeMethodAnnotation = false;

	public JREFAnnotationChecker() {
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
		} else if (name.equals(MERGE_TYPE_ANNOTATION)) {
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

	public boolean isMergeTypeAnnotation() {
		return isMergeTypeAnnotation;
	}

	public boolean isMergeMethodAnnotation() {
		return isMergeMethodAnnotation;
	}
	
	public boolean isJREFAnnotation(){
		return isDefineTypeAnnotation 
				|| isDefineFieldAnnotation 
				|| isDefineMethodAnnotation 
				|| isMergeTypeAnnotation 
				|| isMergeMethodAnnotation;
	}
	
}
