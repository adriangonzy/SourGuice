package com.github.sourguice.request;

import javax.servlet.http.HttpServletRequest;

import com.github.sourguice.view.def.JSPViewRenderer;

/**
 * ForwardableRequestFactory is used to forward the request to a resource :
 *  - Get an instance from Guice
 *  - Call to(path)
 *  - call getRequestDispatcher(path)
 *  - call forward(req, res)
 * In a one line statement:
 *  injector.getInstance(ForwardableRequestFactory.class).to(path).getRequestDispatcher(path).forward(req, res);
 * 
 * @see JSPViewRenderer#render(String, java.util.Map) to get an example of using it
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public interface ForwardableRequestFactory {
	/**
	 * @param path The path you wish to redirect to
	 * @return A request specially configured to redirect to the given path
	 */
	public HttpServletRequest to(String path);
}
