package com.jreframeworker.annotations.fields;

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
 * Adds or removes the final modifier from a field
 * 
 * "type" should be the qualified class name if not defined the target will be
 * the super class of the class the annotation is placed on
 * 
 * "field" should be the name of the field for which to set visibility
 * 
 * "finality" should be a boolean true to add or boolean false to remove the
 * final keyword
 * 
 * @author Ben Holland
 */
@Repeatable(DefineFieldFinalities.class)
public @interface DefineFieldFinality {
	int phase() default 1;
	String type();
	String field();
	boolean finality();
}
