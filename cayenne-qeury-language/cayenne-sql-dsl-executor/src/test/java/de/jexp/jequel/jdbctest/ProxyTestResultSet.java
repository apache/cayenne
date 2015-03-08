package de.jexp.jequel.jdbctest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.util.Arrays;

/**
 * @author mh14 @ jexp.de
 * @since 03.11.2007 08:13:24 (c) 2007 jexp.de
 */
public class ProxyTestResultSet extends ResultSetWrapper implements InvocationHandler {

    public ProxyTestResultSet(final ResultSetData resultSetData) {
        super(resultSetData);
    }

    public static ResultSet createTestResultSet(final Iterable data, final String... columns) {
        final ClassLoader classLoader = ProxyTestResultSet.class.getClassLoader();
        return (ResultSet) Proxy.newProxyInstance(classLoader, new Class[]{ResultSet.class},
                new ProxyTestResultSet(new IterableResultSetData(data, columns)));
    }

    public Object invoke(final Object o, final Method method, final Object[] params) throws Throwable {
        if (ResultSet.class.isAssignableFrom(method.getDeclaringClass())) {
            final String methodName = method.getName();
            if (methodName.equals("next")) {
                return next();
            }
            if (methodName.equals("wasNull")) {
                return false;
            }

            if (methodName.startsWith("get")) {
                if (methodName.equals("getMetaData")) {
                    return getMetaData();
                }
                final Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes != null && paramTypes.length == 1) {
                    final Class<?> paramType = paramTypes[0];
                    if (paramType.equals(String.class)) {
                        return get(method.getReturnType(), (String) params[0]);
                    }
                    if (paramType.equals(int.class)) {
                        return get(method.getReturnType(), (Integer) params[0]);
                    }
                }
            }
        }
        // TODO Object.*-Stuff
        throw new UnsupportedOperationException(method.getDeclaringClass() + "." + method + " " + (params != null ? Arrays.asList(params).toString() : ""));
    }

}
