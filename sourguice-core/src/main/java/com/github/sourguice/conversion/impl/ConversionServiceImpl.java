package com.github.sourguice.conversion.impl;

import java.lang.reflect.Array;
import java.util.HashMap;

import javax.annotation.CheckForNull;
import javax.inject.Singleton;

import com.github.sourguice.annotation.ConvertsTo;
import com.github.sourguice.conversion.ConversionService;
import com.github.sourguice.conversion.Converter;
import com.github.sourguice.conversion.def.ArrayConverter;
import com.github.sourguice.throwable.service.converter.NoConverterException;
import com.github.sourguice.throwable.service.converter.NoConvertsToAnnotationException;

/**
 * Holds all registered converters
 * Permits to the MVC system to convert string from the HTTP request to any type needed
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@SuppressWarnings("unchecked")
@Singleton
public class ConversionServiceImpl implements ConversionService {
	
	/**
	 * Map of registered convertable classes and their associated converter
	 */
	private HashMap<Class<?>, Converter<?>> converters = new HashMap<Class<?>, Converter<?>>();
	
	/**
	 * Register a converter to be associated with the given type
	 * If the type is of type array, than it will register the converter for the array type AND its subtype
	 * @param conv The converter to use when converting from String to the given type
	 * @param type The type to associate the converter with
	 * @return The converter associated with the class
	 */
	@SuppressWarnings("rawtypes")
	private Converter<?> registerConverter(Converter<?> conv, Class<?> type) {
		if (type.isArray()) {
			Class<?> componentType = type.getComponentType();
			Converter<?> componentConverter = registerConverter(conv, componentType);
			Converter<?> arrayConverter = new ArrayConverter(componentConverter);
			converters.put(type, arrayConverter);
			return arrayConverter;
		}

		converters.put(type, conv);
		return conv;
	}
	
	/**
	 * Register a converter
	 * The types to associate this converter with are read from it's @{@link ConvertsTo} annotation
	 * @param conv The converter to register
	 */
	@Override
	public void registerConverter(Converter<?> conv) {
		ConvertsTo to = conv.getClass().getAnnotation(ConvertsTo.class);
		if (to == null)
			throw new NoConvertsToAnnotationException(conv.getClass());
		for (Class<?> type : to.value())
			registerConverter(conv, type);
	}
	
	/**
	 * Utility to calculate the "distance" between a class and one of its parents
	 * if child == parent, distance = 0
	 * if child inherits directly parent, distance == 1
	 * 
	 * @param child The child class
	 * @param parent The parent class
	 * @param n The recursive level of the search
	 * @return The distance between child and parent
	 */
	static private int ClassUtilDistance(Class<?> child, Class<?> parent, int n) {
		if (child.equals(parent))
			return n;

		int distance = Integer.MAX_VALUE;
		
		if (child.getSuperclass() != null) {
			int nSuper = ClassUtilDistance(child.getSuperclass(), parent, n + 1);
			if (nSuper < distance)
				distance = nSuper;
		}
		for (Class<?> clazz : child.getInterfaces()) {
			int nSuper = ClassUtilDistance(clazz, parent, n + 1);
			if (nSuper < distance)
				distance = nSuper;
		}
		return distance;
	}
	
	/**
	 * Gets the better converter for the given class
	 * If a converter is registered for the given class, returns it
	 * If not, tries to find the converter that can convert to a subclass of this class (and gets the closest).
	 * If none is found, returns null
	 * 
	 * @param clazz the class to convert to
	 * @return the converter to use or null if none were found
	 */
	@Override
	public @CheckForNull <T> Converter<T> getConverter(Class<T> clazz) {
		if (converters.containsKey(clazz))
			return (Converter<T>)converters.get(clazz);
		
		int closestDistance = Integer.MAX_VALUE;
		Class<?> closestType = null;
		for (Class<?> type : converters.keySet()) {
			if (type.isAssignableFrom(clazz)) {
				int distance = ClassUtilDistance(clazz, type, 0);
				if (distance < closestDistance) {
					closestDistance = distance;
					closestType = type;
				}
			}
		}
		
		if (closestType != null)
			return (Converter<T>)converters.get(closestType);

		return null;
	}
	
	/**
	 * Converts an array of string into an array of value
	 * Only non-primitives types are allowed as java does not provide a way to create a primitive array with generics
	 * 
	 * @param componentType The class to convert to (Only non-primitives types)
	 * @param from The array of string to convert
	 * @return The array of type converted from the strings
	 * @throws NoConverterException When no converter is found for the specific type (RuntimeException)
	 */
	@Override
	public <T> T[] convertArray(Class<T> componentType, Object[] from) throws NoConverterException {
		if (componentType.isPrimitive())
			throw new RuntimeException("Array conversion does not support primitive types");
		Object[] ret = (Object[])Array.newInstance(componentType, from.length);
		for (int i = 0; i < from.length; ++i)
			ret[i] = convert(componentType, from[i]);
		return (T[])ret;
	}

	/**
	 * Converts a string or an array of string into a value or an array of values
	 * 
	 * @param toClazz The class to convert to.
	 *                If 'from' is an array, then primitive types are not allowed
	 * @param from The String or String[] to convert from (only String or String[])
	 * @return The value or array of values
	 * @throws NoConverterException When no converter is found for the specific type (RuntimeException)
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public @CheckForNull Object convert(Class<?> toClazz, Object from) throws NoConverterException {
		if (from.getClass().equals(String.class)) {
			Converter conv = this.getConverter(toClazz);
			if (conv == null)
				throw new NoConverterException(toClazz);
			return conv.get(toClazz, (String)from);
		}
		else if (from.getClass().isArray()) {
			if (!toClazz.isArray())
				throw new RuntimeException("Cannot convert an array of string into a non-array type");
			return this.convertArray(toClazz.getComponentType(), (Object[])from);
		}
		throw new RuntimeException("Only String, array of String, array of array of string, etc. are allowed");
	}
}
