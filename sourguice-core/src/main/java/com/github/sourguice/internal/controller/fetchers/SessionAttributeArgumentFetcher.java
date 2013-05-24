package com.github.sourguice.internal.controller.fetchers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;

import com.github.sourguice.annotation.request.PathVariablesMap;
import com.github.sourguice.annotation.request.SessionAttribute;
import com.google.inject.Injector;

/**
 * Fetcher that handles @{@link SessionAttribute} annotated arguments
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class SessionAttributeArgumentFetcher<T> extends ArgumentFetcher<T> {

	/**
	 * The annotations containing needed informations to fetch the argument
	 */
	private SessionAttribute infos;
	
	/**
	 * @see ArgumentFetcher#ArgumentFetcher(Type, int, Annotation[])
	 * @param infos The annotations containing needed informations to fetch the argument
	 */
	public SessionAttributeArgumentFetcher(Type type, int pos, Annotation[] annotations, SessionAttribute infos) {
		super(type, pos, annotations);
		this.infos = infos;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected @CheckForNull T getPrepared(HttpServletRequest req, @PathVariablesMap Map<String, String> pathVariables, Injector injector) {
		return (T)req.getSession(true).getAttribute(infos.value());
	}
}
