package com.github.sourguice.appengine.ws;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.List;

import javax.annotation.CheckForNull;

import com.github.sourguice.appengine.upload.UploadResult;
import com.github.sourguice.appengine.upload.annotation.Uploaded;
import com.github.sourguice.appengine.upload.internal.BaseUploadServlet;
import com.github.sourguice.appengine.upload.internal.BaseUploadServlet.UploadInfos;
import com.github.sourguice.ws.exception.WSRuntimeException;
import com.github.sourguice.ws.translat.WSSimpleGuiceTranslaterFactory;
import com.github.sourguice.ws.translat.WSTranslater;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// TODO: Handle multiple upload
public class BlobKeyTranslater implements WSTranslater<BlobKey, String> {
	
	@Singleton
	public static class BlobKeyTranslaterFactory extends WSSimpleGuiceTranslaterFactory<BlobKey, String> {
		public BlobKeyTranslaterFactory() {
			super(BlobKey.class, String.class, BlobKeyTranslater.class);
		}
	}

	@Override
	public @CheckForNull String toWS(BlobKey obj, Type typeOnServer, AnnotatedElement el, @CheckForNull Object enclosing) {
		return obj.getKeyString();
	}

	private JsonWSUploadInfos infos;

	@Inject
	public BlobKeyTranslater(JsonWSUploadInfos infos) {
		this.infos = infos;
	}

	@Override
	public @CheckForNull BlobKey fromWS(String key, Type typeOnServer, AnnotatedElement el, @CheckForNull Object enclosing) {
		
		if (!key.startsWith("__UP:") || infos.maps == null) {
			BlobKey bk = new BlobKey(key);
			try {
				if (new BlobInfoFactory().loadBlobInfo(bk) == null)
					throw new WSRuntimeException("BAD_BLOB_KEY");
			}
			catch (IllegalArgumentException e) {
				throw new WSRuntimeException("BAD_BLOB_KEY");
			}
			return bk;
		}

		Uploaded annotation = el.getAnnotation(Uploaded.class);
		if (annotation == null)
			return null;

		key = key.substring(5);
		if (!infos.maps.blobKeys.containsKey(key))
			return null;

		List<UploadResult> r = infos.maps.results.get(key);
		if (r != null && r.get(0).getStatus() != UploadResult.STATUS.OK)
			return null;
		
		r = BaseUploadServlet.checkUploaded(new UploadInfos(false, annotation), infos.maps.blobKeys.get(key));
		infos.maps.putInResults(key, r, annotation.value());

		return r.get(0).getBlobKey();
	}
}
