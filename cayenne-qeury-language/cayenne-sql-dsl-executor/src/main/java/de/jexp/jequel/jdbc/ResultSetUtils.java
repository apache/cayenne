package de.jexp.jequel.jdbc;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResultSetUtils {
    public static LinkedHashMap<String, String> getColumnNameTypeMapping(ResultSet rs) throws SQLException {
        ResultSetMetaData resultSetMetaData = rs.getMetaData();
        int columnCount = resultSetMetaData.getColumnCount();
        LinkedHashMap<String, String> columnNames = new LinkedHashMap<String, String>(columnCount);
        for (int col = 1; col <= columnCount; col++) {
            columnNames.put(resultSetMetaData.getColumnName(col), resultSetMetaData.getColumnClassName(col));
        }
        return columnNames;
    }

    public static String makeDbColumnName(String columnName) {
        String spreadColumnName = columnName.substring(0, 1) + columnName.substring(1).replaceAll("[A-Z]", "_$0");
        return spreadColumnName.toUpperCase();
    }

    public static String makeColumnName(Map<String, String> columnNameMapping, String columnName) {
        if (columnNameMapping.containsKey(columnName)) {
            return columnName;
        }

        String dbColumnName = makeDbColumnName(columnName);
        if (columnNameMapping.containsKey(dbColumnName)) {
            return dbColumnName;
        }

        dbColumnName = dbColumnName.toLowerCase();
        if (columnNameMapping.containsKey(dbColumnName)) {
            return dbColumnName;
        }
        throw new RuntimeException("Column not found in ResultSet: " + columnName + " or upper/lower " + dbColumnName + " in " + columnNameMapping.keySet());
    }

    public static int checkMethodParamsMatchingResultSet(Method handleResultSetMethod, ResultSet rs) throws SQLException {
        Class<?>[] parameterTypes = handleResultSetMethod.getParameterTypes();
        LinkedHashMap<String, String> columnNameTypeMap = getColumnNameTypeMapping(rs);

        int parameterCount = parameterTypes.length;
        if (parameterCount > columnNameTypeMap.size())
            throw new RuntimeException("Method " + handleResultSetMethod + " does not match ResultSet " + columnNameTypeMap);

        int paramIndex = 0;
        StringBuilder errorMessage = new StringBuilder();
        for (Map.Entry<String, String> columnNameType : columnNameTypeMap.entrySet()) {
            try {
                Class<?> columnType = Class.forName(columnNameType.getValue());
                Class<?> paramType = parameterTypes[paramIndex++];
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

    private static boolean isAssignable(Class<?> parentType, Class<?> subType) {
        return parentType.isAssignableFrom(subType) || isAssignablePrimitive(parentType, subType);
    }

    private static boolean isAssignablePrimitive(Class<?> parentType, Class<?> subType) {
        if (!parentType.isPrimitive()) {
            return false;
        }
        if (parentType.equals(boolean.class) && Boolean.class.isAssignableFrom(subType)) {
            return true;
        }
        // int.class, double.class, float.class, long.class, byte.class, short.class
        return Number.class.isAssignableFrom(subType);
    }

    // boolean, byte, char, short, int, long, float, double
    public static Class<?> convertPrimitiveTypes(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        throw new IllegalArgumentException("Class " + type + " is an unknown primitve class and not resolvable.");
    }
}
