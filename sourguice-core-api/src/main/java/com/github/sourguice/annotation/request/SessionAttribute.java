package com.github.sourguice.annotation.request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.CheckForNull;
import javax.annotation.Untainted;
import javax.annotation.meta.TypeQualifierNickname;

/**
 * Indicates that a parameter is to be binded to a session attribute
 * Using this will enforce the presence of a session and will create it if needed
 * <p>
 * Example:
 * {@code &#64;RequestMapping("/path") }
 * public void my_controller_method(@SessionAttribute("name") String name, @SessionAttribute("age") int age)
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TypeQualifierNickname @CheckForNull @Untainted
public @interface SessionAttribute {
	/**
	 * The name of the session attribute to retrieve
	 */
	String value();
}
