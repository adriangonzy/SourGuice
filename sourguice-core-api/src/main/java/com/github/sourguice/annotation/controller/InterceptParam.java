package com.github.sourguice.annotation.controller;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.CheckForNull;
import javax.annotation.meta.TypeQualifierNickname;

import com.github.sourguice.utils.MVCCallIntercept;

/**
 * When this annotation is present on a parameter of a controller's method, it means that the value
 * of this parameter is to be set by an interceptor.
 * Therefore, the MVC Caller will always set this parameter to false.
 * It is the responsibility of an interceptor that you would configure on the method to replace this
 * null value by the value of the parameter.
 * To do this, you can use the {@link MVCCallIntercept#Set(org.aopalliance.intercept.MethodInvocation, String, Object)} utility method
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.PARAMETER)
@TypeQualifierNickname @CheckForNull
public @interface InterceptParam {
	/**
	 * @return The key that will be used by the interceptor to set the parameter
	 */
	String value();
}
