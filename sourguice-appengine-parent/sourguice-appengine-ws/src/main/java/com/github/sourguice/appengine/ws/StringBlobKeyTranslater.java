package com.github.sourguice.appengine.ws;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.List;

import javax.annotation.CheckForNull;

import com.github.sourguice.appengine.upload.UploadResult;
import com.github.sourguice.appengine.upload.annotation.Uploaded;
import com.github.sourguice.appengine.upload.internal.BaseUploadServlet;
import com.github.sourguice.appengine.upload.internal.BaseUploadServlet.UploadInfos;
import com.github.sourguice.ws.translat.WSSimpleGuiceTranslaterFactory;
import com.github.sourguice.ws.translat.WSTranslater;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// TODO: Handle multiple upload
public class StringBlobKeyTranslater implements WSTranslater<String, String> {
	
	@Singleton
	public static class StringBlobKeyTranslaterFactory extends WSSimpleGuiceTranslaterFactory<String, String> {
		public StringBlobKeyTranslaterFactory() {
			super(String.class, String.class, StringBlobKeyTranslater.class);
		}
	}

	@Override
	public @CheckForNull String toWS(String str, Type typeOnServer, AnnotatedElement el, Object enclosing) {
		return str;
	}

	private JsonWSUploadInfos infos;

	@Inject
	public StringBlobKeyTranslater(JsonWSUploadInfos infos) {
		this.infos = infos;
	}

	@Override
	public @CheckForNull String fromWS(final String str, Type typeOnServer, AnnotatedElement el, Object enclosing) {
		if (infos.maps == null)
			return str;

		if (!str.startsWith("__UP:"))
			return str;
		
		Uploaded annotation = el.getAnnotation(Uploaded.class);
		if (annotation == null)
			return str;

		String key = str.substring(5);
		if (!infos.maps.blobKeys.containsKey(key))
			return str;

		List<UploadResult> r = infos.maps.results.get(key);
		if (r != null && r.get(0).getStatus() != UploadResult.STATUS.OK)
			return null;
		
		r = BaseUploadServlet.checkUploaded(new UploadInfos(false, annotation), infos.maps.blobKeys.get(key));
		infos.maps.putInResults(key, r, annotation.value());

		BlobKey blobKey = r.get(0).getBlobKey();
		if (blobKey == null)
			return null;
		
		return blobKey.getKeyString();
	}
}
