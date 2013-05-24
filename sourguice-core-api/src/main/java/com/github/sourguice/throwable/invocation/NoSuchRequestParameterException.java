package com.github.sourguice.throwable.invocation;

import com.github.sourguice.annotation.request.RequestParam;

/**
 * Exception thrown when a @{@link RequestParam} annotated parameter is not found in the request
 * and the annotation does not provide a default implementation
 * 
 * This exception is caught by the MVC Calling system that provides a default error page stating that the parameter is missing
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class NoSuchRequestParameterException extends Exception {
	@SuppressWarnings("javadoc")
	private static final long serialVersionUID = 8013443036995570231L;

	/**
	 * The name of the missing parameter
	 */
	private String name;
	
	/**
	 * The type of the missing parameter
	 */
	private String type;
	
	/**
	 * @param name The name of the missing parameter
	 * @param type The type of the missing parameter
	 */
	public NoSuchRequestParameterException(String name, String type) {
		super("Missing " + type + ": " + name);
		this.name = name;
		this.type = type;
	}

	/**
	 * @return The name of the missing parameter
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return The type of the missing parameter
	 */
	public String getType() {
		return type;
	}
}
