package com.github.sourguice;

import java.io.File;
import java.io.IOException;

import javax.annotation.CheckForNull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.google.inject.servlet.GuiceFilter;

/**
 * In some application servers, the filter / servlet API will always be called,
 * even if the asked URL points to an existing resource in the war.
 * This (optional) filter will prevent Guice-Servlet from running and force the
 * default system servlet for existing files if the URL points to an existing file.
 * 
 * This servlet has two parameters (that can be passed from web.xml):
 *  - index-root: if true, allows root directory to be listed through default servlet (default: false)
 *  - index-dir: if true, allows any directory other than root to be listed through default servlet (default: false)
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class StaticIgnoreGuiceFilter extends GuiceFilter {

	/**
	 * Whether or not to allow any directory other than root to be listed
	 */
	boolean indexDir = false;

	/**
	 * Whether or not to allow root directory to be listed
	 */
	boolean indexRoot = false;
	
	/**
	 * The servlet config given at init
	 */
	private @CheckForNull ServletContext context;

	@Override
	@OverridingMethodsMustInvokeSuper
	public void doFilter(ServletRequest _req, ServletResponse _res, FilterChain chain) throws IOException, ServletException {
		assert(_req != null);
		assert(chain != null);
		HttpServletRequest req = (HttpServletRequest)_req;
		assert context != null;
		File file = new File(context.getRealPath(req.getRequestURI()));
		if (file.exists()) {
			boolean serve = true;
			if (!indexRoot && req.getRequestURI().equals("/"))
				serve = false;
			else if (!indexDir && file.isDirectory())
				serve = false;

			if (serve) {
				chain.doFilter(_req, _res);
				return ;
			}
		}
		super.doFilter(_req, _res, chain);
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public void init(FilterConfig config) throws ServletException {
		super.init(config);
		
		assert(config != null);
		
		context = config.getServletContext();
		
		String dir = config.getInitParameter("index-dir");
		if (dir != null && dir.equals("true"))
			this.indexDir = true;

		String root = config.getInitParameter("index-root");
		if (root != null && root.equals("true"))
			this.indexRoot = true;
	}
}
