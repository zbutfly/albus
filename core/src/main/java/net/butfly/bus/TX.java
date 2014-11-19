package net.butfly.bus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TX {
	static String INIT_VERSION = "1.0.0";
	static String ALL_VERSION = "*";

	public String value();

	public String version() default INIT_VERSION;
}
