package com.github.sourguice.appengine.upload.internal;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.sourguice.call.MvcCaller;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

@SuppressWarnings({"serial", "unchecked"})
@Singleton
public final class DevUploadServlet extends BaseUploadServlet {

	public static final String MEMCACHE_DEV_PARAMS_ = "com.github.sourguice.appengine.upload.internal.MEMCACHE_DEV_PARAMS_";
	public static final String MEMCACHE_DEV_BLOBS_ = "com.github.sourguice.appengine.upload.internal.MEMCACHE_DEV_BLOBS_";
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		String key = MEMCACHE_DEV_BLOBS_ + req.getParameter("__K");
		Map<String, List<BlobKey>> uploads = (Map<String, List<BlobKey>>)MemcacheServiceFactory.getMemcacheService().get(key);
		MemcacheServiceFactory.getMemcacheService().delete(key);
		if (uploads == null) {
			res.sendError(400, "No uploads registered");
			return ;
		}

		assert injector != null;
		injector.getInstance(MvcCaller.class).setReq(req);
		handleUploads(uploads, req, res);
	}
}
