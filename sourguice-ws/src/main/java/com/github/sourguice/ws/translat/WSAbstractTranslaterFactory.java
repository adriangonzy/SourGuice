package com.github.sourguice.ws.translat;

import com.google.inject.Singleton;

@Singleton
public abstract class WSAbstractTranslaterFactory<S, C> implements WSTranslaterFactory<S, C> {

	Class<S> serverClass;

	public WSAbstractTranslaterFactory(Class<S> serverClass) {
		super();
		this.serverClass = serverClass;
	}

	@Override
	public Class<S> getServerClass() {
		return serverClass;
	}
	
	@Override
	public boolean isInternal() {
		return false;
	}
}
