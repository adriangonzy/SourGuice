package com.github.sourguice.appengine.upload.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.CheckForNull;
import javax.annotation.meta.TypeQualifier;

/**
 * Annotation to use on upload method argument.
 * Contains all constraints that will be applied to an uploaded file.
 * 
 * At least one argument of the method must be annotated for the method to be considered an upload method
 * 
 * This annotation can only be applied on UploadResult and on UploadResult[]
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TypeQualifier @CheckForNull
public @interface Uploaded {
	/**
	 * The name of the form parameter of the uploaded file
	 */
	String value();
	
	/**
	 * The maximum size allowed.
	 * This is different from setting the maximum size in the blobstore upload session
	 *    as this permits you to handle the display of an error when the file size is too big
	 */
	long maxSizeBytes() default -1;
	
	/**
	 * The pattern that will be applied to the content-type to verify if the file is of correct type.
	 */
	String mimeTypePattern() default ".*";
	
	/**
	 * Whether the file is required (the system will throw a 400 error if not present) or can be ommited
	 */
	boolean required() default false;
}
