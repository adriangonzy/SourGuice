package com.github.sourguice.conversion.def;

import javax.annotation.CheckForNull;

import com.github.sourguice.annotation.ConvertsTo;
import com.github.sourguice.conversion.Converter;

/**
 * Converts a String to Double and double
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@ConvertsTo({Double[].class, double[].class})
public class DoubleConverter implements Converter<Double> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public @CheckForNull Double get(Class<? extends Double> clazz, String arg) {
		try {
			return Double.valueOf(arg);
		}
		catch (NumberFormatException e) {
			if (clazz.isPrimitive())
				return Double.valueOf(0);
			return null;
		}
	}
}
