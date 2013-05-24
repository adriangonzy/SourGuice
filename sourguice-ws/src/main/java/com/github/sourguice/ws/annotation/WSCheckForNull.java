package com.github.sourguice.ws.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.CheckForNull;
import javax.annotation.meta.TypeQualifierNickname;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@TypeQualifierNickname @CheckForNull
public @interface WSCheckForNull {
}
