package com.github.sourguice.internal.controller.fetchers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;

import com.github.sourguice.annotation.request.PathVariablesMap;
import com.github.sourguice.call.CalltimeArgumentFetcher;
import com.github.sourguice.conversion.ConversionService;
import com.github.sourguice.throwable.invocation.NoSuchRequestParameterException;
import com.google.inject.Injector;

/**
 * Base class for argument fetchers
 * An argument fetcher is an algorythm that is preconfigured (selected) to retrieve an argument of a method
 * The fetcher is selected / configured when the invocation is created, which means at launch time
 * <p>
 * The fetcher is responsible of getting the right type for the argument and therefore to use {@link ConversionService} if necessary
 * 
 * @param <T> The type of the argument that this class will fetch
 *            This is for type safety only as one fetcher can handle multiple types
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public abstract class ArgumentFetcher<T> {
	
	/**
	 * The type of the argument to fetch
	 */
	protected Type type;
	
	/**
	 * The position of the method's argument to fetch
	 * in the method void foo(int a, int b); a is in position 0 and b in position 1
	 */
	protected int pos;
	
	/**
	 * Annotations that were found on the method's argument
	 */
	protected Annotation annotations[];
	
	/**
	 * @param type The type of the argument to fetch
	 * @param pos The position of the method's argument to fetch
	 * @param annotations Annotations that were found on the method's argument
	 */
	protected ArgumentFetcher(Type type, int pos, Annotation[] annotations) {
		this.type = type;
		this.pos = pos;
		this.annotations = annotations;
	}
	
	/**
	 * This method must fetch the argument
	 * This method will be called at each MVC invocation
	 * This will first check if any "Call-time" Argument Fetcher can get the argument and, if not, ask its subclass via getPrepared
	 * 
	 * @param req The current HTTP request
	 * @param pathVariables Variables that were parsed from request URL
	 * @param injector Guice injector
	 * @param additionalFetchers Any additional fetcher provided at "call-time" directly by the user
	 * @return The argument to be passed to the MVC invocation
	 * @throws NoSuchRequestParameterException In case of a parameter asked from request argument or path variable that does not exists
	 * @throws Throwable Any exception that would be thrown by a fetcher
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public @CheckForNull T get(HttpServletRequest req, @PathVariablesMap Map<String, String> pathVariables, Injector injector, CalltimeArgumentFetcher<?>[] additionalFetchers) throws Throwable {
		for (CalltimeArgumentFetcher f : additionalFetchers)
			if (f.canGet(this.type, pos, annotations))
				return (T)f.get(this.type, pos, annotations);
		
		return getPrepared(req, pathVariables, injector);
	}

	/**
	 * This is where subclass fetch the argument
	 * 
	 * @param req The current HTTP request
	 * @param pathVariables Variables that were parsed from request URL
	 * @param injector Guice injector
	 * @return The argument to be passed to the MVC invocation
	 * @throws NoSuchRequestParameterException In case of a parameter asked from request argument or path variable that does not exists
	 */
	protected abstract @CheckForNull T getPrepared(HttpServletRequest req, @PathVariablesMap Map<String, String> pathVariables, Injector injector) throws Throwable;

	/**
	 * @return The type of the argument to fetch
	 */
	public Type getType() {
		return type;
	}
}
