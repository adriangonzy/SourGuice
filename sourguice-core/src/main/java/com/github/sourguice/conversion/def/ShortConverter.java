package com.github.sourguice.conversion.def;

import javax.annotation.CheckForNull;

import com.github.sourguice.annotation.ConvertsTo;
import com.github.sourguice.conversion.Converter;

/**
 * Converts a String to Short and short
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@ConvertsTo({Short[].class, short[].class})
public class ShortConverter implements Converter<Short> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public @CheckForNull Short get(Class<? extends Short> clazz, String arg) {
		try {
			return Short.valueOf(arg);
		}
		catch (NumberFormatException e) {
			if (clazz.isPrimitive())
				return new Short((short)0);
			return null;
		}
	}
}
