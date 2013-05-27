package com.github.sourguice.controller.fetchers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import com.github.sourguice.annotation.request.PathVariablesMap;
import com.github.sourguice.utils.Annotations;
import com.google.inject.BindingAnnotation;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.googlecode.gentyref.GenericTypeReflector;

/**
 * Fetcher that handles argument that are not annotated with an annotation handled by previous fetchers
 * This will fetch the argument from Guice
 * 
 * @param <T> The type of object to fetch from Guice
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class InjectorArgumentFetcher<T> extends ArgumentFetcher<T> {

	/**
	 * A Guice {@link BindingAnnotation}, if there is one
	 */
	@CheckForNull Annotation bindingAnnotation;
	
	/**
	 * @see ArgumentFetcher#ArgumentFetcher(Type, int, Annotation[])
	 */
	public InjectorArgumentFetcher(Type type, int pos, Annotation[] annotations) {
		super(type, pos, annotations);
		
		bindingAnnotation = Annotations.GetOneAnnotated(BindingAnnotation.class, annotations);
		if (bindingAnnotation == null)
			bindingAnnotation = Annotations.fromArray(annotations).getAnnotation(Named.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected @CheckForNull T getPrepared(HttpServletRequest req, @PathVariablesMap Map<String, String> pathVariables, Injector injector) {
		if (bindingAnnotation != null)
			return (T)injector.getInstance(Key.get(GenericTypeReflector.erase(this.type), bindingAnnotation));
		return (T)injector.getInstance(Key.get(GenericTypeReflector.erase(this.type)));
	}
}
