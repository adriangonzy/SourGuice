package com.github.sourguice.test;

import com.github.sourguice.MvcServletModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

public class StandardContextListener<T extends MvcServletModule> extends GuiceServletContextListener {

	T module;
	
	public StandardContextListener(T module) {
		super();
		this.module = module;
	}

	@Override
	protected Injector getInjector() {
		return Guice.createInjector(module);
	}

}
