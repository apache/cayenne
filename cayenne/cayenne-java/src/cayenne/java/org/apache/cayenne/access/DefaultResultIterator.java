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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.cayenne.CayenneException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.util.Util;

/**
 * Default implementation of ResultIterator interface. Serves as a
 * factory that creates data rows from <code>java.sql.ResultSet</code>.
 *
 * <p><i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 *
 * @author Andrei Adamchik
 * @deprecated Since 1.2 replaced by JDBCResultIterator.
 */
public class DefaultResultIterator implements ResultIterator {
    private static Logger logObj = Logger.getLogger(DefaultResultIterator.class);

    // Connection information
    protected Connection connection;
    protected Statement statement;
    protected ResultSet resultSet;

    // Result descriptor
    protected org.apache.cayenne.access.util.ResultDescriptor descriptor;

    protected int mapCapacity;

    protected boolean closingConnection;
    protected boolean isClosed;

    protected boolean nextRow;
    protected int fetchedSoFar;
    protected int fetchLimit;

    /**
     * Utility method to read stored procedure out parameters as a map. 
     * Returns an empty map if no out parameters exist.
     */
    public static Map readProcedureOutParameters(
        CallableStatement statement,
        org.apache.cayenne.access.util.ResultDescriptor resultDescriptor)
        throws SQLException, Exception {

        int resultWidth = resultDescriptor.getResultWidth();
        if (resultWidth > 0) {
            Map dataRow = new HashMap(resultWidth * 2, 0.75f);
            ExtendedType[] converters = resultDescriptor.getConverters();
            int[] jdbcTypes = resultDescriptor.getJdbcTypes();
            String[] names = resultDescriptor.getNames();
            int[] outParamIndexes = resultDescriptor.getOutParamIndexes();

            // process result row columns,
            for (int i = 0; i < outParamIndexes.length; i++) {
                int index = outParamIndexes[i];

                // note: jdbc column indexes start from 1, not 0 unlike everywhere else
                Object val =
                    converters[index].materializeObject(
                        statement,
                        index + 1,
                        jdbcTypes[index]);
                dataRow.put(names[index], val);
            }

            return dataRow;
        }

        return Collections.EMPTY_MAP;
    }

    public DefaultResultIterator(
        Connection connection,
        Statement statement,
        ResultSet resultSet,
        org.apache.cayenne.access.util.ResultDescriptor descriptor,
        int fetchLimit)
        throws SQLException, CayenneException {

        this.connection = connection;
        this.statement = statement;
        this.resultSet = resultSet;
        this.descriptor = descriptor;
        this.fetchLimit = fetchLimit;

        this.mapCapacity = (int) Math.ceil((descriptor.getNames().length) / 0.75);

        checkNextRow();
    }

    /**
     * Moves internal ResultSet cursor position down one row.
     * Checks if the next row is available.
     */
    protected void checkNextRow() throws SQLException, CayenneException {
        nextRow = false;
        if ((fetchLimit <= 0 || fetchedSoFar < fetchLimit) && resultSet.next()) {
            nextRow = true;
            fetchedSoFar++;
        }
    }

    /**
     * Returns true if there is at least one more record
     * that can be read from the iterator.
     */
    public boolean hasNextRow() {
        return nextRow;
    }

    /**
     * Returns the next result row as a Map.
     */
    public Map nextDataRow() throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException("An attempt to read uninitialized row or past the end of the iterator.");
        }

        try {
            // read
            Map row = readDataRow();

            // rewind
            checkNextRow();

            return row;
        }
        catch (SQLException sqex) {
            throw new CayenneException("Exception reading ResultSet.", sqex);
        }
    }

    /**
     * Returns the number of columns in the result row.
     * 
     * @since 1.0.6
     */
    public int getDataRowWidth() {
        return descriptor.getResultWidth();
    }

    /**
     * Returns all unread data rows from ResultSet, closing
     * this iterator if needed.
     */
    public List dataRows(boolean close) throws CayenneException {
        List list = new ArrayList();

        try {
            while (this.hasNextRow()) {
                list.add(this.nextDataRow());
            }
            return list;
        }
        finally {
            if (close) {
                this.close();
            }
        }
    }

    /**
     * Reads a row from the internal ResultSet at the current
     * cursor position.
     */
    protected Map readDataRow() throws SQLException, CayenneException {
        try {
            Map dataRow = new DataRow(mapCapacity);
            ExtendedType[] converters = descriptor.getConverters();
            int[] jdbcTypes = descriptor.getJdbcTypes();
            String[] names = descriptor.getNames();
            int resultWidth = names.length;

            // process result row columns,
            for (int i = 0; i < resultWidth; i++) {
                // note: jdbc column indexes start from 1, not 0 unlike everywhere else
                Object val =
                    converters[i].materializeObject(resultSet, i + 1, jdbcTypes[i]);
                dataRow.put(names[i], val);
            }

            return dataRow;
        }
        catch (CayenneException cex) {
            // rethrow unmodified
            throw cex;
        }
        catch (Exception otherex) {
            throw new CayenneException(
                "Exception materializing column.",
                Util.unwindException(otherex));
        }
    }

    /**
     * Reads a row from the internal ResultSet at the current
     * cursor position, processing only columns that are part of the ObjectId
     * of a target class.
     */
    protected Map readIdRow(DbEntity entity) throws SQLException, CayenneException {
        try {
            Map idRow = new DataRow(2);
            ExtendedType[] converters = descriptor.getConverters();
            int[] jdbcTypes = descriptor.getJdbcTypes();
            String[] names = descriptor.getNames();
            int[] idIndex = descriptor.getIdIndexes(entity);
            int len = idIndex.length;

            for (int i = 0; i < len; i++) {

                // dereference column index
                int index = idIndex[i];

                // note: jdbc column indexes start from 1, not 0 as in arrays
                Object val =
                    converters[index].materializeObject(
                        resultSet,
                        index + 1,
                        jdbcTypes[index]);
                idRow.put(names[index], val);
            }

            return idRow;
        }
        catch (CayenneException cex) {
            // rethrow unmodified
            throw cex;
        }
        catch (Exception otherex) {
            logObj.warn("Error", otherex);
            throw new CayenneException(
                "Exception materializing id column.",
                Util.unwindException(otherex));
        }
    }

    /**
     * Closes ResultIterator and associated ResultSet. This method must be
     * called explicitly when the user is finished processing the records.
     * Otherwise unused database resources will not be released properly.
     */
    public void close() throws CayenneException {
        if (!isClosed) {

            nextRow = false;

            StringWriter errors = new StringWriter();
            PrintWriter out = new PrintWriter(errors);

            try {
                resultSet.close();
            }
            catch (SQLException e1) {
                out.println("Error closing ResultSet");
                e1.printStackTrace(out);
            }

            if (statement != null) {
                try {
                    statement.close();
                }
                catch (SQLException e2) {
                    out.println("Error closing PreparedStatement");
                    e2.printStackTrace(out);
                }
            }

            // close connection, if this object was explicitly configured to be
            // responsible for doing it
            if (connection != null && this.isClosingConnection()) {
                try {
                    connection.close();
                }
                catch (SQLException e3) {
                    out.println("Error closing Connection");
                    e3.printStackTrace(out);
                }
            }

            try {
                out.close();
                errors.close();
            }
            catch (IOException ioex) {
                // this is totally unexpected,
                // after all we are writing to the StringBuffer
                // in the memory
            }
            StringBuffer buf = errors.getBuffer();

            if (buf.length() > 0) {
                throw new CayenneException("Error closing ResultIterator: " + buf);
            }

            isClosed = true;
        }
    }

    /**
     * Returns <code>true</code> if this iterator is responsible
     * for closing its connection, otherwise a user of the iterator
     * must close the connection after closing the iterator.
     */
    public boolean isClosingConnection() {
        return closingConnection;
    }

    /**
     * Sets the <code>closingConnection</code> property.
     */
    public void setClosingConnection(boolean flag) {
        this.closingConnection = flag;
    }

    /**
     * @see org.apache.cayenne.access.ResultIterator#skipDataRow()
     */
    public void skipDataRow() throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException("An attempt to read uninitialized row or past the end of the iterator.");
        }

        try {
            checkNextRow();
        }
        catch (SQLException sqex) {
            throw new CayenneException("Exception reading ResultSet.", sqex);
        }
    }

    /**
     * Returns a map of ObjectId values from the next result row.
     * Primary key columns are determined from the provided DbEntity.
     * 
     * @since 1.1
     */
    public Map nextObjectId(DbEntity entity) throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException("An attempt to read uninitialized row or past the end of the iterator.");
        }

        try {
            // read
            Map row = readIdRow(entity);

            // rewind
            checkNextRow();

            return row;
        }
        catch (SQLException sqex) {
            throw new CayenneException("Exception reading ResultSet.", sqex);
        }
    }
}
