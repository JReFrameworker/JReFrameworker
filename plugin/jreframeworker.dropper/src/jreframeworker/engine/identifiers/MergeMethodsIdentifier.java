package jreframeworker.engine.identifiers;
import java.util.LinkedList;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MergeMethodsIdentifier {
	
	private LinkedList<MethodNode> mergeMethods = new LinkedList<MethodNode>();

	public MergeMethodsIdentifier(ClassNode classNode) {
    	for (Object o : classNode.methods) {
			MethodNode methodNode = (MethodNode) o;
			if (methodNode.invisibleAnnotations != null) {
				for (Object annotationObject : methodNode.invisibleAnnotations) {
					AnnotationNode annotation = (AnnotationNode) annotationObject;
					JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
					checker.visitAnnotation(annotation.desc, false);
					if(checker.isMergeMethodAnnotation()){
						mergeMethods.add(methodNode);
					}
				}
			}
    	}
    }
	
    public LinkedList<MethodNode> getMergeMethods() {
		return mergeMethods;
	}
    
}
