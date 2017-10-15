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
 * Indicates the base method should be renamed and made private.
 * The annotated method will be inserted along the renamed base
 * method. Calls to the original base method will now point to
 * the annotated method.  Super calls to the original base method
 * will be replaced with calls to the renamed base method.
 * 
 * @author Ben Holland
 */
public @interface MergeMethod {}
