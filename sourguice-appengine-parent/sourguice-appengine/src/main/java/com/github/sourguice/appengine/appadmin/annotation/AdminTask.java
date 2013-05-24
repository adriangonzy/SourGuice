package com.github.sourguice.appengine.appadmin.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.github.sourguice.annotation.controller.Callable;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Callable
public @interface AdminTask {
	public String value();
	public String description() default "";
	public boolean queue() default true;
}
