package com.github.sourguice.exception;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base interface for class that handles an exception to be registered in {@link ExceptionService}
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 * @param <T> The type of the exception to handle
 */
public interface ExceptionHandler<T extends Exception> {
	/**
	 * Handles an exception
	 * 
	 * @param exception The exception to be handled
	 * @param req The current HttpServletRequest
	 * @param res The current HttpServletResponse
	 * @return Whether the exception has been handled or not (if false is returned, the exception will be re-thrown)
	 * @throws IOException If an input or output exception occurs
	 */
	boolean handle(T exception, HttpServletRequest req, HttpServletResponse res) throws IOException;
}
