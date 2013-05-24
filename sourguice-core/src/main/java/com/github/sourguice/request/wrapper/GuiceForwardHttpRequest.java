package com.github.sourguice.request.wrapper;

import java.io.File;

import javax.annotation.CheckForNull;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.github.sourguice.request.ForwardableRequestFactory;

/**
 * This wrapper permits forwarding the request (especially to a JSP) without Guice handling (and preventing) it
 * 
 * The main trick is to use the servlet dispatcher in the context that is untouched by Guice instead of the request's one  
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public final class GuiceForwardHttpRequest extends HttpServletRequestWrapper implements ForwardableRequestFactory {
	
	/**
	 * The directory of the file on which we want to redirect
	 */
	@CheckForNull private String dirPath = null;

	/**
	 * The file name on which we want to redirect
	 */
	@CheckForNull private String fileName = null;

	/**
	 * the untouched context
	 */
	private ServletContext context;

	/**
	 * @param request The current request object
	 * @param context Must be the servlet context, and not the request's
	 */
	public GuiceForwardHttpRequest(HttpServletRequest request, ServletContext context) {
		super(request);
		this.context = context;
	}
	
	/**
	 * Gets a HttpServletRequest that is configured to get a RequestDispatcher that will allow forwarding
	 */
	@Override
	public HttpServletRequest to(String path) {
		File f = new File(path);

		this.fileName = f.getName();
		
		this.dirPath  = f.getParent();
		if (File.separatorChar != '/')
			this.dirPath = this.dirPath.replace(File.separatorChar, '/');

		return this;
	}

	/**
	 * Trick to allow forwarding
	 */
	@Override
	public String getPathInfo() {
		if (this.fileName == null)
			return super.getPathInfo();

		return this.fileName;
	}

	/**
	 * Trick to allow forwarding
	 */
	@Override
	public String getServletPath() {
		if (this.dirPath == null)
			return super.getServletPath();

		if (this.dirPath.endsWith("/"))
			return this.dirPath;

		return this.dirPath + "/";
	}

	/**
	 * Trick to allow forwarding
	 */
	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		return context.getRequestDispatcher(path);
	}
}
