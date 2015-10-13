package checkers;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class JREFAnnotationChecker extends ClassVisitor {

	private boolean isOverwrite = false;

	public JREFAnnotationChecker() {
		super(Opcodes.ASM5);
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String name, boolean visible) {
		// System.out.println("Annotation: " + name);
		if (name.equals("Ljreframeworker/operations/annotations/jref_overwrite;")) {
			isOverwrite = true;
		}
		return null;
	}
	
	public boolean isOverwrite() {
		return isOverwrite;
	}
	
	public boolean isJREFAnnotation(){
		return isOverwrite;
	}
	
}
