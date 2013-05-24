package com.github.sourguice.conversion;

import javax.annotation.CheckForNull;

import com.github.sourguice.annotation.ConvertsTo;
import com.github.sourguice.throwable.service.converter.NoConverterException;

/**
 * Singleton service that handles conversion from string to anything
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public interface ConversionService {

	/**
	 * Register a converter
	 * The types to associate this converter with are read from it's @{@link ConvertsTo} annotation
	 * @param conv The converter to register
	 */
	public abstract void registerConverter(Converter<?> conv);

	/**
	 * Gets the better converter for the given class
	 * If a converter is registered for the given class, returns it
	 * If not, tries to find the converter that can convert to a subclass of this class (and gets the closest).
	 * If none is found, returns null
	 * 
	 * @param clazz the class to convert to
	 * @return the converter to use or null if none were found
	 */
	public @CheckForNull
	abstract <T>Converter<T> getConverter(Class<T> clazz);

	/**
	 * Converts an array of string into an array of value
	 * Only non-primitives types are allowed as java does not provide a way to create a primitive array with generics
	 * 
	 * @param componentType The class to convert to (Only non-primitives types)
	 * @param from The array of string to convert
	 * @return The array of type converted from the strings
	 * @throws NoConverterException When no converter is found for the specific type (RuntimeException)
	 */
	public abstract <T>T[] convertArray(Class<T> componentType, Object[] from) throws NoConverterException;

	/**
	 * Converts a string or an array of string into a value or an array of values
	 * 
	 * @param toClazz The class to convert to.
	 *                If 'from' is an array, then primitive types are not allowed
	 * @param from The String or String[] to convert from (only String or String[])
	 * @return The value or array of values
	 * @throws NoConverterException When no converter is found for the specific type (RuntimeException)
	 */
	public @CheckForNull
	abstract Object convert(Class<?> toClazz, Object from) throws NoConverterException;

}
