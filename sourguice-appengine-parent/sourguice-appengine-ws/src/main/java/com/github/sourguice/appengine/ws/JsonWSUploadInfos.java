package com.github.sourguice.appengine.ws;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;

import com.github.sourguice.appengine.upload.UploadResult;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
public class JsonWSUploadInfos {

	static class Maps {
		Map<String, List<BlobKey>> blobKeys;
		Map<String, List<UploadResult>> results = new HashMap<String, List<UploadResult>>();
		Map<String, List<String>> match = new HashMap<String, List<String>>();
		
		public Maps(Map<String, List<BlobKey>> blobKeys) {
			this.blobKeys = blobKeys;
		}
		
		void putInResults(String key, List<UploadResult> list, String name) {
			results.put(key, list);
			
			List<String> matchList = match.get(name);
			if (matchList == null) {
				matchList = new LinkedList<String>();
				match.put(name, matchList);
			}
			matchList.add(key);
		}
		
	}
	@CheckForNull Maps maps = null;

	public boolean isAnUploadRequest() {
		return maps != null;
	}
	
	public void deleteIfExtraUpload(Collection<String> potentialKeys) {
		if (maps == null)
			return ;
		Iterator<List<UploadResult>> mapIt = maps.results.values().iterator();
		while (mapIt.hasNext()) {
			List<UploadResult> list = mapIt.next();
			Iterator<UploadResult> listIt = list.iterator();
			while (listIt.hasNext()) {
				UploadResult result = listIt.next();
				BlobKey bk = result.getBlobKey();
				if (bk != null && potentialKeys.contains(bk.getKeyString()))
					listIt.remove();
			}
			if (list.isEmpty())
				mapIt.remove();
		}
	}

	public @CheckForNull List<List<UploadResult>> getRequestUploads(String name) {
		if (maps == null)
			return null;
		List<String> matches = maps.match.get(name);
		if (matches == null)
			return null;
		LinkedList<List<UploadResult>> ret = new LinkedList<List<UploadResult>>();
		for (String key : matches) {
			List<UploadResult> list = maps.results.get(key);
			if (list != null)
				ret.add(Collections.unmodifiableList(list));
		}
		return ret;
	}
	
	public boolean hasBeenUploaded(String name) {
		if (maps == null)
			return false;
		return maps.match.containsKey(name);
	}
	
}
