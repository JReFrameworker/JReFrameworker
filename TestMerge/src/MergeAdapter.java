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

import checkers.IncompatibleMergeAnnotationChecker;
import checkers.JREFAnnotationChecker;

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
					JREFAnnotationChecker checker = new JREFAnnotationChecker();
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
			boolean merge = false;
			
			// check if method is annotated with jref_overwrite annotation
			LinkedList<AnnotationNode> jrefAnnotations = new LinkedList<AnnotationNode>();
			LinkedList<AnnotationNode> incompatibleMergeAnnotations = new LinkedList<AnnotationNode>();
			if (methodNode.invisibleAnnotations != null) {
				for (Object annotationObject : methodNode.invisibleAnnotations) {
					AnnotationNode annotation = (AnnotationNode) annotationObject;
					// check if the annotation is a jref annotation
					JREFAnnotationChecker jrefChecker = new JREFAnnotationChecker();
					jrefChecker.visitAnnotation(annotation.desc, false);
					if(jrefChecker.isJREFAnnotation()){
						jrefAnnotations.add(annotation);
						merge = true;
					}
					// check if the annotation is incompatible with a merge operation
					IncompatibleMergeAnnotationChecker compatibilityChecker = new IncompatibleMergeAnnotationChecker();
					compatibilityChecker.visitAnnotation(annotation.desc, false);
					if(compatibilityChecker.isIncompatible()){
						incompatibleMergeAnnotations.add(annotation);
					}
				}
			}
			// if the method has a jref annotation, then merge the method
			if (merge) {
				// strip the jref annotations and merge the method
				methodNode.invisibleAnnotations.removeAll(jrefAnnotations);
				// strip any incompatible annotations (ie @Override)
				methodNode.invisibleAnnotations.removeAll(incompatibleMergeAnnotations);
				String[] exceptions = new String[methodNode.exceptions.size()];
				methodNode.exceptions.toArray(exceptions);
				MethodVisitor mv = cv.visitMethod(methodNode.access, methodNode.name, methodNode.desc, methodNode.signature, exceptions);
				methodNode.instructions.resetLabels();
				methodNode.accept(new RemappingMethodAdapter(methodNode.access, methodNode.desc, mv, new SimpleRemapper(baseClassName, classToMerge.name)));
			}

		}

		super.visitEnd();
	}
}