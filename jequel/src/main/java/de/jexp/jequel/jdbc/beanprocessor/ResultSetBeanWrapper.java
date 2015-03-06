package de.jexp.jequel.jdbc.beanprocessor;

import de.jexp.jequel.jdbc.ResultSetUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author mh14 @ jexp.de
 * @since 02.11.2007 20:40:34 (c) 2007 jexp.de
 */
@SuppressWarnings({"unchecked"})
public class ResultSetBeanWrapper<I> implements InvocationHandler {
    private final ResultSet rs;
    private final Class<I> beanType;
    private final Map<String, String> columnNameMapping;

    public ResultSetBeanWrapper(final ResultSet rs, final Class<I> beanType) throws SQLException {
        this.rs = rs;
        this.beanType = beanType;
        columnNameMapping = ResultSetUtils.getColumnNameTypeMapping(rs);
    }

    public static <I, O> I createBeanMapperWrapper(final ResultSet rs, final BeanRowMapper<I, O> beanRowMapper) throws SQLException {
        return createBeanWrapper(rs, beanRowMapper, BeanRowMapper.MAP_BEAN_METHOD);
    }

    public static <I> I createBeanHandlerWrapper(final ResultSet rs, final BeanRowHandler<I> beanRowHandler) throws SQLException {
        return createBeanWrapper(rs, beanRowHandler, BeanRowHandler.HANDLE_BEAN_METHOD);
    }

    public static <I> I createBeanWrapper(final ResultSet rs, final BeanRowProcessor<I> beanRowProcessor, final String mapBeanMethod) throws SQLException {
        final Class<I> beanType = getBeanType(beanRowProcessor, mapBeanMethod);
        return (I) Proxy.newProxyInstance(beanRowProcessor.getClass().getClassLoader(),
                new Class[]{beanType},
                new ResultSetBeanWrapper<I>(rs, beanType)
        );
    }

    private static <I> Class<I> getBeanType(final BeanRowProcessor<I> beanRowMapper, final String mapBeanMethod) {
        final Class<? extends BeanRowProcessor> beanRowMapperClass = beanRowMapper.getClass();
        for (final Method method : beanRowMapperClass.getDeclaredMethods()) {
            if (method.getName().equals(mapBeanMethod)) {
                return (Class<I>) method.getParameterTypes()[0];
            }
        }
        throw new RuntimeException("No Method mapBean in Class " + beanRowMapper);
    }

    public Object invoke(final Object o, final Method method, final Object[] objects) throws Throwable {
        if (beanType.isAssignableFrom(method.getDeclaringClass())) {
            final String methodName = method.getName();
            if (methodName.startsWith("get") && !method.getReturnType().equals(Void.class)) {
                final String columnName = ResultSetUtils.makeColumnName(columnNameMapping, methodName.substring(3));
                return rs.getObject(columnName);
            }
        }
        return null;
    }
}
