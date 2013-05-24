package com.github.sourguice.appengine;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.github.sourguice.appengine.upload.internal.DevUploadServlet;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.utils.SystemProperty;
import com.google.inject.servlet.GuiceFilter;

/**
 * Filter that extends GuiceFilter which permits AppEngine to work with Guice Servlet and SourGuice
 * Permits every /_ah/* to work with appengine dev server
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class AhSafeGuiceFilter extends GuiceFilter {

	/**
	 * The pattern to test a URI on to check if it is a _ah request
	 */
	private static final Pattern pattern = Pattern.compile("/_ah/.*");
	
	private void doFilterForUpload(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
		if (SystemProperty.environment.value() != SystemProperty.Environment.Value.Development) {
			res.sendError(403, "MvcUploadService.devFilter should only be processed by DEV server");
			return ;
		}

		String key = DevUploadServlet.MEMCACHE_DEV_PARAMS_ + req.getParameter("__K");
		@SuppressWarnings("unchecked")
		final Map<String, String[]> params = (Map<String, String[]>)MemcacheServiceFactory.getMemcacheService().get(key);
		MemcacheServiceFactory.getMemcacheService().delete(key);
		if (params == null) {
			res.sendError(400, "No params registered");
			return ;
		}
		
		super.doFilter(new HttpServletRequestWrapper(req) {
			@Override public String getParameter(String name) {
				String param = super.getParameter(name);
				if (param == null && params.get(name) != null)
					return params.get(name)[0];
				return param;
			}
			@SuppressWarnings("unchecked")
			@Override public Map<String, String[]> getParameterMap() {
				HashMap<String, String[]> map = new HashMap<String, String[]>();
				map.putAll(params);
				map.putAll(super.getParameterMap());
				return map;
			}
			@SuppressWarnings("unchecked")
			@Override public Enumeration<String> getParameterNames() {
				Set<String> names = new HashSet<String>();
				names.addAll(params.keySet());
				names.addAll(Collections.list(super.getParameterNames()));
				return Collections.enumeration(names);
			}
			@Override public String[] getParameterValues(String name) {
				String[] p = super.getParameterValues(name);
				if (p == null && params.get(name) != null)
					return params.get(name);
				return p;
			}
		}, res, chain);
	}
	
	/**
	 * If the request is /_ah/warmup, treats it normally through Guice
	 * If the request is any other /_ah/*, treats it through the normal servlet system WITHOUT Guice
	 * If the request is /_ah/logout, prevents it to throw an exception when used without a redirect URL,
	 */
	@Override
	@OverridingMethodsMustInvokeSuper
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest)request;
		
		// If request is GAE logout, overrides sendRedirect so that, when no redirect URL is given,
		// instead of crashing, the app redirects to root
		if (req.getRequestURI().equals("/_ah/logout")) {
			response = new HttpServletResponseWrapper((HttpServletResponse)response) {
				@Override public void sendRedirect(String location) throws IOException {
					if (location == null)
						location = "/";
					super.sendRedirect(location);
				}
			};
		}
		
		// if request is /_ah/*, use regular servlet system to let GAE handle the request
		// the only exception is /_ah/warmup that is supposed to be handled by the app and not GAE
		if ((pattern.matcher(req.getRequestURI()).matches() && !req.getRequestURI().equals("/_ah/warmup")) || req.getRequestURI().equals("/remote_api")) {
			chain.doFilter(request, response);
			return ;
		}

		if (req.getRequestURI().equals("/__upload_dev")) {
			this.doFilterForUpload(req, (HttpServletResponse)response, chain);
			return ;
		}

		// Let Guice do its magic ;)
		super.doFilter(request, response, chain);
	}
}
