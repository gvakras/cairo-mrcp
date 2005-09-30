/**
 * 
 */
package com.onomatopia.util;

import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.MethodInvocation;

/**
 * @author Niels
 * 
 */
public class DebugInterceptor implements Interceptor {

    public String getName() {
        return "DebugInterceptor";
    }

    /*public Object invoke(Invocation invocation) throws Throwable {
        if (invocation instanceof MethodInvocation) {
            return invoke((MethodInvocation) invocation);
        }
        return invocation.invokeNext();
    }*/

    public Object invoke(Invocation invocation) throws Throwable {
        try {
            String name = invocation.getClass().getName();
            StringBuilder sb = new StringBuilder(">>> Entering DebugInterceptor type: ");
            sb.append(name);//.append('.');
            //sb.append(invocation.getMethod().getName()).append("()");
            System.err.println(sb.toString());
            return invocation.invokeNext();
        } finally {
            System.err.println("<<< Leaving DebugInterceptor");
        }
    }
}
