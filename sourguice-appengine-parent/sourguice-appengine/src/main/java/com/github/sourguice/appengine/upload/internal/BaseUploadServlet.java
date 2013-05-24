package com.github.sourguice.appengine.upload.internal;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.sourguice.appengine.upload.UploadResult;
import com.github.sourguice.appengine.upload.annotation.UploadMapping;
import com.github.sourguice.appengine.upload.annotation.Uploaded;
import com.github.sourguice.call.CalltimeArgumentFetcher;
import com.github.sourguice.call.MvcCaller;
import com.github.sourguice.throwable.invocation.HandledException;
import com.github.sourguice.utils.Annotations;
import com.github.sourguice.utils.Reflect;
import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.inject.Injector;
import com.googlecode.gentyref.GenericTypeReflector;
import com.googlecode.gentyref.TypeToken;

public abstract class BaseUploadServlet extends HttpServlet {
	private static final long serialVersionUID = -7967301385418158472L;

	protected @CheckForNull Injector injector;

	@Inject
	public final void setInjector(Injector injector) {
		this.injector = injector;
	}

	public static final class BlobstoreUtils {
		private static final BlobKey[] witness = new BlobKey[0];

		public static void Delete(BlobstoreService blobstore, Collection<BlobKey> blobs) {
			blobstore.delete(blobs.toArray(witness));
		}
		
		public static void DeleteAll(BlobstoreService blobstore, Map<String, ? extends Collection<BlobKey>> uploads) {
			for (String key : uploads.keySet())
				Delete(blobstore, uploads.get(key));
		}
	}

	public static final class UploadInfos {
		private boolean multiple;
		private Uploaded anno;

		public UploadInfos(boolean array, Uploaded anno) {
			this.multiple = array;
			this.anno = anno;
		}
	}
	
	public static final List<UploadResult> checkUploaded(UploadInfos infos, List<BlobKey> blobKeys) {
		BlobstoreService blobstore = BlobstoreServiceFactory.getBlobstoreService();

		if (!infos.multiple && blobKeys.size() > 1) {
			BlobstoreUtils.Delete(blobstore, blobKeys.subList(1, blobKeys.size()));
			blobKeys = blobKeys.subList(0, 1);
		}

		List<UploadResult> results = new ArrayList<UploadResult>(blobKeys.size());
		ListIterator<BlobKey> it = blobKeys.listIterator();
		while (it.hasNext()) {
			BlobKey blobKey = it.next();
			BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
			if (blobInfo == null || blobInfo.getSize() == 0)
				continue ;
			if (infos.anno.maxSizeBytes() > 0 && blobInfo.getSize() > infos.anno.maxSizeBytes()) {
				blobstore.delete(blobKey);
				it.remove();
				results.add(new UploadResult(null, UploadResult.STATUS.TOO_BIG, blobInfo));
			}
			else if (!Pattern.matches(infos.anno.mimeTypePattern(), blobInfo.getContentType())) {
				blobstore.delete(blobKey);
				it.remove();
				results.add(new UploadResult(null, UploadResult.STATUS.BAD_MIME_TYPE, blobInfo));
			}
			else
				results.add(new UploadResult(blobKey, UploadResult.STATUS.OK, blobInfo));
		}
		
		return results;
	}

	protected final void handleUploads(final Map<String, List<BlobKey>> uploads, HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		BlobstoreService blobstore = BlobstoreServiceFactory.getBlobstoreService();

		try {
			Class<?> uploadClass = Class.forName(req.getParameter("__C"));
			Method method = Reflect.GetMethod(uploadClass, req.getParameter("__M"));
			if (method == null)
				throw new NoSuchMethodException(req.getParameter("__M"));
			UploadMapping uploadMapping = Annotations.GetOneRecursive(UploadMapping.class, method.getAnnotations());
			if (uploadMapping == null)
				throw new NoSuchMethodException("@UploadMapping " + req.getParameter("__M"));

			Map<String, UploadInfos> neededUploads = new HashMap<String, UploadInfos>();

			Class<?>[] parameterTypes = method.getParameterTypes();
			Annotation[][] parameterAnnotations = method.getParameterAnnotations();
			for (int i = 0; i < method.getParameterTypes().length; ++i) {
				Uploaded infos = Annotations.fromArray(parameterAnnotations[i]).getAnnotation(Uploaded.class);
				if (infos != null) {
					if (!parameterTypes[i].equals(UploadResult.class) && !parameterTypes[i].equals(UploadResult[].class))
						throw new UnsupportedOperationException("Only UploadResult or UploadResult[] can be annotated with @Uploaded in an @UploadMapping method");
					if (infos.value().isEmpty())
						throw new UnsupportedOperationException("@Uploaded annotation in an @UploadMapping method must have a non empty name (value)");
					neededUploads.put(infos.value(), new UploadInfos(parameterTypes[i].isArray(), infos));
				}
			}
			
			if (uploadMapping.onlyParamUploads() && neededUploads.isEmpty())
				throw new UnsupportedOperationException("The requested method does not have any parameter annotated with @Uploaded");
			
			final Map<String, List<UploadResult>> resultsMap = new HashMap<String, List<UploadResult>>();
			
			Iterator<Entry<String, List<BlobKey>>> it = uploads.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, List<BlobKey>> entry = it.next();
				String file = entry.getKey();
				List<BlobKey> blobKeys = entry.getValue();
				
				if (!neededUploads.containsKey(file)) {
					if (uploadMapping.onlyParamUploads()) {
						BlobstoreUtils.Delete(blobstore, blobKeys);
						it.remove();
					}
					continue ;
				}
				
				UploadInfos infos = neededUploads.get(file);

				List<UploadResult> results = checkUploaded(infos, blobKeys);
				resultsMap.put(file, results);

				if (!results.isEmpty())
					neededUploads.remove(file);
			}
			
			for (String file : neededUploads.keySet()) {
				if (neededUploads.get(file).anno.required()) {
					res.sendError(400, "Missing file " + file);
					return ;
				}
				resultsMap.put(file, Arrays.asList(new UploadResult(null, UploadResult.STATUS.ABSENT, null)));
			}

			assert injector != null;
			Object ret = injector.getInstance(MvcCaller.class).call(uploadClass, method, null, false,
				new CalltimeArgumentFetcher<Object>() {
					@Override public boolean canGet(Type type, int pos, Annotation[] annos) {
						Class<?> clazz = GenericTypeReflector.erase(type);
						return (clazz.equals(UploadResult.class) || clazz.equals(UploadResult[].class)) && Annotations.fromArray(annos).getAnnotation(Uploaded.class) != null;
					}
					@Override public @CheckForNull Object get(Type type, int pos, Annotation[] annos) {
						Uploaded infos = Annotations.fromArray(annos).getAnnotation(Uploaded.class);
						assert(infos != null);
						if (resultsMap.get(infos.value()) == null)
							return null;
						if (GenericTypeReflector.erase(type).equals(UploadResult.class))
							return resultsMap.get(infos.value()).get(0);
						return resultsMap.get(infos.value()).toArray(new UploadResult[0]);
					}
				},
				new CalltimeArgumentFetcher<Map<String, List<BlobKey>>>() {
					@Override public boolean canGet(Type type, int pos, Annotation[] annos) {
						Type searchType = new TypeToken<Map<String, List<BlobKey>>>(){}.getType();
						return searchType.equals(type);
					}
					@Override public @CheckForNull Map<String, List<BlobKey>> get(Type type, int pos, Annotation[] annos) {
						return uploads;
					}
				}
			);
			if (ret instanceof String)
				res.sendRedirect((String)ret);
		}
		catch (HandledException e) {
			BlobstoreUtils.DeleteAll(blobstore, uploads);
		}
		catch (Throwable e) {
			BlobstoreUtils.DeleteAll(blobstore, uploads);
			throw new ServletException(e);
		}
	}
}
