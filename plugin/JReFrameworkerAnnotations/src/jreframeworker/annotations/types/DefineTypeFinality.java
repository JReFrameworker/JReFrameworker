package jreframeworker.annotations.types;

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
 * Adds or removes the final modifier from a target type
 * 
 * target should be the qualified class name
 * 
 * finality should be a boolean true to add or boolean
 * false to remove final keyword
 * 
 * @author Ben Holland
 */
public @interface DefineTypeFinality {
	String target();
	boolean finality();
}
