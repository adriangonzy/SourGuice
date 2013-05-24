package com.github.sourguice.throwable.invocation;

/**
 * A simple exception that indicates that an exception was thrown by the method BUT handled by the exception service
 * This is thrown only when throwWhenHandled is true in the call methods
 */
public class HandledException extends Exception {
	@SuppressWarnings("javadoc")
	private static final long serialVersionUID = 7498119669424018953L;

	/**
	 * Simple constructor that does nothing but call super
	 * @param e The handled exception
	 */
	public HandledException(Throwable e) {
		super(e);
	}
}