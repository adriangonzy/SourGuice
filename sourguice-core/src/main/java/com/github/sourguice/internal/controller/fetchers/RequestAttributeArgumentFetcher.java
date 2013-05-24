package com.github.sourguice.internal.controller.fetchers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;

import com.github.sourguice.annotation.request.PathVariablesMap;
import com.github.sourguice.annotation.request.RequestAttribute;
import com.google.inject.Injector;

/**
 * Fetcher that handles @{@link RequestAttribute} annotated arguments
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class RequestAttributeArgumentFetcher<T> extends ArgumentFetcher<T> {
	
	/**
	 * The annotations containing needed informations to fetch the argument
	 */
	private RequestAttribute infos;
	
	/**
	 * @see ArgumentFetcher#ArgumentFetcher(Type, int, Annotation[])
	 * @param infos The annotations containing needed informations to fetch the argument
	 */
	public RequestAttributeArgumentFetcher(Type type, int pos, Annotation[] annotations, RequestAttribute infos) {
		super(type, pos, annotations);
		this.infos = infos;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected @CheckForNull T getPrepared(HttpServletRequest req, @PathVariablesMap Map<String, String> pathVariables, Injector injector) {
		return (T)req.getAttribute(infos.value());
	}
}
