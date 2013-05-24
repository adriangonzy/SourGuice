package com.github.sourguice.utils;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.github.sourguice.annotation.controller.InterceptParam;

/**
 * Util class for {@link MethodInterceptor}s
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class MVCCallIntercept {

	/**
	 * Util to set the value of an @{@link InterceptParam} annotated argument from an interceptor.
	 * 
	 * @param invocation The invocation on which to set the argument
	 * @param key The value string given to the annotation of the parameter
	 * @param parameter The value of the parameter to set
	 */
	public static void Set(MethodInvocation invocation, String key, Object parameter) {
		Method method = invocation.getMethod();
		for (int i = 0; i < method.getParameterTypes().length; ++i) {
			InterceptParam interceptParam = Annotations.fromArray(method.getParameterAnnotations()[i]).getAnnotation(InterceptParam.class);
			if (interceptParam != null && interceptParam.value().equals(key)) {
				invocation.getArguments()[i] = parameter;
				return ;
			}
		}
	}

}
