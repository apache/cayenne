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

package org.apache.cayenne.access.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.ResultIterator;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.QueryMetadata;

/**
 * A ResultIterator over the underlying JDBC ResultSet.
 * 
 * @since 1.2
 */
public class JDBCResultIterator implements ResultIterator {

    // Connection information
    protected Connection connection;
    protected Statement statement;
    protected ResultSet resultSet;

    protected RowDescriptor rowDescriptor;
    protected QueryMetadata queryMetadata;

    // last indexed PK

    protected boolean closingConnection;
    protected boolean closed;

    protected boolean nextRow;

    private DataRowPostProcessor postProcessor;
    private RowReader<DataRow> rowReader;
    private RowReader<Object> idRowReader;

    /**
     * Creates new JDBCResultIterator that reads from provided ResultSet.
     * 
     * @since 3.0
     */
    public JDBCResultIterator(Connection connection, Statement statement,
            ResultSet resultSet, RowDescriptor descriptor, QueryMetadata queryMetadata)
            throws CayenneException {

        this.connection = connection;
        this.statement = statement;
        this.resultSet = resultSet;
        this.rowDescriptor = descriptor;
        this.queryMetadata = queryMetadata;

        checkNextRow();

        if (nextRow) {
            this.rowReader = createRowReader(descriptor, queryMetadata);
        }
    }

    /**
     * RowReader factory method.
     */
    private RowReader<DataRow> createRowReader(
            RowDescriptor descriptor,
            QueryMetadata queryMetadata) {
        
        if (queryMetadata.getClassDescriptor() != null
                && queryMetadata.getClassDescriptor().getEntityInheritanceTree() != null) {
            return new InheritanceAwareRowReader(descriptor, queryMetadata);
        }
        else {
            return new FullRowReader(descriptor, queryMetadata);
        }

    }

    /**
     * Returns all unread data rows from ResultSet, closing this iterator if needed.
     */
    public List<DataRow> dataRows(boolean close) throws CayenneException {
        List<DataRow> list = new ArrayList<DataRow>();

        try {
            while (hasNextRow()) {
                list.add((DataRow) nextDataRow());
            }
        }
        finally {
            if (close) {
                this.close();
            }
        }

        return list;
    }

    /**
     * Returns true if there is at least one more record that can be read from the
     * iterator.
     */
    public boolean hasNextRow() {
        return nextRow;
    }

    /**
     * Returns the next result row as a Map.
     */
    public Map<String, Object> nextDataRow() throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        // read
        Map<String, Object> row = readDataRow();

        // rewind
        checkNextRow();
        return row;
    }

    /**
     * Returns a map of ObjectId values from the next result row. Primary key columns are
     * determined from the provided DbEntity.
     * 
     * @deprecated since 3.0 in favor of {@link #nextId(DbEntity)}.
     */
    public Map<String, Object> nextObjectId(DbEntity entity) throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        // index id
        if (idRowReader == null) {
            this.idRowReader = new IdRowReader(rowDescriptor, queryMetadata);
            idRowReader.setPostProcessor(postProcessor);
        }

        // read ...
        // TODO: note a mismatch with 1.1 API - ID positions are preset and are
        // not affected by the entity specified (think of deprecating/replacing this)
        Map<String, Object> row = readIdRow();

        // rewind
        checkNextRow();

        return row;
    }

    /**
     * @since 3.0
     */
    public Object nextId() throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        // index id
        if (idRowReader == null) {
            this.idRowReader = new IdRowReader(rowDescriptor, queryMetadata);
            idRowReader.setPostProcessor(postProcessor);
        }

        Object id = idRowReader.readRow(resultSet);

        // rewind
        checkNextRow();
        return id;
    }

    public void skipDataRow() throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }
        checkNextRow();
    }

    /**
     * Closes ResultIterator and associated ResultSet. This method must be called
     * explicitly when the user is finished processing the records. Otherwise unused
     * database resources will not be released properly.
     */
    public void close() throws CayenneException {
        if (!closed) {
            nextRow = false;

            StringBuffer errors = new StringBuffer();

            try {
                resultSet.close();
            }
            catch (SQLException e1) {
                errors.append("Error closing ResultSet.");
            }

            if (statement != null) {
                try {
                    statement.close();
                }
                catch (SQLException e2) {
                    errors.append("Error closing PreparedStatement.");
                }
            }

            // TODO: andrus, 5/8/2006 - closing connection within JDBCResultIterator is
            // obsolete as this is bound to transaction closing in DataContext. Deprecate
            // this after 1.2

            // close connection, if this object was explicitly configured to be
            // responsible for doing it
            if (connection != null && isClosingConnection()) {
                try {
                    connection.close();
                }
                catch (SQLException e3) {
                    errors.append("Error closing Connection.");
                }
            }

            if (errors.length() > 0) {
                throw new CayenneException("Error closing ResultIterator: "
                        + errors.toString());
            }

            closed = true;
        }
    }

    /**
     * Returns the number of columns in the result row.
     */
    public int getDataRowWidth() {
        return rowDescriptor.getWidth();
    }

    /**
     * Moves internal ResultSet cursor position down one row. Checks if the next row is
     * available.
     */
    protected void checkNextRow() throws CayenneException {
        nextRow = false;
        try {
            if (resultSet.next()) {
                nextRow = true;
            }
        }
        catch (SQLException e) {
            throw new CayenneException("Error rewinding ResultSet", e);
        }
    }

    /**
     * Reads a row from the internal ResultSet at the current cursor position.
     */
    protected Map<String, Object> readDataRow() throws CayenneException {
        return rowReader.readRow(resultSet);
    }

    /**
     * Reads a row from the internal ResultSet at the current cursor position, processing
     * only columns that are part of the ObjectId of a target class.
     * 
     * @deprecated since 3.0 as the calling method is deprecated
     */
    protected Map<String, Object> readIdRow() throws CayenneException {
        Object value = idRowReader.readRow(resultSet);

        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }

        // wrap into a map...

        String pkName = queryMetadata
                .getDbEntity()
                .getPrimaryKeys()
                .iterator()
                .next()
                .getName();
        DataRow dataRow = new DataRow(2);
        dataRow.put(pkName, value);
        return dataRow;
    }

    /**
     * Returns <code>true</code> if this iterator is responsible for closing its
     * connection, otherwise a user of the iterator must close the connection after
     * closing the iterator.
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

    public RowDescriptor getRowDescriptor() {
        return rowDescriptor;
    }

    // TODO: andrus 11/27/2008 refactor the postprocessor hack into a special row reader.
    void setPostProcessor(DataRowPostProcessor postProcessor) {
        this.postProcessor = postProcessor;

        if (rowReader != null) {
            rowReader.setPostProcessor(postProcessor);
        }

        if (idRowReader != null) {
            idRowReader.setPostProcessor(postProcessor);
        }
    }
}
