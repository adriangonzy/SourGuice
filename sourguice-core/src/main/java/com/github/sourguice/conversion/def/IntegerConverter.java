/**
 * Defaults converters
 */
package com.github.sourguice.conversion.def;

import javax.annotation.CheckForNull;

import com.github.sourguice.annotation.ConvertsTo;
import com.github.sourguice.conversion.Converter;

/**
 * Converts a String to Integer and int
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@ConvertsTo({Integer[].class, int[].class})
public class IntegerConverter implements Converter<Integer> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public @CheckForNull Integer get(Class<? extends Integer> clazz, String arg) {
		try {
			return Integer.valueOf(arg);
		}
		catch (NumberFormatException e) {
			if (clazz.isPrimitive())
				return new Integer(0);
			return null;
		}
	}
}
