package com.github.sourguice.conversion.def;

import javax.annotation.CheckForNull;

import com.github.sourguice.annotation.ConvertsTo;
import com.github.sourguice.conversion.Converter;

/**
 * String converter that does nothing and simply returns the string
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@ConvertsTo(String[].class)
public class StringConverter implements Converter<String> {

	/**
	 * {@inheritDoc}
	 */
	@Override @CheckForNull public String get(Class<? extends String> clazz, String arg) {
		return arg;
	}
}
