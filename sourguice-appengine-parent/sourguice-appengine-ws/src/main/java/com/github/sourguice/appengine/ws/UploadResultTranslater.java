package com.github.sourguice.appengine.ws;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckForNull;

import com.github.sourguice.appengine.upload.UploadResult;
import com.github.sourguice.ws.translat.WSSimpleGuiceTranslaterFactory;
import com.github.sourguice.ws.translat.WSTranslater;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.inject.Singleton;
import com.googlecode.gentyref.TypeToken;

public class UploadResultTranslater implements WSTranslater<UploadResult, Map<String, String>> {
	
	@Singleton
	public static class UploadResultTranslaterFactory extends WSSimpleGuiceTranslaterFactory<UploadResult, Map<String, String>> {
		public UploadResultTranslaterFactory() {
			super(UploadResult.class, new TypeToken<Map<String, String>>(){}.getType(), UploadResultTranslater.class);
		}
	}

	@Override
	public @CheckForNull Map<String, String> toWS(UploadResult obj, Type typeOnServer, AnnotatedElement el, @CheckForNull Object enclosing) {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("status", obj.getStatus().toString());
		BlobKey blobKey = obj.getBlobKey();
		if (blobKey != null)
			map.put("blobKey", blobKey.getKeyString());
		return map;
	}

	@Override
	public @CheckForNull UploadResult fromWS(Map<String, String> map, Type typeOnServer, AnnotatedElement el, @CheckForNull Object enclosing) {
		throw new UnsupportedOperationException();
	}
}
