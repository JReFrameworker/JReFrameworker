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
 * Indicates the annotated class should not be marked final
 * in order to allow extending the class for reframeworking
 * 
 * target should be the qualified class name
 * 
 * @author Ben Holland
 */
public @interface NotFinalType {
	String target();
}
