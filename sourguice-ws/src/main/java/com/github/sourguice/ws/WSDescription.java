package com.github.sourguice.ws;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;

import com.github.sourguice.utils.Annotations;
import com.github.sourguice.value.ValueConstants;
import com.github.sourguice.ws.annotation.WSCheckForNull;
import com.github.sourguice.ws.annotation.WSConstant;
import com.github.sourguice.ws.annotation.WSDisregardParent;
import com.github.sourguice.ws.annotation.WSDisregardedParent;
import com.github.sourguice.ws.annotation.WSDoc;
import com.github.sourguice.ws.annotation.WSException;
import com.github.sourguice.ws.annotation.WSExclude;
import com.github.sourguice.ws.annotation.WSFieldName;
import com.github.sourguice.ws.annotation.WSInfos;
import com.github.sourguice.ws.annotation.WSMethod;
import com.github.sourguice.ws.annotation.WSParam;
import com.github.sourguice.ws.annotation.WSSince;
import com.github.sourguice.ws.annotation.WSSubclasses;
import com.github.sourguice.ws.annotation.WSUntil;
import com.github.sourguice.ws.jsontrans.SourJsonTransformer.NoJsonType;
import com.github.sourguice.ws.translat.WSTranslaterFactory;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.googlecode.gentyref.GenericTypeReflector;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public final class WSDescription implements NoJsonType {
	
	public String packageName;
	public String name;
	public double defaultVersion;
	public @CheckForNull String baseUrl;
	
	public Map<String, Object> plugins = new HashMap<>();
	
	public Map<String, WSDClass> objectTypes = new HashMap<>();
	public Map<String, WSDEnum> enumTypes = new HashMap<>();
	public Map<String, WSDConstant> constants = new HashMap<>();
	public Map<String, WSDMethod> methods = new HashMap<>();

	private transient Map<Class<?>, Integer> objectFieldsPasses = new HashMap<>();
	
	public transient Map<Class<?>, WSTranslaterFactory<?, ?>> translaters = new HashMap<>();
	public transient Set<Class<?>> knownClasses = new HashSet<>();
	
	private transient JsonWSController controller;
	
	public static abstract class Versioned implements NoJsonType {
		public @CheckForNull Double since;
		public @CheckForNull Double until;
		public @CheckForNull String[] doc;
		public Map<String, Object> plugins = new HashMap<>();

		@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "GSON")
		private Versioned() {}

		public Versioned(@CheckForNull AnnotatedElement el) {
			if (el == null)
				return ;
			WSSince elSince = el.getAnnotation(WSSince.class);
			if (elSince != null)
				this.since = Double.valueOf(elSince.value());
			WSUntil elUntil = el.getAnnotation(WSUntil.class);
			if (elUntil != null)
				this.until = Double.valueOf(elUntil.value());
			WSDoc elDoc = el.getAnnotation(WSDoc.class);
			if (elDoc != null)
				this.doc = elDoc.value();
		}
	}
	
	private static String getFieldName(Field f) {
		String fieldName = f.getName();
		WSFieldName wsFieldName = f.getAnnotation(WSFieldName.class);
		if (wsFieldName != null)
			fieldName = wsFieldName.value();
		return fieldName;
	}
	
	@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "GSON")
	private WSDescription() {}
	
	public WSDescription(Class<?> cls, Injector injector, JsonWSController controller) {
		this.name = cls.getName();
		this.packageName = cls.getPackage().getName();
		this.defaultVersion = 1.0;
		
		this.controller = controller;

		WSInfos infos = cls.getAnnotation(WSInfos.class);
		
		if (infos != null) {
			if (!infos.name().equals(ValueConstants.DEFAULT_NONE)) {
				if (infos.name() != null && ! Pattern.matches("[a-zA-Z_][a-zA-Z0-9_]*", infos.name()))
					throw new AssertionError("WebServices name must be a regular identifier");
				this.name = infos.name();
			}
			if (infos.defaultVersion() < 0)
				throw new AssertionError("WebServices default version cannot be negative");
			this.defaultVersion = infos.defaultVersion();
		}
		
		Class<?> look = cls;
		while (look != null) {
			WSInfos lookInfos = look.getAnnotation(WSInfos.class);
			if (lookInfos != null) {
				for (Class<? extends WSTranslaterFactory<?, ?>> factClass : lookInfos.translaters()) {
					if (factClass.getAnnotation(Singleton.class) == null && factClass.getAnnotation(javax.inject.Singleton.class) == null)
						throw new AssertionError(factClass + " MUST be annotated with @Singleton");
					WSTranslaterFactory<?, ?> factory = injector.getInstance(factClass);
					translaters.put(factory.getServerClass(), factory);
				}
				
				for (Class<?> type : lookInfos.additionalClasses())
					new WSDTypeReference(type, null, null);
			}
			
			for (Field f : look.getDeclaredFields()) {
				if (f.getAnnotation(WSExclude.class) != null)
					continue ;
				if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()) && f.getAnnotation(WSConstant.class) != null)
					constants.put(getFieldName(f), new WSDConstant(f));
			}
			
			look = look.getSuperclass();
		}
		
		for (Method m : cls.getMethods()) {
			WSMethod a = Annotations.GetOneRecursive(WSMethod.class, m.getAnnotations());
			if (a != null)
				new WSDMethod(cls, m, a);
		}
	}
	
	public final class WSDClass extends Versioned {
		public @CheckForNull String parent = null;
		public @CheckForNull Boolean isAbstract;
		public Map<String, WSDTypeReference> properties = new HashMap<>();
		@SuppressWarnings("hiding")
		public Map<String, WSDConstant> constants = new HashMap<>();
		
		@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "GSON")
		private WSDClass() {}

		public WSDClass(Type type, String typeName) {
			super(GenericTypeReflector.erase(type));
			Class<?> cls = GenericTypeReflector.erase(type);
			if (cls.getTypeParameters().length > 0)
				throw new RuntimeException(cls + " is generic, genericity is not yet supported.");
			knownClasses.add(cls);
			objectTypes.put(typeName, this);
			if (Modifier.isAbstract(cls.getModifiers()))
				this.isAbstract = Boolean.TRUE;
			if (cls.getSuperclass() != null) {
				if (cls.getSuperclass().equals(Exception.class) || cls.getSuperclass().equals(Throwable.class) || cls.getSuperclass().equals(Error.class) || cls.getSuperclass().equals(RuntimeException.class))
					this.parent = "!" + cls.getSuperclass().getName();
				else if (cls.getAnnotation(WSDisregardParent.class) == null && cls.getSuperclass().getAnnotation(WSDisregardedParent.class) == null && !cls.getSuperclass().equals(Object.class))
					this.parent = new WSDTypeReference(cls.getSuperclass(), null, null).ref;
			}
			for (Field f : cls.getDeclaredFields()) {
				if (f.getAnnotation(WSExclude.class) != null)
					continue ;
				String fieldName = getFieldName(f);

				if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()) && f.getAnnotation(WSConstant.class) != null)
					constants.put(fieldName, new WSDConstant(f));
				else if (!f.isSynthetic() && !Modifier.isTransient(f.getModifiers()) && !Modifier.isStatic(f.getModifiers())) {
					WSDTypeReference typeRef = new WSDTypeReference(GenericTypeReflector.getExactFieldType(f, type), f, f);
					if (f.getAnnotation(WSCheckForNull.class) != null)
						typeRef.nullable = Boolean.TRUE;
					controller.pluginField(typeRef, f);
					properties.put(fieldName, typeRef);
				}
			}
			WSSubclasses wsPolymorphic = cls.getAnnotation(WSSubclasses.class);
			if (wsPolymorphic != null)
				for (Class<?> subCls : wsPolymorphic.value())
					new WSDTypeReference(subCls, null, null);
			controller.pluginClass(this, type);
		}
	}

	public final class WSDConstant extends Versioned {
		public WSDType type;
		public Object value;
		public WSDConstant(Field f) {
			super(f);
			Class<?> cls = f.getType();
			if (cls.equals(boolean.class) || cls.equals(Boolean.class))
				this.type = WSDType.BOOLEAN_P;
			else if (cls.equals(float.class) || cls.equals(double.class) || Float.class.isAssignableFrom(cls) || Double.class.isAssignableFrom(cls))
				this.type = WSDType.FLOAT_P;
			else if (cls.isPrimitive() || Number.class.isAssignableFrom(cls))
				this.type = WSDType.INT_P;
			else if (cls.equals(String.class))
				this.type = WSDType.STRING;
			else
				throw new UnsupportedOperationException("Unsupported WSConstant: " + f);

			f.setAccessible(true);
			try {
				this.value = f.get(null);
			}
			catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			f.setAccessible(false);
		}
	}
	
	public final class WSDEnum extends Versioned {
		public List<String> values = new LinkedList<>();
		
		@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "GSON")
		private WSDEnum() {}

		public WSDEnum(Class<?> cls, String typeName) {
			super(cls);
			for (Object c : cls.getEnumConstants())
				values.add(c.toString());
			
			enumTypes.put(typeName, this);
			controller.pluginEnum(this, cls);
		}
	}

	public final class WSDMethod extends Versioned {
		public @CheckForNull Map<String, WSDMParam> params;
		public WSDTypeReference returns;
		public List<WSDTypeReference> exceptions;
		
		@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "GSON")
		private WSDMethod() {}
		
		public WSDMethod(Class<?> cls, Method m, WSMethod a) {
			super(m);
			String methodName = a.name();
			if (methodName.isEmpty())
				methodName = m.getName();
			methods.put(methodName, this);

			this.returns = new WSDTypeReference(GenericTypeReflector.getExactReturnType(m, cls), null, null);
			
			Type[] paramTypes = GenericTypeReflector.getExactParameterTypes(m, cls);
			Annotation[][] paramAnnos = m.getParameterAnnotations();
			for (int i = 0; i < paramTypes.length; ++i) {
				WSParam wsParam = Annotations.GetOneRecursive(WSParam.class, paramAnnos[i]);
				if (wsParam != null)
					new WSDMParam(wsParam, Annotations.fromArray(paramAnnos[i]), paramTypes[i]);
			}
			
			for (Type exceptionType : m.getGenericExceptionTypes()) {
				if (exceptions == null)
					exceptions = new ArrayList<>();
				if (Annotations.GetOneTreeRecursive(GenericTypeReflector.erase(exceptionType), WSException.class) != null)
					exceptions.add(new WSDTypeReference(exceptionType, null, null));
			}
			controller.pluginMethod(this, m);
		}

		public final class WSDMParam extends Versioned {
			public int position;
			public WSDTypeReference type;
			
			@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "GSON")
			private WSDMParam() {}
			
			public WSDMParam(WSParam wsParam, AnnotatedElement el, Type type) {
				super(el);
				
				this.type = new WSDTypeReference(type, null, null);

				if (el.isAnnotationPresent(WSCheckForNull.class))
					this.type.nullable = Boolean.TRUE;

				if (params == null)
					params = new LinkedHashMap<>();
				this.position = params.size();
				params.put(wsParam.value(), this);
				controller.pluginParam(this, el, type);
			}
		}
		
	}
	
	public enum WSDType { VOID, BOOLEAN_P, BOOLEAN_O, INT_P, INT_O, FLOAT_P, FLOAT_O, STRING, LIST, MAP, ENUM, DATE, OBJECT };

	public final class WSDTypeReference extends Versioned {
		
		public WSDType type;
		public String ref;
		public @CheckForNull WSDTypeReference collectionType;
		public @CheckForNull Boolean nullable;
		
		@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "GSON")
		private WSDTypeReference() {}
		
		public WSDTypeReference(Type type, @CheckForNull AnnotatedElement el, @CheckForNull final Field cause) {
			super(el);
			if (type instanceof WildcardType)
				type = ((WildcardType)type).getUpperBounds()[0];
			Class<?> cls = GenericTypeReflector.erase(type);
			if (translaters.containsKey(cls)) {
				WSTranslaterFactory<?, ?> tf = translaters.get(cls);
				if (!tf.isInternal()) {
					type = tf.getClientType(type, el);
					cls = GenericTypeReflector.erase(type);
				}
			}
			
			if (cls.equals(void.class) || cls.equals(Void.class))
				this.type = WSDType.VOID;

			else if (cls.equals(boolean.class))
				this.type = WSDType.BOOLEAN_P;
			else if (cls.equals(Boolean.class))
				this.type = WSDType.BOOLEAN_O;

			else if (cls.equals(float.class) || cls.equals(double.class))
				this.type = WSDType.FLOAT_P;
			else if (Float.class.isAssignableFrom(cls) || Double.class.isAssignableFrom(cls))
				this.type = WSDType.FLOAT_O;

			else if (cls.isPrimitive())
				this.type = WSDType.INT_P;
			else if (Number.class.isAssignableFrom(cls))
				this.type = WSDType.INT_O;

			else if (cls.equals(String.class))
				this.type = WSDType.STRING;
			
			else if (cls.isEnum()) {
				String typeName = cls.getName();
				
				this.type = WSDType.ENUM;
				this.ref = typeName;
				
				if (!enumTypes.containsKey(typeName))
					new WSDEnum(cls, typeName);
			}
			
			else if (cls.isArray()) {
				this.type = WSDType.LIST;
				this.collectionType = new WSDTypeReference(GenericTypeReflector.getArrayComponentType(type), null, null);
			}

			else if (Collection.class.isAssignableFrom(cls)) {
				this.type = WSDType.LIST;
				ParameterizedType listType = (ParameterizedType)GenericTypeReflector.getExactSuperType(type, Collection.class);
				this.collectionType = new WSDTypeReference(listType.getActualTypeArguments()[0], null, cause);
			}
			
			else if (Map.class.isAssignableFrom(cls)) {
				this.type = WSDType.MAP;
				ParameterizedType mapType = (ParameterizedType)GenericTypeReflector.getExactSuperType(type, Map.class);
				if (!String.class.isAssignableFrom(GenericTypeReflector.erase(mapType.getActualTypeArguments()[0])))
					throw new RuntimeException("Map first generic argument must be a String in " + cause);
				this.collectionType = new WSDTypeReference(mapType.getActualTypeArguments()[1], null, cause);
			}
			
			else if (Date.class.isAssignableFrom(cls)) {
				this.type = WSDType.DATE;
			}

			else {
				int pass = 1;
				if (objectFieldsPasses.containsKey(cls))
					pass = objectFieldsPasses.get(cls).intValue() + 1;
				objectFieldsPasses.put(cls, Integer.valueOf(pass));
				if (pass >= 100)
					throw new RuntimeException("This is the 500th description pass of " + cls + ". This class is most likely recursive. Probably (but not certainly) due this field: " + cause);
				

				this.type = WSDType.OBJECT;
				String typeName = GenericTypeReflector.getTypeName(type);
				if (!objectTypes.containsKey(typeName))
					new WSDClass(type, typeName);
				this.ref = typeName;
			}
		}
	}
}
