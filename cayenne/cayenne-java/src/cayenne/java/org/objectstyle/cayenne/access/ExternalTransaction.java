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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;

import org.objectstyle.cayenne.CayenneException;

/**
 * Represents a container-managed transaction.
 * 
 * @since 1.2 moved to a top-level class.
 * @author Andrus Adamchik
 */
class ExternalTransaction extends Transaction {

    ExternalTransaction() {
    }

    ExternalTransaction(TransactionDelegate delegate) {
        setDelegate(delegate);
    }

    public synchronized void begin() {
        if (status != Transaction.STATUS_NO_TRANSACTION) {
            throw new IllegalStateException(
                    "Transaction must have 'STATUS_NO_TRANSACTION' to begin. "
                            + "Current status: "
                            + Transaction.decodeStatus(status));
        }

        status = Transaction.STATUS_ACTIVE;
    }

    public boolean addConnection(String name, Connection connection) throws SQLException {
        if (super.addConnection(name, connection)) {

            // implicitly begin transaction
            if (status == Transaction.STATUS_NO_TRANSACTION) {
                begin();
            }

            if (status != Transaction.STATUS_ACTIVE) {
                throw new IllegalStateException(
                        "Transaction must have 'STATUS_ACTIVE' to add a connection. "
                                + "Current status: "
                                + Transaction.decodeStatus(status));
            }

            fixConnectionState(connection);
            return true;
        }
        else {
            return false;
        }

    }

    public void commit() throws IllegalStateException, SQLException, CayenneException {
        try {
            if (status == Transaction.STATUS_NO_TRANSACTION) {
                return;
            }

            if (delegate != null && !delegate.willCommit(this)) {
                return;
            }

            if (status != Transaction.STATUS_ACTIVE) {
                throw new IllegalStateException(
                        "Transaction must have 'STATUS_ACTIVE' to be committed. "
                                + "Current status: "
                                + Transaction.decodeStatus(status));
            }

            processCommit();

            status = Transaction.STATUS_COMMITTED;
            if (delegate != null) {
                delegate.didCommit(this);
            }
        }
        finally {
            close();
        }
    }

    public void rollback() throws IllegalStateException, SQLException, CayenneException {

        try {
            if (status == Transaction.STATUS_NO_TRANSACTION
                    || status == Transaction.STATUS_ROLLEDBACK
                    || status == Transaction.STATUS_ROLLING_BACK) {
                return;
            }

            if (delegate != null && !delegate.willRollback(this)) {
                return;
            }

            if (status != Transaction.STATUS_ACTIVE
                    && status != Transaction.STATUS_MARKED_ROLLEDBACK) {
                throw new IllegalStateException(
                        "Transaction must have 'STATUS_ACTIVE' or 'STATUS_MARKED_ROLLEDBACK' to be rolled back. "
                                + "Current status: "
                                + Transaction.decodeStatus(status));
            }

            processRollback();

            status = Transaction.STATUS_ROLLEDBACK;
            if (delegate != null) {
                delegate.didRollback(this);
            }
        }
        finally {
            close();
        }
    }

    void fixConnectionState(Connection connection) throws SQLException {
        // NOOP
    }

    void processCommit() throws SQLException, CayenneException {
        QueryLogger
                .logCommitTransaction("no commit - transaction controlled externally.");
    }

    void processRollback() throws SQLException, CayenneException {
        QueryLogger
                .logRollbackTransaction("no rollback - transaction controlled externally.");
    }

    /**
     * Closes all connections associated with transaction.
     */
    void close() {
        if (connections == null || connections.isEmpty()) {
            return;
        }

        Iterator it = connections.values().iterator();
        while (it.hasNext()) {
            try {

                ((Connection) it.next()).close();
            }
            catch (Throwable th) {
                // TODO: chain exceptions...
                // ignore for now
            }
        }
    }
}