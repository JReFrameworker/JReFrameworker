package com.jreframeworker.engine.utils;

import java.util.List;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class AnnotationUtils {

	public static void clearTypeAnnotations(ClassNode clazz) {
		// clear visible annotations
		if(clazz.visibleAnnotations != null) clazz.visibleAnnotations.clear();
		if(clazz.visibleTypeAnnotations != null) clazz.visibleTypeAnnotations.clear();
		
		// clear invisible annotations
		if(clazz.invisibleAnnotations != null) clazz.invisibleAnnotations.clear();
		if(clazz.invisibleTypeAnnotations != null) clazz.invisibleTypeAnnotations.clear();
	}
	
	public static void clearFieldAnnotations(FieldNode field) {
		// clear visible annotations
		if(field.visibleAnnotations != null) field.visibleAnnotations.clear();
		if(field.visibleTypeAnnotations != null) field.visibleTypeAnnotations.clear();
		
		// clear invisible annotations
		if(field.invisibleAnnotations != null) field.invisibleAnnotations.clear();
		if(field.invisibleTypeAnnotations != null) field.invisibleTypeAnnotations.clear();
	}
	
	public static void clearMethodAnnotations(MethodNode method) {
		// clear visible annotations
		if(method.visibleAnnotations != null) method.visibleAnnotations.clear();
		if(method.visibleLocalVariableAnnotations != null) method.visibleLocalVariableAnnotations.clear();
		if(method.visibleTypeAnnotations != null) method.visibleTypeAnnotations.clear();
		if(method.visibleParameterAnnotations != null){
			for(@SuppressWarnings("rawtypes") List parameterAnnotations : method.visibleParameterAnnotations){
				parameterAnnotations.clear();
			}
		}
		
		// clear invisible annotations
		if(method.invisibleAnnotations != null) method.invisibleAnnotations.clear();
		if(method.invisibleLocalVariableAnnotations != null) method.invisibleLocalVariableAnnotations.clear();
		if(method.invisibleTypeAnnotations != null) method.invisibleTypeAnnotations.clear();
		if(method.invisibleParameterAnnotations != null){
			for(@SuppressWarnings("rawtypes") List parameterAnnotations : method.invisibleParameterAnnotations){
				parameterAnnotations.clear();
			}
		}
	}
	
}
