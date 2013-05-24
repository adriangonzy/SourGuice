package com.github.sourguice.appengine.upload;

import java.io.Serializable;

import javax.annotation.CheckForNull;

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobKey;

/**
 * Represents a result of an upload
 * This is just a container for the 3 values
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
public final class UploadResult implements Serializable {
	private static final long serialVersionUID = -1265161755267975778L;

	/**
	 * Status type of an uploaded element
	 */
	public enum STATUS {
		/**
		 * The element has successfully been uploaded and matches all constraints
		 * The blobkey points to a valid blob
		 */
		OK,
		
		/**
		 * The element was bigger than the limit constraints
		 */
		TOO_BIG,
		
		/**
		 * The element content-type did not match the mime-type regex constraint
		 */
		BAD_MIME_TYPE,
		
		/**
		 * No element with the given name was present in the form
		 */
		ABSENT
	}
	
	private @CheckForNull BlobKey blobKey;
	private STATUS status;
	private @CheckForNull BlobInfo infos;

	public UploadResult(@CheckForNull BlobKey blobKey, STATUS status, @CheckForNull BlobInfo infos) {
		this.blobKey = blobKey;
		this.status = status;
		this.infos = infos;
	}

	/**
	 * The blobkey that points to the blob
	 * Will be null if the status is not OK
	 */
	public @CheckForNull BlobKey getBlobKey() {
		return blobKey;
	}

	/**
	 * The status of the upload
	 */
	public STATUS getStatus() {
		return status;
	}

	/**
	 * The infos of the uploaded blob.
	 */
	public @CheckForNull BlobInfo getInfos() {
		return infos;
	}
	
	/**
	 * Utility that transforms an array of UploadResult into an array of BlobKey
	 * This is usefull when using the Blobstore API
	 */
	public static BlobKey[] BlobKeys(UploadResult[] results) {
		BlobKey[] ret = new BlobKey[results.length];
		for (int i = 0; i < results.length; ++i)
			ret[i] = results[i].blobKey;
		return ret;
	}
}
