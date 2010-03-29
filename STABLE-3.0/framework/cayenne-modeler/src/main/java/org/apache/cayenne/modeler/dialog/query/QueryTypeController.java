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

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.QueryEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.QueryDisplayEvent;
import org.apache.cayenne.modeler.undo.CreateQueryUndoableEdit;
import org.apache.cayenne.project.NamedObjectFactory;
import org.apache.cayenne.query.AbstractQuery;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.Query;
import org.scopemvc.controller.basic.BasicController;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;

/**
 * A Scope controller for QueryTypeDialog.
 * 
 */
public class QueryTypeController extends BasicController {

	public static final String CANCEL_CONTROL = "cayenne.modeler.queryType.cancel.button";
	public static final String CREATE_CONTROL = "cayenne.modeler.queryType.create.button";
	public static final String OBJECT_QUERY_CONTROL = "cayenne.modeler.queryType.selectQuery.radio";
	public static final String SQL_QUERY_CONTROL = "cayenne.modeler.queryType.sqlQuery.radio";
	public static final String PROCEDURE_QUERY_CONTROL = "cayenne.modeler.queryType.procedureQuery.radio";
	public static final String EJBQL_QUERY_CONTROL = "cayenne.modeler.queryType.ejbqlQuery.radio";

	protected ProjectController mediator;
	protected DataMap dataMap;
	protected DataDomain domain;
	protected Query query;

	public QueryTypeController(ProjectController mediator) {
		this.mediator = mediator;
		this.dataMap = mediator.getCurrentDataMap();
		this.domain = mediator.getCurrentDataDomain();
	}

	@Override
	protected void doHandleControl(Control control) throws ControlException {
		if (control.matchesID(CANCEL_CONTROL)) {
			shutdown();
		} else if (control.matchesID(CREATE_CONTROL)) {
			createQuery();
		} else if (control.matchesID(OBJECT_QUERY_CONTROL)) {
			// do nothing... need to match control
		} else if (control.matchesID(SQL_QUERY_CONTROL)) {
			// do nothing... need to match control
		} else if (control.matchesID(PROCEDURE_QUERY_CONTROL)) {
			// do nothing... need to match control
		} else if (control.matchesID(EJBQL_QUERY_CONTROL)) {
			// do nothing... need to match control
		}
	}

	/**
	 * Creates and runs QueryTypeDialog.
	 */
	@Override
	public void startup() {
		setModel(new QueryTypeModel(mediator.getCurrentDataMap()));
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
		if (query instanceof EJBQLQuery) {
			((EJBQLQuery) query).setName(queryName);
		} else {
			((AbstractQuery) query).setName(queryName);
		}
		
		dataMap.addQuery(query);

		mediator.getApplication().getUndoManager().addEdit(
				new CreateQueryUndoableEdit(domain, dataMap, query));

		// notify listeners
		fireQueryEvent(this, mediator, domain, dataMap, query);
		shutdown();
	}

	/**
	 * Fires events when a query was added
	 */
	public static void fireQueryEvent(Object src, ProjectController mediator,
			DataDomain domain, DataMap dataMap, Query query) {
		mediator.fireQueryEvent(new QueryEvent(src, query, MapEvent.ADD,
				dataMap));
		mediator.fireQueryDisplayEvent(new QueryDisplayEvent(src, query,
				dataMap, domain));
	}
}
