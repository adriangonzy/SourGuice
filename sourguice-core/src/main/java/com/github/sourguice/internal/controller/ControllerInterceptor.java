package com.github.sourguice.internal.controller;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.CheckForNull;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.github.sourguice.annotation.controller.InterceptWith;
import com.github.sourguice.utils.Annotations;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

/**
 * This interceptor will get all @{@link InterceptWith} annotations on the given method.
 * It will then construct a recursive MethodInvocation tree.
 * Each node defers its execution to the interceptor given in the annotation.
 * That way, if an interceptor chooses to stop the invocation and *not* call {@link MethodInvocation#proceed()},
 * the contained interceptors will not be called.
 * The tree is constructed with {@link Annotations#GetAllTreeRecursive(java.lang.reflect.AnnotatedElement, Class)}
 * which means that the "closest" annotation will be used first.
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Singleton
public class ControllerInterceptor implements MethodInterceptor {

	/**
	 * Injector used to fetch actual Interceptors
	 */
	@Inject @CheckForNull Injector injector;

	/**
	 * Wrapper around {@link MethodInvocation} to allow the invocation to be intercepted
	 * by a guice registered MethodInterceptor
	 * 
	 * @author Salomon BRYS <salomon.brys@gmail.com>
	 */
	private class InterceptorInvocation implements MethodInvocation {

		/**
		 * The original MethodInvocation
		 */
		MethodInvocation invocation;
		
		/**
		 * The class of the interceptor. An instance will be asked to guice when proceeding
		 */
		Class<? extends MethodInterceptor> interceptorClass;
		
		/**
		 * @param invocation The original MethodInvocation
		 * @param interceptor The class of the interceptor. An instance will be asked to guice when proceeding
		 */
		InterceptorInvocation(MethodInvocation invocation, Class<? extends MethodInterceptor> interceptor) {
			super();
			this.invocation = invocation;
			this.interceptorClass = interceptor;
		}

		/**
		 * Wrapper proxy
		 */
		@Override public AccessibleObject getStaticPart() { return invocation.getStaticPart(); }

		/**
		 * Wrapper proxy
		 */
		@Override public Object getThis() { return invocation.getThis(); }

		/**
		 * Wrapper proxy
		 */
		@Override public Object[] getArguments() { return invocation.getArguments(); }

		/**
		 * Wrapper proxy
		 */
		@Override public Method getMethod() { return invocation.getMethod(); }

		/**
		 * Will delay the actual invocation to the guice fetched MethodInterceptor
		 */
		@Override
		public Object proceed() throws Throwable {
			assert injector != null;
			return injector.getInstance(interceptorClass).invoke(invocation);
		}
		
	}
	
	/**
	 * Constructs the {@link MethodInvocation} tree and launches the execution of the found interceptors.
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		
		List<InterceptWith> interceptWithAnnos = Annotations.GetAllTreeRecursive(invocation.getMethod(), InterceptWith.class);

		for (InterceptWith interceptWith : interceptWithAnnos)
			for (Class<? extends MethodInterceptor> interceptor : interceptWith.value())
				invocation = new InterceptorInvocation(invocation, interceptor);
		
		return invocation.proceed();
	}
}
