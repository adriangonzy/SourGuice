package com.github.sourguice.exception;

import javax.annotation.CheckForNull;

import com.github.sourguice.throwable.service.exception.UnreachableExceptionHandlerException;

/**
 * Singleton service that handles exceptions thrown by the controllers that are registered into it.
 * This allows you to have a standard way of treating exceptions throughout all your controllers.
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public interface ExceptionService {

	/**
	 * Registers an exception class and its corresponding exception handler
	 * 
	 * @param clazz The exception class to handle
	 * @param handler The handler that handles the exception
	 * @throws UnreachableExceptionHandlerException When an ExceptionHandler will never be reached because a previous ExceptionHandler
	 *                                              has been registered that already handles this class of exception
	 */
	public abstract <T extends Exception>void registerHandler(Class<T> clazz, ExceptionHandler<? super T> handler) throws UnreachableExceptionHandlerException;

	/**
	 * Get the first handler that can handle a given exception class
	 * 
	 * @param clazz The class of the exception to be handled
	 * @return The handler or null
	 */
	public @CheckForNull
	abstract <T extends Exception>ExceptionHandler<? super T> getHandler(Class<T> clazz);

}
