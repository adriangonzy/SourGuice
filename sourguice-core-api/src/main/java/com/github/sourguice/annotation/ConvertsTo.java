package com.github.sourguice.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates which types a converter can handle
 * for each type, it is better to indicates the array type of this type
 * That DOES NOT mean that the converter can handle arrays
 * BUT that arrays can be converted, each value of it through the anotated converter
 * For a very simple example, refer to Core's IntegerConverter
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConvertsTo {
	/**
	 * @return The array class of the type that the annotated converter converts to
	 */
	public Class<?>[] value();
}
