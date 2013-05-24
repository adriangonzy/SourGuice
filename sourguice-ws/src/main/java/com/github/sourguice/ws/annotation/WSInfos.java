package com.github.sourguice.ws.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Nonnegative;

import com.github.sourguice.value.ValueConstants;
import com.github.sourguice.ws.translat.WSRuntimeTranslater;
import com.github.sourguice.ws.translat.WSTranslaterFactory;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface WSInfos {
	
	String name() default ValueConstants.DEFAULT_NONE;
	
	@Nonnegative double defaultVersion() default 1.0;
	
	Class<? extends WSTranslaterFactory<?, ?>>[] translaters() default {};
	
	Class<?>[] additionalClasses() default {};
	
	Class<? extends WSRuntimeTranslater<?>>[] runtimeExceptionTranslaters() default {};
}
