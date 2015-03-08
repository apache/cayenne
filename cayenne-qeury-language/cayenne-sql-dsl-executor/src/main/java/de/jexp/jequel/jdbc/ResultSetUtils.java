package de.jexp.jequel.jdbc;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author mh14 @ jexp.de
 * @since 02.11.2007 22:01:19 (c) 2007 jexp.de
 */
public class ResultSetUtils {
    public static LinkedHashMap<String, String> getColumnNameTypeMapping(final ResultSet rs) throws SQLException {
        final ResultSetMetaData resultSetMetaData = rs.getMetaData();
        final int columnCount = resultSetMetaData.getColumnCount();
        final LinkedHashMap<String, String> columnNames = new LinkedHashMap<String, String>(columnCount);
        for (int col = 1; col <= columnCount; col++) {
            columnNames.put(resultSetMetaData.getColumnName(col), resultSetMetaData.getColumnClassName(col));
        }
        return columnNames;
    }

    public static String makeDbColumnName(final String columnName) {
        final String spreadColumnName = columnName.substring(0, 1) + columnName.substring(1).replaceAll("[A-Z]", "_$0");
        return spreadColumnName.toUpperCase();
    }

    public static String makeColumnName(final Map<String, String> columnNameMapping, final String columnName) {
        if (columnNameMapping.containsKey(columnName)) return columnName;
        String dbColumnName = makeDbColumnName(columnName);
        if (columnNameMapping.containsKey(dbColumnName)) return dbColumnName;
        dbColumnName = dbColumnName.toLowerCase();
        if (columnNameMapping.containsKey(dbColumnName)) return dbColumnName;
        throw new RuntimeException("Column not found in ResultSet: " + columnName + " or upper/lower " + dbColumnName + " in " + columnNameMapping.keySet());
    }

    public static int checkMethodParamsMatchingResultSet(final Method handleResultSetMethod, final ResultSet rs) throws SQLException {
        final Class<?>[] parameterTypes = handleResultSetMethod.getParameterTypes();
        final LinkedHashMap<String, String> columnNameTypeMap = getColumnNameTypeMapping(rs);

        final int parameterCount = parameterTypes.length;
        if (parameterCount > columnNameTypeMap.size())
            throw new RuntimeException("Method " + handleResultSetMethod + " does not match ResultSet " + columnNameTypeMap);

        int paramIndex = 0;
        final StringBuilder errorMessage = new StringBuilder();
        for (final Map.Entry<String, String> columnNameType : columnNameTypeMap.entrySet()) {
            try {
                final Class<?> columnType = Class.forName(columnNameType.getValue());
                final Class<?> paramType = parameterTypes[paramIndex++];
                if (!isAssignable(paramType, columnType)) {
                    errorMessage.append(String.format("Type mismatch for column %s ResultSetType %s MethodType %s",
                            columnNameType.getKey(), columnType.getName(), paramType.getName()));
                }
            } catch (ClassNotFoundException e) {
                errorMessage.append(String.format(" Class not found: %s ", columnNameType.getValue()));
            }
        }

        if (errorMessage.length() > 0)
            throw new RuntimeException("Method " + handleResultSetMethod + " does not match ResultSet " + columnNameTypeMap + "\n Errors: " + errorMessage);
        return parameterCount;
    }

    private static boolean isAssignable(final Class<?> parentType, final Class<?> subType) {
        return parentType.isAssignableFrom(subType) || isAssignablePrimitive(parentType, subType);
    }

    private static boolean isAssignablePrimitive(final Class<?> parentType, final Class<?> subType) {
        if (!parentType.isPrimitive()) return false;
        if (parentType.equals(boolean.class) && Boolean.class.isAssignableFrom(subType)) return true;
        // int.class, double.class, float.class, long.class, byte.class, short.class
        return Number.class.isAssignableFrom(subType);
    }

    // boolean, byte, char, short, int, long, float, double
    public static Class<?> convertPrimitiveTypes(final Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == boolean.class) return Boolean.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        if (type == float.class) return Float.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        throw new IllegalArgumentException("Class " + type + " is an unknown primitve class and not resolvable.");
    }
}
