package com.github.sourguice.throwable.controller;

import java.io.IOException;

import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletResponse;

/**
 * Exception used to send an HTTP error from a controller's method
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class MVCHttpServletResponseSendErrorException extends MVCHttpServletResponseException {
	@SuppressWarnings("javadoc")
	private static final long serialVersionUID = -5463694767632145191L;

	/**
	 * The HTTP error code (Often 404 or 403)
	 */
	public int code;
	
	/**
	 * The HTTP error message
	 */
	public @CheckForNull String message = null;
	
	/**
	 * Constructor with only an HTTP error code
	 * The message will be the default one for this code
	 * 
	 * @param code The HTTP error code
	 */
	public MVCHttpServletResponseSendErrorException(int code) {
		this.code = code;
	}

	/**
	 * Constructor with an HTTP error code and a custom error message
	 * 
	 * @param code The HTTP error code
	 * @param reason The message for this HTTP error
	 */
	public MVCHttpServletResponseSendErrorException(int code, String reason) {
		this.code = code;
		this.message = reason;
	}

	/**
	 * Sends the configured error to the HTTP response
	 */
	@Override
	public void execute(HttpServletResponse res) throws IOException {
		if (message != null)
			res.sendError(code, message);
		else
			res.sendError(code);
	}
}

