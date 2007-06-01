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

package org.objectstyle.cayenne.modeler.dialog.query;

import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.query.SelectQuery;
import org.scopemvc.core.Selector;

/**
 * @author Andrei Adamchik
 */
public class QueryTypeModel {

    public static final Selector OBJECT_SELECT_QUERY_SELECTOR = Selector
            .fromString("objectSelectQuery");
    public static final Selector RAW_SQL_QUERY_SELECTOR = Selector
            .fromString("rawSQLQuery");
    public static final Selector PROCEDURE_QUERY_SELECTOR = Selector
            .fromString("procedureQuery");

    // query prototypes...
    protected Query objectSelectQuery = new SelectQuery();
    protected Query rawSQLQuery = new SQLTemplate(true);
    protected Query procedureQuery = new ProcedureQuery();

    protected Query selectedQuery;

    public QueryTypeModel() {
        selectedQuery = objectSelectQuery;
    }

    public Query getSelectedQuery() {
        return selectedQuery;
    }

    public void setSelectedQuery(Query selectedQuery) {
        this.selectedQuery = selectedQuery;
    }

    public boolean isObjectSelectQuery() {
        return selectedQuery == objectSelectQuery;
    }

    public void setObjectSelectQuery(boolean flag) {
        if (!flag && isObjectSelectQuery()) {
            selectedQuery = null;
        }
        else if (flag && !isObjectSelectQuery()) {
            selectedQuery = objectSelectQuery;
        }
    }

    public boolean isRawSQLQuery() {
        return selectedQuery == rawSQLQuery;
    }

    public void setRawSQLQuery(boolean flag) {
        if (!flag && isRawSQLQuery()) {
            selectedQuery = null;
        }
        else if (flag && !isRawSQLQuery()) {
            selectedQuery = rawSQLQuery;
        }
    }

    public boolean isProcedureQuery() {
        return selectedQuery == procedureQuery;
    }

    public void setProcedureQuery(boolean flag) {
        if (!flag && isProcedureQuery()) {
            selectedQuery = null;
        }
        else if (flag && !isProcedureQuery()) {
            selectedQuery = procedureQuery;
        }
    }
}