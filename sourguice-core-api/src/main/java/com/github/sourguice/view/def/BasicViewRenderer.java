package com.github.sourguice.view.def;

import java.io.PrintWriter;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import com.github.sourguice.view.ViewRenderer;
import com.google.inject.servlet.RequestScoped;

/**
 * Very simple view renderer.
 * You basically have to define a class that extends BasicViewRenderer and annotate each "view method" with {@link RenderFor}.
 * Each "view method" must have exactly the following signature: void(PrintWriter, Map<String, Object>);
 * You then annotate the controller with @RenderWith(YourBasiciewRenderer.class)
 * <p>
 * This utility class IS NOT intended for application wide rendering
 * but for specific SourGuice plugins that do not want to rely on a specific view renderer plugin.
 * It is NOT optimized, NOT cached, NOT whateveryouwant. It is very simple, fast and complexion free to be able to quickly build a very simple UI.
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@RequestScoped
public abstract class BasicViewRenderer implements ViewRenderer {

	/**
	 * Each "view method" of the BasicViewRenderer subclass must be annotated with this
	 * 
	 * @author Salomon BRYS <salomon.brys@gmail.com>
	 */
	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	public @interface RenderFor {
		/**
		 * The name of the view it is rendering
		 */
		public String value();
	}

	/**
	 * Exception thrown when no method is found for a given view name
	 * 
	 * This is a runtime exception because it is a programming error and therefore should only be caught in very specific circumstances.
	 * 
	 * @author Salomon BRYS <salomon.brys@gmail.com>
	 */
	public class NoSuchBasicViewMethodException extends RuntimeException {
		@SuppressWarnings("javadoc")
		private static final long serialVersionUID = -962911669036518369L;

		/**
		 * @param clazz The class that's missing the annotation
		 * @param view The missing view
		 */
		public NoSuchBasicViewMethodException(Class<?> clazz, String view) {
			super(clazz.getCanonicalName() + " has no method annotated with @RenderFor(\"" + view + "\")");
		}
	}
	
	/**
	 * Contains all view name registered and their associated methods
	 */
	private Map<String, Method> map = new HashMap<String, Method>();

	/**
	 * The current HTTP Response
	 */
	private HttpServletResponse res;

	/**
	 * @param res The response on which to write the views
	 */
	@Inject
	public BasicViewRenderer(HttpServletResponse res) {
		this.res = res;

		// Gets all annotated method and "remembers" them
		for (Method method : this.getClass().getMethods())
			if (method.getAnnotation(RenderFor.class) != null)
				map.put(method.getAnnotation(RenderFor.class).value(), method);
	}

	/**
	 * Calls the method registered to the given view name and passes the model to it
	 */
	@Override
	public final void render(String view, Map<String, Object> model) throws NoSuchBasicViewMethodException, Throwable {
		if (map.containsKey(view)) {
			try (PrintWriter out = res.getWriter()) {
				map.get(view).invoke(this, out, model);
				out.flush();
			}
			return ;
		}
		throw new NoSuchBasicViewMethodException(this.getClass(), view);
	}

}
