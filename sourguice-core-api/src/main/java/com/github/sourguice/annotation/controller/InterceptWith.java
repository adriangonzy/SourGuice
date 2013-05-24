package com.github.sourguice.annotation.controller;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.aopalliance.intercept.MethodInterceptor;

/**
 * Defines that the annotated class or method will be intercepted by a {@link MethodInterceptor}
 * The Method interceptor will be retrived from guice with its class
 *  
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Documented
public @interface InterceptWith {
	/**
	 * @return The class to use to get the interceptor from guice
	 */
	public Class<? extends MethodInterceptor>[] value();
}
