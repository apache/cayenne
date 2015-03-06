package de.jexp.jequel.generator;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * @author mh14 @ jexp.de
 * @since 31.10.2007 22:24:50 (c) 2007 jexp.de
 */
public class ColumnTypeHandler {
    public static Class getJavaType(final int columnType) {
        switch (columnType) {
            case Types.VARCHAR:
            case Types.CHAR:
                return String.class;
            case Types.SMALLINT:
            case Types.NUMERIC:
            case Types.DECIMAL:
                return BigDecimal.class;
            case Types.INTEGER:
                return Integer.class;
            case Types.DATE:
            case Types.TIME:
                return Date.class;
            case Types.TIMESTAMP:
                return Timestamp.class;
            case Types.BIT:
                return Boolean.class;
            default:
                return Object.class;
        }
    }

    /**
     * @deprecated
     */
    public static boolean allowedTableName(final String tableName) {
        final String forbiddenTableNameRegexp = "^(_|SYS|EX|PM|TMP|(DBA|USER|CI|SDD|ALL|RM|TMP|TEST|DWH|SAV|SAVE)_).*";
        return !tableName.contains("$") && !tableName.matches(forbiddenTableNameRegexp);
    }
}
