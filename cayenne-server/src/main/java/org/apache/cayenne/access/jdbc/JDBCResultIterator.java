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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.util.ResultIteratorIterator;

/**
 * A ResultIterator over the underlying JDBC ResultSet.
 * 
 * @since 1.2
 */
public class JDBCResultIterator<T> implements ResultIterator<T> {

    protected Statement statement;
    protected ResultSet resultSet;

    protected boolean closed;
    protected boolean nextRow;

    private RowReader<T> rowReader;

    /**
     * Creates new JDBCResultIterator that reads from provided ResultSet.
     * 
     * @since 3.2
     */
    public JDBCResultIterator(Statement statement, ResultSet resultSet, RowReader<T> rowReader) {

        this.statement = statement;
        this.resultSet = resultSet;
        this.rowReader = rowReader;

        checkNextRow();
    }

    /**
     * @since 3.2
     */
    @Override
    public Iterator<T> iterator() {
        return new ResultIteratorIterator<T>(this);
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
    
    // TODO: andrus 11/27/2008 refactor the postprocessor hack into a special
    // row reader.
    void setPostProcessor(DataRowPostProcessor postProcessor) {

        if (rowReader != null) {
            rowReader.setPostProcessor(postProcessor);
        }
    }
}
