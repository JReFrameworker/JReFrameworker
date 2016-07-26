package jreframeworker.engine.identifiers;
import java.util.LinkedList;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class DefineMethodsIdentifier {
	
	private LinkedList<MethodNode> defineMethods = new LinkedList<MethodNode>();

	public DefineMethodsIdentifier(ClassNode classNode) {
    	for (Object o : classNode.methods) {
			MethodNode methodNode = (MethodNode) o;
			if (methodNode.invisibleAnnotations != null) {
				for (Object annotationObject : methodNode.invisibleAnnotations) {
					AnnotationNode annotation = (AnnotationNode) annotationObject;
					JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
					checker.visitAnnotation(annotation.desc, false);
					if(checker.isDefineMethodAnnotation()){
						defineMethods.add(methodNode);
					}
				}
			}
    	}
    }
	
    public LinkedList<MethodNode> getDefineMethods() {
		return defineMethods;
	}
    
}
