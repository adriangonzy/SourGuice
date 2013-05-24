/**
 * Plugin of SourGuice to permit and optimise the use of SourGuice on Google App Engine
 */
package com.github.sourguice.appengine;

import javax.annotation.OverridingMethodsMustInvokeSuper;

import com.github.sourguice.MvcServletModule;
import com.github.sourguice.appengine.appadmin.AppAdminController;
import com.github.sourguice.appengine.appadmin.MaintenanceFilter;
import com.github.sourguice.appengine.appadmin.internal.AppsstatsWithoutAAFilter;
import com.github.sourguice.appengine.upload.MvcUploadService;
import com.github.sourguice.appengine.upload.internal.DevUploadServlet;
import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.capabilities.CapabilitiesService;
import com.google.appengine.api.capabilities.CapabilitiesServiceFactory;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.mail.MailService;
import com.google.appengine.api.mail.MailServiceFactory;
import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.oauth.OAuthService;
import com.google.appengine.api.oauth.OAuthServiceFactory;
import com.google.appengine.api.prospectivesearch.ProspectiveSearchService;
import com.google.appengine.api.prospectivesearch.ProspectiveSearchServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;
import com.google.appengine.tools.appstats.AppstatsServlet;
import com.google.inject.Provides;
import com.google.inject.servlet.RequestScoped;

/**
 * Guice module that registers most AppEngine services on Guice and permits to enable app engine specific behaviors
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class AppengineMvcServletModule extends MvcServletModule {

	@Provides @RequestScoped public AppIdentityService appIdentityService() {
		return AppIdentityServiceFactory.getAppIdentityService();
	}

	@Provides @RequestScoped public MailService mailService() {
		return MailServiceFactory.getMailService();
	}

	@Provides @RequestScoped public BlobstoreService blobStoreService() {
		return BlobstoreServiceFactory.getBlobstoreService();
	}
	
	@Provides @RequestScoped public FileService fileService() {
		return FileServiceFactory.getFileService();
	}
	
	@Provides @RequestScoped public CapabilitiesService capabilitiesService() {
		return CapabilitiesServiceFactory.getCapabilitiesService();
	}
	
	@Provides @RequestScoped public ChannelService channelService() {
		return ChannelServiceFactory.getChannelService();
	}
	
	@Provides @RequestScoped public ImagesService imagesService() {
		return ImagesServiceFactory.getImagesService();
	}
	
	@Provides @RequestScoped public MemcacheService memcacheService() {
		return MemcacheServiceFactory.getMemcacheService();
	}

	@Provides @RequestScoped public AsyncMemcacheService asyncMmcacheService() {
		return MemcacheServiceFactory.getAsyncMemcacheService();
	}
	
	@Provides @RequestScoped public OAuthService oAuthService() {
		return OAuthServiceFactory.getOAuthService();
	}
	
	@Provides @RequestScoped public ProspectiveSearchService prospectiveSearchService() {
		return ProspectiveSearchServiceFactory.getProspectiveSearchService();
	}
	
	@Provides @RequestScoped public Queue defaultQueue() {
		return QueueFactory.getDefaultQueue();
	}
	
	@Provides @RequestScoped public URLFetchService urlFetchService() {
		return URLFetchServiceFactory.getURLFetchService();
	}
	
	@Provides @RequestScoped public UserService userService() {
		return UserServiceFactory.getUserService();
	}
	
	@Provides @RequestScoped public XMPPService xmppService() {
		return XMPPServiceFactory.getXMPPService();
	}

	/**
	 * Upload service is broken on the dev Server when using Guice Servlets, this enables a fix
	 */
	@Override
	@OverridingMethodsMustInvokeSuper
	protected void configureControllers() {
		serve("/__upload").with(MvcUploadService.class);
		serve("/__upload_dev").with(DevUploadServlet.class);
	}
	
	/**
	 * To be called from {@link #configureControllers()}, enables the app admin console
	 * 
	 * @param prefix The URL prefix on which to register app admin: "_aa" is highly advised
	 * @param aaClass The appadmin class that contains your admin tasks: use AppAdminController.class when no ones are defined
	 * @param withAppStats Whether or not to enable AppStats (and therefore AppStats filter)
	 */
	protected final void enableAppAdmin(String prefix, Class<? extends AppAdminController> aaClass, boolean withAppStats) {
		if (aaClass.getAnnotation(com.google.inject.Singleton.class) == null && aaClass.getAnnotation(javax.inject.Singleton.class) == null)
			throw new UnsupportedOperationException("The AppAdmin class must be annotated with @Singleton");
		
		control(prefix + "/*").with(aaClass);
		if (!aaClass.equals(AppAdminController.class))
			bind(AppAdminController.class).to(aaClass);
		redirect(prefix).to(prefix + "/");
		
		AppAdminController.Path = prefix + "/";
		
		if (withAppStats)
			enableAppStats(prefix);
	}

	protected final void enableAppAdmin(Class<? extends AppAdminController> aaClass, boolean withAppStats) {
		enableAppAdmin("/_aa", aaClass, withAppStats);
	}

	protected final void enableAppAdmin(String prefix, boolean withAppStats) {
		enableAppAdmin(prefix, AppAdminController.class, withAppStats);
	}

	protected final void enableAppAdmin(boolean withAppStats) {
		enableAppAdmin("/_aa", AppAdminController.class, withAppStats);
	}

	/**
	 * Enables appstats on the given prefix URL
	 */
	protected final void enableAppStats(String prefix) {
		filter("/*").through(new AppsstatsWithoutAAFilter(prefix));
		
		serve(prefix + "/appstats/*").with(new AppstatsServlet());
		redirect(prefix + "/appstats").to(prefix + "/appstats/");
		AppAdminController.AppstatsEnabled = true;
	}

	/**
	 * Enable the maintenance Filter, to enable to pass the application into maintenance mode through the app admin console
	 */
	protected final void enableMaintenanceFilter() {
		filter("/*").through(MaintenanceFilter.class);
		MaintenanceFilter.FilterEnabled = true;
	}
	
	protected final void scanForTask(Class<?> abs) {
		if (AppAdminController.toScan == null)
			throw new RuntimeException("Too late to add new classes to scan for tasks");
		AppAdminController.toScan.add(abs);
	}

}
