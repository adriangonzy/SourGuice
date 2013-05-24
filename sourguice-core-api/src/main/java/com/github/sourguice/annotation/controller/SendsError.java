package com.github.sourguice.annotation.controller;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Means that the annotated method will send an HTTP error.
 * If the method returns an int, then it will be used as the error code, whether or not
 * a default code is given in the annotation
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SendsError {
	/**
	 * @return Default HTTP error code that will be sent to the response
	 */
	public int value() default -1;
	
	/**
	 * @return Default HTTP error message that will be sent to the response
	 */
	public String message() default "";
}
