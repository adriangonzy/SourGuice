package com.github.sourguice.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.Nonnull;
import javax.annotation.Tainted;
import javax.annotation.meta.TypeQualifierDefault;

/**
 * Annotation only used for development.
 * It says FindBugs to set @Nonnull and @Tainted by default to every thing
 * This must be applied on EVERY package of SourGuice to ensure good code quality
 * 
 * @author Salomon BRYS <salomon.brys@gmail.com>
 */
@Documented
@Nonnull
@Tainted
@TypeQualifierDefault({ ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD, ElementType.LOCAL_VARIABLE })
@Retention(RetentionPolicy.RUNTIME)
public @interface EverythingIsNonnullAndTaintedByDefault {

}
