package com.github.sourguice.controller.fetchers;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;

import com.github.sourguice.annotation.request.PathVariablesMap;
import com.github.sourguice.annotation.request.RequestParam;
import com.github.sourguice.conversion.ConversionService;
import com.github.sourguice.throwable.invocation.NoSuchRequestParameterException;
import com.github.sourguice.value.ValueConstants;
import com.google.inject.Injector;
import com.googlecode.gentyref.GenericTypeReflector;

/**
 * Fetcher that handles @{@link RequestParam} annotated arguments
 * 
 * @param <T> The type of the argument to fetch
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class RequestParamArgumentFetcher<T> extends ArgumentFetcher<T> {

	/**
	 * The annotations containing needed informations to fetch the argument
	 */
	private RequestParam infos;
	
	/**
	 * @see ArgumentFetcher#ArgumentFetcher(Type, int, Annotation[])
	 * @param type The type of the argument to fetch
	 * @param pos The position of the method's argument to fetch
	 * @param annotations Annotations that were found on the method's argument
	 * @param infos The annotations containing needed informations to fetch the argument
	 */
	public RequestParamArgumentFetcher(Type type, int pos, Annotation[] annotations, RequestParam infos) {
		super(type, pos, annotations);
		this.infos = infos;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected @CheckForNull T getPrepared(HttpServletRequest req, @PathVariablesMap Map<String, String> pathVariables, Injector injector) throws NoSuchRequestParameterException {
		ConversionService conversionService = injector.getInstance(ConversionService.class);
		// TODO: Handle Sets & concrete collection types
		// If a List is requested, gets an array and converts it to list
		if (GenericTypeReflector.erase(this.type).equals(List.class)) {
			if (req.getParameterValues(this.infos.value()) == null || req.getParameterValues(this.infos.value()).length == 0) {
				// If there are no value and not default value, throws the exception
				if (this.infos.defaultValue() == ValueConstants.DEFAULT_NONE)
					throw new NoSuchRequestParameterException(this.infos.value(), "request parameters");
				return (T)new ArrayList();
			}
			// Gets converted array and returns it as list
			Object[] objs = conversionService.convertArray((Class<?>)((ParameterizedType)GenericTypeReflector.getExactSuperType(type, List.class)).getActualTypeArguments()[0], req.getParameterValues(this.infos.value()));
			return (T)Arrays.asList(objs);
		}
		// If a Map is requested, gets all name[key] or name:key request parameter and fills the map with converted values
		if (GenericTypeReflector.erase(this.type).equals(Map.class)) {
			Map<Object, Object> ret = new HashMap<Object, Object>();
			Enumeration<String> names = req.getParameterNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				if (name.startsWith(infos.value() + ":"))
					ret.put(
							conversionService.convert((Class<?>)((ParameterizedType)GenericTypeReflector.getExactSuperType(type, Map.class)).getActualTypeArguments()[0], name.substring(infos.value().length() + 1)),
							conversionService.convert((Class<?>)((ParameterizedType)GenericTypeReflector.getExactSuperType(type, Map.class)).getActualTypeArguments()[1], req.getParameter(name)));
				else if (name.startsWith(infos.value() + "[") && name.endsWith("]"))
					ret.put(
							conversionService.convert((Class<?>)((ParameterizedType)GenericTypeReflector.getExactSuperType(type, Map.class)).getActualTypeArguments()[0], name.substring(infos.value().length() + 1, name.length() - 1)),
							conversionService.convert((Class<?>)((ParameterizedType)GenericTypeReflector.getExactSuperType(type, Map.class)).getActualTypeArguments()[1], req.getParameter(name)));
			}
			if (ret.size() == 0 && this.infos.defaultValue() == ValueConstants.DEFAULT_NONE)
				throw new NoSuchRequestParameterException(this.infos.value(), "request parameters");
			return (T)ret;
		}
		// If the parameter does not exists, returns the default value or, if there are none, throw an exception
		if (req.getParameter(this.infos.value()) == null) {
			if (!this.infos.defaultValue().equals(ValueConstants.DEFAULT_NONE))
				return (T) conversionService.convert(GenericTypeReflector.erase(this.type), this.infos.defaultValue());
			throw new NoSuchRequestParameterException(this.infos.value(), "request parameters");
		}
		// Returns the converted parameter value
		if (req.getParameterValues(this.infos.value()).length == 1)
			return (T) conversionService.convert(GenericTypeReflector.erase(this.type), req.getParameter(this.infos.value()));
		return (T) conversionService.convert(GenericTypeReflector.erase(this.type), req.getParameterValues(this.infos.value()));
	}
}
