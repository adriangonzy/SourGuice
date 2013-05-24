package com.github.sourguice.annotation.controller;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a view directory (prefix) that will be applied to all view path returned by this controller
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ViewDirectory {
	/**
	 * @return The view directory without ending '/'
	 */
	public String value();
}
