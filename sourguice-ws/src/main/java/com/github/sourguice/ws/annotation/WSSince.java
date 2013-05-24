package com.github.sourguice.ws.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.CheckForNull;
import javax.annotation.meta.TypeQualifierNickname;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@TypeQualifierNickname @CheckForNull
public @interface WSSince {
	double value();
}
