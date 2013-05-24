package com.github.sourguice.ws.translat;

import javax.annotation.CheckForNull;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public abstract class WSGuiceTranslaterFactory<S, C> extends WSAbstractTranslaterFactory<S, C> {

	Class<? extends WSTranslater<S, C>> translaterClass;
	
	@Inject @CheckForNull Injector injector;
	
	public WSGuiceTranslaterFactory(Class<S> serverClass, Class<? extends WSTranslater<S, C>> translaterCls) {
		super(serverClass);
		this.translaterClass = translaterCls;
	}

	@Override
	public WSTranslater<S, C> getTranslater() {
		assert injector != null;
		return injector.getInstance(translaterClass);
	}
}
