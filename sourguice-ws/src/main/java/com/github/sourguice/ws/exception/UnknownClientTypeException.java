package com.github.sourguice.ws.exception;


public class UnknownClientTypeException extends RuntimeException {
	private static final long serialVersionUID = 3590054085580608330L;

	public UnknownClientTypeException(Class<?> cls, Throwable e) {
		super(cls.getName() + " is not known by the client and is therefore thrown server side", e);
	}

	public UnknownClientTypeException(Class<?> cls) {
		super(cls.getName() + " is not known by the client and cannot be returned to him");
	}
}
