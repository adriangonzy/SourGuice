package com.github.sourguice.controller;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;

import com.github.sourguice.annotation.controller.InterceptParam;
import com.github.sourguice.annotation.request.PathVariable;
import com.github.sourguice.annotation.request.PathVariablesMap;
import com.github.sourguice.annotation.request.RequestAttribute;
import com.github.sourguice.annotation.request.RequestHeader;
import com.github.sourguice.annotation.request.RequestMapping;
import com.github.sourguice.annotation.request.RequestParam;
import com.github.sourguice.annotation.request.SessionAttribute;
import com.github.sourguice.call.CalltimeArgumentFetcher;
import com.github.sourguice.call.impl.PathVariablesProvider;
import com.github.sourguice.controller.ControllerHandler.InvocationInfos;
import com.github.sourguice.controller.fetchers.ArgumentFetcher;
import com.github.sourguice.controller.fetchers.InjectorArgumentFetcher;
import com.github.sourguice.controller.fetchers.NullArgumentFetcher;
import com.github.sourguice.controller.fetchers.PathVariableArgumentFetcher;
import com.github.sourguice.controller.fetchers.RequestAttributeArgumentFetcher;
import com.github.sourguice.controller.fetchers.RequestHeaderArgumentFetcher;
import com.github.sourguice.controller.fetchers.RequestParamArgumentFetcher;
import com.github.sourguice.controller.fetchers.SessionAttributeArgumentFetcher;
import com.github.sourguice.throwable.invocation.NoSuchRequestParameterException;
import com.github.sourguice.utils.Annotations;
import com.github.sourguice.utils.Arrays;
import com.github.sourguice.utils.HttpStrings;
import com.github.sourguice.value.RequestMethod;
import com.google.inject.Injector;
import com.googlecode.gentyref.GenericTypeReflector;

/**
 * Class that holds every informations available at compile time needed to call a controller's method
 * Each Invocation object corresponds to a method of a controller
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public final class MvcInvocation {

	/**
	 * Pattern to detect a variable in a url string
	 */
	static private final Pattern search = Pattern.compile("\\{([a-zA-Z0-9\\-_]+)\\}");

	/**
	 * Regular expressions to be applied to a URL to see if it matches
	 * It bascially is the @{@link RequestMapping} URL like /foo-{bar}/plop to /foo-[^/]+/plop
	 */
	private ArrayList<Pattern> patterns = new ArrayList<Pattern>();
	
	/**
	 * The Annotation of a controller's method
	 */
	private @CheckForNull RequestMapping mapping;
	
	/**
	 * All fetchers for each arguments of the method
	 */
	private ArgumentFetcher<?>[] fetchers;
	
	/**
	 * The method of this Invocation
	 */
	private Method method;

	/**
	 * The class of this Invocation
	 */
	private Class<?> clazz;
	
	/**
	 * The reference of each path variable name and their position in the url regex
	 */
	HashMap<String, Integer> matchRef = new HashMap<String, Integer>();

	/**
	 * @param mapping The annotation that must be present on each invocation method
	 * @param clazz The class on witch to call the method
	 * @param method The method to call
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MvcInvocation(@CheckForNull RequestMapping mapping, Class<?> clazz, Method method) {
		// Set properties
		this.mapping = mapping;
		this.method = method;
		this.clazz = clazz;
		
		// Transform URL like "/foo-{bar}" into /foo-[^/]+ and registers "bar" as match 1
		if (this.mapping != null)
			for (String location : this.mapping.value()) {
				Matcher matcher = search.matcher(location);
				int n = 1;
				while (matcher.find()) {
					matchRef.put(matcher.group(1), new Integer(n));
					++n;
				}
				location = matcher.replaceAll("([^/]+)");
				patterns.add(Pattern.compile(location));
			}
		
		// Registers all fetchers
		// Fetchers are configured in constructor so they are constrcted only once
		// If no fetcher is suitable, then uses the guice fetcher
		Type[] parameterTypes = GenericTypeReflector.getExactParameterTypes(method, clazz);
		Annotation[][] annotations = method.getParameterAnnotations();
		fetchers = new ArgumentFetcher<?>[parameterTypes.length];
		for (int n = 0; n < parameterTypes.length; ++n) {
			AnnotatedElement annos = Annotations.fromArray(annotations[n]);

			RequestParam requestParam = annos.getAnnotation(RequestParam.class);
			if (requestParam != null) {
				fetchers[n] = new RequestParamArgumentFetcher(parameterTypes[n], n, annotations[n], requestParam);
				continue ;
			}
			
			PathVariable pathVariable = annos.getAnnotation(PathVariable.class);
			if (pathVariable != null) {
				fetchers[n] = new PathVariableArgumentFetcher(parameterTypes[n], n, annotations[n], pathVariable, matchRef, this.mapping != null && this.mapping.value().length > 0);
				continue ;
			}
			
			RequestAttribute requestAttribute = annos.getAnnotation(RequestAttribute.class);
			if (requestAttribute != null) {
				fetchers[n] = new RequestAttributeArgumentFetcher(parameterTypes[n], n, annotations[n], requestAttribute);
				continue ;
			}
			
			SessionAttribute sessionAttribute = annos.getAnnotation(SessionAttribute.class);
			if (sessionAttribute != null) {
				fetchers[n] = new SessionAttributeArgumentFetcher(parameterTypes[n], n, annotations[n], sessionAttribute);
				continue ;
			}
			
			RequestHeader requestHeader = annos.getAnnotation(RequestHeader.class);
			if (requestHeader != null) {
				fetchers[n] = new RequestHeaderArgumentFetcher(parameterTypes[n], n, annotations[n], requestHeader);
				continue ;
			}
			
			InterceptParam interceptParam = annos.getAnnotation(InterceptParam.class);
			if (interceptParam != null) {
				fetchers[n] = new NullArgumentFetcher(parameterTypes[n], n, annotations[n]);
				continue ;
			}
			
			fetchers[n] = new InjectorArgumentFetcher(parameterTypes[n], n, annotations[n]);
		}
	}
	
	/**
	 * This will determine if this invocation can serve for this request and how confident it is to serve it
	 * The more confident it is, the more specialised it is for this request.
	 * @param req The request
	 * @return InvocationInfos with all infos (including confidence) if it can, null if it can't
	 */
	public @CheckForNull InvocationInfos canServe(HttpServletRequest req) {
		if (this.mapping == null)
			return null;
		
		InvocationInfos ret = new InvocationInfos(this);
		
		// Checks if the URL declared in @RequestMapping matches. This is mandatory
		for (Pattern pattern : patterns) {
			String p = req.getPathInfo();
			if (p == null)
				p = "/";
			Matcher matcher = pattern.matcher(p);
			if (matcher.matches()) {
				ret.urlMatch = matcher.toMatchResult();
				break ;
			}
		}
		if (ret.urlMatch == null)
			return null;

		// Checks the HTTP Method
		if (this.mapping.method().length > 0) {
			RequestMethod requestMethod = RequestMethod.valueOf(req.getMethod());
			if (Arrays.Contains(mapping.method(), requestMethod))
				++ret.confidence;
			else
				return null;
		}
		
		// Checks request parametes
		if (mapping.params().length > 0)
			for (String param : mapping.params())
				if (req.getParameter(param) != null)
					++ret.confidence;
				else
					return null;

		// Checks HTTP headers
		if (mapping.headers().length > 0)
			for (String header : mapping.headers())
				if (req.getHeader(header) != null)
					++ret.confidence;
				else
					return null;
		
		// Checks HTTP header Content-Type
		if (mapping.consumes().length > 0) {
			if (Arrays.Contains(mapping.consumes(), req.getContentType()))
				++ret.confidence;
			else
				return null;
		}
		
		// Checks HTTP header Accept
		if (mapping.produces().length > 0) {
			if (req.getHeader("Accept") == null)
				return null;

			if (HttpStrings.AcceptContains(req.getHeader("Accept"), mapping.produces()))
				++ret.confidence;
			else
				return null;
		}
		
		return ret;
	}

	/**
	 * This is where the magic happens: This will invoke the method by fetching all of its arguments and call it
	 * 
	 * @param req The current HTTP request
	 * @param pathVariables Variables that were parsed from request URL
	 * @param injector Guice injector
	 * @param additionalFetchers Any additional fetcher provided at "call-time" directly by the user
	 * @return What the method call returned
	 * @throws NoSuchRequestParameterException In case of a parameter asked from request argument or path variable that does not exists
	 * @throws Throwable Any thing that the method call might have thrown
	 */
	public @CheckForNull Object Invoke(
			HttpServletRequest req,
			@CheckForNull @PathVariablesMap Map<String, String> pathVariables,
			Injector injector,
			CalltimeArgumentFetcher<?>... additionalFetchers
			) throws NoSuchRequestParameterException, Throwable  {

		if (pathVariables == null)
			pathVariables = PathVariablesProvider._coercePathVariablesMap(new HashMap<String, String>());
		
		// Pushes path variables to the stack, this permits to have invocations inside invocations
		injector.getInstance(PathVariablesProvider.class).push(pathVariables);

		try {
			// Fetches all arguments
			Object[] params = new Object[this.fetchers.length];
			Object invocRet = null;
			for (int n = 0; n < this.fetchers.length; ++n)
				params[n] = this.fetchers[n].get(req, pathVariables, injector, additionalFetchers);

			try {
				// Calls the method
				Object ret = method.invoke(injector.getInstance(this.clazz), params);
				
				// If method did not returned void, gets what it returned
				if (!method.getReturnType().equals(Void.TYPE) && !method.getReturnType().equals(void.class))
					invocRet = ret;
			}
			// Catches anything that might be thrown by the method call
			catch (InvocationTargetException thrown) {
				throw thrown.getCause();
			}
			
			// Returns whatever the method call returned
			return invocRet;
		}
		finally {
			// Pops path variables from the stack, this invocation is over 
			injector.getInstance(PathVariablesProvider.class).pop();
		}
	}
	
	/**
	 * Invoke proxy for internal use
	 */
	@SuppressWarnings("javadoc")
	public @CheckForNull Object Invoke(
			HttpServletRequest req,
			MatchResult urlMatch,
			Injector injector,
			CalltimeArgumentFetcher<?>... additionalFetchers
			) throws NoSuchRequestParameterException, Throwable {
		return Invoke(req, PathVariablesProvider.fromMatch(urlMatch, matchRef), injector, additionalFetchers);
	}

	/**
	 * @return The invocation's method
	 */
	public Method getMethod() {
		return method;
	}
}
