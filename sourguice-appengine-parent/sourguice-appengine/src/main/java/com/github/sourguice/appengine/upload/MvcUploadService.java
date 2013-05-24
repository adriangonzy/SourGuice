/**
 * Upload handling
 * Please read the documentation to understand how to use this system
 */
package com.github.sourguice.appengine.upload;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

import javax.annotation.CheckForNull;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.sourguice.appengine.upload.internal.BaseUploadServlet;
import com.github.sourguice.appengine.upload.internal.DevUploadServlet;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.blobstore.UploadOptions;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

@Singleton
public final class MvcUploadService extends BaseUploadServlet {
	private static final long serialVersionUID = -4810920951685124146L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		if (injector == null) {
			// This situation should only arise on DEV server !
			// The resolution is certainly NOT optimal but since production GAE servers should never provoque this,
			// this way of solving the problem is not a real problem...
			// It basically saves the uploaded blob keys into memcache and then forward to the same URL
			// Thanks to this forward, Guice will take control of the new request.
			// We will then get back the uploaded blob keys from memcache and process them.

			String key = new BigInteger(64, new Random()).toString(32);
			MemcacheServiceFactory.getMemcacheService().put(DevUploadServlet.MEMCACHE_DEV_BLOBS_ + key, BlobstoreServiceFactory.getBlobstoreService().getUploads(req));
			MemcacheServiceFactory.getMemcacheService().put(DevUploadServlet.MEMCACHE_DEV_PARAMS_ + key, req.getParameterMap());
			String qr = req.getQueryString() + "&__K=" + key;
			res.sendRedirect("/__upload_dev?" + qr);
			return ;
		}

		handleUploads(BlobstoreServiceFactory.getBlobstoreService().getUploads(req), req, res);
	}

	public static String createUploadUrl(Class<?> clazz, String method, @CheckForNull UploadOptions opts, @CheckForNull String getParams) {
		
		String url = "/__upload"
				+	"?__C=" + clazz.getCanonicalName()
				+	"&__M=" + method
				+	(getParams != null ? "&" + getParams : "");
		
		if (opts != null)
			return BlobstoreServiceFactory.getBlobstoreService().createUploadUrl(url, opts);
		else
			return BlobstoreServiceFactory.getBlobstoreService().createUploadUrl(url);
	}

	
	public static String createUploadUrl(Class<?> clazz, String method, UploadOptions opts) {
		return createUploadUrl(clazz, method, opts, null);
	}

	public static String createUploadUrl(Class<?> clazz, String method, String getParams) {
		return createUploadUrl(clazz, method, null, getParams);
	}

	public static String createUploadUrl(Class<?> clazz, String method) {
		return createUploadUrl(clazz, method, null, null);
	}
}
