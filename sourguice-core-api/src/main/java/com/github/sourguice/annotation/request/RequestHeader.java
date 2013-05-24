package com.github.sourguice.annotation.request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Nonnull;
import javax.annotation.Tainted;
import javax.annotation.meta.TypeQualifierNickname;

import com.github.sourguice.value.ValueConstants;

/**
 * Indicates that a parameter is to be binded to a request header
 * Example:
 * {@code &#64;RequestMapping("/path") }
 * {@code public void my_controller_method(&#64;Requestheader("If-Modified-Since") String IfModifiedSince) }
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TypeQualifierNickname @Nonnull @Tainted
public @interface RequestHeader {

	/**
	 * The name of the request header
	 */
	String value();
	
	/**
	 * If defined, this request header is not mandatory and, when not defined, will have this value
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
