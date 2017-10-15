package com.jreframeworker.engine.identifiers;
import java.util.LinkedList;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class BaseMethodsIdentifier {

	private LinkedList<MethodNode> baseMethods = new LinkedList<MethodNode>();

	public BaseMethodsIdentifier(ClassNode classNode) {
    	for (Object o : classNode.methods) {
			MethodNode methodNode = (MethodNode) o;
			baseMethods.add(methodNode);
    	}
    }
	
    public LinkedList<MethodNode> getBaseMethods() {
		return baseMethods;
	}
    
}
