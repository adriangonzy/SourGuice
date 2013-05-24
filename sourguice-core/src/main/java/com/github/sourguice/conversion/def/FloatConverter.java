package com.github.sourguice.conversion.def;

import javax.annotation.CheckForNull;

import com.github.sourguice.annotation.ConvertsTo;
import com.github.sourguice.conversion.Converter;

/**
 * Converts a String to Float and float
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@ConvertsTo({Float[].class, float[].class})
public class FloatConverter implements Converter<Float> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public @CheckForNull Float get(Class<? extends Float> clazz, String arg) {
		try {
			return Float.valueOf(arg);
		}
		catch (NumberFormatException e) {
			if (clazz.isPrimitive())
				return Float.valueOf(0);
			return null;
		}
	}
}
