package com.github.sourguice.conversion.def;

import java.util.regex.Pattern;

import javax.annotation.CheckForNull;

import com.github.sourguice.annotation.ConvertsTo;
import com.github.sourguice.conversion.Converter;

/**
 * Converts a String to Boolean and boolean
 * Return true when equals "true", "Y" or any non-zero number
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@ConvertsTo({Boolean[].class, boolean[].class})
public class BooleanConverter implements Converter<Boolean> {

	/**
	 * Pattern of a floating point number
	 */
	static private final Pattern number = Pattern.compile("[0-9]*\\.?[0-9]+");
	
	/**
	 * Pattern of a zero
	 */
	static private final Pattern zero = Pattern.compile("0?\\.?0+");

	/**
	 * {@inheritDoc}
	 */
	@Override
	public @CheckForNull Boolean get(Class<? extends Boolean> clazz, String arg) {
		if (arg.equalsIgnoreCase("true") || arg.equalsIgnoreCase("on") || arg.equalsIgnoreCase("Y") || arg.equalsIgnoreCase("yes") || (number.matcher(arg).matches() && !zero.matcher(arg).matches()))
			return new Boolean(true);
		return new Boolean(false);
	}
}
