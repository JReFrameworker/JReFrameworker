import identifiers.JREFAnnotationIdentifier;

import java.util.LinkedList;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.RemappingMethodAdapter;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * This class is responsible for merging two class files based on 
 * the merging strategies in the JReFrameworker framework.
 * 
 * References: http://www.jroller.com/eu/entry/merging_class_methods_with_asm
 * 
 * @author Ben Holland
 */
public class MergeAdapter extends ClassVisitor {
	private ClassNode classToMerge;
	private String baseClassName;

	public MergeAdapter(ClassVisitor baseClassVisitor, ClassNode classToMerge) {
		super(Opcodes.ASM5, baseClassVisitor);
		this.classToMerge = classToMerge;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		baseClassName = name;
	}

	@SuppressWarnings("unchecked")
	public void visitEnd() {
		// copy each field of the class to merge in to the original class
		for (Object o : classToMerge.fields) {
			FieldNode fieldNode = (FieldNode) o;
			// only insert the field if it is annotated
			if(fieldNode.invisibleAnnotations != null){
				for(Object o2 : fieldNode.invisibleAnnotations){
					AnnotationNode annotationNode = (AnnotationNode) o2;
					JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
					checker.visitAnnotation(annotationNode.desc, false);
					if(checker.isDefineFieldAnnotation()){
						// insert the field
						fieldNode.accept(this);
					}
				}
			}
		}

		// copy each method of the class to merge that is annotated
		// with a jref annotation to the original class
		for (Object o : classToMerge.methods) {
			MethodNode methodNode = (MethodNode) o;
			boolean define = false;
			boolean merge = false;
			
			// check if method is annotated with a jref annotation
			LinkedList<AnnotationNode> jrefAnnotations = new LinkedList<AnnotationNode>();
			if (methodNode.invisibleAnnotations != null) {
				for (Object annotationObject : methodNode.invisibleAnnotations) {
					AnnotationNode annotation = (AnnotationNode) annotationObject;
					// check if the annotation is a jref annotation
					JREFAnnotationIdentifier jrefChecker = new JREFAnnotationIdentifier();
					jrefChecker.visitAnnotation(annotation.desc, false);
					if(jrefChecker.isJREFAnnotation()){
						jrefAnnotations.add(annotation);
						if(jrefChecker.isDefineMethodAnnotation()){
							define = true;
						}
						if(jrefChecker.isMergeMethodAnnotation()){
							merge = true;
						}
					}
				}
			}
			
			// if the method is annotated with @DefineMethod or @MergeMethod, add the method
			if(define || merge){
				// in any case, strip the jref annotations from the method
				methodNode.invisibleAnnotations.removeAll(jrefAnnotations);
				// then add the method, the renaming or deletion of any existing methods 
				// is done earlier so its safe to just add it now
				addMethod(methodNode);
			}
		}

		super.visitEnd();
	}

	@SuppressWarnings("unchecked")
	private void addMethod(MethodNode methodNode) {
		String[] exceptions = new String[methodNode.exceptions.size()];
		methodNode.exceptions.toArray(exceptions);
		MethodVisitor mv = cv.visitMethod(methodNode.access, methodNode.name, methodNode.desc, methodNode.signature, exceptions);
		methodNode.instructions.resetLabels();
		methodNode.accept(new RemappingMethodAdapter(methodNode.access, methodNode.desc, mv, new SimpleRemapper(baseClassName, classToMerge.name)));
	}
}