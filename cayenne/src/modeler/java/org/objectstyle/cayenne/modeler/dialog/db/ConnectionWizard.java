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
package org.objectstyle.cayenne.modeler.dialog.db;

import java.sql.Connection;

import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.modeler.ClassLoadingService;
import org.objectstyle.cayenne.modeler.pref.DBConnectionInfo;
import org.objectstyle.cayenne.modeler.util.CayenneController;

/**
 * A component for choosing a DataSource. Users can choose from the DataSources configured
 * in preferences, and one extra set of connection settings. This object will create and
 * keep open a JDBC connection. It is caller responsibility to dispose of it properly.
 * 
 * @author Andrei Adamchik
 */
// TODO: after refactoring DbLoader to accept a DataSource instead of connection this
// dialog should be merged with superclass - DataSourceWizard.
public class ConnectionWizard extends DataSourceWizard {

    protected Connection connection;
    protected DbAdapter adapter;

    public ConnectionWizard(CayenneController parent, String title,
            String altDataSourceKey, DBConnectionInfo altDataSource) {
        super(parent, title, altDataSourceKey, altDataSource);
    }

    /**
     * Overrides superclass to keep an open connection around for the caller's use.
     */
    public void okAction() {
        // build connection and adapter...

        DBConnectionInfo info = getConnectionInfo();
        ClassLoadingService classLoader = getApplication().getClassLoadingService();

        try {
            this.adapter = info.makeAdapter(classLoader);
        }
        catch (Throwable th) {
            reportError("DbAdapter Error", th);
            return;
        }

        try {
            this.connection = info.makeDataSource(classLoader).getConnection();
        }
        catch (Throwable th) {
            reportError("Connection Error", th);
            return;
        }

        // set success flag, and unblock the caller...
        canceled = false;
        view.dispose();
    }

    /**
     * Returns configured DB connection.
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Returns configured DbAdapter.
     */
    public DbAdapter getAdapter() {
        return adapter;
    }
}