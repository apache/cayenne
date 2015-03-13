package de.jexp.jequel.jdbc.beanprocessor;

import de.jexp.jequel.jdbc.ResultSetUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class ResultSetBeanWrapper<I> implements InvocationHandler {
    private static final String HANDLE_BEAN_METHOD = "handleBean";

    private final ResultSet rs;
    private final Class<I> beanType;
    private final Map<String, String> columnNameMapping;

    public ResultSetBeanWrapper(ResultSet rs, Class<I> beanType) throws SQLException {
        this.rs = rs;
        this.beanType = beanType;
        columnNameMapping = ResultSetUtils.getColumnNameTypeMapping(rs);
    }

    public static <I, O> I createBeanMapperWrapper(ResultSet rs, BeanRowMapper<I, O> beanRowMapper) throws SQLException {
        return createBeanWrapper(rs, beanRowMapper, BeanRowMapper.MAP_BEAN_METHOD);
    }

    public static <I> I createBeanHandlerWrapper(ResultSet rs, BeanRowHandler<I> beanRowHandler) throws SQLException {
        return createBeanWrapper(rs, beanRowHandler, HANDLE_BEAN_METHOD);
    }

    public static <I> I createBeanWrapper(ResultSet rs, BeanRowProcessor<I> beanRowProcessor, String mapBeanMethod) throws SQLException {
        Class<I> beanType = getBeanType(beanRowProcessor, mapBeanMethod);
        return (I) Proxy.newProxyInstance(beanRowProcessor.getClass().getClassLoader(),
                new Class[]{beanType},
                new ResultSetBeanWrapper<I>(rs, beanType)
        );
    }

    private static <I> Class<I> getBeanType(BeanRowProcessor<I> beanRowMapper, String mapBeanMethod) {
        Class<? extends BeanRowProcessor> beanRowMapperClass = beanRowMapper.getClass();
        for (Method method : beanRowMapperClass.getDeclaredMethods()) {
            if (method.getName().equals(mapBeanMethod)) {
                return (Class<I>) method.getParameterTypes()[0];
            }
        }
        throw new RuntimeException("No Method mapBean in Class " + beanRowMapper);
    }

    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        if (beanType.isAssignableFrom(method.getDeclaringClass())) {
            String methodName = method.getName();
            if (methodName.startsWith("get") && !method.getReturnType().equals(Void.class)) {
                String columnName = ResultSetUtils.makeColumnName(columnNameMapping, methodName.substring(3));
                return rs.getObject(columnName);
            }
        }
        return null;
    }
}
