package checkers;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Identifies Java annotations in a class to merge that would be 
 * incompatible in a merge with a base class
 * 
 * @author Ben Holland
 */
public class IncompatibleMergeAnnotationChecker extends ClassVisitor {

	private boolean isCompatible = true;
	
	public IncompatibleMergeAnnotationChecker() {
		super(Opcodes.ASM5);
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String name, boolean visible) {
		// System.out.println("Annotation: " + name);
		if (name.equals("Ljava/lang/Override;")) {
			isCompatible = false;
		}
		return null;
	}
	
	public boolean isIncompatible() {
		return !isCompatible;
	}
	
	public boolean isCompatible() {
		return isCompatible;
	}
	
}
