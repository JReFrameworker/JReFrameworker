package com.jreframeworker.annotations.fields;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// this annotation is valid for methods
@Target({ ElementType.FIELD })

// annotation will be recorded in the class file by the compiler,
// but won't be retained by the VM at run time (invisible annotation)
@Retention(RetentionPolicy.CLASS)

/**
 * Indicates the annotated field should be inserted into the base 
 * class during a merge.  If the field already exists, the field 
 * will be replaced with the annotated field.
 * 
 * @author Ben Holland
 */
public @interface DefineField {}
