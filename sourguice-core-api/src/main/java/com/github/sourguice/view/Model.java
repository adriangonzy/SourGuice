package com.github.sourguice.view;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckForNull;

/**
 * Key / Values passed to a view
 * This basically encapsulates a map
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public final class Model {
	
	/**
	 * The key / value pairs
	 */
	HashMap<String, Object> map = new HashMap<String, Object>();
	
	/**
	 * Adds an attribute and its value to the model
	 * 
	 * @param attributeName The name of the attribute to add
	 * @param attributeValue The value of the attribute to add
	 * @return itself to permit command chain
	 */
	public Model addAttribute(String attributeName, @CheckForNull Object attributeValue) {
		map.put(attributeName, attributeValue);
		return this;
	}
	
	/**
	 * Alias to {@link Model#addAttribute(String, Object)}
	 * 
	 * @param name The name of the attribute to add
	 * @param value The value of the attribute to add
	 * @return itself to permit command chain
	 */
	public Model put(String name, @CheckForNull Object value) {
		return this.addAttribute(name, value);
	}

	/**
	 * Adds all attributes of a given map to the model, erasing existing attributes with new ones
	 * 
	 * @param attributes The attributes to add
	 * @return itself to permit command chain
	 */
	public Model addAllAttributes(Map<String, ?> attributes) {
		map.putAll(attributes);
		return this;
	}
	
	/**
	 * Adds all attributes of a given map to the model, keeping old ones in case of duplicates
	 * 
	 * @param attributes The attributes to merge
	 * @return itself to permit command chain
	 */
	public Model mergeAttributes(Map<String, ?> attributes) {
		for (String key : attributes.keySet())
			if (!map.containsKey(key))
				map.put(key, attributes.get(key));
		return this;
	}

	/**
	 * @return The key / value pairs of this model as a map
	 */
	public Map<String, Object> asMap() {
		return map;
	}
}
