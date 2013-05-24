package com.github.sourguice.annotation.request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierNickname;

/**
 * Indicates that the MVC system must writes the return of the annotated method directly to the response
 * rather than interpreting it as the view name.
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TypeQualifierNickname @Nonnull
public @interface Writes {
	/**
	 * @return Size of the buffer that will be used to write to the response
	 */
	public @Nonnegative int bufferSize() default 512;
}
