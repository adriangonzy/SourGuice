package com.github.sourguice.call;

import java.lang.reflect.Method;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.sourguice.annotation.controller.Callable;
import com.github.sourguice.annotation.request.PathVariable;
import com.github.sourguice.annotation.request.PathVariablesMap;
import com.github.sourguice.annotation.request.RequestMapping;
import com.github.sourguice.throwable.invocation.HandledException;
import com.github.sourguice.throwable.invocation.NoSuchRequestParameterException;

/**
 * Class that permits to call @{@link Callable} annotated methods like @{@link RequestMapping}
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public interface MvcCaller {

	/**
	 * @see #call(Class, Method, Map, boolean, CalltimeArgumentFetcher...)
	 */
	@SuppressWarnings("javadoc")
	public @CheckForNull
	abstract Object call(Class<?> clazz, String methodName, @CheckForNull @PathVariablesMap Map<String, String> pathVariables, boolean throwWhenHandled, CalltimeArgumentFetcher<?>... additionalFetchers) throws HandledException, NoSuchMethodException, NoSuchRequestParameterException, Throwable;

	/**
	 * Executes a call to a given method
	 * The object on which to call the method will be retrived from Guice,
	 * therefore, the given class must be registered in Guice (or it will be instanciate by Guice via Just-In-Time binding)
	 * 
	 * @param clazz The class on which to find the method 
	 * @param method The method to call
	 * @param pathVariables The URIPathVariables object to use to retrieve @{@link PathVariable} annotated method parameters
	 * @param throwWhenHandled throwWhenHandled Whether or not to throw a {@link HandledException} when an exception has been thrown AND handled by the Exception Service
	 *        This should mainly be set to false. It should be set to true when you want to prevent treatment on the returned object when an exception has been thrown, even when handled.
	 * @param additionalFetchers Additional {@link CalltimeArgumentFetcher} that will be used to retrieve arguments for methods
	 * @return The object returned by the method
	 *         If throwWhenHandled is false and an exception has been thrown AND handled, will return null
	 * @throws HandledException Only when throwWhenHandled is true and an exception has been thrown AND handled
	 * @throws NoSuchRequestParameterException When retrieval of parameter annotated with anotations from the {@link com.github.sourguice.annotation.request} package failed
	 * @throws Throwable Any throwable thrown by the method called and not handled
	 */
	public @CheckForNull
	abstract Object call(Class<?> clazz, Method method, @CheckForNull @PathVariablesMap Map<String, String> pathVariables, boolean throwWhenHandled, CalltimeArgumentFetcher<?>... additionalFetchers) throws HandledException, NoSuchRequestParameterException, Throwable;

	/**
	 * @return The request that is registered for this caller
	 */
	abstract public HttpServletRequest getReq();
	
	/**
	 * Sets the request registered for this caller
	 * This can be used to register a HttpServletRequestWrapper
	 * This WILL NOT replace the HttpServletRequest returned by Guice
	 * BUT all HttpServletRequest required by @{@link RequestMapping} annotated method will get this request instead of the one registered in Guice
	 * @param req The Request to set
	 */
	abstract public void setReq(HttpServletRequest req);
	
	/**
	 * @return The response that is registered for this caller
	 */
	abstract public HttpServletResponse getRes();

	/**
	 * Sets the response registered for this caller
	 * This can be used to register a HttpServletResponseWrapper
	 * This WILL NOT replace the HttpServletResponse returned by Guice
	 * BUT all HttpServletResponse required by @{@link RequestMapping} annotated method will get this response instead of the one registered in Guice
	 * @param res The Response to set
	 */
	abstract public void setRes(HttpServletResponse res);
}
