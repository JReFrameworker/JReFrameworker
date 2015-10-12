import java.util.LinkedList;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.RemappingMethodAdapter;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * This code was adapted from example at http://www.jroller.com/eu/entry/merging_class_methods_with_asm
 * 
 * @author Ben Holland
 */
public class MergeAdapter extends ClassAdapter {
	private ClassNode classNode;
	private String className;

	// since ASM is very SAX parser like, we use some global variables to keep track of state
	private boolean merge = false; // holds state of true if the next element should be merged
	
	public MergeAdapter(ClassVisitor classVisitor, ClassNode classNode) {
		super(classVisitor);
		this.classNode = classNode;
	}

	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.className = name;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String name, boolean visible) {
//		System.out.println("Annotation: " + name);
		if (name.equals("Ljreframeworker/operations/annotations/jref_overwrite;")){
			merge = true;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public void visitEnd() {

		// copy each field of the class to merge to the original class
		for(Object o : classNode.fields){
			FieldNode fieldNode = (FieldNode) o;
			fieldNode.accept(this);
		}
		
		// copy each method of the class to merge that is annotated with a jref annotation
		// to the original class
		for(Object o : classNode.methods){
			MethodNode methodNode = (MethodNode) o;
			
			// check if method is annotated with jref_overwrite annotation
			merge = false;
			LinkedList<AnnotationNode> annotationsToRemove = new LinkedList<AnnotationNode>();
			if(methodNode.invisibleAnnotations != null){
				for(Object annotationObject : methodNode.invisibleAnnotations){
					AnnotationNode annotation = (AnnotationNode) annotationObject;
					visitAnnotation(annotation.desc, false);
					if(merge){
						annotationsToRemove.add(annotation); // remove the jref annotation
					}
				}
			}
			if(merge){
				// add the method to the original class
				methodNode.invisibleAnnotations.removeAll(annotationsToRemove);
				String[] exceptions = new String[methodNode.exceptions.size()];
				methodNode.exceptions.toArray(exceptions);
				MethodVisitor mv = cv.visitMethod(methodNode.access, methodNode.name, methodNode.desc, methodNode.signature, exceptions);
				methodNode.instructions.resetLabels();
				methodNode.accept(new RemappingMethodAdapter(methodNode.access, methodNode.desc, mv, new SimpleRemapper(className, classNode.name)));
				merge = false;
			}

		}
		
		super.visitEnd();
	}
}