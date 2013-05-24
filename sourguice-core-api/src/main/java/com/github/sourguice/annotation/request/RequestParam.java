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
 * Indicates that a parameter is to be binded to a request parameter
 * Example:
 * {@code &#64;RequestMapping("/path") }
 * public void my_controller_method(@RequestParam("name") String name, @RequestParam("age") int age)
 * will bind /path?name=salomon&age=25 to the corresponding variables
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TypeQualifierNickname @Nonnull @Tainted
public @interface RequestParam {

	/**
	 * The name of the request parameter
	 */
	String value();
	
	/**
	 * If defined, this request parameter is not mandatory and, when not defined, will have this value
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
