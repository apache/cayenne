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
package org.apache.cayenne.modeler.action;

import java.awt.event.ActionEvent;

import javax.swing.JButton;
import org.apache.cayenne.modeler.Application;

import org.apache.cayenne.modeler.dialog.datadomain.FilterDialog;
import org.apache.cayenne.modeler.util.CayenneAction;

public class FilterAction extends CayenneAction{
    
	private FilterDialog filterDialog = null;
	
    public static String getActionName() {
        return "Filter tree";
    }

	public FilterAction(Application application) {
        super(getActionName(), application);
    }
	
    @Override
    public String getIconName() {
        return "icon-filter.png";
    }

	@Override
	public void performAction(ActionEvent e) {
		JButton source = (JButton)e.getSource();
        if(filterDialog == null) {
            filterDialog = new FilterDialog(getApplication().getFrameController().getEditorView().getFilterController());
        }
		filterDialog.pack();
		filterDialog.show(source, 0, source.getHeight());
	}

    public void resetDialog() {
        filterDialog = null;
    }
}