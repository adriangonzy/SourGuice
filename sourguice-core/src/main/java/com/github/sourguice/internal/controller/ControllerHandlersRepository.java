package com.github.sourguice.internal.controller;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

/**
 * Class that holds all {@link ControllerHandler} existing in this server instance
 * This is to ensure that each controller class has one and only one handler
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Singleton
public final class ControllerHandlersRepository {
	/**
	 * All classes registered and their corresponding {@link ControllerHandler}
	 */
	private Map<Class<?>, ControllerHandler<?>> map = new HashMap<Class<?>, ControllerHandler<?>>();

	/**
	 * Gets the {@link ControllerHandler} for a given class and creates one if none is yet registered for this class
	 * 
	 * @param clazz The class on which to get / create a {@link ControllerHandler}
	 * @return The handler for the given class
	 */
	@SuppressWarnings("unchecked")
	public <T> ControllerHandler<T> get(Class<T> clazz) {
		if (map.containsKey(clazz))
			return (ControllerHandler<T>)map.get(clazz);
		ControllerHandler<T> c = new ControllerHandler<T>(clazz);
		map.put(clazz, c);
		return c;
	}
}
