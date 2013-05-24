package com.github.sourguice.appengine.appadmin;

import java.io.IOException;

import javax.annotation.CheckForNull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.inject.Singleton;

/**
 * Filter that, when enabled, allows the app to be in maintenance mode.
 * Maintenance mode can be enabled or disabled through {@link AppAdminController}
 * When in maintenance mode, all request are intercepted and a maintenance message is displayed
 * unless you are registered as app admin with your google account
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Singleton
public class MaintenanceFilter implements Filter {

	/**
	 * Whether or not the filter is enabled
	 * this is only for internal use only
	 */
	public static boolean FilterEnabled = false;
	
	/**
	 * Namespace to use on memcache and datastore
	 */
	public static final String GAE_NAMESPACE = MaintenanceFilter.class.getCanonicalName();
	
	/**
	 * KEY on wich to save whether the maintenance is enabled on memcache and datastore
	 */
	public static final String GAE_KEY = MaintenanceFilter.class.getCanonicalName() + ".MAINTENANCE_ENABLED";
	
	@Override public void init(FilterConfig config) throws ServletException {}
	@Override public void destroy() {}
	
	/**
	 * @return the maintenance entity (wich contains the boolean that says whether or not maintenance is enabled)
	 */
	static final @CheckForNull Entity getMaintenanceEntity() {
		String ns = NamespaceManager.get();
		NamespaceManager.set(GAE_NAMESPACE);
		try {
			MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
			Key key = KeyFactory.createKey(GAE_KEY, GAE_KEY);
			Entity maintenance = null;

			if (memcache.contains(GAE_KEY))
				maintenance = (Entity)memcache.get(GAE_KEY);
			else
				try {
					DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
					maintenance = datastore.get(key);
					memcache.put(GAE_KEY, maintenance);
				}
				catch (EntityNotFoundException e) {}

			return maintenance;
		}
		finally {
			NamespaceManager.set(ns);
		}
	}

	/**
	 * If maintenance is enabled and google user not connected or not admin : displays maintenance message
	 * Else, do nothing
	 */
	@OverridingMethodsMustInvokeSuper
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest _req = (HttpServletRequest)req;
		HttpServletResponse _res = (HttpServletResponse)res;
		
		if (AppAdminController.Path != null && (_req.getRequestURI().startsWith(AppAdminController.Path) || _req.getRequestURI().startsWith("/_ah/"))) {
			chain.doFilter(req, res);
			return ;
		}
		
		Entity maintenance = getMaintenanceEntity(); 
		if (maintenance != null && ((Boolean)maintenance.getProperty("m")).booleanValue()) {
			UserService users = UserServiceFactory.getUserService();
			if (!users.isUserLoggedIn() || !users.isUserAdmin()) {
				_res.setStatus(503);
				_res.getWriter().write("<h1>Application under maintenance</h1><p>" + maintenance.getProperty("s") + "</p>");
				return ;
			}
		}

		chain.doFilter(req, res);
	}
}
