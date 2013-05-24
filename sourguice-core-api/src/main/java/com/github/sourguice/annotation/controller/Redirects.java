package com.github.sourguice.annotation.controller;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Means that the annotated method will send an HTTP redirection.
 * If the method returns an String, then it will be used as the redirection url, whether or not
 * a default redirection url is given in the annotation
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Redirects {
	/**
	 * @return Default redirection url that will be sent to the response
	 */
	public String value() default "";
}
