package com.github.sourguice.view.def;

import java.io.IOException;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.sourguice.request.ForwardableRequestFactory;
import com.github.sourguice.view.ViewRenderer;
import com.google.inject.Inject;

/**
 * Default view renderer plugin that renders JSP
 * Or more exactly that forwards requests to JSPs
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class JSPViewRenderer implements ViewRenderer {

	/**
	 * The current HTTP Response
	 */
	private HttpServletResponse res;
	
	/**
	 * Will be used to forward request
	 */
	private ForwardableRequestFactory fact;
	
	/**
	 * @param res The current HTTP Response
	 */
	@Inject
	public JSPViewRenderer(HttpServletResponse res, ForwardableRequestFactory fact) {
		this.res = res;
		this.fact = fact;
	}

	/**
	 * Displays the given JSP
	 * 
	 * Loads all model key/value into the request and redirects to the JSP
	 */
	@Override
	@OverridingMethodsMustInvokeSuper
	public void render(String view, @CheckForNull Map<String, Object> model) throws IOException, ServletException {

		if (view == null)
			throw new RuntimeException("Cannot render a null JSP");
		
		HttpServletRequest req = fact.to(view);
		
		if (model != null)
			for (String key : model.keySet())
				req.setAttribute(key, model.get(key));
		
		RequestDispatcher dispatcher = req.getRequestDispatcher(view);
		
		if (dispatcher == null) {
			res.sendError(404, "JSP NOT FOUND: " + view);
			return ;
		}
		
		req.getRequestDispatcher(view).forward(req, res);
	}

}
