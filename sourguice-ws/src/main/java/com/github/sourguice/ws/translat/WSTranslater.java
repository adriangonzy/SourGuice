package com.github.sourguice.ws.translat;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import javax.annotation.CheckForNull;

public interface WSTranslater<S, C> {
	public @CheckForNull C toWS(S obj, Type typeOnServer, AnnotatedElement el, @CheckForNull Object enclosing);
	public @CheckForNull S fromWS(C obj, Type typeOnServer, AnnotatedElement el, @CheckForNull Object enclosing);
}
