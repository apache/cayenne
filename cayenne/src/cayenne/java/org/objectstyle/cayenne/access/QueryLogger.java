/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.Util;

/** 
 * A QueryLogger is intended to log special events during query executions.
 * This includes generated SQL statements, result counts, connection events etc.
 * It is a single consistent place for that kind of logging and should be used
 * by all Cayenne classes that work with the database directly.
 * 
 * <p>In many cases it is important to use this class as opposed to logging 
 * from the class that performs a particular operation, since QueryLogger 
 * will generate consistently formatted logs that are easy to analyze
 * and turn on/off.</p>
 * 
 * <p><i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 * 
 * @author Andrei Adamchik
 */
public class QueryLogger {
    private static final Logger logObj = Logger.getLogger(QueryLogger.class);

    public static final Level DEFAULT_LOG_LEVEL = Query.DEFAULT_LOG_LEVEL;
    public static final int TRIM_VALUES_THRESHOLD = 300;

    /** 
     * Utility method that appends SQL literal for the specified object to the buffer.
    *
    * <p>Note: this method is not intended to build SQL queries, rather this is used 
    * in logging routines only. In particular it will trim large values to avoid 
    * flooding the logs.</p> 
    *
    *  @param buf buffer to append value
    *  @param anObject object to be transformed to SQL literal.
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
                appendFormattedByte(buf, b[i]);
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
            buf
                .append("[")
                .append(anObject.getClass().getName())
                .append(": ")
                .append(anObject)
                .append("]");
        }
    }

    /**
     * Prints a byte value to a StringBuffer as a double digit hex value.
     */
    protected static void appendFormattedByte(StringBuffer buffer, byte byteValue) {
        final String digits = "0123456789ABCDEF";

        buffer.append(digits.charAt((byteValue >>> 4) & 0xF));
        buffer.append(digits.charAt(byteValue & 0xF));
    }

    /** 
     * Returns current logging level.
     */
    public static Level getLoggingLevel() {
        Level level = logObj.getLevel();
        return (level != null) ? level : DEFAULT_LOG_LEVEL;
    }

    /**
     * Sets logging level.
     */
    public static void setLoggingLevel(Level level) {
        logObj.setLevel(level);
    }

    /**
     * Logs database connection event using container data source.
     */
    public static void logConnect(Level logLevel, String dataSource) {
        if (isLoggable(logLevel)) {
            logObj.log(logLevel, "Connecting. JNDI path: " + dataSource);
        }
    }

    public static void logConnect(
        Level logLevel,
        String url,
        String userName,
        String password) {
        if (isLoggable(logLevel)) {
            StringBuffer buf = new StringBuffer("Opening connection: ");

            // append URL on the same line to make log somewhat grep-friendly
            buf.append(url);
            buf.append("\n\tLogin: ").append(userName);
            buf.append("\n\tPassword: *******");

            logObj.log(logLevel, buf.toString());
        }
    }

    /**
     * Logs database connection event.
     */
    public static void logPoolCreated(Level logLevel, DataSourceInfo dsi) {
        if (isLoggable(logLevel)) {
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

            logObj.log(logLevel, buf.toString());
        }
    }

    public static void logConnectSuccess(Level logLevel) {
        logObj.log(logLevel, "+++ Connecting: SUCCESS.");
    }

    public static void logConnectFailure(Level logLevel, Throwable th) {
        logObj.log(logLevel, "*** Connecting: FAILURE.", th);
    }

    public static void logQuery(Level logLevel, String queryStr, List params) {
        logQuery(logLevel, queryStr, params, -1);
    }

    /** 
     * Log query content using Log4J Category with "INFO" priority.
     *
     * @param queryStr Query SQL string
     * @param params optional list of query parameters that are used when 
     * executing query in prepared statement.
     */
    public static void logQuery(
        Level logLevel,
        String queryStr,
        List params,
        long time) {
        if (isLoggable(logLevel)) {
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

            logObj.log(logLevel, buf.toString());
        }
    }

    public static void logQueryParameters(
        Level logLevel,
        String label,
        List parameters) {

        if (isLoggable(logLevel) && parameters.size() > 0) {
            int len = parameters.size();
            StringBuffer buf = new StringBuffer("[");
            buf.append(label).append(": ");

            sqlLiteralForObject(buf, parameters.get(0));

            for (int i = 1; i < len; i++) {
                buf.append(", ");
                sqlLiteralForObject(buf, parameters.get(i));
            }

            buf.append(']');
            logObj.log(logLevel, buf.toString());
        }
    }

    public static void logSelectCount(Level logLevel, int count) {
        logSelectCount(logLevel, count, -1);
    }

    public static void logSelectCount(Level logLevel, int count, long time) {
        if (isLoggable(logLevel)) {
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

            logObj.log(logLevel, buf.toString());
        }
    }

    public static void logUpdateCount(Level logLevel, int count) {
        if (isLoggable(logLevel)) {
            String countStr =
                (count == 1) ? "=== updated 1 row." : "=== updated " + count + " rows.";
            logObj.log(logLevel, countStr);
        }
    }

    /**
     * @deprecated Since 1.1 use {@link #logCommitTransaction(Level,String)}.
     */
    public static void logCommitTransaction(Level logLevel) {
        logObj.log(logLevel, "+++ transaction committed.");
    }

    /**
     * @deprecated Since 1.1 use {@link #logRollbackTransaction(Level,String)}.
     */
    public static void logRollbackTransaction(Level logLevel) {
        logObj.log(logLevel, "*** transaction rolled back.");
    }

    /**
     * @since 1.1
     */
    public static void logBeginTransaction(Level logLevel, String transactionLabel) {
        if (isLoggable(logLevel)) {
            logObj.log(logLevel, "--- " + transactionLabel);
        }
    }

    /**
     * @since 1.1
     */
    public static void logCommitTransaction(Level logLevel, String transactionLabel) {
        if (isLoggable(logLevel)) {
            logObj.log(logLevel, "+++ " + transactionLabel);
        }
    }

    /**
     * @since 1.1
     */
    public static void logRollbackTransaction(Level logLevel, String transactionLabel) {
        if (isLoggable(logLevel)) {
            logObj.log(logLevel, "*** " + transactionLabel);
        }
    }

    public static void logQueryError(Level logLevel, Throwable th) {
        if (th != null) {
            th = Util.unwindException(th);
        }

        logObj.log(logLevel, "*** error.", th);

        if (th instanceof SQLException) {
            SQLException sqlException = ((SQLException) th).getNextException();
            while (sqlException != null) {
                logObj.log(logLevel, "*** nested SQL error.", sqlException);
                sqlException = sqlException.getNextException();
            }
        }
    }

    public static void logQueryStart(Level logLevel, int count) {
        if (isLoggable(logLevel)) {
            String countStr =
                (count == 1)
                    ? "--- will run 1 query."
                    : "--- will run " + count + " queries.";
            logObj.log(logLevel, countStr);
        }
    }

    public static boolean isLoggable(Level logLevel) {
        return logObj.isEnabledFor(logLevel);
    }

}
