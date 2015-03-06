package de.jexp.jequel.jdbc.valuehandler;

import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.jdbc.core.SingleColumnRowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mh14 @ jexp.de
 * @since 02.11.2007 23:24:42 (c) 2007 jexp.de
 */
public class ResultSetValueConverter<T> extends SingleColumnRowMapper {
    private final static Map<Class<?>, ResultSetValueConverter<?>> instances = new HashMap<Class<?>, ResultSetValueConverter<?>>();

    public ResultSetValueConverter(final Class<T> requiredType) {
        super(requiredType);
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T getConvertedValue(final ResultSet rs, final int columnIndex, final Class<T> requiredType) throws SQLException {
        final ResultSetValueConverter resultSetValueConverter = getInstance(requiredType);
        // Extract column value from JDBC ResultSet
        final Object result = resultSetValueConverter.getColumnValue(rs, columnIndex, requiredType);
        if (result != null && requiredType != null && !requiredType.isInstance(result)) {
            // Extracted value does not match already: try to convert it.
            try {
                return (T) resultSetValueConverter.convertValueToRequiredType(result, requiredType);
            }
            catch (IllegalArgumentException ex) {
                throw new TypeMismatchDataAccessException("Type mismatch affecting column type " + result.getClass() +
                        " required " + requiredType + " Error: " + ex.getMessage());
            }
        }
        return (T) result;
    }

    private synchronized static <T> ResultSetValueConverter getInstance(final Class<T> requiredType) {
        final ResultSetValueConverter<T> resultSetValueConverter = (ResultSetValueConverter<T>) instances.get(requiredType);
        if (resultSetValueConverter != null) return resultSetValueConverter;
        final ResultSetValueConverter<T> newResultSetValueConverter = new ResultSetValueConverter<T>(requiredType);
        instances.put(requiredType, newResultSetValueConverter);
        return newResultSetValueConverter;
    }
}
