package com.yu.annotation;


import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})  //加在指定的类型上
@Retention(RetentionPolicy.RUNTIME)  //保持到运行时
@Documented
public @interface MyRequestMapping {

    String value() default "";

}
