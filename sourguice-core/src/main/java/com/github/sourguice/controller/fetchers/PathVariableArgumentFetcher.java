package com.github.sourguice.controller.fetchers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;

import com.github.sourguice.annotation.request.PathVariable;
import com.github.sourguice.annotation.request.PathVariablesMap;
import com.github.sourguice.conversion.ConversionService;
import com.github.sourguice.throwable.invocation.NoSuchPathVariableException;
import com.github.sourguice.throwable.invocation.NoSuchRequestParameterException;
import com.github.sourguice.value.ValueConstants;
import com.google.inject.Injector;
import com.googlecode.gentyref.GenericTypeReflector;

/**
 * Fetcher that handles @{@link PathVariable} annotated arguments
 * 
 * @param <T> The type of the argument to fetch
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class PathVariableArgumentFetcher<T> extends ArgumentFetcher<T> {

	/**
	 * The annotations containing needed informations to fetch the argument
	 */
	private PathVariable infos;
	
	/**
	 * @see ArgumentFetcher#ArgumentFetcher(Type, int, Annotation[])
	 * @param type The type of the argument to fetch
	 * @param pos The position of the method's argument to fetch
	 * @param annotations Annotations that were found on the method's argument
	 * @param infos The annotations containing needed informations to fetch the argument
	 * @param ref The reference map that links path variable name to their index when a url matches
	 * @param check Whether or not to check that ref contains the reference to the path variable
	 */
	public PathVariableArgumentFetcher(Type type, int pos, Annotation[] annotations, PathVariable infos, HashMap<String, Integer> ref, boolean check) {
		super(type, pos, annotations);
		this.infos = infos;
		if (check && !ref.containsKey(infos.value()))
			throw new NoSuchPathVariableException(infos.value());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected @CheckForNull T getPrepared(HttpServletRequest req, @PathVariablesMap Map<String, String> pathVariables, Injector injector) throws NoSuchRequestParameterException {
		if (pathVariables == null || pathVariables.get(infos.value()) == null) {
			if (!this.infos.defaultValue().equals(ValueConstants.DEFAULT_NONE)) 
				return (T) injector.getInstance(ConversionService.class).convert(GenericTypeReflector.erase(this.type), this.infos.defaultValue());
			throw new NoSuchRequestParameterException(this.infos.value(), "path variables");
		}
		return (T) injector.getInstance(ConversionService.class).convert(GenericTypeReflector.erase(this.type), pathVariables.get(infos.value()));
	}
}
