package com.github.sourguice.annotation.request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.github.sourguice.annotation.controller.Callable;
import com.github.sourguice.call.MvcCaller;
import com.github.sourguice.value.RequestMethod;

/**
 * This is the annotation that must be present on all methods that can be called from a HTTP Request.
 * It basically tells the engine all requirements that are needed from the HTTP Request to be called.
 * The caller will then decide which method to use by :
 * 1/ selecting all methods that accept the given request
 * 2/ If there are more than one, select the most specialized
 * <p>
 * For example, given a GET request to /path?variable=true
 * and 2 methods anotated with :
 * {@code &#64;RequestMapping("/path") }
 * and
 * {@code &#64;RequestMapping(value = "/path", method = RequestMethod.GET, params = &#123;"variable"&#125;) }
 * The two methods are candidates for this request but the second has a higher specialization (index 3 versus 1)
 * Therefore, the second will be called.
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Callable
public @interface RequestMapping {

	/**
	 * The most important information : the path on which this method is registered
	 * When absent, the method CANNOT be called directly form MVC (but may be called manually via {@link MvcCaller})
	 */
	String[] value();

	/**
	 * Specializes the method to receive only call from request that are made via specified methods
	 */
	RequestMethod[] method() default {};

	/**
	 * Specializes the method to receive only call from request that contains the given parameters
	 */
	String[] params() default {};

	/**
	 * Specializes the method to receive only call from request that contains the given header
	 */
	String[] headers() default {};

	/**
	 * Specializes the method to receive only call from request that contains the given "Content-type" header
	 */
	String[] consumes() default {};

	/**
	 * Specializes the method to receive only call from request that contains the given "Accept" header
	 */
	String[] produces() default {};
}
