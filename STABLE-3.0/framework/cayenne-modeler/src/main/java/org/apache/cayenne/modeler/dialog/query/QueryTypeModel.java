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


package org.apache.cayenne.modeler.dialog.query;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.AbstractQuery;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.scopemvc.core.Selector;

/**
 */
public class QueryTypeModel {

    public static final Selector OBJECT_SELECT_QUERY_SELECTOR = Selector
            .fromString("objectSelectQuery");
    public static final Selector RAW_SQL_QUERY_SELECTOR = Selector
            .fromString("rawSQLQuery");
    public static final Selector PROCEDURE_QUERY_SELECTOR = Selector
            .fromString("procedureQuery");
    public static final Selector EJBQL_QUERY_SELECTOR = Selector
    .fromString("ejbqlQuery");
 
    // query prototypes...
    protected AbstractQuery objectSelectQuery;
    protected AbstractQuery rawSQLQuery;
    protected AbstractQuery procedureQuery;
    protected EJBQLQuery ejbqlQuery;

    protected Query selectedQuery;

    public QueryTypeModel(DataMap root) {
        // create query prototypes:
        objectSelectQuery = new SelectQuery();
        procedureQuery = new ProcedureQuery();

        SQLTemplate rawSQLQuery = new SQLTemplate();
        rawSQLQuery.setRoot(root);
        rawSQLQuery.setFetchingDataRows(true);
        this.rawSQLQuery = rawSQLQuery;
        
        ejbqlQuery = new EJBQLQuery();
        // by default use object query...
        selectedQuery = objectSelectQuery;
    }

    public Query getSelectedQuery() {
        return selectedQuery;
    }

    public void setSelectedQuery(AbstractQuery selectedQuery) {
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
    
    public boolean isEjbqlQuery() {
        return selectedQuery == ejbqlQuery;
    }

    public void setEjbqlQuery(boolean flag) {
        if (!flag && isEjbqlQuery()) {
            selectedQuery = null;
        }
        else if (flag && !isEjbqlQuery()) {
            selectedQuery = ejbqlQuery;
        }
    }
}
