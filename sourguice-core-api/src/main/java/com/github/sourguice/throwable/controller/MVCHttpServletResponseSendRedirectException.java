package com.github.sourguice.throwable.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * Exception used to send an HTTP redirect from a controller's method
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class MVCHttpServletResponseSendRedirectException extends MVCHttpServletResponseException {
	@SuppressWarnings("javadoc")
	private static final long serialVersionUID = -5613860675750051649L;

	/**
	 * The URL to redirect to
	 */
	public String to;
	
	/**
	 * @param to The URL to redirect to
	 */
	public MVCHttpServletResponseSendRedirectException(String to) {
		this.to = to;
	}

	/**
	 * Sends the configured redirect to the HTTP response
	 */
	@Override
	public void execute(HttpServletResponse res) throws IOException {
		res.sendRedirect(to);
	}
}
