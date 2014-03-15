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
package org.apache.cayenne.log;

import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ExtendedEnumeration;
import org.apache.cayenne.access.jdbc.ParameterBinding;
import org.apache.cayenne.access.translator.batch.BatchParameterBinding;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.util.IDUtil;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link JdbcEventLogger} built on top of commons-logging.
 * 
 * @since 3.1
 */
public class CommonsJdbcEventLogger implements JdbcEventLogger {

    private static final Log logger = LogFactory.getLog(CommonsJdbcEventLogger.class);

    private static final int TRIM_VALUES_THRESHOLD = 30;

    protected long queryExecutionTimeLoggingThreshold;

    public CommonsJdbcEventLogger(@Inject RuntimeProperties runtimeProperties) {
        this.queryExecutionTimeLoggingThreshold = runtimeProperties.getLong(
                Constants.QUERY_EXECUTION_TIME_LOGGING_THRESHOLD_PROPERTY, 0);
    }

    void sqlLiteralForObject(StringBuilder buffer, Object object) {
        if (object == null) {
            buffer.append("NULL");
        } else if (object instanceof String) {
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
        } else if (object instanceof Number) {
            // process numeric value (do something smart in the future)
            buffer.append(object);
        } else if (object instanceof java.sql.Date) {
            buffer.append('\'').append(object).append('\'');
        } else if (object instanceof java.sql.Time) {
            buffer.append('\'').append(object).append('\'');
        } else if (object instanceof java.util.Date) {
            long time = ((java.util.Date) object).getTime();
            buffer.append('\'').append(new java.sql.Timestamp(time)).append('\'');
        } else if (object instanceof java.util.Calendar) {
            long time = ((java.util.Calendar) object).getTimeInMillis();
            buffer.append(object.getClass().getName()).append('(').append(new java.sql.Timestamp(time)).append(')');
        } else if (object instanceof Character) {
            buffer.append(((Character) object).charValue());
        } else if (object instanceof Boolean) {
            buffer.append('\'').append(object).append('\'');
        } else if (object instanceof Enum<?>) {
            // buffer.append(object.getClass().getName()).append(".");
            buffer.append(((Enum<?>) object).name()).append("=");
            if (object instanceof ExtendedEnumeration) {
                Object value = ((ExtendedEnumeration) object).getDatabaseValue();
                if (value instanceof String)
                    buffer.append("'");
                buffer.append(value);
                if (value instanceof String)
                    buffer.append("'");
            } else {
                buffer.append(((Enum<?>) object).ordinal());
                // FIXME -- this isn't quite right
            }
        } else if (object instanceof ParameterBinding) {
            sqlLiteralForObject(buffer, ((ParameterBinding) object).getValue());
        } else if (object.getClass().isArray()) {
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
        } else {
            buffer.append(object.getClass().getName()).append("@").append(System.identityHashCode(object));
        }
    }

    @Override
    public void log(String message) {
        if (message != null) {
            logger.info(message);
        }
    }

    @Override
    public void logConnect(String dataSource) {
        if (isLoggable()) {
            logger.info("Connecting. JNDI path: " + dataSource);
        }
    }

    @Override
    public void logConnect(String url, String userName, String password) {
        if (isLoggable()) {
            StringBuilder buf = new StringBuilder("Opening connection: ");

            // append URL on the same line to make log somewhat grep-friendly
            buf.append(url);
            buf.append("\n\tLogin: ").append(userName);
            buf.append("\n\tPassword: *******");

            logger.info(buf.toString());
        }
    }

    @Override
    public void logPoolCreated(DataSourceInfo dsi) {
        if (isLoggable()) {
            StringBuilder buf = new StringBuilder("Created connection pool: ");

            if (dsi != null) {
                // append URL on the same line to make log somewhat
                // grep-friendly
                buf.append(dsi.getDataSourceUrl());

                if (dsi.getAdapterClassName() != null) {
                    buf.append("\n\tCayenne DbAdapter: ").append(dsi.getAdapterClassName());
                }

                buf.append("\n\tDriver class: ").append(dsi.getJdbcDriver());

                if (dsi.getMinConnections() >= 0) {
                    buf.append("\n\tMin. connections in the pool: ").append(dsi.getMinConnections());
                }
                if (dsi.getMaxConnections() >= 0) {
                    buf.append("\n\tMax. connections in the pool: ").append(dsi.getMaxConnections());
                }
            } else {
                buf.append(" pool information unavailable");
            }

            logger.info(buf.toString());
        }
    }

    @Override
    public void logConnectSuccess() {
        logger.info("+++ Connecting: SUCCESS.");
    }

    @Override
    public void logConnectFailure(Throwable th) {
        logger.info("*** Connecting: FAILURE.", th);
    }

    @Override
    public void logGeneratedKey(DbAttribute attribute, Object value) {
        if (isLoggable()) {
            String entity = attribute.getEntity().getName();
            String key = attribute.getName();

            logger.info("Generated PK: " + entity + "." + key + " = " + value);
        }
    }

    @Override
    public void logQuery(String queryStr, List<?> params) {
        logQuery(queryStr, null, params, -1);
    }

    @Deprecated
    private void buildLog(StringBuilder buffer, String prefix, String postfix, List<DbAttribute> attributes,
            List<?> parameters, boolean isInserting) {
        if (parameters != null && parameters.size() > 0) {
            DbAttribute attribute = null;
            Iterator<DbAttribute> attributeIterator = null;
            int position = 0;

            if (attributes != null)
                attributeIterator = attributes.iterator();

            for (Object parameter : parameters) {
                // If at the beginning, output the prefix, otherwise a
                // separator.
                if (position++ == 0)
                    buffer.append(prefix);
                else
                    buffer.append(", ");

                // Find the next attribute and SKIP generated attributes. Only
                // skip when logging inserts, though. Should show previously
                // generated keys on DELETE, UPDATE, or SELECT.
                while (attributeIterator != null && attributeIterator.hasNext()) {
                    attribute = attributeIterator.next();

                    if (isInserting == false || attribute.isGenerated() == false)
                        break;
                }

                buffer.append(position);

                if (attribute != null) {
                    buffer.append("->");
                    buffer.append(attribute.getName());
                }

                buffer.append(":");
                sqlLiteralForObject(buffer, parameter);
            }

            buffer.append(postfix);
        }
    }

    private void buildLog(StringBuilder buffer, List<BatchParameterBinding> bindings) {

        int len = bindings.size();

        for (int i = 0; i < len; i++) {

            if (i > 0) {
                buffer.append(", ");
            }

            BatchParameterBinding b = bindings.get(i);
            DbAttribute attribute = b.getAttribute();

            buffer.append(i + 1);

            if (attribute != null) {
                buffer.append("->");
                buffer.append(attribute.getName());
            }

            buffer.append(":");
            sqlLiteralForObject(buffer, b.getValue());
        }
    }

    private boolean isInserting(String query) {
        if (query == null || query.length() == 0)
            return false;

        char firstCharacter = query.charAt(0);

        if (firstCharacter == 'I' || firstCharacter == 'i')
            return true;
        else
            return false;
    }

    @Override
    public void logQuery(String queryStr, List<DbAttribute> attrs, List<?> params, long time) {
        if (isLoggable()) {
            StringBuilder buffer = new StringBuilder(queryStr);
            buildLog(buffer, " [bind: ", "]", attrs, params, isInserting(queryStr));

            // log preparation time only if it is something significant
            if (time > 5) {
                buffer.append(" - prepared in ").append(time).append(" ms.");
            }

            logger.info(buffer.toString());
        }
    }

    @Deprecated
    @Override
    public void logQueryParameters(String label, List<DbAttribute> attrs, List<Object> parameters, boolean isInserting) {
        String prefix = "[" + label + ": ";
        if (isLoggable() && parameters.size() > 0) {
            StringBuilder buffer = new StringBuilder();
            buildLog(buffer, prefix, "]", attrs, parameters, isInserting);
            logger.info(buffer.toString());
        }
    }

    @Override
    public void logQueryParameters(String label, List<BatchParameterBinding> bindings) {

        if (isLoggable() && bindings.size() > 0) {
            StringBuilder buffer = new StringBuilder();

            buffer.append("[").append(label).append(": ");
            buildLog(buffer, bindings);
            buffer.append("]");

            logger.info(buffer.toString());
        }
    }

    @Override
    public void logSelectCount(int count, long time) {
        logSelectCount(count, time, null);
    }

    @Override
    public void logSelectCount(int count, long time, String sql) {

        if (isLoggable()) {
            StringBuilder buf = new StringBuilder();

            if (count == 1) {
                buf.append("=== returned 1 row.");
            } else {
                buf.append("=== returned ").append(count).append(" rows.");
            }

            if (time >= 0) {
                buf.append(" - took ").append(time).append(" ms.");
            }

            logger.info(buf.toString());
        }

        if (queryExecutionTimeLoggingThreshold > 0 && time > queryExecutionTimeLoggingThreshold) {
            StringBuilder buf = new StringBuilder();
            buf.append("Query time exceeded threshold (").append(time).append(" ms): ");
            buf.append(sql);
            String message = buf.toString();
            logger.warn(message, new CayenneRuntimeException(message));
        }
    }

    @Override
    public void logUpdateCount(int count) {
        if (isLoggable()) {
            if (count < 0) {
                logger.info("=== updated ? rows");
            } else {
                String countStr = (count == 1) ? "=== updated 1 row." : "=== updated " + count + " rows.";
                logger.info(countStr);
            }
        }
    }

    @Override
    public void logBeginTransaction(String transactionLabel) {
        logger.info("--- " + transactionLabel);
    }

    @Override
    public void logCommitTransaction(String transactionLabel) {
        logger.info("+++ " + transactionLabel);
    }

    @Override
    public void logRollbackTransaction(String transactionLabel) {
        logger.info("*** " + transactionLabel);
    }

    @Override
    public void logQueryError(Throwable th) {
        if (isLoggable()) {
            if (th != null) {
                th = Util.unwindException(th);
            }

            logger.info("*** error.", th);

            if (th instanceof SQLException) {
                SQLException sqlException = ((SQLException) th).getNextException();
                while (sqlException != null) {
                    logger.info("*** nested SQL error.", sqlException);
                    sqlException = sqlException.getNextException();
                }
            }
        }
    }

    @Override
    public boolean isLoggable() {
        return logger.isInfoEnabled();
    }
}
