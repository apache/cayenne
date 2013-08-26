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
package org.apache.cayenne.modeler.dialog.datadomain;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.MenuSelectionManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI;

import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.swing.BindingBuilder;

public class FilterDialog extends JPopupMenu {
	
	private String SHOW_ALL = "Show all";
	
	private JCheckBoxMenuItem dbEntity;
	private JCheckBoxMenuItem objEntity;
	private JCheckBoxMenuItem embeddable;
	private JCheckBoxMenuItem procedure;
	private JCheckBoxMenuItem query;
	private JCheckBoxMenuItem all;
	private ProjectController eventController;
	private FilterController filterController;
	
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
		Set<String> keys=filterController.getFilterMap().keySet();
		
		for(String key : keys) {
			if(filterController.getFilterMap().get(key) != true) {
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
		
		all = new JCheckBoxMenuItem(SHOW_ALL);
		dbEntity = new JCheckBoxMenuItem("DbEntity");	
		objEntity = new JCheckBoxMenuItem("ObjEntity");
		embeddable = new JCheckBoxMenuItem("Embeddable");
		procedure = new JCheckBoxMenuItem("Procedure");
		query = new JCheckBoxMenuItem("Query");
		
		all.setUI(new StayOpenCheckBoxMenuItemUI());
		dbEntity.setUI(new StayOpenCheckBoxMenuItemUI());
		objEntity.setUI(new StayOpenCheckBoxMenuItemUI());
		embeddable.setUI(new StayOpenCheckBoxMenuItemUI());
		procedure.setUI(new StayOpenCheckBoxMenuItemUI());
		query.setUI(new StayOpenCheckBoxMenuItemUI());
		
		add(all);
		add(new JSeparator());
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
	  
		all.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent e) {
				dbEntity.setState(true);
				objEntity.setState(true);
				embeddable.setState(true);
				procedure.setState(true);
				query.setState(true);
				all.setEnabled(false);

				filterController.getTreeModel().setFiltered(filterController.getFilterMap());	
				filterController.treeExpOrCollPath("expand");
			}
		});
	}	

	void checkAllStates() {
		if(!isAll()) {
			all.setState(false);
			all.setEnabled(true);
		}
		else {
			all.setState(true);
			all.setEnabled(false);		
		}
	}
	
	private boolean isAll() {
		Set<String> keys=filterController.getFilterMap().keySet();
		
		for(String key : keys) {
			if(filterController.getFilterMap().get(key) == false) {
				return false;
			}
		}

		return true;
	}
	
	
	private class CheckListener implements ActionListener {

		String key;
		
		public CheckListener(String key) {
			this.key = key;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			filterController.getFilterMap().put(key, ((JCheckBoxMenuItem)e.getSource()).getState());
			filterController.getTreeModel().setFiltered(filterController.getFilterMap());
			filterController.treeExpOrCollPath("expand");
			checkAllStates();
		}
	}
	
	public static class StayOpenCheckBoxMenuItemUI extends BasicCheckBoxMenuItemUI {  
		  
		   @Override  
		   protected void doClick(MenuSelectionManager msm) {  
		      menuItem.doClick();  
		   }  
		  
		   public static ComponentUI createUI(JComponent c) {  
		      return new StayOpenCheckBoxMenuItemUI();  
		   }  
		}  
}