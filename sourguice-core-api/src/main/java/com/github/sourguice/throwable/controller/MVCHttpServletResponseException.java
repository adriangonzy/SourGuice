package com.github.sourguice.throwable.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * Base of all exceptions in this package.
 * All exceptions in this package are caught by the MVC Call system and execute a treatment on the response
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public abstract class MVCHttpServletResponseException extends Exception {
	@SuppressWarnings("javadoc")
	private static final long serialVersionUID = 8302019360183385619L;

	/**
	 * @param res The response on which to execute the exception tratment
	 * @throws IOException If an input or output exception occurs
	 */
	public abstract void execute(HttpServletResponse res) throws IOException;
}
