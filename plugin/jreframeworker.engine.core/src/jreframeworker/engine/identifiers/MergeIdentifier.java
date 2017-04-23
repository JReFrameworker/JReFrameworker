package jreframeworker.engine.identifiers;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MergeIdentifier {
	
	private static final String SUPERTYPE = "supertype";
	
	public static Set<String> getMergeTargets(ClassNode classNode) throws IOException {
		Set<String> targets = new HashSet<String>();
		
		// TODO: address innerclasses, classNode.innerClasses, could these even be found from class files? they would be different files...
		if(classNode.invisibleAnnotations != null){
			for(Object annotationObject : classNode.invisibleAnnotations){
				AnnotationNode annotationNode = (AnnotationNode) annotationObject;
				MergeTypeAnnotation mergeTypeAnnotation = getMergeTypeAnnotation(classNode, annotationNode);
				targets.add(mergeTypeAnnotation.getSupertype());
			}
		}
		
		return targets;
	}
	
	private LinkedList<MethodNode> mergeMethods = new LinkedList<MethodNode>();

	public MergeIdentifier(ClassNode classNode) {
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
