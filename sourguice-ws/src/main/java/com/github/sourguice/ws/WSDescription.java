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

/**
 * Represents the architecture of a web services class,
 * it describes each type, enum and methods.
 * It is used to be translated into JSON to enable code generation.
 * This is basically the WSDL of SOAP, but for SourGuice JSON based WS
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public final class WSDescription implements NoJsonType {
	
	/**
	 * The package of the WS
	 */
	public String packageName;
	
	/**
	 * The name of the WS
	 */
	public String name;
	
	/**
	 * The default version declared by the WS
	 */
	public double defaultVersion;
	
	/**
	 * The base URL for those WS to be prepend to get real WS urls.
	 */
	public @CheckForNull String baseUrl;
	
	/**
	 * List of active plugins on this WS.
	 * This CAN affect code generation (or not).
	 */
	public Map<String, Object> plugins = new HashMap<>();
	
	/**
	 * List of type (Java classes) that are handled by these WS
	 */
	public Map<String, WSDClass> objectTypes = new HashMap<>();

	/**
	 * List of enums that are handled by these WS
	 */
	public Map<String, WSDEnum> enumTypes = new HashMap<>();
	
	/**
	 * List of Constants that are helpful for these WS
	 */
	public Map<String, WSDConstant> constants = new HashMap<>();
	
	/**
	 * List of methods that are callable from these WS
	 */
	public Map<String, WSDMethod> methods = new HashMap<>();

	/**
	 * Count of how much time each class has been look at.
	 * This is used to detect recursive classes (after 500 passes, an exception is thrown)
	 */
	private transient Map<Class<?>, Integer> objectFieldsPasses = new HashMap<>();
	
	/**
	 * List of translaters that will be applicable
	 */
	public transient Map<Class<?>, WSTranslaterFactory<?, ?>> translaters = new HashMap<>();
	
	/**
	 * List of known, parsed classes.
	 * This is useful for external access to check if a particular type has been described
	 */
	public transient Set<Class<?>> knownClasses = new HashSet<>();
	
	/**
	 * Controller that can put plugins on various elements
	 */
	private transient JsonWSController controller;
	
	/**
	 * Base class for most elements in this description.
	 * 
	 * @author Salomon BRYS <salomon.brys@gmail.com>
	 */
	public static abstract class Versioned implements NoJsonType {
		/**
		 * This states that the element is valid since a particular version
		 */
		public @CheckForNull Double since;
		
		/**
		 * This states that the element is valid until a particular version (including)
		 */
		public @CheckForNull Double until;
		
		/**
		 * The documentation of the element.
		 * Used by code generators to generate doc for methods / types / properties / arguments / etc.
		 */
		public @CheckForNull String[] doc;
		
		/**
		 * List of active plugins on this element.
		 * This CAN affect code generation (or not).
		 */
		public Map<String, Object> plugins = new HashMap<>();

		/**
		 * Constructor used by Json parsers to create then fill the objects.
		 * DO NOT USE MANUALLY!
		 */
		@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "JSON")
		private Versioned() {}

		/**
		 * @param el The element that will be described
		 */
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
	
	/**
	 * Utility that get the name of the field,
	 * either from the WSFieldName annotation or from reflexivity
	 * 
	 * @param f The field to get the name
	 * @return The name
	 */
	private static String getFieldName(Field f) {
		String fieldName = f.getName();
		WSFieldName wsFieldName = f.getAnnotation(WSFieldName.class);
		if (wsFieldName != null)
			fieldName = wsFieldName.value();
		return fieldName;
	}
	
	/**
	 * Constructor used by Json parsers to create then fill the objects.
	 * DO NOT USE MANUALLY!
	 */
	@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "JSON")
	private WSDescription() {}

	/**
	 * @param cls The WS class that contains all the WS methods to describe
	 * @param injector Guice injector
	 * @param controller Used to plugins
	 */
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
	
	/**
	 * A class that will be handled (in or out) by the web services.
	 */
	public final class WSDClass extends Versioned {
		/**
		 * It's super class
		 */
		public @CheckForNull String parent = null;
		
		/**
		 * Whether or not this in an abstract class
		 */
		public @CheckForNull Boolean isAbstract;
		
		/**
		 * List of properties
		 */
		public Map<String, WSDTypeReference> properties = new HashMap<>();
		
		/**
		 * List of constants defined within it's scope.
		 */
		@SuppressWarnings("hiding")
		public Map<String, WSDConstant> constants = new HashMap<>();
		
		/**
		 * Constructor used by Json parsers to create then fill the objects.
		 * DO NOT USE MANUALLY!
		 */
		@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "JSON")
		private WSDClass() {}

		/**
		 * @param type The type to describe
		 * @param typeName It's name
		 */
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

	/**
	 * A constant used by WS
	 */
	public final class WSDConstant extends Versioned {
		/**
		 * It's type
		 */
		public WSDType type;
		
		/**
		 * It's value
		 */
		public Object value;
		
		/**
		 * @param f The constant field to describe
		 */
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
	
	/**
	 * An enum that will be handled (in or out) by the web services.
	 */
	public final class WSDEnum extends Versioned {
		/**
		 * List of enum values
		 */
		public List<String> values = new LinkedList<>();

		/**
		 * Constructor used by Json parsers to create then fill the objects.
		 * DO NOT USE MANUALLY!
		 */
		@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "JSON")
		private WSDEnum() {}

		/**
		 * @param cls The enum type to describe
		 * @param typeName It's name
		 */
		public WSDEnum(Class<?> cls, String typeName) {
			super(cls);
			for (Object c : cls.getEnumConstants())
				values.add(c.toString());
			
			enumTypes.put(typeName, this);
			controller.pluginEnum(this, cls);
		}
	}

	/**
	 * A method in the WS class
	 */
	public final class WSDMethod extends Versioned {
		/**
		 * The method's parameters
		 */
		public @CheckForNull Map<String, WSDMParam> params;
		
		/**
		 * The type of the return of the method
		 */
		public WSDTypeReference returns;
		
		/**
		 * List of exceptions declared by this method
		 */
		public List<WSDTypeReference> exceptions;
		
		/**
		 * Constructor used by Json parsers to create then fill the objects.
		 * DO NOT USE MANUALLY!
		 */
		@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "JSON")
		private WSDMethod() {}
		
		/**
		 * @param cls The class of the method to describe
		 * @param method The method to describe
		 * @param anno The WSMethod annotating this method
		 */
		public WSDMethod(Class<?> cls, Method method, WSMethod anno) {
			super(method);
			String methodName = anno.name();
			if (methodName.isEmpty())
				methodName = method.getName();
			methods.put(methodName, this);

			this.returns = new WSDTypeReference(GenericTypeReflector.getExactReturnType(method, cls), null, null);
			
			Type[] paramTypes = GenericTypeReflector.getExactParameterTypes(method, cls);
			Annotation[][] paramAnnos = method.getParameterAnnotations();
			for (int i = 0; i < paramTypes.length; ++i) {
				WSParam wsParam = Annotations.GetOneRecursive(WSParam.class, paramAnnos[i]);
				if (wsParam != null)
					new WSDMParam(wsParam, Annotations.fromArray(paramAnnos[i]), paramTypes[i]);
			}
			
			for (Type exceptionType : method.getGenericExceptionTypes()) {
				if (exceptions == null)
					exceptions = new ArrayList<>();
				if (Annotations.GetOneTreeRecursive(GenericTypeReflector.erase(exceptionType), WSException.class) != null)
					exceptions.add(new WSDTypeReference(exceptionType, null, null));
			}
			controller.pluginMethod(this, method);
		}

		/**
		 * Parameter of a method
		 */
		public final class WSDMParam extends Versioned {
			/**
			 * Position of the parameter
			 */
			public int position;
			
			/**
			 * Type of the parameter
			 */
			public WSDTypeReference type;
			
			/**
			 * Constructor used by Json parsers to create then fill the objects.
			 * DO NOT USE MANUALLY!
			 */
			@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "JSON")
			private WSDMParam() {}

			/**
			 * @param wsParam The WSParam annotation annotating this param
			 * @param el The annotated element representing this parameter
			 * @param type The type of the parameter
			 */
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
	
	/**
	 * The standard types known by SourGuice-WS code generators.
	 */
	public enum WSDType {
		/** void */											VOID,
		/** Primitive boolean */							BOOLEAN_P,
		/** Boolean object */								BOOLEAN_O,
		/** Primitive int */								INT_P,
		/** Integer object */								INT_O,
		/** Primitive float */								FLOAT_P,
		/** Float object */									FLOAT_O,
		/** String object */								STRING,
		/** Collection */									COLLECTION,
		/** Map */											MAP,
		/** Enum type */									ENUM,
		/** Date (java.util.Date) */						DATE,
		/** Any other object (composed by properties) */	OBJECT
	};

	/**
	 * A reference to a type
	 */
	public final class WSDTypeReference extends Versioned {
		
		/**
		 * The type (class) of the reference
		 */
		public WSDType type;
		
		/**
		 * The name of the reference
		 */
		public String ref;
		
		/**
		 * The generic type parameters (if any)
		 */
		public @CheckForNull List<WSDTypeReference> parameterTypes;
		
		/**
		 * Whether or not this can be transmitted null (only when reference is a parameter)
		 */
		public @CheckForNull Boolean nullable;
		
		/**
		 * Constructor used by Json parsers to create then fill the objects.
		 * DO NOT USE MANUALLY!
		 */
		@SuppressFBWarnings(value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "JSON")
		private WSDTypeReference() {}

		/**
		 * @param type The type of the reference
		 * @param el The annotated element representing the reference
		 * @param cause What caused this reference (for internal count with {@link WSDescription#objectFieldsPasses})
		 */
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
				this.type = WSDType.COLLECTION;
				parameterTypes = new ArrayList<>(1);
				this.parameterTypes.add(new WSDTypeReference(GenericTypeReflector.getArrayComponentType(type), null, null));
			}

			else if (Collection.class.isAssignableFrom(cls)) {
				this.type = WSDType.COLLECTION;
				parameterTypes = new ArrayList<>(1);
				ParameterizedType listType = (ParameterizedType)GenericTypeReflector.getExactSuperType(type, Collection.class);
				this.parameterTypes.add(new WSDTypeReference(listType.getActualTypeArguments()[0], null, cause));
			}
			
			else if (Map.class.isAssignableFrom(cls)) {
				this.type = WSDType.MAP;
				ParameterizedType mapType = (ParameterizedType)GenericTypeReflector.getExactSuperType(type, Map.class);
				if (!String.class.isAssignableFrom(GenericTypeReflector.erase(mapType.getActualTypeArguments()[0])))
					throw new RuntimeException("Map first generic argument must be a String in " + cause);
				parameterTypes = new ArrayList<>(1);
				this.parameterTypes.add(new WSDTypeReference(mapType.getActualTypeArguments()[1], null, cause));
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
