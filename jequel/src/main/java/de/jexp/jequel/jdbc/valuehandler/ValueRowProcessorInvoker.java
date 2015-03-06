package de.jexp.jequel.jdbc.valuehandler;

import de.jexp.jequel.jdbc.ResultSetUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author mh14 @ jexp.de
 * @since 02.11.2007 21:56:20 (c) 2007 jexp.de
 */
public class ValueRowProcessorInvoker {
    private final ValueRowProcessor valueRowMapper;
    private final Method mapValueMethod;
    private int parameterCount;
    private final Class<?>[] parameterTypes;

    public ValueRowProcessorInvoker(final ValueRowProcessor valueRowProcessor, final ResultSet rs, final String processValueMethod) throws SQLException {
        this.valueRowMapper = valueRowProcessor;
        this.mapValueMethod = getMapValueMethod(valueRowProcessor, processValueMethod);
        this.parameterCount = ResultSetUtils.checkMethodParamsMatchingResultSet(this.mapValueMethod, rs);
        this.parameterTypes = this.mapValueMethod.getParameterTypes();
    }

    protected Method getMapValueMethod(final ValueRowProcessor valueRowMapper, final String mapValueMethod) {
        final Method[] declaredMethods = valueRowMapper.getClass().getDeclaredMethods();
        for (final Method declaredMethod : declaredMethods) {
            if (declaredMethod.getName().equals(mapValueMethod)) {
                declaredMethod.setAccessible(true);
                return declaredMethod;
            }
        }
        throw new RuntimeException("No Method " + mapValueMethod + " in ValueRowMapper " + valueRowMapper);
    }

    protected Object processRow(final ResultSet rs) throws SQLException {
        final Collection<Object> params = new ArrayList<Object>();
        for (int col = 1; col <= parameterCount; col++) {
            params.add(ResultSetValueConverter.getConvertedValue(rs, col, ResultSetUtils.convertPrimitiveTypes(parameterTypes[col - 1])));
        }
        try {
            return mapValueMethod.invoke(valueRowMapper, params.toArray());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error accessing Method " + mapValueMethod, e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error calling Method " + mapValueMethod, e.getTargetException());
        }
    }
}


