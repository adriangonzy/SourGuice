package com.github.sourguice.annotation.request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifier;
import javax.annotation.meta.TypeQualifierNickname;
import javax.servlet.http.HttpServletRequest;

import com.google.inject.BindingAnnotation;

/**
 * This is a Guice Binding Annotation that must be used only on HttpServletRequest.
 * This indicates that the requested request object is the one modified by Guice (and therefore with a non null PathInfo)
 * rather than the one directly provided by the servlet container.
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@BindingAnnotation
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TypeQualifier(applicableTo = HttpServletRequest.class)
@TypeQualifierNickname @Nonnull
public @interface GuiceRequest {
}
