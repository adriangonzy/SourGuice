package com.github.sourguice.ws.jsontrans;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import com.github.sourguice.utils.Annotations;
import com.github.sourguice.ws.annotation.WSCheckForNull;
import com.github.sourguice.ws.annotation.WSDisregardParent;
import com.github.sourguice.ws.annotation.WSDisregardedParent;
import com.github.sourguice.ws.annotation.WSExclude;
import com.github.sourguice.ws.annotation.WSFieldName;
import com.github.sourguice.ws.annotation.WSSince;
import com.github.sourguice.ws.annotation.WSStrict;
import com.github.sourguice.ws.annotation.WSUntil;
import com.github.sourguice.ws.exception.UnknownClientTypeException;
import com.github.sourguice.ws.translat.StrictValue;
import com.github.sourguice.ws.translat.WSTranslaterFactory;
import com.googlecode.gentyref.GenericTypeReflector;

public class SourJsonTransformer {

	@SuppressWarnings("serial")
	public static class JsonTransformException extends Exception {
		public JsonTransformException(String message) {
			super(message);
		}
		public JsonTransformException(Exception exc) {
			super(exc);
		}
	}
	
	public static interface NoJsonType {}
	
	private @CheckForNull Collection<Class<?>> knownClasses;
	
	@SuppressWarnings("serial")
	private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS = new HashMap<Class<?>, Class<?>>() {
		{
	      this.put(boolean.class, Boolean.class);
	      this.put(byte.class, Byte.class);
	      this.put(char.class, Character.class);
	      this.put(double.class, Double.class);
	      this.put(float.class, Float.class);
	      this.put(int.class, Integer.class);
	      this.put(long.class, Long.class);
	      this.put(short.class, Short.class);
	      this.put(void.class, Void.class);
		}
	};

	private Collection<WSTranslaterFactory<?, ?>> translaters;
	
	public SourJsonTransformer(Collection<WSTranslaterFactory<?, ?>> translaters, @CheckForNull Collection<Class<?>> knownClasses) {
		this.translaters = translaters;
		this.knownClasses = knownClasses;
	}
	
	private boolean isSystem(Class<?> cls) {
		Package objectPackage = cls.getPackage();
		String objectPackageName = objectPackage != null ? objectPackage.getName() : "";
		return objectPackageName.startsWith("java.") || objectPackageName.startsWith("javax.") || cls.getClassLoader() == null;
	}

	static boolean IsJSONPrintable(Field field) {
		return	!field.isSynthetic()
			&&	!field.isAnnotationPresent(WSExclude.class)
			&&	!Modifier.isStatic(field.getModifiers())
			&&	!Modifier.isTransient(field.getModifiers())
		;
	}
	
	private String getFieldName(Field field) {
		WSFieldName wsFieldName = field.getAnnotation(WSFieldName.class);
		if (wsFieldName != null)
			return wsFieldName.value();
		return field.getName();
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private @CheckForNull StrictValue transformToWS(Object obj, Type type, AnnotatedElement el, @CheckForNull Object enclosing) {
		for (WSTranslaterFactory fact : translaters)
			if (fact.getServerClass().isAssignableFrom(GenericTypeReflector.erase(type))) {
				Object ret = fact.getTranslater().toWS(obj, type, el, enclosing);
				if (ret == null)
					return null;
				if (ret instanceof StrictValue)
					return (StrictValue)ret;
				return new StrictValue(ret.getClass(), ret);
			}
		return new StrictValue(type, obj);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private @CheckForNull Object transformFromWS(Object obj, Type typeOnServer, AnnotatedElement el, @CheckForNull Object enclosing) {
		for (WSTranslaterFactory fact : translaters)
			if (fact.getServerClass().isAssignableFrom(GenericTypeReflector.erase(typeOnServer)))
				return fact.getTranslater().fromWS(obj, typeOnServer, el, enclosing);
		return obj;
	}

	private @CheckForNull Type getParent(Class<?> cls) {
		if (cls.getAnnotation(WSDisregardParent.class) != null)
			return null;

		Class<?> superClass = cls.getSuperclass();
		if (superClass == null)
			return null;

		if (superClass.getAnnotation(WSDisregardedParent.class) != null)
			return null;
		
		if (isSystem(superClass))
			return null;
		
		return cls.getGenericSuperclass();
	}
	
	@SuppressWarnings("unchecked")
	public @CheckForNull Object toJSON(@CheckForNull Object from, Type fromType, double version, AnnotatedElement fromAnno, boolean allowEmpty, @CheckForNull Object enclosing) throws JsonTransformException {

		if (from == null || Void.class.isAssignableFrom(GenericTypeReflector.erase(fromType)))
			return null;

		StrictValue transformed = transformToWS(from, fromType, fromAnno, enclosing);
		if (transformed == null)
			return null;

		from = transformed.getValue();
		fromType = transformed.getType();
		Class<?> fromClass = GenericTypeReflector.erase(fromType);

		if (	   from instanceof JSONObject   || from instanceof JSONArray
				|| from instanceof Number	    || from instanceof Boolean
				|| from instanceof JSONAware    || from instanceof JSONStreamAware
				|| from instanceof String
				) {
			return from;
		}
		
		if (Collection.class.isAssignableFrom(fromClass)) {
			if (!allowEmpty && ((Collection<?>)from).isEmpty())
				return null;
			Type colType = GenericTypeReflector.getTypeParameter(fromType, Collection.class.getTypeParameters()[0]);
			JSONArray array = new JSONArray();
			Iterator<?> it = ((Collection<?>)from).iterator();
			while (it.hasNext()) {
				Object value = it.next();
				if (value != null) {
					Type valueType = fromAnno.isAnnotationPresent(WSStrict.class) ? colType : value.getClass();
					Object json = toJSON(value, valueType, version, fromAnno, false, from);
					array.add(json);
				}
			}
			return array;
		}

		if (fromClass.isArray()) {
			JSONArray array = new JSONArray();
			int length = Array.getLength(from);
			if (!allowEmpty && length == 0)
				return null;
			Type arrayComponentType = GenericTypeReflector.getArrayComponentType(fromType);
			for (int i = 0; i < length; ++i) {
				Object element = Array.get(from, i);
				Type elementType = fromAnno.isAnnotationPresent(WSStrict.class) ? arrayComponentType : element.getClass();
				Object json = toJSON(element, elementType, version, fromAnno, false, from);
				if (json != null)
					array.add(json);
			}
			return array;
		}

		if (Map.class.isAssignableFrom(fromClass)) {
			if (!allowEmpty && ((Map<?, ?>)from).isEmpty())
				return null;
			Type mapType = GenericTypeReflector.getTypeParameter(fromType, Map.class.getTypeParameters()[1]);
			JSONObject obj = new JSONObject();
			Iterator<?> it = ((Map<?, ?>)from).entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<?, ?> e = (Map.Entry<?, ?>) it.next();
				Object value = e.getValue();
				if (value != null) {
					Type valueType = fromAnno.isAnnotationPresent(WSStrict.class) ? mapType : value.getClass();
					Object json = toJSON(value, valueType, version, fromAnno, false, from);
					if (json != null)
						obj.put(e.getKey().toString(), json);
				}
			}
			return obj;
		}
		
		if (fromClass.isEnum())
			return from.toString();

		if (isSystem(fromClass))
			return from.toString();

		if (knownClasses != null && !knownClasses.contains(fromClass))
			throw new UnknownClientTypeException(fromClass);

		JSONObject object = new JSONObject();

		Type typeOnServer = fromType;

		// TODO: Should be Cached
		while (fromType != null) {
			fromClass = GenericTypeReflector.erase(fromType);

			for (Field field : fromClass.getDeclaredFields()) {
				if (!IsJSONPrintable(field))
					continue ;

				WSUntil until = field.getAnnotation(WSUntil.class);
				if (until != null && until.value() > version)
					continue ;

				WSSince since = field.getAnnotation(WSSince.class);
				if (since != null && since.value() < version)
					continue ;

				field.setAccessible(true);
				try {
					Object fieldValue = field.get(from);
					if (fieldValue == null)
						continue ;

					Type fieldType = fieldValue.getClass();
					Class<?> fieldClass = GenericTypeReflector.erase(fieldType);
					if (Map.class.isAssignableFrom(fieldClass) || Collection.class.isAssignableFrom(fieldClass) || Annotations.GetOneTree(field, WSStrict.class) != null)
						fieldType = GenericTypeReflector.getExactFieldType(field, fromType);

					Object json = toJSON(fieldValue, fieldType, version, field, false, from);
					if (json != null)
						object.put(getFieldName(field), json);
				}
				catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
				finally {
					field.setAccessible(false);
				}
			}
			
			fromType = getParent(fromClass);
		}

		if (!allowEmpty && object.isEmpty())
			return null;
		
		if (!(from instanceof NoJsonType))
			object.put("!type", GenericTypeReflector.getTypeName(typeOnServer));
		
		return object;
	}
	
	private AnnotatedElement empty = Annotations.fromArray(new Annotation[0]);
	
	public @CheckForNull Object toJSON(Object from, Type fromType, double version, boolean allowEmpty) throws JsonTransformException {
		return toJSON(from, fromType, version, empty, allowEmpty, null);
	}

	public @CheckForNull Object toJSON(Object from, Type fromType, double version) throws JsonTransformException {
		return toJSON(from, fromType, version, empty, true, null);
	}

	private Object construct(Class<?> cls) throws JsonTransformException {
		try {
			Constructor<?> constructor = cls.getDeclaredConstructor();
			constructor.setAccessible(true);
			try {
				return constructor.newInstance();
			}
			finally {
				constructor.setAccessible(false);
			}
		}
		catch (Exception e) {
			throw new JsonTransformException("Cannot construct " + cls);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public @CheckForNull <T> T fromJSON(@CheckForNull Object from, Type toType, double version, AnnotatedElement toAnno, @CheckForNull Object enclosing) throws JsonTransformException {
		if (from == null)
			return null;
		
		Class<?> toClass = GenericTypeReflector.erase(toType);
		
		if (from instanceof JSONArray) {
			JSONArray fromArray = (JSONArray)from;
			
			if (!Collection.class.isAssignableFrom(toClass) && !toClass.isArray())
				throw new JsonTransformException("Can only unserialize an array into a Collection, not " + toClass);
			Collection<Object> col;
			if (toClass.isArray())
				col = new ArrayList<Object>();
			else if (List.class.equals(toClass))
				col = new LinkedList<Object>();
			else if (Set.class.equals(toClass))
				col = new HashSet<Object>();
			else
				col = (Collection<Object>)construct(toClass);
			
			Type colToType;
			if (toClass.isArray())
				colToType = GenericTypeReflector.getArrayComponentType(toType);
			else
				colToType = GenericTypeReflector.getTypeParameter(toType, Collection.class.getTypeParameters()[0]);
			
			for (Object item : fromArray)
				col.add(fromJSON(item, colToType, version, toAnno, col));
			
			Object ret = col;
			
			if (toClass.isArray())
				ret = col.toArray((Object[])Array.newInstance(toClass.getComponentType(), col.size()));
			
			return (T)transformFromWS(ret, toType, toAnno, enclosing);
			
		}
		if (from instanceof JSONObject) {
			JSONObject fromObject = (JSONObject)from;

			if (Map.class.isAssignableFrom(toClass)) {
				Map<String, Object> map; 
				if (Map.class.equals(toClass))
					map = new HashMap<String, Object>();
				else
					map = (Map<String, Object>)construct(toClass);
				
				Type mapToType = GenericTypeReflector.getTypeParameter(toType, Map.class.getTypeParameters()[1]);
				
				for (Entry<Object, Object> entry : (Set<Map.Entry<Object, Object>>)fromObject.entrySet())
					map.put(String.valueOf(entry.getKey()), fromJSON(entry.getValue(), mapToType, version, toAnno, map));

				return (T)transformFromWS(map, toType, toAnno, enclosing);
			}

			Type typeOnServer = toType;

			if (fromObject.containsKey("!type"))
				try {
					toType = Class.forName(String.valueOf(fromObject.get("!type")));
					toClass = (Class<?>)toType;
				}
				catch (ClassNotFoundException e1) {
					throw new JsonTransformException("Could not find " + String.valueOf(fromObject.get("!type")));
				}

			T ret = (T)construct(toClass);

			// TODO: Should be Cached
			while (toType != null) {
				toClass = GenericTypeReflector.erase(toType);

				for (Field field : toClass.getDeclaredFields()) {
					if (!IsJSONPrintable(field))
						continue ;

					String fieldName = getFieldName(field);
					
					if (!fromObject.containsKey(fieldName) || fromObject.get(fieldName) == null) {
						WSUntil until = field.getAnnotation(WSUntil.class);
						if (until != null && until.value() > version)
							continue ;

						WSSince since = field.getAnnotation(WSSince.class);
						if (since != null && since.value() < version)
							continue ;

						if (field.isAnnotationPresent(WSCheckForNull.class))
							continue ;

						throw new JsonTransformException("Missing from JSON : " + field);
					}
					
					Object json = fromJSON(fromObject.get(fieldName), GenericTypeReflector.getExactFieldType(field, toType), version, field, ret);
					field.setAccessible(true);
					try {
						field.set(ret, json);
					}
					catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					}
					finally {
						field.setAccessible(false);
					}
				}
				
				toType = getParent(toClass);
			}
			
			return (T)transformFromWS(ret, typeOnServer, toAnno, enclosing);
		}
		else if (toClass.isEnum()) {
			return (T)Enum.valueOf((Class<? extends Enum>)toClass, from.toString());
		}
		else {
			Object ret = transformFromWS(from, toType, toAnno, enclosing);
			
			if (ret != null) {
				
				if (toClass.isPrimitive())
					toClass = PRIMITIVES_TO_WRAPPERS.get(toClass);				
				if (!toClass.isAssignableFrom(ret.getClass())) {
					if (Number.class.isAssignableFrom(ret.getClass()) && Number.class.isAssignableFrom(toClass))
						try {
							ret = toClass.getConstructor(String.class).newInstance(ret.toString());
						}
						catch (ReflectiveOperationException e) {
							throw new JsonTransformException(e);
						}
					else
						throw new JsonTransformException("Cannot transform " + from.getClass() + " to " + toClass);
				}
			}
			return (T)ret;
		}
	}

}
