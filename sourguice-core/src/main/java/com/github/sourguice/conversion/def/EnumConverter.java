package com.github.sourguice.conversion.def;

import javax.annotation.CheckForNull;

import com.github.sourguice.annotation.ConvertsTo;
import com.github.sourguice.conversion.Converter;

/**
 * Converts a String to an Enum value
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@SuppressWarnings("rawtypes")
@ConvertsTo(Enum[].class)
public class EnumConverter implements Converter<Enum> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public @CheckForNull Enum get(Class<? extends Enum> clazz, String arg) {
		try {
			return Enum.valueOf(clazz, arg);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}
}
