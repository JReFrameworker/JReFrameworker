import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class ClassAdapter extends ClassVisitor implements Opcodes {

    public ClassAdapter(final ClassVisitor cv) {
        super(ASM5, cv);
    }
    
}