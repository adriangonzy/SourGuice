package com.github.sourguice.throwable.service.converter;

import com.github.sourguice.annotation.ConvertsTo;

/**
 * Exception thrown when registering a converter not annotated with @{@link ConvertsTo} annotation
 * 
 * This is a runtime exception because it is a programming error and therefore should only be caught in very specific circumstances.
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class NoConvertsToAnnotationException extends RuntimeException {
	@SuppressWarnings("javadoc")
	private static final long serialVersionUID = 4234056481279632940L;

	/**
	 * @param clazz The converter class that is not annotated
	 */
	public NoConvertsToAnnotationException(Class<?> clazz) {
		super("Converter " + clazz.getCanonicalName() + " does not includes the @ConvertsTo annotation");
	}
}
