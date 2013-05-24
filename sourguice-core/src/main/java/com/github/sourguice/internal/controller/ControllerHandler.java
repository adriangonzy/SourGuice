package com.github.sourguice.internal.controller;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.regex.MatchResult;

import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;

import com.github.sourguice.MvcServletModule;
import com.github.sourguice.annotation.controller.Callable;
import com.github.sourguice.annotation.controller.RenderWith;
import com.github.sourguice.annotation.controller.ViewDirectory;
import com.github.sourguice.annotation.request.RequestMapping;
import com.github.sourguice.annotation.request.View;
import com.github.sourguice.utils.Annotations;
import com.github.sourguice.view.ViewRenderer;

/**
 * Handles a controller class.
 * A controller class can be any class that is declared in {@link MvcServletModule} configureControllers method
 * using the syntax control(pattern).with(controller.class)
 * This class is responsible for creating and managing all possible invocations for the given class
 *   (which are all methods annotated with @{@link RequestMapping})
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 * @param <T> The controller class to handle
 */
public final class ControllerHandler<T> {
	/**
	 * The Class object of the controller class to handle
	 */
	Class<T> clazz;
	
	/**
	 * List of available invocations for this controller
	 */
	private ArrayList<MvcInvocation> invocations = new ArrayList<MvcInvocation>();
	
	/**
	 * Information of a specific invocation
	 * Used to compare different invocation and to carry with the invocation, request matching intels
	 */
	public static class InvocationInfos {
		/**
		 * The concerned invocation
		 */
		public MvcInvocation invocation;

		/**
		 * Specialization indice for this method to match the request
		 * The higher it is, the more it is specialized for the request
		 */
		public int confidence = 0;
		
		/**
		 * The match result after parsing the request URL
		 */
		public @CheckForNull MatchResult urlMatch = null;
		
		/**
		 * The view directory, declared directly on the controller using {@link ViewDirectory}
		 */
		public @CheckForNull String viewDirectory = null;
		
		/**
		 * The default view delcared on the method using {@link View}
		 */
		public @CheckForNull String defaultView = null;
		
		/**
		 * The view renderer, declared directly on the controller using {@link ViewRenderer}
		 */
		public @CheckForNull Class<? extends ViewRenderer> viewRenderer = null;
		
		/**
		 * @param invocation The invocation on which calculates informations
		 */
		public InvocationInfos(MvcInvocation invocation) {
			this.invocation = invocation;
		}

		/**
		 * Compare a given InvocationInfos to this
		 * 
		 * @param infos The InvocationInfos to compare to this
		 * @return Wheter this invocation is better than the one given
		 */
		public boolean isBetterThan(@CheckForNull InvocationInfos infos) {
			if (infos == null)
				return true;
			if (this.urlMatch != null && infos.urlMatch != null) {
				if (this.urlMatch.groupCount() < infos.urlMatch.groupCount())
					return true;
				else if (this.urlMatch.groupCount() > infos.urlMatch.groupCount())
					return false;
			}
			
			return this.confidence > infos.confidence;
		}
		
		/**
		 * Calculates the best invocation between two given invocations
		 * 
		 * @param left The first invocation to compare
		 * @param right The second invocation to compare
		 * @return The best invocation between both given (nulls are accepted)
		 */
		static public @CheckForNull InvocationInfos GetBest(@CheckForNull InvocationInfos left, @CheckForNull InvocationInfos right) {
			if (left == null)
				return right;
			return left.isBetterThan(right) ? left : right;
		}
	}
	
	/**
	 * @param clazz The controller class to handle
	 */
	public ControllerHandler(Class<T> clazz) {
		this.clazz = clazz;
		
		for (Method method : clazz.getMethods())
			if (Annotations.GetOneTreeRecursive(method, Callable.class) != null)
				invocations.add(new MvcInvocation(Annotations.GetOneRecursive(RequestMapping.class, method.getAnnotations()), clazz, method));
	}
	
	/**
	 * Gets the best invocation of all the invocable methods of this controller for this request
	 * 
	 * @param req The request to get invocation for
	 * @return All infos opf the best invocation
	 */
	public @CheckForNull InvocationInfos getBestInvocation(HttpServletRequest req) {
		// Get the best invocation for the given request
		InvocationInfos infos = null;
		for (MvcInvocation invocation : invocations)
			infos = InvocationInfos.GetBest(infos, invocation.canServe(req));

		// If found (not null) gather invocation informations from annotations
		if (infos != null) {
			ViewDirectory vdAnno = Annotations.GetOneTreeRecursive(clazz, ViewDirectory.class);
			if (vdAnno != null)
				infos.viewDirectory = vdAnno.value();

			RenderWith rwAnno = Annotations.GetOneTreeRecursive(infos.invocation.getMethod(), RenderWith.class);
			if (rwAnno != null)
				infos.viewRenderer = rwAnno.value();

			View vAnno = Annotations.GetOneTreeRecursive(infos.invocation.getMethod(), View.class);
			if (vAnno != null)
				infos.defaultView = vAnno.value();
		}
		return infos;
	}

	/**
	 * @return All invocations that were found on this controller class
	 */
	public ArrayList<MvcInvocation> getInvocations() {
		return invocations;
	}
}
