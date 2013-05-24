package com.github.sourguice.annotation.controller;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.github.sourguice.view.ViewRenderer;

/**
 * Defines a ViewRenderer specific to a Controller class
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RenderWith {
	/**
	 * @return The ViewRenderer class whose guice instance will be used to render the view.
	 */
	public Class<? extends ViewRenderer> value();
}
