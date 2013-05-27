package com.github.sourguice.ws;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.github.sourguice.annotation.request.GuiceRequest;
import com.github.sourguice.annotation.request.PathVariable;
import com.github.sourguice.annotation.request.RequestMapping;
import com.github.sourguice.call.CalltimeArgumentFetcher;
import com.github.sourguice.call.MvcCaller;
import com.github.sourguice.throwable.controller.MVCHttpServletResponseSendErrorException;
import com.github.sourguice.utils.Annotations;
import com.github.sourguice.value.RequestMethod;
import com.github.sourguice.ws.WSDescription.WSDClass;
import com.github.sourguice.ws.WSDescription.WSDEnum;
import com.github.sourguice.ws.WSDescription.WSDMethod;
import com.github.sourguice.ws.WSDescription.WSDTypeReference;
import com.github.sourguice.ws.WSDescription.WSDMethod.WSDMParam;
import com.github.sourguice.ws.annotation.WSCheckForNull;
import com.github.sourguice.ws.annotation.WSInfos;
import com.github.sourguice.ws.annotation.WSMethod;
import com.github.sourguice.ws.annotation.WSParam;
import com.github.sourguice.ws.annotation.WSSince;
import com.github.sourguice.ws.annotation.WSStrict;
import com.github.sourguice.ws.annotation.WSUntil;
import com.github.sourguice.ws.exception.UnknownClientTypeException;
import com.github.sourguice.ws.exception.WSRuntimeException;
import com.github.sourguice.ws.jsontrans.ArrayContentHandler;
import com.github.sourguice.ws.jsontrans.SourJsonTransformer;
import com.github.sourguice.ws.jsontrans.ArrayContentHandler.ArrayItemListener;
import com.github.sourguice.ws.jsontrans.SourJsonTransformer.JsonTransformException;
import com.github.sourguice.ws.translat.WSRuntimeTranslater;
import com.github.sourguice.ws.translat.WSTranslaterFactory;
import com.github.sourguice.ws.translat.def.ClassTranslater.ClassTranslaterFactory;
import com.github.sourguice.ws.translat.def.DateTranslater.DateTranslaterFactory;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.googlecode.gentyref.GenericTypeReflector;

/**
 * This is the base class you have to subclass to create a web service class
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@WSInfos(
		translaters = {
			DateTranslaterFactory.class,
			ClassTranslaterFactory.class
		}
	)
@Singleton
public abstract class JsonWSController {

	/**
	 * The description of the WS, which will be generated at startup
	 */
	protected WSDescription description;

	/**
	 * Guice injector
	 */
	private Injector injector;
	
	/**
	 * TODO: Delete this !
	 */
	private Map<Class<? extends Exception>, WSRuntimeTranslater<?>> runtimeExceptionTranslaters = new HashMap<>();
	
	/**
	 * Cache for the {@link #findMethod(String, double)} function
	 */
	private Map<String, Method> methodCache = new HashMap<>();
	
	/**
	 * JSON Util that is responsible for transforming Json to Objects and vice versa
	 */
	protected SourJsonTransformer jsonTransformer;
	
	/**
	 * @param injector Guice injector
	 */
	@Inject
	public JsonWSController(Injector injector) {
		this.injector = injector;
		description = new WSDescription(this.getUnmodifiedClass(), injector, this);
		description.knownClasses.add(WSRuntimeException.class);
		WSInfos infos = this.getUnmodifiedClass().getAnnotation(WSInfos.class);
		if (infos != null) {
			for (Class<? extends WSRuntimeTranslater<?>> trClass : infos.runtimeExceptionTranslaters()) {
				if (trClass.getAnnotation(Singleton.class) == null && trClass.getAnnotation(javax.inject.Singleton.class) == null)
					throw new AssertionError(trClass + " MUST be annotated with @Singleton");
				WSRuntimeTranslater<?> tr = injector.getInstance(trClass);
				runtimeExceptionTranslaters.put(tr.getExceptionClass(), tr);
			}
		}
		
		this.jsonTransformer = new SourJsonTransformer(description.translaters.values(), description.knownClasses);
	}

	/**
	 * Override this to add plugins to a type / class
	 * 
	 * @param cls The class being created
	 * @param type The java type of the class being processed
	 */
	@SuppressWarnings("unused")
	protected void pluginClass(WSDClass cls, Type type) {}
	
	/**
	 * Override this to add plugins to a field
	 * 
	 * @param ref The type to the field being created
	 * @param field The java field being processed
	 */
	@SuppressWarnings("unused")
	protected void pluginField(WSDTypeReference ref, Field field) {}
	
	/**
	 * Override this to add plugins to an enum
	 * 
	 * @param en The enum being created
	 * @param cls The java class of the enum being processed
	 */
	@SuppressWarnings("unused")
	protected void pluginEnum(WSDEnum en, Class<?> cls) {}
	
	/**
	 * Override this to add plugins to a method
	 * 
	 * @param method The method being created
	 * @param m The java method being processed
	 */
	@SuppressWarnings("unused")
	protected void pluginMethod(WSDMethod method, Method m) {}
	
	/**
	 * Override this to add plugins to a parameter
	 * 
	 * @param param The parameter being created
	 * @param el The annotations of the parameter being processed
	 * @param type The type of the parameter being processed
	 */
	@SuppressWarnings("unused")
	protected void pluginParam(WSDMParam param, AnnotatedElement el, Type type) {}

	/**
	 * @return This class *before* being enhanced by guice (in case of AOP)
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends JsonWSController> getUnmodifiedClass() {
		Class<? extends JsonWSController> cls = this.getClass();
		if (cls.getSimpleName().contains("$$EnhancerByGuice$$"))
			cls = (Class<? extends JsonWSController>) cls.getSuperclass();
		return cls;
	}

	/**
	 * Get the next JSON element from the request body
	 * 
	 * @param req The request to read
	 * @param cls The class of the element that should be read
	 * @return The parsed JSON element
	 * @throws ParseException If a parsing error occurs
	 * @throws IOException If an input or output error occurs
	 */
	@SuppressWarnings("unchecked")
	protected @CheckForNull <T> T getJsonElement(final HttpServletRequest req, final Class<T> cls) throws ParseException, IOException {
		CharsetDecoder utf8Decoder = Charset.forName("UTF-8").newDecoder();
		utf8Decoder.onMalformedInput(CodingErrorAction.IGNORE);
		utf8Decoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
		Object ret = new JSONParser().parse(new InputStreamReader(req.getInputStream(), utf8Decoder));
		if (ret == null)
			throw new ParseException(ParseException.ERROR_UNEXPECTED_EXCEPTION);
		if (!ret.getClass().equals(cls))
			throw new IOException("Not a " + cls.getSimpleName());
		return (T)ret;
	}

	/**
	 * Uncached {@link #findMethod(String, double)}
	 * 
	 * @param function The name of the method to find
	 * @param version The version of the WS to use
	 * @return The method found
	 * @throws NoSuchMethodException If no method was found
	 */
	private Method _findMethod(String function, double version) throws NoSuchMethodException {
		Class<?> uc = this.getUnmodifiedClass();
		for (Method method : this.getClass().getMethods()) {
			Method um = method;
			if (!uc.equals(this.getClass()))
				try {
					um = uc.getMethod(method.getName(), method.getParameterTypes());
				}
				catch (NoSuchMethodException e) {}
			WSMethod wsMethod = Annotations.GetOneRecursive(WSMethod.class, um.getAnnotations());
			if (wsMethod == null)
				continue ;
			String name = wsMethod.name();
			if (name.isEmpty())
				name = method.getName();
			if (name.equals(function)) {
				WSSince since = um.getAnnotation(WSSince.class);
				if (since != null && since.value() >= 0 && since.value() > version)
					continue ;
				WSUntil until = um.getAnnotation(WSUntil.class);
				if (until != null && until.value() >= 0 && until.value() < version)
					continue ;

				return method;
			}
		}
		throw new NoSuchMethodException();
	}
	
	/**
	 * Finds a method with the given name (will match the method's annotation if given, or else it's name)
	 * according to the given version
	 * This use an internal cache to only look for each method only once
	 * 
	 * @param function The name of the method to find
	 * @param version The version of the WS to use
	 * @return The method found
	 * @throws NoSuchMethodException If no method was found
	 */
	protected Method findMethod(String function, double version) throws NoSuchMethodException {
		String cacheStr = function + " - " + version;
		if (!methodCache.containsKey(cacheStr))
			methodCache.put(cacheStr, _findMethod(function, version));
		return methodCache.get(cacheStr);
	}
	
	/**
	 * Status that can be assign to a Web Service call
	 */
	private static enum CallResultStatus {
		/** The WS call ended normally */				OK,
		/** The WS call threw an exception */			EXCEPTION,
		/** The WS call threw a runtime exception */	RUNTIME_EXCEPTION,
		/** The WS call threw a server error */			CALL_ERROR
	}
	
	/**
	 * The result of a call is a composition of it's status and it's return value or it's exception
	 */
	private static final class CallResult {
		/**
		 * Status of the call
		 */
		CallResultStatus status = CallResultStatus.OK;
		
		/**
		 * If the status is OK, this is the return value of the call.
		 * If the status is CALL_ERROR, this is null.
		 * Else, this is the thrown exception.
		 */
		@CheckForNull Object result;
	}
	
	/**
	 * Exception thrown when a parameter is missing in a call
	 */
	private static class NoSuchWSParamException extends Exception
	{
		@SuppressWarnings("javadoc")
		private static final long serialVersionUID = 5701697332216439430L;

		/**
		 * @param name The name of the missing parameter
		 * @param cls The type of the missing parameter
		 * @param notNull Whether or not the parameteris allowed to be null
		 */
		public NoSuchWSParamException(String name, Class<?> cls, boolean notNull) {
			super("Missing " + (notNull ? "NOT NULL" : "") + " parameter " + cls.getSimpleName() + " " + name);
		}
	}

	/**
	 * Makes a call to the given WS function
	 * 
	 * @param function The name of the method to call
	 * @param version The version of the WS to use
	 * @param jsonObject The JSON object representing the parameters to the call
	 * @return The result of the WS call
	 * @throws Throwable If anything went wrong ;)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private CallResult call(final String function, final double version, final JSONObject jsonObject) throws Throwable {
		// Get the method corresponding to the WS call
		Method method = findMethod(function, version);
		// An empty CallResult that will be filled
		CallResult result = new CallResult();
		try {
			// Make the actual call
			// The given CalltimeArgumentFetcher handles the @WSParam annotated parameters
			Object ret = injector.getInstance(MvcCaller.class).call(this.getClass(), method, null, true, new CalltimeArgumentFetcher<Object>() {
				@Override public boolean canGet(Type type, int pos, Annotation[] annos) {
					return Annotations.fromArray(annos).isAnnotationPresent(WSParam.class);
				}
				@Override public @CheckForNull Object get(Type type, int pos, Annotation[] annos) throws NoSuchWSParamException {
					Class<?> clazz = GenericTypeReflector.erase(type);
					AnnotatedElement annoEl = Annotations.fromArray(annos);
					WSParam wsParam = annoEl.getAnnotation(WSParam.class);
					assert(wsParam != null);

					boolean nullable = annoEl.isAnnotationPresent(WSCheckForNull.class);

					// Throw an exception if the JSON parameter is null and it's not allowed to be
					if (jsonObject == null) {
						if (nullable)
							return null;
						throw new NoSuchWSParamException(wsParam.value(), clazz, !nullable);
					}

					// Get the parameter from the JSON using the transformer
					try {
						Object obj = jsonTransformer.fromJSON(jsonObject.get(wsParam.value()), type, version, annoEl, null);
						if (obj == null && !nullable)
							throw new NoSuchWSParamException(wsParam.value(), clazz, !nullable);
						return obj;
					}
					catch (JsonTransformException e) {
						e.printStackTrace();
						throw new NoSuchWSParamException(wsParam.value(), clazz, !nullable);
					}
				}
			});

			// If the method is enhanced by Guice, we get the original version
			// This could be dangerous if we would use this method object to make a call it (as it would prevent Guice AOP)
			// Be we only use this method object to get it's annotations and informations
			if (method.getDeclaringClass().getSimpleName().contains("$$EnhancerByGuice$$"))
				method = method.getDeclaringClass().getSuperclass().getMethod(method.getName(), method.getParameterTypes());

			if (ret != null) {
				Type retType = ret.getClass();
				Class<?> retClass = GenericTypeReflector.erase(retType);
				if (Map.class.isAssignableFrom(retClass) || Collection.class.isAssignableFrom(retClass) || Annotations.GetOneTree(method, WSStrict.class) != null)
					retType = GenericTypeReflector.getExactReturnType(method, this.getClass());
	
				// Transform the result from java bean to JSON
				result.result = jsonTransformer.toJSON(ret, retType, version, method, true, null);
			}
		}
		// Thrown when a parameter is missing
		catch (NoSuchWSParamException e) {
			result.status = CallResultStatus.CALL_ERROR;
			result.result = e.getMessage();
		}
		// Thrown when a runtime exception that is to be passed to the client is thrown
		catch (WSRuntimeException e) {
			result.status = CallResultStatus.RUNTIME_EXCEPTION;
			result.result = jsonTransformer.toJSON(e, WSRuntimeException.class, version);
		}
		// Any other case
		catch (Exception e) {
			Class<?> exClass = e.getClass();
			// Check that the exception is known by the client
			if (!description.knownClasses.contains(exClass)) {
				if (runtimeExceptionTranslaters.containsKey(exClass)) {
					result.status = CallResultStatus.RUNTIME_EXCEPTION;
					WSRuntimeTranslater translater = runtimeExceptionTranslaters.get(exClass);
					result.result = jsonTransformer.toJSON(translater.transformException(e), WSRuntimeException.class, version);
				}
				else
					throw new UnknownClientTypeException(exClass, e);
			}
			else {
				result.status = CallResultStatus.EXCEPTION;
				result.result = jsonTransformer.toJSON(e, exClass, version);
			}
		}
		return result;
	}

	/**
	 * Get a writer for the response that is configured correctly (content-type & encoding header)
	 * 
	 * @param res The current response object from which to get the writer
	 * @param req The current request object (used to check for specific config)
	 * @param checkForRaw 
	 * @return
	 * @throws IOException
	 */
	protected Writer responseWriter(HttpServletResponse res, HttpServletRequest req, boolean checkForRaw) throws IOException {
		res.setCharacterEncoding("UTF-8");
		boolean jsonCT = true;
		if (checkForRaw)
			jsonCT = (req.getParameter("__rawContentType") == null);
		if (jsonCT)
			res.setContentType("application/json; charset=utf-8");
		else
			res.setContentType("text/html; charset=utf-8");
		res.setHeader("Access-Control-Allow-Origin", "*");

		CharsetEncoder utf8Encoder = Charset.forName("UTF-8").newEncoder();
		utf8Encoder.onMalformedInput(CodingErrorAction.IGNORE);
		utf8Encoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
		return new OutputStreamWriter(res.getOutputStream(), utf8Encoder);
//		return res.getWriter();
	}
	
	@SuppressWarnings("unchecked")
	protected JSONObject handleREST(HttpServletResponse res, HttpServletRequest req, String function, double version, JSONObject jsonObject) throws Throwable {
		CallResult result = call(function, version, jsonObject);
		JSONObject obj = new JSONObject();
		int status = 200;
		switch (result.status) {
		case OK:
			obj.put("success", Boolean.TRUE);
			break;
		case CALL_ERROR:
			status = 400;
			obj.put("success", Boolean.FALSE);
			break;
		case EXCEPTION:
			status = 406;
			obj.put("success", Boolean.FALSE);
			break;
		case RUNTIME_EXCEPTION:
			obj.put("success", Boolean.FALSE);
			status = 460;
			break;
		}
		
		if (req.getParameter("__rawContentType") == null)
			res.setStatus(status);
		else
			obj.put("status", Integer.valueOf(status));
		
		obj.put("RESULT", result.result);

		return obj;
	}
	
	
	@RequestMapping(value = "/rest/{version}/{function}", method = RequestMethod.POST)
	@OverridingMethodsMustInvokeSuper
	public void rest(@GuiceRequest HttpServletRequest req, HttpServletResponse res,
			@PathVariable("version") double version,
			@PathVariable("function") String function
			) throws Throwable {
		try {
			JSONObject jsonObject = getJsonElement(req, JSONObject.class);
			assert jsonObject != null;
			
			JSONObject obj = handleREST(res, req, function, version, jsonObject);
			try (Writer out = responseWriter(res, req, true)) {
				out.write(obj.toString());
				out.flush();
			}
		}
		catch (NoSuchMethodException e) {
			throw new MVCHttpServletResponseSendErrorException(404);
		}
		catch (ParseException e) {
			e.printStackTrace();
			throw new MVCHttpServletResponseSendErrorException(400, "JSON ERROR: " + e.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	protected @CheckForNull JSONObject handleRPCCall(JSONObject obj, double version) throws Throwable {
		if (!obj.containsKey("id") || !obj.containsKey("method"))
			return null;
		
		Logger.getLogger("SourGuice-WS-RPC").info((String)obj.get("method"));
		
		JSONObject jsonObject = (JSONObject)obj.get("params");

		JSONObject jsonResult = new JSONObject();
		try {
			CallResult result = call((String)obj.get("method"), version, jsonObject);
			jsonResult.put("status", result.status.toString());
			jsonResult.put("result", result.result);
		}
		catch (NoSuchMethodException e) {
			jsonResult.put("status", CallResultStatus.CALL_ERROR.toString());
			jsonResult.put("result", "No method " + (String)obj.get("method"));
		}
		jsonResult.put("id", obj.get("id"));
		return jsonResult;
	}

	@SuppressWarnings("serial")
	public class EncapsulatedException extends RuntimeException {
		public EncapsulatedException(Throwable t) {
			super(t);
		}
	}
	
	@RequestMapping(value = "/rpc/{version}", method = RequestMethod.POST)
	@OverridingMethodsMustInvokeSuper
	public void rpc(@GuiceRequest HttpServletRequest req, HttpServletResponse res,
			@PathVariable("version") final double version)
					throws Throwable {
		try {
			try (final Writer out = responseWriter(res, req, false)) {
				out.write('[');

				ArrayContentHandler handler = new ArrayContentHandler(new ArrayItemListener() {
					boolean coma = false;
					@Override public void onItem(Object item) {
						JSONObject json = (JSONObject)item;
						try {
							JSONObject result = handleRPCCall(json, version);
							if (result != null) {
								if (coma)
									out.write(',');
								coma = true;
								result.writeJSONString(out);
							}
						}
						catch (Throwable e) {
							throw new EncapsulatedException(e);
						}
					}
				});

				new JSONParser().parse(new InputStreamReader(req.getInputStream(), "UTF-8"), handler);

				out.write(']');
				out.flush();
			}
			catch (EncapsulatedException e) {
				throw e.getCause();
			}
		}
		catch (ParseException e) {
			e.printStackTrace();
			throw new MVCHttpServletResponseSendErrorException(400, "JSON ERROR: " + e.getMessage());
		}

	}
	
	@RequestMapping("/description")
	public final void description(HttpServletRequest req, HttpServletResponse res) throws IOException, JsonTransformException {
		res.setCharacterEncoding("UTF-8");
		res.setContentType("application/json");
		res.setContentType("text/plain; charset=utf-8");
		res.setHeader("Access-Control-Allow-Origin", "*");

		if (description.baseUrl == null) {
			description.baseUrl = req.getRequestURL().toString();
			description.baseUrl = description.baseUrl.substring(0, description.baseUrl.lastIndexOf('/'));
		}
		SourJsonTransformer json = new SourJsonTransformer(new ArrayList<WSTranslaterFactory<?,?>>(0), null);
		Object ret = json.toJSON(description, WSDescription.class, 1.0f);
		if (ret != null)
			res.getWriter().write(ret.toString());
	}

	@RequestMapping("/explorer")
	public final void explorer(HttpServletResponse res) throws IOException {
		res.setCharacterEncoding("UTF-8");
		res.setContentType("text/html; charset=utf-8");
		res.setHeader("Access-Control-Allow-Origin", "*");

		try (
				InputStream inputStream = JsonWSController.class.getResourceAsStream("explorer.html");
				InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
		) {
			char buf[] = new char[512];
			int len = 0;
			while ((len = reader.read(buf)) > 0)
				res.getWriter().write(buf, 0, len);
		}
	}
	
	public @CheckForNull Object localCall(final String function, final double version, @CheckForNull final Map<String, Object> params) throws Exception {
		Method method;
		try {
			method = findMethod(function, version);
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		try {
			return injector.getInstance(MvcCaller.class).call(this.getClass(), method, null, true, new CalltimeArgumentFetcher<Object>() {
				@Override public boolean canGet(Type type, int pos, Annotation[] annos) {
					return Annotations.fromArray(annos).isAnnotationPresent(WSParam.class);
				}
				@Override public @CheckForNull Object get(Type type, int pos, Annotation[] annos) throws NoSuchWSParamException {
					Class<?> clazz = GenericTypeReflector.erase(type);
					AnnotatedElement annoEl = Annotations.fromArray(annos);
					WSParam wsParam = annoEl.getAnnotation(WSParam.class);
					assert(wsParam != null);

					boolean nullable = annoEl.isAnnotationPresent(WSCheckForNull.class);

					if (params == null) {
						if (nullable)
							return null;
						throw new NoSuchWSParamException(wsParam.value(), clazz, !nullable);
					}

					Object obj = params.get(wsParam.value());
					if (obj == null && !nullable)
						throw new NoSuchWSParamException(wsParam.value(), clazz, !nullable);
					return obj;
				}
			});
		}
		catch (Exception e) {
			throw e;
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}

	}
}
