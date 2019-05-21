/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
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
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ResultIterator;

/**
 * A {@link ResultIterator} wrapper that handles closing a connection. Also
 * internally counts processed rows, mostly for the benefit of subclasses.
 * Subclasses are used in iterators that are returned to the end users and are
 * not implicitly managed by Cayenne.
 * 
 * @since 4.0
 */
public class ConnectionAwareResultIterator<T> implements ResultIterator<T> {

    private ResultIterator<T> delegate;
    private Connection connection;
    private boolean closed;
    protected int rowCounter;

    public ConnectionAwareResultIterator(ResultIterator<T> delegate, Connection connection) {
        this.delegate = delegate;
        this.connection = connection;
    }

    @Override
    public void close() {

        if (!closed) {
            doClose();
            closed = true;
        }
    }

    protected void doClose() {
        StringBuilder errors = null;

        try {
            delegate.close();
        } catch (Exception e1) {
            errors = new StringBuilder();
            errors.append("Error closing ResultSet: ").append(e1);
        }

        try {
            connection.close();
        } catch (SQLException e2) {
            if (errors == null) {
                errors = new StringBuilder();
            }

            errors.append("Error closing connection: ").append(e2);
        }

        if (errors != null) {
            throw new CayenneRuntimeException("Error closing ResultIterator: %s", errors);
        }
    }

    @Override
    public List<T> allRows() {
        return delegate.allRows();
    }

    @Override
    public boolean hasNextRow() {
        return delegate.hasNextRow();
    }

    @Override
    public T nextRow() {
        rowCounter++;
        return delegate.nextRow();
    }

    @Override
    public Iterator<T> iterator() {
        return delegate.iterator();
    }

    @Override
    public void skipRow() {
        delegate.skipRow();
    }
}
