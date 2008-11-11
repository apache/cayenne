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
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.ExtendedEnumeration;
import org.apache.cayenne.access.jdbc.ParameterBinding;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.util.IDUtil;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * QueryLogger is intended to log special events that happen whenever Cayenne interacts
 * with a database. This includes execution of generated SQL statements, result counts,
 * connection events, etc. Normally QueryLogger methods are not invoked directly by the
 * . Rather it is a single logging point used by the framework.
 * <p>
 * Internally QueryLogger uses commons-logging at the "info" level.
 * </p>
 * 
 */
public class QueryLogger {

    private static final Log logObj = LogFactory.getLog(QueryLogger.class);

    public static final int TRIM_VALUES_THRESHOLD = 30;

    /**
     * @since 1.2
     */
    @SuppressWarnings("unchecked")
    static ThreadLocal logLevel = new ThreadLocal();

    /**
     * Appends SQL literal for the specified object to the buffer. This is a utility
     * method and is not intended to build SQL queries, rather this is used in logging
     * routines. In particular it will trim large values to avoid flooding the logs.
     * </p>
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
//            buffer.append(object.getClass().getName()).append(".");
            buffer.append(((Enum<?>) object).name()).append("=");
            if (object instanceof ExtendedEnumeration) {
                Object value = ((ExtendedEnumeration) object).getDatabaseValue();
                if (value instanceof String) buffer.append("'");
                buffer.append(value);
                if (value instanceof String) buffer.append("'");
            }
            else buffer.append(((Enum<?>) object).ordinal()); // FIXME -- this isn't quite right
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
        if (message != null) {
            logObj.info(message);
        }
    }

    /**
     * Logs database connection event using container data source.
     * 
     * @since 1.2
     */
    public static void logConnect(String dataSource) {
        if (isLoggable()) {
            logObj.info("Connecting. JNDI path: " + dataSource);
        }
    }

    /**
     * @since 1.2
     */
    public static void logConnect(String url, String userName, String password) {

        if (isLoggable()) {
            StringBuilder buf = new StringBuilder("Opening connection: ");

            // append URL on the same line to make log somewhat grep-friendly
            buf.append(url);
            buf.append("\n\tLogin: ").append(userName);
            buf.append("\n\tPassword: *******");

            logObj.info(buf.toString());
        }
    }

    /**
     * Logs database connection event.
     * 
     * @since 1.2
     */
    public static void logPoolCreated(DataSourceInfo dsi) {
        if (isLoggable()) {
            StringBuilder buf = new StringBuilder("Created connection pool: ");

            if (dsi != null) {
                // append URL on the same line to make log somewhat grep-friendly
                buf.append(dsi.getDataSourceUrl());

                if (dsi.getAdapterClassName() != null) {
                    buf.append("\n\tCayenne DbAdapter: ").append(
                            dsi.getAdapterClassName());
                }

                buf.append("\n\tDriver class: ").append(dsi.getJdbcDriver());

                if (dsi.getMinConnections() >= 0) {
                    buf.append("\n\tMin. connections in the pool: ").append(
                            dsi.getMinConnections());
                }
                if (dsi.getMaxConnections() >= 0) {
                    buf.append("\n\tMax. connections in the pool: ").append(
                            dsi.getMaxConnections());
                }
            }
            else {
                buf.append(" pool information unavailable");
            }

            logObj.info(buf.toString());
        }
    }

    /**
     * @since 1.2
     */
    public static void logConnectSuccess() {
        logObj.info("+++ Connecting: SUCCESS.");
    }

    /**
     * @since 1.2
     */
    public static void logConnectFailure(Throwable th) {
        logObj.info("*** Connecting: FAILURE.", th);
    }

    /**
     * @since 3.0
     */
    public static void logGeneratedKey(DbAttribute attribute, Object value)
    {
        if (isLoggable()) {
            String entity = attribute.getEntity().getName();
            String key    = attribute.getName();

            logObj.info("Generated PK: " + entity + "." + key + " = " + value);
        }
    }

    private static void buildLog(StringBuffer      buffer,
                                 String            prefix,
                                 String            postfix,
                                 List<DbAttribute> attributes,
                                 List<?>           parameters,
                                 boolean           isInserting)
    {
        if (parameters != null && parameters.size() > 0)
        {
            DbAttribute           attribute         = null;
            Iterator<DbAttribute> attributeIterator = null;
            int                   position          = 0;

            if (attributes != null)
                attributeIterator = attributes.iterator();

            for (Object parameter : parameters)
            {
                // If at the beginning, output the prefix, otherwise a separator.
                if (position++ == 0)
                    buffer.append(prefix);
                else
                    buffer.append(", ");

                // Find the next attribute and SKIP generated attributes.  Only
                // skip when logging inserts, though.  Should show previously
                // generated keys on DELETE, UPDATE, or SELECT.
                while (attributeIterator != null && attributeIterator.hasNext())
                {
                    attribute = attributeIterator.next();

                    if (isInserting == false || attribute.isGenerated() == false)
                        break;
                }

                buffer.append(position);

              if (attribute != null)
              {
                  buffer.append("->");
                  buffer.append(attribute.getName());
              }

              buffer.append(":");
              sqlLiteralForObject(buffer, parameter);
            }

            buffer.append(postfix);
        }
    }

    private static boolean isInserting(String query)
    {
        if (query == null || query.length() == 0)
            return false;

        char firstCharacter = query.charAt(0);
        
        if (firstCharacter == 'I' || firstCharacter == 'i')
            return true;
        else
            return false;
    }

    /**
     * @since 1.2
     */
    public static void logQuery(String queryStr, List<?> params) {
        logQuery(queryStr, null, params, -1);
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
    public static void logQuery(String queryStr, List<DbAttribute> attrs, List<?> params, long time) {
        if (isLoggable()) {
            StringBuffer buf = new StringBuffer(queryStr);
            buildLog(buf, " [bind: ", "]", attrs, params, isInserting(queryStr));

            // log preparation time only if it is something significant
            if (time > 5) {
                buf.append(" - prepared in ").append(time).append(" ms.");
            }

            logObj.info(buf.toString());
        }
    }

    /**
     * @since 1.2
     */
    public static void logQueryParameters(String           label,
                                         List<DbAttribute> attrs,
                                         List<Object>      parameters,
                                         boolean           isInserting) {
        String prefix = "[" + label + ": ";
        if (isLoggable() && parameters.size() > 0) {
            StringBuffer buf = new StringBuffer();
            buildLog(buf, prefix, "]", attrs, parameters, isInserting);
            logObj.info(buf.toString());
        }
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
        if (isLoggable()) {
            StringBuilder buf = new StringBuilder();

            if (count == 1) {
                buf.append("=== returned 1 row.");
            }
            else {
                buf.append("=== returned ").append(count).append(" rows.");
            }

            if (time >= 0) {
                buf.append(" - took ").append(time).append(" ms.");
            }

            logObj.info(buf.toString());
        }
    }

    /**
     * @since 1.2
     */
    public static void logUpdateCount(int count) {
        if (isLoggable()) {

            if (count < 0) {
                logObj.info("=== updated ? rows");
            }
            else {
                String countStr = (count == 1) ? "=== updated 1 row." : "=== updated "
                        + count
                        + " rows.";
                logObj.info(countStr);
            }
        }
    }

    /**
     * @since 1.2
     */
    public static void logBeginTransaction(String transactionLabel) {
        logObj.info("--- " + transactionLabel);
    }

    /**
     * @since 1.2
     */
    public static void logCommitTransaction(String transactionLabel) {
        logObj.info("+++ " + transactionLabel);
    }

    /**
     * @since 1.2
     */
    public static void logRollbackTransaction(String transactionLabel) {
        logObj.info("*** " + transactionLabel);
    }

    /**
     * @since 1.2
     */
    public static void logQueryError(Throwable th) {
        if (isLoggable()) {
            if (th != null) {
                th = Util.unwindException(th);
            }

            logObj.info("*** error.", th);

            if (th instanceof SQLException) {
                SQLException sqlException = ((SQLException) th).getNextException();
                while (sqlException != null) {
                    logObj.info("*** nested SQL error.", sqlException);
                    sqlException = sqlException.getNextException();
                }
            }
        }
    }

    /**
     * @since 1.2
     */
    public static void logQueryStart(int count) {
        if (isLoggable()) {
            String countStr = (count == 1) ? "--- will run 1 query." : "--- will run "
                    + count
                    + " queries.";
            logObj.info(countStr);
        }
    }

    /**
     * Returns true if current thread default log level is high enough for QueryLogger to
     * generate output.
     * 
     * @since 1.2
     */
    public static boolean isLoggable() {
        return logObj.isInfoEnabled();
    }
}
