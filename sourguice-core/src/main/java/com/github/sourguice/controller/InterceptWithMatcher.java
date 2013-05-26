package com.github.sourguice.controller;

import java.lang.reflect.AnnotatedElement;

import com.github.sourguice.annotation.controller.InterceptWith;
import com.github.sourguice.utils.Annotations;
import com.google.inject.matcher.AbstractMatcher;

/**
 * Guice matcher that will check for the @{@link InterceptWith} annotation on the element
 * AND on it's java tree and parent
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public class InterceptWithMatcher extends AbstractMatcher<AnnotatedElement> {

	@Override
	public boolean matches(AnnotatedElement element) {
		return Annotations.GetOneTreeRecursive(element, InterceptWith.class) != null;
	}

}
