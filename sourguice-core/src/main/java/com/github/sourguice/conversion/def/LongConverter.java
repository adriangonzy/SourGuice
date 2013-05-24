package com.github.sourguice.conversion.def;

import javax.annotation.CheckForNull;

import com.github.sourguice.annotation.ConvertsTo;
import com.github.sourguice.conversion.Converter;

/**
 * Converts a String to Long and long
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@ConvertsTo({Long[].class, long[].class})
public class LongConverter implements Converter<Long> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public @CheckForNull Long get(Class<? extends Long> clazz, String arg) {
		try {
			return Long.valueOf(arg);
		}
		catch (NumberFormatException e) {
			if (clazz.isPrimitive())
				return Long.valueOf(0);
			return null;
		}
	}
}
