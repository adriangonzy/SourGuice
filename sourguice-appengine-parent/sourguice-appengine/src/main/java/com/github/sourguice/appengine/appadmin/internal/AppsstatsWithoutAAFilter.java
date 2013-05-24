package com.github.sourguice.appengine.appadmin.internal;

import java.io.IOException;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.google.appengine.tools.appstats.AppstatsFilter;

public class AppsstatsWithoutAAFilter extends AppstatsFilter {

	private String prefixDir;

	public AppsstatsWithoutAAFilter(String prefix) {
		this.prefixDir = prefix + "/";
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public void doFilter(ServletRequest _req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest)_req;
		if (req.getRequestURI().startsWith(prefixDir)) {
			chain.doFilter(req, res);
			return ;
		}
		super.doFilter(req, res, chain);
	}
}
