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

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.ResultIterator;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.ScalarResultSegment;

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

    private RowReader<?> rowReader;

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
    private RowReader<?> createRowReader(
            RowDescriptor descriptor,
            QueryMetadata queryMetadata) {

        List<Object> rsMapping = queryMetadata.getResultSetMapping();
        if (rsMapping != null) {

            int resultWidth = rsMapping.size();
            if (resultWidth == 0) {
                throw new CayenneRuntimeException("Empty result descriptor");
            }
            else if (resultWidth == 1) {

                Object segment = rsMapping.get(0);

                if (segment instanceof EntityResultSegment) {
                    return createEntityRowReader(
                            descriptor,
                            (EntityResultSegment) segment);
                }
                else {
                    return new ScalarRowReader(descriptor, (ScalarResultSegment) segment);
                }
            }
            else {
                CompoundRowReader reader = new CompoundRowReader(resultWidth);

                for (int i = 0; i < resultWidth; i++) {
                    Object segment = rsMapping.get(i);

                    if (segment instanceof EntityResultSegment) {
                        reader.addRowReader(i, createEntityRowReader(
                                descriptor,
                                (EntityResultSegment) segment));
                    }
                    else {
                        reader.addRowReader(i, new ScalarRowReader(
                                descriptor,
                                (ScalarResultSegment) segment));
                    }
                }

                return reader;
            }
        }
        else {
            return createFullRowReader(descriptor, queryMetadata);
        }
    }

    private RowReader<?> createEntityRowReader(
            RowDescriptor descriptor,
            EntityResultSegment resultMetadata) {

        if (queryMetadata.getPageSize() > 0) {
            return new IdRowReader(descriptor, queryMetadata);
        }
        else if (resultMetadata.getClassDescriptor() != null
                && resultMetadata.getClassDescriptor().getEntityInheritanceTree() != null) {
            return new InheritanceAwareEntityRowReader(descriptor, resultMetadata);
        }
        else {
            return new EntityRowReader(descriptor, resultMetadata);
        }
    }

    private RowReader<?> createFullRowReader(
            RowDescriptor descriptor,
            QueryMetadata queryMetadata) {

        if (queryMetadata.getPageSize() > 0) {
            return new IdRowReader(descriptor, queryMetadata);
        }
        else if (queryMetadata.getClassDescriptor() != null
                && queryMetadata.getClassDescriptor().getEntityInheritanceTree() != null) {
            return new InheritanceAwareRowReader(descriptor, queryMetadata);
        }
        else {
            return new FullRowReader(descriptor, queryMetadata);
        }
    }

    /**
     * @since 3.0
     */
    public List<?> allRows() throws CayenneException {
        List<Object> list = new ArrayList<Object>();

        while (hasNextRow()) {
            list.add(nextRow());
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
     * @since 3.0
     */
    public Object nextRow() throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        Object row = rowReader.readRow(resultSet);
        checkNextRow();
        return row;
    }

    /**
     * @since 3.0
     */
    public void skipRow() throws CayenneException {
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

        if (rowReader != null) {
            rowReader.setPostProcessor(postProcessor);
        }
    }
}
