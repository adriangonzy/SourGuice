package com.github.sourguice;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.sourguice.MvcServletModule;
import com.github.sourguice.MvcServletModule.ControlBuilder;
import com.github.sourguice.MvcServletModule.MvcServletModuleHelperProxy;
import com.github.sourguice.MvcServletModule.RedirectBuilder;
import com.github.sourguice.annotation.request.GuiceRequest;
import com.github.sourguice.annotation.request.PathVariablesMap;
import com.github.sourguice.call.MvcCaller;
import com.github.sourguice.call.impl.MvcCallerImpl;
import com.github.sourguice.call.impl.PathVariablesProvider;
import com.github.sourguice.conversion.ConversionService;
import com.github.sourguice.conversion.def.BooleanConverter;
import com.github.sourguice.conversion.def.DoubleConverter;
import com.github.sourguice.conversion.def.EnumConverter;
import com.github.sourguice.conversion.def.FloatConverter;
import com.github.sourguice.conversion.def.IntegerConverter;
import com.github.sourguice.conversion.def.LongConverter;
import com.github.sourguice.conversion.def.ShortConverter;
import com.github.sourguice.conversion.def.StringConverter;
import com.github.sourguice.conversion.impl.ConversionServiceImpl;
import com.github.sourguice.exception.ExceptionService;
import com.github.sourguice.exception.def.MVCHttpServletResponseExceptionHandler;
import com.github.sourguice.exception.impl.ExceptionServiceImpl;
import com.github.sourguice.internal.controller.ControllerHandlersRepository;
import com.github.sourguice.internal.controller.ControllerInterceptor;
import com.github.sourguice.internal.controller.ControllersServlet;
import com.github.sourguice.internal.controller.InterceptWithMatcher;
import com.github.sourguice.request.ForwardableRequestFactory;
import com.github.sourguice.request.wrapper.GuiceForwardHttpRequest;
import com.github.sourguice.throwable.controller.MVCHttpServletResponseException;
import com.github.sourguice.throwable.service.exception.UnreachableExceptionHandlerException;
import com.github.sourguice.utils.RequestScopeContainer;
import com.github.sourguice.view.Model;
import com.github.sourguice.view.ViewRenderer;
import com.github.sourguice.view.def.JSPViewRenderer;
import com.google.inject.matcher.Matchers;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.servlet.ServletScopes;

/**
 * This is used by {@link MvcServletModule} to actually bind the implementations of SourGuices classes
 * This is needed because SourGuice builds two different jars : one for API and one for implementation
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class MvcServletModuleHelperImpl implements MvcServletModuleHelperProxy {

	/**
	 * The list of registered path and their corresponding controllers
	 * The purpose of this is to ensure that a path is handled by one controller,
	 * even if this path has been registered multiple times
	 */
	private @CheckForNull HashMap<String, ControllersServlet> servlets = null;

	/**
	 * Contains all ControllerHandlers
	 * This is to make sure that there will be one and only one ControllerHandler for each controller class
	 */
	private ControllerHandlersRepository repository = new ControllerHandlersRepository();

	/**
	 * The actual module that ws subclassed to bind controllers
	 */
	MvcServletModule module;

	/**
	 * Constructor, used by {@link MvcServletModule}.
	 * @param module The module itself, so this helper will access to Guice binding methods
	 */
	public MvcServletModuleHelperImpl(MvcServletModule module) {
		super();
		this.module = module;
	}

	/**
	 * Used by {@link MvcServletModule#getForwardableRequestFactory(HttpServletRequest, ServletContext)}
	 * to get a ForwardableRequestFactory implementation
	 */
	@Override
	public ForwardableRequestFactory getForwardableRequestFactory(@GuiceRequest HttpServletRequest req, ServletContext context) {
		return new GuiceForwardHttpRequest(req, context);
	}
	
	/**
	 * Used by {@link MvcServletModule#configureServlets()}
	 * to actually configure servlets and bind objects
	 */
	@Override
	public final void configureServlets() {
		
		// Binds RequestScope container that will contains RequestScope objects that cannot be directly integrated into Guice
		module.binder().bind(RequestScopeContainer.class);
		
		// Creates a conversion service and registers it in guice
		// We create it because we need to handle it directly in this method
		ConversionService conversionService = new ConversionServiceImpl();
		module.binder().bind(ConversionService.class).toInstance(conversionService);

		// Creates an exception service and registers it in guice
		// We create it because we need to handle it directly in this method
		ExceptionService exceptionService = new ExceptionServiceImpl();
		module.binder().bind(ExceptionService.class).toInstance(exceptionService);

		// Binds view related classes
		module.binder().bind(ViewRenderer.class).to(JSPViewRenderer.class);
		module.binder().bind(Model.class).in(ServletScopes.REQUEST);

		// Binds method calling related classes
		module.binder().bind(MvcCaller.class).to(MvcCallerImpl.class).in(RequestScoped.class);;
		module.binder().bind(Map.class).annotatedWith(PathVariablesMap.class).toProvider(PathVariablesProvider.class).in(RequestScoped.class);
		
		// Creates a controllerHandler repository and registers it in guice
		// We create it because we need to handle it directly in this method
		module.binder().bind(ControllerHandlersRepository.class).toInstance(repository);

		// Binds interceptors
		ControllerInterceptor interceptor = new ControllerInterceptor();
		module.binder().requestInjection(interceptor);
//		bindInterceptor(Matchers.any(), Matchers.annotatedWith(InterceptWith.class), interceptor);
//		bindInterceptor(Matchers.annotatedWith(InterceptWith.class), Matchers.any(), interceptor);
		module.binder().bindInterceptor(Matchers.any(), new InterceptWithMatcher(), interceptor);
		module.binder().bindInterceptor(new InterceptWithMatcher(), Matchers.any(), interceptor);
		
		// Creates servlet map to be later filled by configureControllers()
		servlets = new HashMap<String, ControllersServlet>();

		// Asks for controller registration by subclass
		// This will fill the servlets map
		module.configureControllers();

		assert servlets != null;
		Map<String, ControllersServlet> allServlets = servlets;
		// Loops through all registered patterns and their corresponding ControllerHandler.
		// Registers each couple in Guice.
		for (String pattern : allServlets.keySet()) {
			ControllersServlet servlet = allServlets.get(pattern);
			module.binder().requestInjection(servlet);
			module._serve(pattern).with(servlet);
		}

		// Registers default converters
		conversionService.registerConverter(new BooleanConverter());
		conversionService.registerConverter(new DoubleConverter());
		conversionService.registerConverter(new EnumConverter());
		conversionService.registerConverter(new FloatConverter());
		conversionService.registerConverter(new IntegerConverter());
		conversionService.registerConverter(new LongConverter());
		conversionService.registerConverter(new ShortConverter());
		conversionService.registerConverter(new StringConverter());

		// Registers default exception handlers
		try {
			exceptionService.registerHandler(MVCHttpServletResponseException.class, new MVCHttpServletResponseExceptionHandler());
		} catch (UnreachableExceptionHandlerException e) {
			// THIS SHOULD NEVER HAPPEN WHILE THIS METHOD IS NOT OVERRIDEN
			throw new RuntimeException(e);
		}

		// Sets null to the servlets variable so any further call to control().with() will raise a NullPointerException
		servlets = null;
	}

	/**
	 * Registers a pattern to a controller class
	 * This is called by {@link ControlBuilder#with(Class)}
	 * 
	 * @param clazz The controller class to register
	 * @param pattern The pattern on which to register to controller
	 */
	private void register(Class<?> clazz, final String pattern) {
		// Registers all filters that are declared by the @FilterThrough annotation of this class and of all its parents
		Map<String, String> initParams = new HashMap<String, String>();
		initParams.put("pattern", pattern);

		// Creates a controller servlet for this pattern or gets it if this pattern has already been registered
		ControllersServlet servlet;
		assert servlets != null;
		if (servlets.containsKey(pattern))
			servlet = servlets.get(pattern);
		else {
			servlet = new ControllersServlet();
			servlets.put(pattern, servlet);
		}
		
		// Registers a controller handler into the controller servlet
		// The handler is retrived from the repository to avoid creating two handlers for the same controller class
		servlet.addController(repository.get(clazz));
	}

	/**
	 * First method of the syntax control(pattern).with(controller.class)
	 * 
	 * @param pattern The pattern to register for the later controller
	 * @param patterns Any additional patterns to register
	 */
	@Override
	public final ControlBuilder control(final String pattern, final String... patterns) {
		return new ControlBuilder() {
			@Override public void with(Class<?> clazz) {
				// Now that we have all the patterns and the corresponding controller class,
				// registers all patterns to the given controller class.
				module.binder().bind(clazz);
				register(clazz, pattern);
				for (String p : patterns)
					register(clazz, p);
			}
		};
	}
	
	/**
	 * First method of the syntax redirect(pattern).to(path)
	 * 
	 * @param pattern The pattern to redirect to the later path
	 * @param patterns Any additional patterns to redirect
	 */
	@Override
	public final RedirectBuilder redirect(final String pattern, final String... patterns) {
		return new RedirectBuilder() {
			@Override public void to(final String dest) {
				module._serve(pattern, patterns).with(new HttpServlet() {
					private static final long serialVersionUID = 1L;

					@Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
						assert(res != null);
						res.sendRedirect(dest);
					}
				});
			}
		};
	}
}