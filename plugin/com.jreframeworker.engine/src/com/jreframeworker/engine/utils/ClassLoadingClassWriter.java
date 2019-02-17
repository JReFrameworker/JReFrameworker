package com.jreframeworker.engine.utils;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class ClassLoadingClassWriter extends ClassWriter {

	public ClassLoadingClassWriter(int flags) {
		super(flags);
	}

	public ClassLoadingClassWriter(ClassReader classReader, int flags) {
		super(classReader, flags);
	}

	@Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        Class<?> c = null;
        Class<?> d = null;
        
        for(ClassLoader classLoader : ClassLoaders.getClassLoaders()){
        	try {
                c = Class.forName(type1.replace('/', '.'), false, classLoader);
                d = Class.forName(type2.replace('/', '.'), false, classLoader);
                break;
            } catch (Exception e) {
                continue;
            }
        }
        
        if(c == null || d == null){
        	throw new RuntimeException("Could not find common super class of: [type1=" + type1 + "], [type2=" + type2 + "]");
        }
        
        if (c.isAssignableFrom(d)) {
            return type1;
        }
        if (d.isAssignableFrom(c)) {
            return type2;
        }
        if (c.isInterface() || d.isInterface()) {
            return "java/lang/Object";
        } else {
            do {
                c = c.getSuperclass();
            } while (!c.isAssignableFrom(d));
            return c.getName().replace('.', '/');
        }
    }
	
}
