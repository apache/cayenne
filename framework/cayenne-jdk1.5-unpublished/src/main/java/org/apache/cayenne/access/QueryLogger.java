/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access;

import java.lang.reflect.Array;
import java.util.List;

import org.apache.cayenne.ExtendedEnumeration;
import org.apache.cayenne.access.jdbc.ParameterBinding;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.log.CommonsJdbcEventLogger;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.util.IDUtil;

/**
 * A static wrapper around {@link JdbcEventLogger}.
 * 
 * @deprecated since 3.1 replaced by injectable {@link JdbcEventLogger}.
 */
@Deprecated
public class QueryLogger {

    private static final int TRIM_VALUES_THRESHOLD = 30;

    private static JdbcEventLogger logger = new CommonsJdbcEventLogger();

    public static void setLogger(JdbcEventLogger logger) {
        QueryLogger.logger = logger;
    }

    public static JdbcEventLogger getLogger() {
        return logger;
    }

    /**
     * Appends SQL literal for the specified object to the buffer. This is a utility
     * method and is not intended to build SQL queries, rather this is used in logging
     * routines. In particular it will trim large values to avoid flooding the logs. </p>
     * 
     * @param buffer buffer to append value
     * @param object object to be transformed to SQL literal.
     */
    public static void sqlLiteralForObject(StringBuffer buffer, Object object) {

        if (object == null) {
            buffer.append("NULL");
        }
        else if (object instanceof String) {
            buffer.append('\'');
            // lets escape quotes
            String literal = (String) object;
            if (literal.length() > TRIM_VALUES_THRESHOLD) {
                literal = literal.substring(0, TRIM_VALUES_THRESHOLD) + "...";
            }

            int curPos = 0;
            int endPos = 0;

            while ((endPos = literal.indexOf('\'', curPos)) >= 0) {
                buffer.append(literal.substring(curPos, endPos + 1)).append('\'');
                curPos = endPos + 1;
            }

            if (curPos < literal.length())
                buffer.append(literal.substring(curPos));

            buffer.append('\'');
        }
        // handle byte pretty formatting
        else if (object instanceof Byte) {
            IDUtil.appendFormattedByte(buffer, ((Byte) object).byteValue());
        }
        else if (object instanceof Number) {
            // process numeric value (do something smart in the future)
            buffer.append(object);
        }
        else if (object instanceof java.sql.Date) {
            buffer.append('\'').append(object).append('\'');
        }
        else if (object instanceof java.sql.Time) {
            buffer.append('\'').append(object).append('\'');
        }
        else if (object instanceof java.util.Date) {
            long time = ((java.util.Date) object).getTime();
            buffer.append('\'').append(new java.sql.Timestamp(time)).append('\'');
        }
        else if (object instanceof java.util.Calendar) {
            long time = ((java.util.Calendar) object).getTimeInMillis();
            buffer.append(object.getClass().getName()).append('(').append(
                    new java.sql.Timestamp(time)).append(')');
        }
        else if (object instanceof Character) {
            buffer.append(((Character) object).charValue());
        }
        else if (object instanceof Boolean) {
            buffer.append('\'').append(object).append('\'');
        }
        else if (object instanceof Enum) {
            // buffer.append(object.getClass().getName()).append(".");
            buffer.append(((Enum<?>) object).name()).append("=");
            if (object instanceof ExtendedEnumeration) {
                Object value = ((ExtendedEnumeration) object).getDatabaseValue();
                if (value instanceof String)
                    buffer.append("'");
                buffer.append(value);
                if (value instanceof String)
                    buffer.append("'");
            }
            else
                buffer.append(((Enum<?>) object).ordinal()); // FIXME -- this isn't quite
            // right
        }
        else if (object instanceof ParameterBinding) {
            sqlLiteralForObject(buffer, ((ParameterBinding) object).getValue());
        }
        else if (object.getClass().isArray()) {
            buffer.append("< ");

            int len = Array.getLength(object);
            boolean trimming = false;
            if (len > TRIM_VALUES_THRESHOLD) {
                len = TRIM_VALUES_THRESHOLD;
                trimming = true;
            }

            for (int i = 0; i < len; i++) {
                if (i > 0) {
                    buffer.append(",");
                }
                sqlLiteralForObject(buffer, Array.get(object, i));
            }

            if (trimming) {
                buffer.append("...");
            }

            buffer.append('>');
        }
        else {
            buffer.append(object.getClass().getName()).append("@").append(
                    System.identityHashCode(object));
        }

    }

    /**
     * @since 1.2 logs an arbitrary message using logging level setup for QueryLogger.
     */
    public static void log(String message) {
        logger.log(message);
    }

    /**
     * Logs database connection event using container data source.
     * 
     * @since 1.2
     */
    public static void logConnect(String dataSource) {
        logger.logConnect(dataSource);
    }

    /**
     * @since 1.2
     */
    public static void logConnect(String url, String userName, String password) {
        logger.logConnect(url, userName, password);
    }

    /**
     * Logs database connection event.
     * 
     * @since 1.2
     */
    public static void logPoolCreated(DataSourceInfo dsi) {
        logger.logPoolCreated(dsi);
    }

    /**
     * @since 1.2
     */
    public static void logConnectSuccess() {
        logger.logConnectSuccess();
    }

    /**
     * @since 1.2
     */
    public static void logConnectFailure(Throwable th) {
        logger.logConnectFailure(th);
    }

    /**
     * @since 3.0
     */
    public static void logGeneratedKey(DbAttribute attribute, Object value) {
        logger.logGeneratedKey(attribute, value);
    }

    /**
     * @since 1.2
     */
    public static void logQuery(String queryStr, List<?> params) {
        logger.logQuery(queryStr, params);
    }

    /**
     * Log query content using Log4J Category with "INFO" priority.
     * 
     * @param queryStr Query SQL string
     * @param attrs optional list of DbAttribute (can be null)
     * @param params optional list of query parameters that are used when executing query
     *            in prepared statement.
     * @since 1.2
     */
    public static void logQuery(
            String queryStr,
            List<DbAttribute> attrs,
            List<?> params,
            long time) {
        logger.logQuery(queryStr, attrs, params, time);
    }

    /**
     * @since 1.2
     */
    public static void logQueryParameters(
            String label,
            List<DbAttribute> attrs,
            List<Object> parameters,
            boolean isInserting) {
        logger.logQueryParameters(label, attrs, parameters, isInserting);
    }

    /**
     * @since 1.2
     */
    public static void logSelectCount(int count) {
        logSelectCount(count, -1);
    }

    /**
     * @since 1.2
     */
    public static void logSelectCount(int count, long time) {
        logger.logSelectCount(count, time);
    }

    /**
     * @since 1.2
     */
    public static void logUpdateCount(int count) {
        logger.logUpdateCount(count);
    }

    /**
     * @since 1.2
     */
    public static void logBeginTransaction(String transactionLabel) {
        logger.logBeginTransaction(transactionLabel);
    }

    /**
     * @since 1.2
     */
    public static void logCommitTransaction(String transactionLabel) {
        logger.logCommitTransaction(transactionLabel);
    }

    /**
     * @since 1.2
     */
    public static void logRollbackTransaction(String transactionLabel) {
        logger.logRollbackTransaction(transactionLabel);
    }

    /**
     * @since 1.2
     */
    public static void logQueryError(Throwable th) {
        logger.logQueryError(th);
    }

    /**
     * @since 1.2
     */
    public static void logQueryStart(int count) {
        if (isLoggable()) {
            String countStr = (count == 1) ? "--- will run 1 query." : "--- will run "
                    + count
                    + " queries.";
            logger.log(countStr);
        }
    }

    /**
     * Returns true if current thread default log level is high enough for QueryLogger to
     * generate output.
     * 
     * @since 1.2
     */
    public static boolean isLoggable() {
        return logger.isLoggable();
    }
}
