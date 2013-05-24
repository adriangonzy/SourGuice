package com.github.sourguice.utils;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;

/**
 * Arrays related utils
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public final class Arrays {

	/**
	 * Checks if a value is contained in an array
	 * @param array The array to search in
	 * @param value The value to search for
	 * @return Whether or not the value was found in the array
	 */
	public static <T> boolean Contains(T[] array, T value) {
		for (T o : array)
			if (o.equals(value))
				return true;

		return false;
	}

	/**
	 * This will check recursively into an array of A for an F.
	 * This is only useful in case of a recursive search
	 * 
	 * @see Finder How to use this
	 * @see Annotations#GetOneAnnotated(Class, java.lang.annotation.Annotation[]) An exemple of utilisation
	 * 
	 * @param <F> The type of object to look for
	 */
	public static class Getter<F> {

		/**
		 * Already checked objects
		 */
		private Set<Object> checked = new HashSet<Object>();

		/**
		 * @param <F> The type of object to look for
		 * @param <A> The type of object in the array
		 */
		public static abstract class Finder<F, A> {
			/**
			 * Returns an F if one was found inside this A.
			 * For a recursive search, this should call {@link Getter#get(Object[], Finder)} with this as a second argument
			 *  for each sub-A.
			 *  
			 * @param obj The object in which to find a F
			 * @return the F found or null
			 */
			public abstract @CheckForNull F findIn(A obj);
			
			/**
			 * Returns an object that will prevent this A from being re-searched again if another A gives the same object
			 * Trivial implementations will: return this;
			 * 
			 * @param obj The object that is curently being look at
			 * @return An object that will identify this object
			 */
			protected Object getCheck(A obj) { return obj; }
		}
		
		/**
		 * @see Getter
		 * 
		 * @param array The array to search in
		 * @param finder A Getter to find the F
		 * @return The F if found or null
		 */
		public @CheckForNull <A> F get(A[] array, Getter.Finder<F, A> finder) {
			for (A obj : array) {
				Object check = finder.getCheck(obj);
				if (!checked.contains(check)) {
					checked.add(check);
					F f = finder.findIn(obj);
					if (f != null)
						return f;
				}
			}
			return null;
		}
	}

	/**
	 * Same as {@link Getter}, except it will return a list of F founds
	 * 
	 * @see Adder How to use this
	 * @see Annotations#GetAllAnnotated(Class, java.lang.annotation.Annotation[]) An exemple of utilisation
	 * 
	 * @param <F> The type of object to look for
	 */
	public static class AllGetter<F> {

		/**
		 * List of F found
		 */
		private List<F> found = new LinkedList<F>();
		
		/**
		 * Already checked objects
		 */
		private Set<Object> checked = new HashSet<Object>();

		/**
		 * @param <F> The type of object to look for
		 * @param <A> The type of object in the array
		 */
		public static abstract class Adder<F, A> {
			/**
			 * Adds all F found inside the list.
			 * For a recursive search, this should call {@link AllGetter#find(Object[], Adder)} with this as a second argument
			 *  for each sub-A.
			 * 
			 * @param list The list on which to add all found Fs
			 * @param obj The object in which to find all Fs
			 */
			public abstract void addIn(List<F> list, A obj);
			
			/**
			 * Returns an object that will prevent this A from being re-searched again if another A gives the same object
			 * Trivial implementations will: return this;
			 * 
			 * @param obj The object that is curently being look at
			 * @return An object that will identify this object
			 */
			protected Object getCheck(A obj) { return obj; }
		}

		/**
		 * @see AllGetter
		 * 
		 * @param array The array to search in
		 * @param adder An adder to find all the Fs
		 */
		public <A> void find(A[] array, AllGetter.Adder<F, A> adder) {
			for (A obj : array) {
				Object check = adder.getCheck(obj);
				if (!checked.contains(check)) {
					checked.add(check);
					adder.addIn(found, obj);
				}
			}
		}
		
		/**
		 * @return the list of found Fs
		 */
		public List<F> found() {
			return found;
		}
	}
}