package com.github.sourguice.appengine.ws;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.github.sourguice.ws.annotation.WSMethod;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@WSMethod
public @interface WSUploadMethod {
	long maxUploadSizeBytes() default -1;
	long maxUploadSizeBytesPerBlob() default -1;
}
