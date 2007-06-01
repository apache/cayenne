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

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.event.MapEvent;
import org.objectstyle.cayenne.map.event.QueryEvent;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.event.QueryDisplayEvent;
import org.objectstyle.cayenne.project.NamedObjectFactory;
import org.objectstyle.cayenne.query.Query;
import org.scopemvc.controller.basic.BasicController;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;

/**
 * A Scope controller for QueryTypeDialog.
 * 
 * @author Andrei Adamchik
 */
public class QueryTypeController extends BasicController {

    public static final String CANCEL_CONTROL = "cayenne.modeler.queryType.cancel.button";
    public static final String CREATE_CONTROL = "cayenne.modeler.queryType.create.button";
    public static final String OBJECT_QUERY_CONTROL = "cayenne.modeler.queryType.selectQuery.radio";
    public static final String SQL_QUERY_CONTROL = "cayenne.modeler.queryType.sqlQuery.radio";
    public static final String PROCEDURE_QUERY_CONTROL = "cayenne.modeler.queryType.procedureQuery.radio";

    protected ProjectController mediator;
    protected DataMap dataMap;
    protected DataDomain domain;
    protected Query query;

    public QueryTypeController(ProjectController mediator) {
        this.mediator = mediator;
        this.dataMap = mediator.getCurrentDataMap();
        this.domain = mediator.getCurrentDataDomain();
    }

    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(CANCEL_CONTROL)) {
            shutdown();
        }
        else if (control.matchesID(CREATE_CONTROL)) {
            createQuery();
        }
        else if (control.matchesID(OBJECT_QUERY_CONTROL)) {
            // do nothing... need to match control
        }
        else if (control.matchesID(SQL_QUERY_CONTROL)) {
            // do nothing... need to match control
        }
        else if (control.matchesID(PROCEDURE_QUERY_CONTROL)) {
            // do nothing... need to match control
        }
    }

    /**
     * Creates and runs QueryTypeDialog.
     */
    public void startup() {
        setModel(new QueryTypeModel());
        setView(new QueryTypeDialog());
        showView();
    }

    /**
     * Action method that creates a query for the specified query type.
     */
    public void createQuery() {
        QueryTypeModel model = (QueryTypeModel) getModel();
        Query query = model.getSelectedQuery();
        if (query == null) {
            // wha?
            return;
        }

        // update query...
        String queryName = NamedObjectFactory.createName(Query.class, dataMap);
        query.setName(queryName);
        dataMap.addQuery(query);

        // notify listeners
        mediator.fireQueryEvent(new QueryEvent(this, query, MapEvent.ADD));
        mediator
                .fireQueryDisplayEvent(new QueryDisplayEvent(this, query, dataMap, domain));
        shutdown();
    }
}