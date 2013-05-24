package com.github.sourguice.ws.exception;

import javax.annotation.CheckForNull;

import com.github.sourguice.ws.annotation.WSDisregardParent;

@WSDisregardParent
public final class WSRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 7504756444267626862L;

	public String code;
	public @CheckForNull String message;
	
	public WSRuntimeException(String code, String message) {
		super(message);
		this.code = code;
		this.message = message;
	}

	public WSRuntimeException(String code) {
		super();
		this.code = code;
	}
	
	public String getCode() {
		return code;
	}

}
