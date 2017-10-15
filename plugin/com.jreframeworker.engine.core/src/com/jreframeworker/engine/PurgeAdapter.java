package com.jreframeworker.engine;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import com.jreframeworker.engine.log.Log;

/**
 * This class is responsible for purging methods and fields from a class 
 * 
 * Reference: http://asm.ow2.org/doc/faq.html#Q1
 * 
 * @author Ben Holland
 */
public class PurgeAdapter extends ClassVisitor {
	
	private Set<MethodNode> methodsToPurge;
	private Set<FieldNode> fieldsToPurge;
	
	public PurgeAdapter(ClassVisitor classVisitor, MethodNode... methodsToPurgeArray) {
		super(Opcodes.ASM5, classVisitor);
		this.methodsToPurge = new HashSet<MethodNode>();
		this.fieldsToPurge = new HashSet<FieldNode>();
		for(MethodNode methodNode : methodsToPurgeArray){
			methodsToPurge.add(methodNode);
		}
	}
	
	public PurgeAdapter(ClassVisitor classVisitor, FieldNode... fieldsToPurgeArray) {
		super(Opcodes.ASM5, classVisitor);
		this.methodsToPurge = new HashSet<MethodNode>();
		this.fieldsToPurge = new HashSet<FieldNode>();
		for(FieldNode fieldNode : fieldsToPurgeArray){
			fieldsToPurge.add(fieldNode);
		}
	}
	
	public PurgeAdapter(ClassVisitor classVisitor, Set<MethodNode> methodsToPurge, Set<FieldNode> fieldsToPurge) {
		super(Opcodes.ASM5, classVisitor);
		this.methodsToPurge = methodsToPurge;
		this.fieldsToPurge = fieldsToPurge;
	}
	
//	private Collection<PurgeMethodAnnotation> methodsToPurgeAnnotations;
//	private Collection<PurgeFieldAnnotation> fieldsToPurgeAnnotations;
//	
//	public PurgeAdapter(ClassVisitor baseClassVisitor, Collection<PurgeMethodAnnotation> methodsToPurgeAnnotations, Collection<PurgeFieldAnnotation> fieldsToPurgeAnnotations) {
//		super(Opcodes.ASM5, baseClassVisitor);
//		this.methodsToPurgeAnnotations = methodsToPurgeAnnotations;
//		this.fieldsToPurgeAnnotations = fieldsToPurgeAnnotations;
//	}
	
	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		// purge based on FieldNode references
		for (FieldNode fieldToPurge : fieldsToPurge) {
			if (fieldToPurge.signature != null && signature != null) {
				if (fieldToPurge.signature.equals(signature)) {
					if (fieldToPurge.name.equals(name) && fieldToPurge.desc.equals(desc)) {
						// return null in order to remove this field
						Log.info("Purged Field: " + name);
						return null;
					}
				}
			} else {
				// signature was null, fall back to name and description only
				if (fieldToPurge.name.equals(name) && fieldToPurge.desc.equals(desc)) {
					// return null in order to remove this field
					Log.info("Purged Field: " + name);
					return null;
				}
			}
		}
		
		// make the next visitor visit this field, in order to keep it
		return super.visitField(access, name, desc, signature, value);
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions){
		for (MethodNode methodToPurge : methodsToPurge) {
			if (methodToPurge.signature != null && signature != null) {
				if (methodToPurge.signature.equals(signature)) {
					if (methodToPurge.name.equals(name) && methodToPurge.desc.equals(desc)) {
						// return null in order to remove this method
						Log.info("Purged Method: " + name);
						return null;
					}
				}
			} else {
				// signature was null, fall back to name and description only
				if (methodToPurge.name.equals(name) && methodToPurge.desc.equals(desc)) {
					// return null in order to remove this method
					Log.info("Purged Method: " + name);
					return null;
				}
			}
		}
		
		// make the next visitor visit this field, in order to keep it
		return super.visitMethod(access, name, desc, signature, exceptions);
	}
	
}