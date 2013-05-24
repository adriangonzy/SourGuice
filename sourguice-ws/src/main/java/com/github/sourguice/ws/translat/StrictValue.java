package com.github.sourguice.ws.translat;

import java.lang.reflect.Type;


public class StrictValue {

	private Type type;
	
	private Object value;

	public <T> StrictValue(Class<T> strictClass, T value) {
		this((Type)strictClass, (Object)value);
	}
	
	public StrictValue(Type type, Object value) {
		this.type = type;
		this.value = value;
	}

	public Type getType() {
		return type;
	}
	
	public Object getValue() {
		return value;
	}
}
