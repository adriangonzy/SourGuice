package com.github.sourguice.appengine.ws;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.github.sourguice.annotation.request.PathVariable;
import com.github.sourguice.annotation.request.RequestMapping;
import com.github.sourguice.annotation.request.RequestParam;
import com.github.sourguice.appengine.upload.MvcUploadService;
import com.github.sourguice.appengine.upload.UploadResult;
import com.github.sourguice.appengine.upload.annotation.UploadMapping;
import com.github.sourguice.appengine.upload.annotation.Uploaded;
import com.github.sourguice.appengine.upload.internal.BaseUploadServlet.BlobstoreUtils;
import com.github.sourguice.appengine.ws.BlobKeyTranslater.BlobKeyTranslaterFactory;
import com.github.sourguice.appengine.ws.StringBlobKeyTranslater.StringBlobKeyTranslaterFactory;
import com.github.sourguice.appengine.ws.UploadResultTranslater.UploadResultTranslaterFactory;
import com.github.sourguice.throwable.controller.MVCHttpServletResponseException;
import com.github.sourguice.throwable.controller.MVCHttpServletResponseSendErrorException;
import com.github.sourguice.ws.JsonWSController;
import com.github.sourguice.ws.WSDescription.Versioned;
import com.github.sourguice.ws.WSDescription.WSDMethod;
import com.github.sourguice.ws.WSDescription.WSDMethod.WSDMParam;
import com.github.sourguice.ws.WSDescription.WSDTypeReference;
import com.github.sourguice.ws.annotation.WSInfos;
import com.github.sourguice.ws.jsontrans.SourJsonTransformer.JsonTransformException;
import com.github.sourguice.ws.jsontrans.SourJsonTransformer.NoJsonType;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.blobstore.UploadOptions;
import com.google.inject.Injector;
import com.googlecode.gentyref.TypeToken;

@WSInfos(
			translaters = {
				BlobKeyTranslaterFactory.class,
				StringBlobKeyTranslaterFactory.class,
				UploadResultTranslaterFactory.class
			}
		)
public class JsonWSAppengineController extends JsonWSController {

	public JsonWSAppengineController(Injector injector) {
		super(injector);
		description.knownClasses.add(UrlReturn.class);
	}
	
	private static class UploadCallInfos implements Serializable {
		private static final long serialVersionUID = -6025752146983413300L;

		enum Type {
			REST,
			RPC
		}
		
		Type type;
		@CheckForNull String function;
		double version;
		Object json;

		public UploadCallInfos(Type type, @CheckForNull String function, double version, Object json) {
			super();
			this.type = type;
			this.function = function;
			this.version = version;
			this.json = json;
		}
	}
	
	@Override
	protected void pluginMethod(WSDMethod method, Method m) {
		super.pluginMethod(method, m);
		WSUploadMethod anno = m.getAnnotation(WSUploadMethod.class);
		if (anno != null) {
			Map<String, Object> infos = new HashMap<String, Object>();
			infos.put("maxUploadSizeBytes", Long.valueOf(anno.maxUploadSizeBytes()));
			infos.put("maxUploadSizeBytesPerBlob", Long.valueOf(anno.maxUploadSizeBytesPerBlob()));
			method.plugins.put("aaUpload", infos);
		}
	}
	
	private void pluginUpload(Versioned obj, AnnotatedElement el) {
		Uploaded anno = el.getAnnotation(Uploaded.class);
		if (anno != null) {
			Map<String, Object> infos = new HashMap<String, Object>();
			infos.put("serverName", anno.value());
			infos.put("maxSizeBytes", Long.valueOf(anno.maxSizeBytes()));
			infos.put("mimeTypePattern", anno.mimeTypePattern());
			obj.plugins.put("aaUpload", infos);
		}
	}
	
	@Override
	protected void pluginField(WSDTypeReference ref, Field f) {
		super.pluginField(ref, f);
		pluginUpload(ref, f);
	}
	
	@Override
	protected void pluginParam(WSDMParam param, AnnotatedElement el, Type type) {
		super.pluginParam(param, el, type);
		pluginUpload(param, el);
	}
	
	public static class UrlReturn implements NoJsonType {
		String url;
		Map<String, Boolean> uploads = new HashMap<String, Boolean>();
		public UrlReturn(String url) {
			this.url = url;
		}
	}
	
	// TODO: Handle multiple upload
	private void lookForUploads(Object obj, Map<String, Boolean> map) {
		if (obj instanceof String) {
			String str = (String)obj;
			if (str.startsWith("__UP:"))
				map.put(str.substring(5), Boolean.FALSE);
			return ;
		}

		if (obj instanceof JSONObject) {
			JSONObject jobj = (JSONObject)obj;
			
			for (Object object : jobj.values())
				lookForUploads(object, map);
		}
		else if (obj instanceof JSONArray) {
			JSONArray array = (JSONArray)obj;
			
			for (Object object : array)
				lookForUploads(object, map);
		}
	}
	
	@RequestMapping("/rest/{version}/{function}/upload")
	public final void prepareRESTUpload(HttpServletRequest req, HttpSession session, HttpServletResponse res,
			@PathVariable("version") double version,
			@PathVariable("function") String function
			) throws MVCHttpServletResponseException, IOException {
		try {
			Method method = findMethod(function, version);
			if (method.getDeclaringClass().getSimpleName().contains("$$EnhancerByGuice$$"))
				method = method.getDeclaringClass().getSuperclass().getMethod(method.getName(), method.getParameterTypes());
			WSUploadMethod upm = method.getAnnotation(WSUploadMethod.class);
			if (upm == null)
				throw new MVCHttpServletResponseSendErrorException(400, "This method does not supports upload. (Not annotated with @WSUploadMethod)");
			JSONObject jsonObject = getJsonElement(req, JSONObject.class);
			assert jsonObject != null;
			String key = UUID.randomUUID().toString();
			session.setAttribute("WS-UPLOAD-" + key, new UploadCallInfos(UploadCallInfos.Type.REST, function, version, jsonObject));
			UploadOptions options = UploadOptions.Builder.withDefaults();
			if (upm.maxUploadSizeBytes() > 0)
				options.maxUploadSizeBytes(upm.maxUploadSizeBytes());
			else if (upm.maxUploadSizeBytesPerBlob() > 0)
				options.maxUploadSizeBytesPerBlob(upm.maxUploadSizeBytesPerBlob());
			UrlReturn ret = new UrlReturn(MvcUploadService.createUploadUrl(this.getUnmodifiedClass(), "uploaded", options, "key=" + key));
			lookForUploads(jsonObject, ret.uploads);
			if (ret.uploads.isEmpty())
				throw new MVCHttpServletResponseSendErrorException(400, "No upload placeholder were given in the JSON");
			Object retJson = jsonTransformer.toJSON(ret, UrlReturn.class, version, false);
			assert retJson != null;
			try (Writer out = responseWriter(res, req, true)) {
				out.write(retJson.toString());
				out.flush();
			}
		}
		catch (JsonTransformException | ParseException | NoSuchMethodException e) {
			e.printStackTrace();
			throw new MVCHttpServletResponseSendErrorException(400, "JSON ERROR: " + e.getMessage());
		}
	}

	@RequestMapping("/rpc/{version}/upload")
	public final void prepareRPCUpload(HttpServletRequest req, HttpSession session, HttpServletResponse res,
			@PathVariable("version") double version
			) throws MVCHttpServletResponseException, IOException {
		try {
			JSONArray jsonArray = getJsonElement(req, JSONArray.class);
			assert jsonArray != null;

			boolean hasOneUploadMethod = false;
			long maxUploadSizeBytes = -1;
			long maxUploadSizeBytesPerBlob = -1;
			for (Object object : jsonArray) {
				JSONObject obj = (JSONObject) object;
				
				if (obj.containsKey("method")) {
					try {
						Method method = findMethod((String)obj.get("method"), version);
						if (method.getDeclaringClass().getSimpleName().contains("$$EnhancerByGuice$$"))
							method = method.getDeclaringClass().getSuperclass().getMethod(method.getName(), method.getParameterTypes());
						WSUploadMethod upm = method.getAnnotation(WSUploadMethod.class);
						if (upm != null) {
							hasOneUploadMethod = true;
							if (upm.maxUploadSizeBytes() > maxUploadSizeBytes)
								maxUploadSizeBytes = upm.maxUploadSizeBytes();
							if (upm.maxUploadSizeBytesPerBlob() > maxUploadSizeBytesPerBlob)
								maxUploadSizeBytesPerBlob = upm.maxUploadSizeBytesPerBlob();
						}
					}
					catch (NoSuchMethodException e) {}
				}
			}

			if (!hasOneUploadMethod)
				throw new MVCHttpServletResponseSendErrorException(400, "No method called on this RPC request does supports upload. (Annotated with @WSUploadMethod)");

			String key = UUID.randomUUID().toString();
			session.setAttribute("WS-UPLOAD-" + key, new UploadCallInfos(UploadCallInfos.Type.RPC, null, version, jsonArray));
			UploadOptions options = UploadOptions.Builder.withDefaults();
			if (maxUploadSizeBytes > 0)
				options.maxUploadSizeBytes(maxUploadSizeBytes);
			else if (maxUploadSizeBytesPerBlob > 0)
				options.maxUploadSizeBytesPerBlob(maxUploadSizeBytesPerBlob);
			UrlReturn ret = new UrlReturn(MvcUploadService.createUploadUrl(this.getUnmodifiedClass(), "uploaded", options, "key=" + key));
			lookForUploads(jsonArray, ret.uploads);
			if (ret.uploads.isEmpty())
				throw new MVCHttpServletResponseSendErrorException(400, "No upload name were given in the JSON");
			Object retJson = jsonTransformer.toJSON(ret, UrlReturn.class, version, false);
			assert retJson != null;
			try (Writer out = responseWriter(res, req, false)) {
				out.write(retJson.toString());
				out.flush();
			}
		}
		catch (ParseException | JsonTransformException e) {
			e.printStackTrace();
			throw new MVCHttpServletResponseSendErrorException(400, "JSON ERROR: " + e.getMessage());
		}
	}

	private JSONObject handleUploads(JsonWSUploadInfos infos) throws JsonTransformException {
		BlobstoreService blobstore = BlobstoreServiceFactory.getBlobstoreService();
		UploadResult absentResult = new UploadResult(null, UploadResult.STATUS.ABSENT, null);
		assert infos.maps != null;
		for (String name : infos.maps.blobKeys.keySet()) {
			List<BlobKey> blobkeysList = infos.maps.blobKeys.get(name);
			if (infos.maps.results.containsKey(name)) {
				List<UploadResult> resultsList = infos.maps.results.get(name);
				List<BlobKey> remaining = new LinkedList<BlobKey>(blobkeysList);
				for (UploadResult result : resultsList)
					remaining.remove(result.getBlobKey());
				BlobstoreUtils.Delete(blobstore, remaining);
			}
			else {
				List<UploadResult> resultList = new ArrayList<UploadResult>();
				for (int i = 0; i < blobkeysList.size(); ++i)
					resultList.add(absentResult);
				BlobstoreUtils.Delete(blobstore, blobkeysList);
				infos.maps.results.put(name, resultList);
			}
		}
		
		JSONObject ret = (JSONObject)jsonTransformer.toJSON(infos.maps.results, new TypeToken<Map<String, List<UploadResult>>>(){}.getType(), 1.0f);
		assert ret != null;
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	@UploadMapping(onlyParamUploads = false)
	public final void uploaded(HttpServletResponse res, HttpSession session, JsonWSUploadInfos infos,
			Map<String, List<BlobKey>> blobKeys, HttpServletRequest req,
			@RequestParam("key") String key
			) throws Throwable {
		UploadCallInfos uci = (UploadCallInfos)session.getAttribute("WS-UPLOAD-" + key);
		if (uci == null)
			throw new MVCHttpServletResponseSendErrorException(400, "No pending upload request");
		
		infos.maps = new JsonWSUploadInfos.Maps(blobKeys);

		switch (uci.type) {
		case REST: {
			assert uci.function != null;
			JSONObject ret = handleREST(res, req, uci.function, uci.version, (JSONObject)uci.json);
			ret.put("!uploads", handleUploads(infos));
			try (Writer out = responseWriter(res, req, true)) {
				ret.writeJSONString(out);
				out.flush();
			}
			return ;
		}
		case RPC: {
			LinkedList<Object> list = new LinkedList<>();
			
			for (Object object : (JSONArray)uci.json) {
				JSONObject json = (JSONObject)object;
				JSONObject result = handleRPCCall(json, uci.version);
				if (result != null)
					list.add(result);
			}

			JSONObject upContainer = new JSONObject();
			upContainer.put("id", "!uploads");
			upContainer.put("uploads", handleUploads(infos));
			list.addFirst(upContainer);

			JSONArray ret = new JSONArray();
			ret.addAll(list);
			try (Writer out = responseWriter(res, req, false)) {
				ret.writeJSONString(out);
				out.flush();
			}
			return ;
		}
		default:
			throw new UnsupportedOperationException("WTF ?!?!");
		}
	}

}
