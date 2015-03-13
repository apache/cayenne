package de.jexp.jequel.jdbc.valuehandler;

import de.jexp.jequel.jdbc.ResultSetUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

public class ValueRowProcessorInvoker {
    private final ValueRowProcessor valueRowMapper;
    private final Method mapValueMethod;
    private final int parameterCount;
    private final Class<?>[] parameterTypes;

    public ValueRowProcessorInvoker(ValueRowProcessor valueRowProcessor, ResultSet rs, String processValueMethod) throws SQLException {
        this.valueRowMapper = valueRowProcessor;
        this.mapValueMethod = getMapValueMethod(valueRowProcessor, processValueMethod);
        this.parameterCount = ResultSetUtils.checkMethodParamsMatchingResultSet(this.mapValueMethod, rs);
        this.parameterTypes = this.mapValueMethod.getParameterTypes();
    }

    protected Method getMapValueMethod(ValueRowProcessor valueRowMapper, String mapValueMethod) {
        Method[] declaredMethods = valueRowMapper.getClass().getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            if (declaredMethod.getName().equals(mapValueMethod)) {
                declaredMethod.setAccessible(true);
                return declaredMethod;
            }
        }
        throw new RuntimeException("No Method " + mapValueMethod + " in ValueRowMapper " + valueRowMapper);
    }

    protected Object processRow(ResultSet rs) throws SQLException {
        Collection<Object> params = new ArrayList<Object>();
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


