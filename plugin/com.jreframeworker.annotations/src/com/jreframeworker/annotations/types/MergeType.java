package com.jreframeworker.annotations.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// this annotation is valid for types
@Target({ ElementType.TYPE })

// annotation will be recorded in the class file by the compiler,
// but won't be retained by the VM at run time (invisible annotation)
@Retention(RetentionPolicy.CLASS)

/**
 * Indicates the annotated type (class, abstract class, interface) should be 
 * inserted into the runtime. If the runtime type already exists it will be 
 * replaced with the annotated type. Ignores all other JReFrameworker annotations.
 * 
 * "supertype" forces merges into the specified qualified type
 * This option is useful for nasty edge case hacks...or forcing stubborn compiles to work.
 * Hopefully in the future this option can be removed
 * 
 * @author Ben Holland
 */
public @interface MergeType {
	int phase() default 1;
	String supertype() default "";
}
