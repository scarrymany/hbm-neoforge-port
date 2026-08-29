package com.hbm.interfaces;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

//Universal notation for shitty code
//I know TODO is a thing, but it's nice to hover over a class and see wtf is wrong with it
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface Spaghetti {
    String value();
}
