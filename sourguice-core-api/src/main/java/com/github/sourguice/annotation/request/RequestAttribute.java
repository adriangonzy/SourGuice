package com.github.sourguice.annotation.request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.CheckForNull;
import javax.annotation.meta.TypeQualifierNickname;

/**
 * Indicates that a variable is to be taken from a request attribute
 * That variable will NOT be converted. Wrong type correspondance will result in an exception.
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TypeQualifierNickname @CheckForNull
public @interface RequestAttribute {
	/**
	 * The name of the request attribute to retrieve
	 */
	String value();
}
