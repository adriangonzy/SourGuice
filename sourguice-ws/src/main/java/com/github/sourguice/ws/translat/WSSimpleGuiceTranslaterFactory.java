package com.github.sourguice.ws.translat;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import com.google.inject.Singleton;

@Singleton
public abstract class WSSimpleGuiceTranslaterFactory<S, C> extends WSGuiceTranslaterFactory<S, C> {

	private Type clientClass;
	
	public WSSimpleGuiceTranslaterFactory(Class<S> serverClass, Type clientClass, Class<? extends WSTranslater<S, C>> translaterClass) {
		super(serverClass, translaterClass);
		this.clientClass = clientClass;
	}

	@Override
	public Type getClientType(Type typeOnServer, AnnotatedElement element) {
		return clientClass;
	}
}
