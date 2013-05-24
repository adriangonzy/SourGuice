package com.github.sourguice;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.regex.MatchResult;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.sourguice.annotation.request.GuiceRequest;
import com.github.sourguice.annotation.request.RequestMapping;
import com.github.sourguice.request.ForwardableRequestFactory;
import com.github.sourguice.utils.RequestScopeContainer;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.servlet.ServletModule;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This class is the base guice module class to inherit
 * To configure a MVC module, create a subclass of this class
 * then override {@link #configureControllers()}
 * Then, use the syntax control(pattern).with(controller.class)
 * You can, of course, use the standard guice and guice-servlet commands.
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public abstract class MvcServletModule extends ServletModule {

	/**
	 * This class is needed because API and implementation are not in the same JAR project.
	 * This means that while the module is in the API jar, all implementations are in a seperate jar
	 * that is unknown from the API code.
	 * To connect to the implementation, the API code uses reflexivity to find the implementation class.
	 * This implementation class implements MvcServletModuleHelperProxy that the API module will use
	 * to delegate the actual implementation and bindings.
	 * @author salomon
	 */
	static interface MvcServletModuleHelperProxy {
		/**
		 * @see MvcServletModule#getForwardableRequestFactory(HttpServletRequest, ServletContext)
		 */
		public ForwardableRequestFactory getForwardableRequestFactory(@GuiceRequest HttpServletRequest req, ServletContext context);
		
		/**
		 * @see MvcServletModule#configureServlets()
		 */
		public void configureServlets();
		
		/**
		 * @see MvcServletModule#control(String, String...)
		 */
		public ControlBuilder control(final String pattern, final String... patterns);
		
		/**
		 * @see MvcServletModule#redirect(String, String...)
		 */
		public RedirectBuilder redirect(final String pattern, final String... patterns);
	}

	/**
	 * The helper proxy on which every implementation call will be forwarded
	 * This should be an instance of com.github.sourguice.MvcServletModuleHelperImpl
	 * found by reflexivity
	 */
	private MvcServletModuleHelperProxy helper;
	
	/**
	 * This will check that the implementation jar is actually in the classpath
	 */
	public MvcServletModule() {
		try {
			helper = (MvcServletModuleHelperProxy)
						Class
							.forName("com.github.sourguice.MvcServletModuleHelperImpl")
							.getConstructor(MvcServletModule.class)
							.newInstance(this);
		}
		catch (ReflectiveOperationException e) {
			throw new RuntimeException("Cannot find SourGuice Implementation, make sure it is deployed with your application", e);
		}
	}
	
	/**
	 * Registers in guice the PrintWriter class to be binded to the request's response writer
	 */
	@Provides @RequestScoped public PrintWriter getRequestPrintWriter(HttpServletResponse res) throws IOException {
		return res.getWriter();
	}

	/**
	 * Registers in guice the Writer class to be binded to the request's response writer
	 */
	@Provides @RequestScoped public Writer getRequestWriter(HttpServletResponse res) throws IOException {
		return res.getWriter();
	}
	
	/**
	 * Registers in guice the HttpServletRequest class annotated with @{@link GuiceRequest}
	 * to be binded to the Guice modified request
	 */
	@SuppressFBWarnings({"TQ_NEVER_VALUE_USED_WHERE_ALWAYS_REQUIRED"})
	@Provides @RequestScoped @GuiceRequest public HttpServletRequest getGuiceRequest(RequestScopeContainer container) {
		return container.get(HttpServletRequest.class);
	}

	/**
	 * Registers in guice the MatchResult class to be binded to the request's URL parsed path variables
	 * according to the request's {@link RequestMapping}
	 */
	@Provides @RequestScoped public MatchResult getPathMatcher(RequestScopeContainer container) {
		return container.get(MatchResult.class);
	}
	
	/**
	 * Registers in guice the ForwardableRequestFactory class that easily allow servlet request forwarding
	 * Attention: not to be confused with HTTP redirection!
	 */
	@Provides @RequestScoped public ForwardableRequestFactory getForwardableRequestFactory(@GuiceRequest HttpServletRequest req, ServletContext context) {
		return helper.getForwardableRequestFactory(req, context);
	}
	
	/**
	 * This is the method that guice requires to overrides to configure servlets.
	 * To configure controllers or additional servlets, override {@link #configureControllers()}
	 * 
	 * This is where the magic happens ;)
	 * This methods binds all necessary classes and interface to make SourGuice work
	 */
	@Override
	protected final void configureServlets() {
		super.configureServlets();

		helper.configureServlets();
	}

	/**
	 * This is the method to override to configure controllers.
	 * In this method, you shall use the syntax control(pattern).with(controller.class)
	 * Refer to the documentation for more informations
	 */
	abstract protected void configureControllers();
	
	/**
	 * Interface returned by {@link #control(String, String...)} to permit the syntax control(pattern).with(controller.class)
	 */
	public static interface ControlBuilder {
		/**
		 * Second method of the syntax control(pattern).with(controller.class)
		 * Associates the previously defined pattern to a class
		 * 
		 * @param clazz The class to register
		 */
		public void with(Class<?> clazz);

		// TODO: Handle withInstance
//		public void withInstance(Object controller);
	}
	
	/**
	 * First method of the syntax control(pattern).with(controller.class)
	 * 
	 * @param pattern The pattern to register for the later controller
	 * @param patterns Any additional patterns to register
	 * @return ControlBuilder on which {@link ControlBuilder#with(Class)} must be called
	 */
	public final ControlBuilder control(final String pattern, final String... patterns) {
		return helper.control(pattern, patterns);
	}
	
	/**
	 * Interface returned by {@link MvcServletModule#redirect(String, String...)} to permit the syntax redirect(pattern).to(path)
	 */
	public static interface RedirectBuilder {
		/**
		 * Second method of the syntax redirect(pattern).to(path)
		 * Associates previously defined pattern to the path
		 * 
		 * @param path The path on which redirect
		 */
		public void to(String path);
	}
	
	/**
	 * First method of the syntax redirect(pattern).to(path)
	 * 
	 * @param pattern The pattern to redirect to the later path
	 * @param patterns Any additional patterns to redirect
	 * @return RedirectBuilder on which {@link RedirectBuilder#to(String)} must be called
	 */
	public final RedirectBuilder redirect(final String pattern, final String... patterns) {
		return helper.redirect(pattern, patterns);
	}
	
	/**
	 * Allows implementation proxy class to access the actual binder
	 * This is needed because the protected permission allows the implementation proxy,
	 * which is on the same package as this module class, to access this method.
	 */
	@Override
	protected Binder binder() {
		return super.binder();
	}
	
	/**
	 * Allows implementation proxy to access the Guice-Servlet syntax serve().with()
	 * This is needed because the protected permission allows the implementation proxy,
	 * which is on the same package as this module class, to access this method.
	 */
	protected ServletModule.ServletKeyBindingBuilder _serve(String urlPattern, String... morePatterns) {
		return serve(urlPattern, morePatterns);
	}

}