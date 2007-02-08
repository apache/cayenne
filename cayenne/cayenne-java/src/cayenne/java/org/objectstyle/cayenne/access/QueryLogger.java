/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.jdbc.ParameterBinding;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.util.IDUtil;
import org.objectstyle.cayenne.util.Util;

/**
 * QueryLogger is intended to log special events that happen whenever Cayenne interacts
 * with a database. This includes execution of generated SQL statements, result counts,
 * connection events, etc. Normally QueryLogger methods are not invoked directly by the
 * user. Rather it is a single logging point used by the framework.
 * <p>
 * Internally QueryLogger uses Log4J. See a chapter on logging in Cayenne User Guide on
 * how to setup Log4J.
 * </p>
 * 
 * @author Andrus Adamchik
 */
public class QueryLogger {

    private static final Logger logObj = Logger.getLogger(QueryLogger.class);

    /**
     * @deprecated unused since 1.2
     */
    public static final Level DEFAULT_LOG_LEVEL = Level.INFO;

    public static final int TRIM_VALUES_THRESHOLD = 300;

    /**
     * @since 1.2
     */
    static ThreadLocal logLevel = new ThreadLocal();

    /**
     * Utility method that appends SQL literal for the specified object to the buffer.
     * <p>
     * Note: this method is not intended to build SQL queries, rather this is used in
     * logging routines only. In particular it will trim large values to avoid flooding
     * the logs.
     * </p>
     * 
     * @param buf buffer to append value
     * @param anObject object to be transformed to SQL literal.
     */
    public static void sqlLiteralForObject(StringBuffer buf, Object anObject) {
        // 0. Null
        if (anObject == null) {
            buf.append("NULL");
        }
        // 1. String literal
        else if (anObject instanceof String) {
            buf.append('\'');
            // lets escape quotes
            String literal = (String) anObject;
            if (literal.length() > TRIM_VALUES_THRESHOLD) {
                literal = literal.substring(0, TRIM_VALUES_THRESHOLD) + "...";
            }

            int curPos = 0;
            int endPos = 0;

            while ((endPos = literal.indexOf('\'', curPos)) >= 0) {
                buf.append(literal.substring(curPos, endPos + 1)).append('\'');
                curPos = endPos + 1;
            }

            if (curPos < literal.length())
                buf.append(literal.substring(curPos));

            buf.append('\'');
        }
        // 2. Numeric literal
        else if (anObject instanceof Number) {
            // process numeric value (do something smart in the future)
            buf.append(anObject);
        }
        // 3. Date
        else if (anObject instanceof java.sql.Date) {
            buf.append('\'').append(anObject).append('\'');
        }
        // 4. Date
        else if (anObject instanceof java.sql.Time) {
            buf.append('\'').append(anObject).append('\'');
        }
        // 5 Date
        else if (anObject instanceof java.util.Date) {
            long time = ((java.util.Date) anObject).getTime();
            buf.append('\'').append(new java.sql.Timestamp(time)).append('\'');
        }
        // 6. byte[]
        else if (anObject instanceof byte[]) {
            buf.append("< ");
            byte[] b = (byte[]) anObject;

            int len = b.length;
            boolean trimming = false;
            if (len > TRIM_VALUES_THRESHOLD) {
                len = TRIM_VALUES_THRESHOLD;
                trimming = true;
            }

            for (int i = 0; i < len; i++) {
                IDUtil.appendFormattedByte(buf, b[i]);
                buf.append(' ');
            }

            if (trimming) {
                buf.append("...");
            }
            buf.append('>');
        }
        else if (anObject instanceof Boolean) {
            buf.append('\'').append(anObject).append('\'');

        }
        else if (anObject instanceof ParameterBinding) {
            sqlLiteralForObject(buf, ((ParameterBinding) anObject).getValue());
        }
        else {
            // unknown
            buf.append("[").append(anObject.getClass().getName()).append(": ").append(
                    anObject).append("]");
        }
    }

    /**
     * Prints a byte value to a StringBuffer as a double digit hex value.
     * 
     * @deprecated since 1.2 use a namesake method from IDUtil.
     */
    protected static void appendFormattedByte(StringBuffer buffer, byte byteValue) {
        IDUtil.appendFormattedByte(buffer, byteValue);
    }

    /**
     * Returns current logging level.
     */
    public static Level getLoggingLevel() {
        Level level = (Level) logLevel.get();
        return (level != null) ? level : Level.INFO;
    }

    /**
     * Sets logging level for the current thread.
     */
    public static void setLoggingLevel(Level level) {
        logLevel.set(level);
    }

    /**
     * @since 1.2 logs an arbitrary message using logging level setup for QueryLogger.
     */
    public static void log(String message) {
        if (message != null) {
            logObj.log(getLoggingLevel(), message);
        }
    }

    /**
     * Logs database connection event using container data source.
     * 
     * @since 1.2
     */
    public static void logConnect(String dataSource) {
        if (isLoggable()) {
            logObj.log(getLoggingLevel(), "Connecting. JNDI path: " + dataSource);
        }
    }

    /**
     * @since 1.2
     */
    public static void logConnect(String url, String userName, String password) {

        if (isLoggable()) {
            StringBuffer buf = new StringBuffer("Opening connection: ");

            // append URL on the same line to make log somewhat grep-friendly
            buf.append(url);
            buf.append("\n\tLogin: ").append(userName);
            buf.append("\n\tPassword: *******");

            logObj.log(getLoggingLevel(), buf.toString());
        }
    }

    /**
     * Logs database connection event.
     * 
     * @since 1.2
     */
    public static void logPoolCreated(DataSourceInfo dsi) {
        if (isLoggable()) {
            StringBuffer buf = new StringBuffer("Created connection pool: ");

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

            logObj.log(getLoggingLevel(), buf.toString());
        }
    }

    /**
     * @since 1.2
     */
    public static void logConnectSuccess() {
        logObj.log(getLoggingLevel(), "+++ Connecting: SUCCESS.");
    }

    /**
     * @since 1.2
     */
    public static void logConnectFailure(Throwable th) {
        logObj.log(getLoggingLevel(), "*** Connecting: FAILURE.", th);
    }

    /**
     * @since 1.2
     */
    public static void logQuery(String queryStr, List params) {
        logQuery(queryStr, params, -1);
    }

    /**
     * Log query content using Log4J Category with "INFO" priority.
     * 
     * @param queryStr Query SQL string
     * @param params optional list of query parameters that are used when executing query
     *            in prepared statement.
     * @since 1.2
     */
    public static void logQuery(String queryStr, List params, long time) {
        if (isLoggable()) {
            StringBuffer buf = new StringBuffer(queryStr);
            if (params != null && params.size() > 0) {
                buf.append(" [bind: ");
                sqlLiteralForObject(buf, params.get(0));

                int len = params.size();
                for (int i = 1; i < len; i++) {
                    buf.append(", ");
                    sqlLiteralForObject(buf, params.get(i));
                }

                buf.append(']');
            }

            // log preparation time only if it is something significant
            if (time > 5) {
                buf.append(" - prepared in ").append(time).append(" ms.");
            }

            logObj.log(getLoggingLevel(), buf.toString());
        }
    }

    /**
     * @since 1.2
     */
    public static void logQueryParameters(String label, List parameters) {

        if (isLoggable() && parameters.size() > 0) {
            int len = parameters.size();
            StringBuffer buf = new StringBuffer("[");
            buf.append(label).append(": ");

            sqlLiteralForObject(buf, parameters.get(0));

            for (int i = 1; i < len; i++) {
                buf.append(", ");
                sqlLiteralForObject(buf, parameters.get(i));
            }

            buf.append(']');
            logObj.log(getLoggingLevel(), buf.toString());
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
            StringBuffer buf = new StringBuffer();

            if (count == 1) {
                buf.append("=== returned 1 row.");
            }
            else {
                buf.append("=== returned ").append(count).append(" rows.");
            }

            if (time >= 0) {
                buf.append(" - took ").append(time).append(" ms.");
            }

            logObj.log(getLoggingLevel(), buf.toString());
        }
    }

    /**
     * @since 1.2
     */
    public static void logUpdateCount(int count) {
        if (isLoggable()) {

            if (count < 0) {
                logObj.log(getLoggingLevel(), "=== updated ? rows");
            }
            else {
                String countStr = (count == 1) ? "=== updated 1 row." : "=== updated "
                        + count
                        + " rows.";
                logObj.log(getLoggingLevel(), countStr);
            }
        }
    }

    /**
     * @since 1.2
     */
    public static void logBeginTransaction(String transactionLabel) {
        logObj.log(getLoggingLevel(), "--- " + transactionLabel);
    }

    /**
     * @since 1.2
     */
    public static void logCommitTransaction(String transactionLabel) {
        logObj.log(getLoggingLevel(), "+++ " + transactionLabel);
    }

    /**
     * @since 1.2
     */
    public static void logRollbackTransaction(String transactionLabel) {
        logObj.log(getLoggingLevel(), "*** " + transactionLabel);
    }

    /**
     * @since 1.2
     */
    public static void logQueryError(Throwable th) {
        if (isLoggable()) {
            if (th != null) {
                th = Util.unwindException(th);
            }

            logObj.log(getLoggingLevel(), "*** error.", th);

            if (th instanceof SQLException) {
                SQLException sqlException = ((SQLException) th).getNextException();
                while (sqlException != null) {
                    logObj.log(getLoggingLevel(), "*** nested SQL error.", sqlException);
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
            logObj.log(getLoggingLevel(), countStr);
        }
    }

    /**
     * Returns true if current thread default log level is high enough for QueryLogger to
     * generate output.
     * 
     * @since 1.2
     */
    public static boolean isLoggable() {
        return logObj.isEnabledFor(getLoggingLevel());
    }

    /**
     * Logs database connection event using container data source.
     * 
     * @deprecated since 1.2
     */
    public static void logConnect(Level logLevel, String dataSource) {
        logConnect(dataSource);
    }

    /**
     * @deprecated since 1.2
     */
    public static void logConnect(
            Level logLevel,
            String url,
            String userName,
            String password) {
        logConnect(url, userName, password);
    }

    /**
     * Logs database connection event.
     * 
     * @deprecated since 1.2
     */
    public static void logPoolCreated(Level logLevel, DataSourceInfo dsi) {
        logPoolCreated(dsi);
    }

    /**
     * @deprecated since 1.2
     */
    public static void logConnectSuccess(Level logLevel) {
        logConnectSuccess();
    }

    /**
     * @deprecated since 1.2
     */
    public static void logConnectFailure(Level logLevel, Throwable th) {
        logConnectFailure(th);
    }

    /**
     * @deprecated since 1.2
     */
    public static void logQuery(Level logLevel, String queryStr, List params) {
        logQuery(queryStr, params);
    }

    /**
     * @deprecated since 1.2
     */
    public static void logQuery(Level logLevel, String queryStr, List params, long time) {
        logQuery(queryStr, params, time);
    }

    /**
     * @deprecated since 1.2
     */
    public static void logQueryParameters(Level logLevel, String label, List parameters) {
        logQueryParameters(label, parameters);
    }

    /**
     * @deprecated since 1.2
     */
    public static void logSelectCount(Level logLevel, int count) {
        logSelectCount(count);
    }

    /**
     * @deprecated since 1.2
     */
    public static void logSelectCount(Level logLevel, int count, long time) {
        logSelectCount(count, time);
    }

    /**
     * @deprecated since 1.2
     */
    public static void logUpdateCount(Level logLevel, int count) {
        logUpdateCount(count);
    }

    /**
     * @since 1.1
     * @deprecated since 1.2
     */
    public static void logBeginTransaction(Level logLevel, String transactionLabel) {
        logBeginTransaction(transactionLabel);
    }

    /**
     * @since 1.1
     * @deprecated since 1.2
     */
    public static void logCommitTransaction(Level logLevel, String transactionLabel) {
        logCommitTransaction(transactionLabel);
    }

    /**
     * @since 1.1
     * @deprecated since 1.2
     */
    public static void logRollbackTransaction(Level logLevel, String transactionLabel) {
        logRollbackTransaction(transactionLabel);
    }

    /**
     * @deprecated since 1.2
     */
    public static void logQueryError(Level logLevel, Throwable th) {
        logQueryError(th);
    }

    /**
     * @deprecated since 1.2
     */
    public static void logQueryStart(Level logLevel, int count) {
        logQueryStart(count);
    }

    /**
     * @deprecated since 1.2
     */
    public static boolean isLoggable(Level logLevel) {
        return isLoggable();
    }
}
