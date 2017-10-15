package com.jreframeworker.annotations.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// this annotation is valid for types
@Target({ ElementType.TYPE })

// annotation will be recorded in the class file by the compiler,
// but won't be retained by the VM at run time (invisible annotation)
@Retention(RetentionPolicy.CLASS)

/**
 * Sets the visibility of a type
 * 
 * "type" should be the qualified class name, if not defined the target will be
 * the super class of the class the annotation is placed on
 * 
 * "visibility" should be "public", "protected", or "private"
 * 
 * @author Ben Holland
 */
@Repeatable(DefineTypeVisibilities.class)
public @interface DefineTypeVisibility {
	int phase() default 1;
	String type();
	String visibility();
}
