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
package org.apache.cayenne.modeler.dialog.datadomain;

import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.swing.BindingBuilder;

import javax.swing.JCheckBox;
import javax.swing.JPopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FilterDialog extends JPopupMenu {

	private JCheckBox dbEntity;
	private JCheckBox objEntity;
	private JCheckBox embeddable;
	private JCheckBox procedure;
	private JCheckBox query;
	private JCheckBox all;
	private ProjectController eventController;
	private FilterController filterController;

	/* NOTE: Setters and getters are used by view bindings, don't delete them */

	public Boolean getDbEntityFilter() {
		return filterController.getFilterMap().get("dbEntity");
	}
	public void setDbEntityFilter(Boolean value) {
		filterController.getFilterMap().put("dbEntity", value);
	}
	
	public Boolean getObjEntityFilter() {
		return filterController.getFilterMap().get("objEntity");
	}

	public void setObjEntityFilter(Boolean value) {
		filterController.getFilterMap().put("objEntity", value);
	}
	
	public Boolean getEmbeddableFilter() {
		return filterController.getFilterMap().get("embeddable");
	}
	public void setEmbeddableFilter(Boolean value) {
		filterController.getFilterMap().put("embeddable", value);
	}
	
	public Boolean getProcedureFilter() {
		return filterController.getFilterMap().get("procedure");
	}

	public void setProcedureFilter(Boolean value) {
		filterController.getFilterMap().put("procedure", value);
	}
	
	public Boolean getQueryFilter() {
		return filterController.getFilterMap().get("query");
	}
	public void setQueryFilter(Boolean value) {
		filterController.getFilterMap().put("query", value);
	}

	public Boolean getAllFilter() {
		for(Boolean selected : filterController.getFilterMap().values()) {
			if(!selected) {
				return false;
			}
		}

		return true;
	}

	public void setAllFilter(Boolean value) {
	}
	
	public FilterDialog(FilterController filterController){
		super();
		this.filterController=filterController;
		this.eventController=filterController.getEventController();
		initView(); 
		initController();
	}
	
	public void initView(){

		all = new JCheckBox("Show all");
		dbEntity = new JCheckBox("DbEntity");
		objEntity = new JCheckBox("ObjEntity");
		embeddable = new JCheckBox("Embeddable");
		procedure = new JCheckBox("Procedure");
		query = new JCheckBox("Query");

		add(all);
		addSeparator();
		add(dbEntity);
		add(objEntity);
		add(embeddable);
		add(procedure);
		add(query);
	}

	private void initController() {
		BindingBuilder builder = new BindingBuilder(
			  eventController.getApplication().getBindingFactory(),
			  this);

		builder.bindToStateChange(dbEntity, "dbEntityFilter").updateView();
		builder.bindToStateChange(objEntity, "objEntityFilter").updateView();
		builder.bindToStateChange(embeddable, "embeddableFilter").updateView();
		builder.bindToStateChange(procedure, "procedureFilter").updateView();
		builder.bindToStateChange(query, "queryFilter").updateView();
		builder.bindToStateChange(all, "allFilter").updateView();

		dbEntity.addActionListener(new CheckListener("dbEntity"));
		objEntity.addActionListener(new CheckListener("objEntity"));
		embeddable.addActionListener(new CheckListener("embeddable"));
		procedure.addActionListener(new CheckListener("procedure"));
		query.addActionListener(new CheckListener("query"));

        all.setEnabled(false);
		all.addActionListener(e -> {
			dbEntity.setSelected(true);
			objEntity.setSelected(true);
			embeddable.setSelected(true);
			procedure.setSelected(true);
			query.setSelected(true);
			all.setEnabled(false);

			filterController.getTreeModel().setFiltered(filterController.getFilterMap());
			filterController.getTree().updateUI();
		});
	}

	void checkAllStates() {
		if(!getAllFilter()) {
			all.setSelected(false);
			all.setEnabled(true);
		} else {
			all.setSelected(true);
			all.setEnabled(false);
		}
	}

	private class CheckListener implements ActionListener {

		String key;

		public CheckListener(String key) {
			this.key = key;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			filterController.getFilterMap().put(key, ((JCheckBox) e.getSource()).isSelected());
			filterController.getTreeModel().setFiltered(filterController.getFilterMap());
			filterController.getTree().updateUI();
            checkAllStates();
		}
	}
}