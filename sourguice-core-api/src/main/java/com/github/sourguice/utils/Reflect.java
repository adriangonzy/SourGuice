package com.github.sourguice.utils;

import java.lang.reflect.Method;
import javax.annotation.CheckForNull;

/**
 * Reflection related utils
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public final class Reflect {

	/**
	 * Gets the first method of the specified class whose name is the given one
	 * This will return the FIRST method it finds with the given name
	 * 
	 * @param clazz The class on which to search the method
	 * @param name The name of the method to search
	 * @return The method found or null
	 */
	public static @CheckForNull Method GetMethod(Class<?> clazz, final String name) {
		for (Method method : clazz.getMethods())
			if (method.getName().equals(name))
				return method;
		return null;
	}
}
