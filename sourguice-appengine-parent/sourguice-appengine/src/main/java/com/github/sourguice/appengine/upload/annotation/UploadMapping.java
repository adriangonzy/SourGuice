package com.github.sourguice.appengine.upload.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.github.sourguice.annotation.controller.Callable;

/**
 * This is an alias to @Callable. It permits to distinguish Upload methods from simple callables
 * @author salomon
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Callable
public @interface UploadMapping {
	boolean onlyParamUploads() default true;
}
