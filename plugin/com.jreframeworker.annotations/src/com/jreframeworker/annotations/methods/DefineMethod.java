package com.jreframeworker.annotations.methods;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// this annotation is valid for methods
@Target({ ElementType.METHOD })

// annotation will be recorded in the class file by the compiler,
// but won't be retained by the VM at run time (invisible annotation)
@Retention(RetentionPolicy.CLASS)

/**
 * Indicates the annotated method should be inserted into the base type 
 * during a merge. If the base type already contains the method then 
 * the base method will be replaced with the annotated method.
 * 
 * @author Ben Holland
 */
public @interface DefineMethod {}
