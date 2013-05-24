package com.github.sourguice.ws.translat;

import com.github.sourguice.ws.exception.WSRuntimeException;

public interface WSRuntimeTranslater<T extends Exception> {

	public Class<T> getExceptionClass();
	
	public WSRuntimeException transformException(T e);
	
}
