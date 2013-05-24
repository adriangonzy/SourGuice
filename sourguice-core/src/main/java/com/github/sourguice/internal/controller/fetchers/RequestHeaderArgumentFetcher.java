package com.github.sourguice.internal.controller.fetchers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;

import com.github.sourguice.annotation.request.PathVariable;
import com.github.sourguice.annotation.request.PathVariablesMap;
import com.github.sourguice.annotation.request.RequestHeader;
import com.github.sourguice.conversion.ConversionService;
import com.github.sourguice.throwable.invocation.NoSuchRequestParameterException;
import com.github.sourguice.value.ValueConstants;
import com.google.inject.Injector;
import com.googlecode.gentyref.GenericTypeReflector;

/**
 * Fetcher that handles @{@link PathVariable} annotated arguments
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class RequestHeaderArgumentFetcher<T> extends ArgumentFetcher<T> {

	/**
	 * The annotations containing needed informations to fetch the argument
	 */
	private RequestHeader infos;
	
	/**
	 * @see ArgumentFetcher#ArgumentFetcher(Type, int, Annotation[])
	 * @param infos The annotations containing needed informations to fetch the argument
	 */
	public RequestHeaderArgumentFetcher(Type type, int pos, Annotation[] annotations, RequestHeader infos) {
		super(type, pos, annotations);
		this.infos = infos;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected @CheckForNull T getPrepared(HttpServletRequest req, @PathVariablesMap Map<String, String> pathVariables, Injector injector) throws NoSuchRequestParameterException {
		if (req.getHeader(infos.value()) == null) {
			if (!this.infos.defaultValue().equals(ValueConstants.DEFAULT_NONE)) 
				return (T) injector.getInstance(ConversionService.class).convert(GenericTypeReflector.erase(this.type), this.infos.defaultValue());
			throw new NoSuchRequestParameterException(this.infos.value(), "path variables");
		}
		return (T) injector.getInstance(ConversionService.class).convert(GenericTypeReflector.erase(this.type), req.getHeader(infos.value()));
	}
}
