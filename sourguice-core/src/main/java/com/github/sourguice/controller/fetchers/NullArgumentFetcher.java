package com.github.sourguice.controller.fetchers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;

import org.aopalliance.intercept.MethodInterceptor;

import com.github.sourguice.annotation.controller.InterceptParam;
import com.github.sourguice.annotation.request.PathVariablesMap;
import com.google.inject.Injector;

/**
 * Fetcher that will always return null
 * This is used for @{@link InterceptParam} annotated argument.
 * They return null but the actual argument will later be injected in a {@link MethodInterceptor}
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class NullArgumentFetcher extends ArgumentFetcher<Object> {
	
	/**
	 * @see ArgumentFetcher#ArgumentFetcher(Type, int, Annotation[])
	 */
	public NullArgumentFetcher(Type type, int pos, Annotation[] annotations) {
		super(type, pos, annotations);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected @CheckForNull Object getPrepared(HttpServletRequest req, @PathVariablesMap Map<String, String> pathVariables, Injector injector) {
		return null;
	}
}
