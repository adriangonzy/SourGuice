package com.github.sourguice.request.wrapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * This wrapper removes the jsessionid from the request Path Info as Guice does not removes it
 * 
 * The jsessionid should be passed by cookie and not by url as much as possible but there are some cases in which it is not possible 
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public final class NoJsessionidHttpRequest extends HttpServletRequestWrapper {

	/**
	 * Pattern to capture a jsessionid
	 */
	static private final Pattern jsessionidPattern = Pattern.compile(";jsessionid=[a-zA-Z0-9\\-\\._]+");

	/**
	 * The pathInfo with jessionid removed
	 */
	private String pathInfo;
	
	/**
	 * @param request The current request to wrap
	 */
	public NoJsessionidHttpRequest(HttpServletRequest request) {
		super(request);

		this.pathInfo = request.getPathInfo();
		
		Matcher matcher = jsessionidPattern.matcher(this.pathInfo);
		if (matcher.find())
			this.pathInfo = this.pathInfo.substring(0, matcher.start()) + this.pathInfo.substring(matcher.end());
	}

	/**
	 * The pathInfo without the jessionid if it was previously present
	 */
	@Override
	public String getPathInfo() {
		return this.pathInfo;
	}
	
}
