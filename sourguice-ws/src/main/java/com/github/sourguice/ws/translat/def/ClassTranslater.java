package com.github.sourguice.ws.translat.def;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import javax.annotation.CheckForNull;

import com.github.sourguice.ws.translat.WSSimpleGuiceTranslaterFactory;
import com.github.sourguice.ws.translat.WSTranslater;
import com.google.inject.Singleton;

@SuppressWarnings("rawtypes")
public class ClassTranslater implements WSTranslater<Class, String> {
	
	@Singleton
	public static class ClassTranslaterFactory extends WSSimpleGuiceTranslaterFactory<Class, String> {
		public ClassTranslaterFactory() {
			super(Class.class, String.class, ClassTranslater.class);
		}
	}

	@Override
	public @CheckForNull String toWS(Class obj, Type typeOnServer, AnnotatedElement el, @CheckForNull Object enclosing) {
		return obj.getName();
	}

	@Override
	public @CheckForNull Class fromWS(String str, Type typeOnServer, AnnotatedElement el, @CheckForNull Object enclosing) {
		try {
			return Class.forName(str);
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
