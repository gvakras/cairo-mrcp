/**
 * 
 */
package com.onomatopia.util;

import java.lang.reflect.Method;

import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.reflection.ReflectionAspect;

/**
 * @author Niels
 *
 */
public class ReflectInterceptor extends ReflectionAspect {

    @Override
    protected Object interceptMethod(Invocation invocation,
            Method method,
            Object instance,
            Object[] args)
     throws Throwable {
        System.out.println("<<< ReflectInterceptor.interceptMethod()");
        return super.interceptMethod(invocation, method, instance, args);
    }

}
