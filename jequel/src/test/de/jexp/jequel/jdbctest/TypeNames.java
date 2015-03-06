package de.jexp.jequel.jdbctest;

import java.lang.reflect.Field;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mh14 @ jexp.de
 * @since 03.11.2007 13:32:16 (c) 2007 jexp.de
 */
public abstract class TypeNames {
    public static final Map<Integer, String> JAVA_SQL_TYPES = getJavaSqlTypes();

    public static String getTypeName(final int type) {
        return JAVA_SQL_TYPES.get(type);
    }

    private static Map<Integer, String> getJavaSqlTypes() {

        final Map<Integer, String> javaSqlTypes = new HashMap<Integer, String>();
        final Field[] staticFields = Types.class.getDeclaredFields();
        for (final Field field : staticFields) {
            try {
                final String fieldName = field.getName();
                final Integer fieldValue = (Integer) field.get(null);
                javaSqlTypes.put(fieldValue, fieldName);
            }
            catch (final SecurityException e) {
                // ignore
            }
            catch (final IllegalAccessException e) {
                // ignore
            }
        }

        return Collections.unmodifiableMap(javaSqlTypes);
    }

    public static int getTypeForClass(final Class columnClass) {
        return 0;
    }
}
