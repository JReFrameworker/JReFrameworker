package com.jreframeworker.annotations.fields;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// this annotation is valid for methods
@Target({ ElementType.TYPE })

// annotation will be recorded in the class file by the compiler,
// but won't be retained by the VM at run time (invisible annotation)
@Retention(RetentionPolicy.CLASS)

/**
 * Indicates the annotated field should be purged from the base class.
 * 
 * @author Ben Holland
 */
@Repeatable(PurgeFields.class)
public @interface PurgeField {
	String type();
	String field();
}
