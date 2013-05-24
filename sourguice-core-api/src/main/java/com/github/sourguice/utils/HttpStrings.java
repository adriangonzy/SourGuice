package com.github.sourguice.utils;


/**
 * HTTP related utils
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public final class HttpStrings {
	
	/**
	 * Checks if the given accept header matches one of the given values
	 * 
	 * @param acceptHeader The accept HTTP header string
	 * @param types An array of acceptable type string to compare to
	 * @return Whether or not the acceptHeader string matches one of the given types 
	 */
	public static boolean AcceptContains(String acceptHeader, String[] types) {
		String[] accepts = acceptHeader.split(",");
		
		for (String accept : accepts) {
			if (accept.contains(";"))
				accept = accept.substring(0, accept.indexOf(';'));

			// TODO: Should evaluate for things like text/* or even */*
			if (Arrays.Contains(types, accept.trim()))
				return true;
		}
		return false;
	}
}