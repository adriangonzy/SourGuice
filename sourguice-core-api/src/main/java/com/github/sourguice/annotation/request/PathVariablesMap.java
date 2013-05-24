package com.github.sourguice.annotation.request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import javax.annotation.meta.TypeQualifier;

import com.google.inject.BindingAnnotation;

/**
 * This is a Guice Binding Annotation that must be used only on Map<String, String>.
 * This indicates that the requested map object contains the mapping found by parsing the URL.
 * The map binds {@link PathVariable} name to their value
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@BindingAnnotation
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.LOCAL_VARIABLE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TypeQualifier(applicableTo = Map.class)
public @interface PathVariablesMap {
}
