package com.hbm.interfaces;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documentation-only marker for a class whose methods are known to funnel checked exceptions
 * (e.g. via {@link java.lang.invoke.MethodHandle#invokeExact}, which is spec'd to allow throwing
 * any {@link Throwable} without a compile-time checked-exception declaration) through unchecked
 * paths deliberately, rather than by omission. Carries no runtime behavior - source-only so it
 * has zero classfile/reflection footprint.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface SuppressCheckedExceptions {
}
