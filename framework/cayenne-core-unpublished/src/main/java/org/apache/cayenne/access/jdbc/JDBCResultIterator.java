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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.ScalarResultSegment;
import org.apache.cayenne.util.ResultIteratorIterator;

/**
 * A ResultIterator over the underlying JDBC ResultSet.
 * 
 * @since 1.2
 */
public class JDBCResultIterator<T> implements ResultIterator<T> {

    protected Statement statement;
    protected ResultSet resultSet;

    protected RowDescriptor rowDescriptor;
    protected QueryMetadata queryMetadata;

    protected boolean closed;
    protected boolean nextRow;

    private RowReader<T> rowReader;

    /**
     * Creates new JDBCResultIterator that reads from provided ResultSet.
     * 
     * @since 3.0
     * @deprecated since 3.2 use
     *             {@link #JDBCResultIterator(Statement, ResultSet, RowDescriptor, QueryMetadata)}
     */
    @Deprecated
    public JDBCResultIterator(Connection connection, Statement statement, ResultSet resultSet,
            RowDescriptor descriptor, QueryMetadata queryMetadata) throws CayenneException {
        this(statement, resultSet, descriptor, queryMetadata);
    }

    /**
     * Creates new JDBCResultIterator that reads from provided ResultSet.
     * 
     * @since 3.2
     */
    public JDBCResultIterator(Statement statement, ResultSet resultSet, RowDescriptor descriptor,
            QueryMetadata queryMetadata) throws CayenneException {

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
     * @since 3.2
     */
    @Override
    public Iterator<T> iterator() {
        return new ResultIteratorIterator<T>(this);
    }

    /**
     * RowReader factory method.
     */
    @SuppressWarnings("unchecked")
    private RowReader<T> createRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata) {

        List<Object> rsMapping = queryMetadata.getResultSetMapping();
        if (rsMapping != null) {

            int resultWidth = rsMapping.size();
            if (resultWidth == 0) {
                throw new CayenneRuntimeException("Empty result descriptor");
            } else if (resultWidth == 1) {

                Object segment = rsMapping.get(0);

                if (segment instanceof EntityResultSegment) {
                    return createEntityRowReader(descriptor, (EntityResultSegment) segment);
                } else {
                    return new ScalarRowReader<T>(descriptor, (ScalarResultSegment) segment);
                }
            } else {
                CompoundRowReader reader = new CompoundRowReader(resultWidth);

                for (int i = 0; i < resultWidth; i++) {
                    Object segment = rsMapping.get(i);

                    if (segment instanceof EntityResultSegment) {
                        reader.addRowReader(i, createEntityRowReader(descriptor, (EntityResultSegment) segment));
                    } else {
                        reader.addRowReader(i, new ScalarRowReader<Object>(descriptor, (ScalarResultSegment) segment));
                    }
                }

                return (RowReader<T>) reader;
            }
        } else {
            return createFullRowReader(descriptor, queryMetadata);
        }
    }

    @SuppressWarnings("unchecked")
    private RowReader<T> createEntityRowReader(RowDescriptor descriptor, EntityResultSegment resultMetadata) {

        if (queryMetadata.getPageSize() > 0) {
            return new IdRowReader<T>(descriptor, queryMetadata);
        } else if (resultMetadata.getClassDescriptor() != null && resultMetadata.getClassDescriptor().hasSubclasses()) {
            return (RowReader<T>) new InheritanceAwareEntityRowReader(descriptor, resultMetadata);
        } else {
            return (RowReader<T>) new EntityRowReader(descriptor, resultMetadata);
        }
    }

    @SuppressWarnings("unchecked")
    private RowReader<T> createFullRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata) {

        if (queryMetadata.getPageSize() > 0) {
            return new IdRowReader<T>(descriptor, queryMetadata);
        } else if (queryMetadata.getClassDescriptor() != null && queryMetadata.getClassDescriptor().hasSubclasses()) {
            return (RowReader<T>) new InheritanceAwareRowReader(descriptor, queryMetadata);
        } else {
            return (RowReader<T>) new FullRowReader(descriptor, queryMetadata);
        }
    }

    /**
     * @since 3.0
     */
    @Override
    public List<T> allRows() {
        List<T> list = new ArrayList<T>();

        while (hasNextRow()) {
            list.add(nextRow());
        }

        return list;
    }

    /**
     * Returns true if there is at least one more record that can be read from
     * the iterator.
     */
    @Override
    public boolean hasNextRow() {
        return nextRow;
    }

    /**
     * @since 3.0
     */
    @Override
    public T nextRow() {
        if (!hasNextRow()) {
            throw new NoSuchElementException("An attempt to read uninitialized row or past the end of the iterator.");
        }

        T row = rowReader.readRow(resultSet);
        checkNextRow();
        return row;
    }

    /**
     * @since 3.0
     */
    @Override
    public void skipRow() {
        if (!hasNextRow()) {
            throw new NoSuchElementException("An attempt to read uninitialized row or past the end of the iterator.");
        }
        checkNextRow();
    }

    /**
     * Closes ResultIterator and associated ResultSet. This method must be
     * called explicitly when the user is finished processing the records.
     * Otherwise unused database resources will not be released properly.
     */
    @Override
    public void close() throws NoSuchElementException {
        if (!closed) {
            nextRow = false;

            StringBuilder errors = new StringBuilder();

            try {
                resultSet.close();
            } catch (SQLException e1) {
                errors.append("Error closing ResultSet.");
            }

            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e2) {
                    errors.append("Error closing PreparedStatement.");
                }
            }

            if (errors.length() > 0) {
                throw new CayenneRuntimeException("Error closing ResultIterator: " + errors.toString());
            }

            closed = true;
        }
    }

    /**
     * Moves internal ResultSet cursor position down one row. Checks if the next
     * row is available.
     */
    protected void checkNextRow() {
        nextRow = false;
        try {
            if (resultSet.next()) {
                nextRow = true;
            }
        } catch (SQLException e) {
            throw new CayenneRuntimeException("Error rewinding ResultSet", e);
        }
    }

    /**
     * @deprecated since 3.2 always returns false. Connection closing is outside
     *             the scope of this iterator. See
     *             {@link ConnectionAwareResultIterator} for a replacement.
     */
    @Deprecated
    public boolean isClosingConnection() {
        return false;
    }

    /**
     * Sets the <code>closingConnection</code> property.
     * 
     * @deprecated since 3.2 does nothing. Connection closing is outside the
     *             scope of this iterator. See
     *             {@link ConnectionAwareResultIterator} for a replacement.
     */
    @Deprecated
    public void setClosingConnection(boolean flag) {
        // noop
    }

    public RowDescriptor getRowDescriptor() {
        return rowDescriptor;
    }

    // TODO: andrus 11/27/2008 refactor the postprocessor hack into a special
    // row reader.
    void setPostProcessor(DataRowPostProcessor postProcessor) {

        if (rowReader != null) {
            rowReader.setPostProcessor(postProcessor);
        }
    }
}
