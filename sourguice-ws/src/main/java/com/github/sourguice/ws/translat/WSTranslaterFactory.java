package com.github.sourguice.ws.translat;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import com.google.inject.Singleton;

@Singleton
public interface WSTranslaterFactory<S, C> {

	Class<S> getServerClass();

	Type getClientType(Type typeOnServer, AnnotatedElement el);
	
	WSTranslater<S, C> getTranslater();
	
	boolean isInternal();
}
