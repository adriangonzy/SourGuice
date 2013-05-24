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
 * Indicates that a parameter is to be binded to a path variable
 * Example:
 * {@code &#64;RequestMapping("/a/&#123;name&#125;/&#123;age&#125; }
 * public void my_controller_method(@PathVariable("name") String name, @PathVariable("age") int age)
 * 
 * A variable can contain anything but a slash
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TypeQualifierNickname @Nonnull @Tainted
public @interface PathVariable {

	/**
	 * The name of the variable declared in @RequestMapping
	 */
	String value();
	
	/**
	 * If defined, this path variable is not mandatory and, when not defined, will have this value
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
