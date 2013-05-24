package com.github.sourguice.annotation.request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the view to be used by a method when selected by the MVC caller.
 * There are two possible ways for a method to name the view to display :
 * 1/ Return a String
 * 2/ Be annotated with @View
 * 
 * If a method returns a String AND is annotated by @View, than the returned String is always used,
 * except when the method returns null, in which case the annotation is used
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface View {
	/**
	 * The name of the view to be displayed
	 */
	public String value();
}
