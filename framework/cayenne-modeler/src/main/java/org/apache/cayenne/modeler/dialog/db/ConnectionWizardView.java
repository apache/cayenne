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
package org.apache.cayenne.modeler.dialog.db;

import javax.swing.JComboBox;

import org.apache.cayenne.modeler.dialog.pref.DBConnectionInfoEditorView;
import org.apache.cayenne.modeler.util.CayenneController;

import com.jgoodies.forms.builder.DefaultFormBuilder;

/**
 * Dialog special for reverse engineering. Has one special combobox for
 * choosing naming strategy 
 * @author Andrey Razumovsky
 */
public class ConnectionWizardView extends DataSourceWizardView {
    /**
     * Combobox for naming strategy
     */
    protected JComboBox strategyCombo;
    
    public ConnectionWizardView(CayenneController controller) {
        super(controller);
        
        strategyCombo = new JComboBox();
        strategyCombo.setEditable(true);
        
        DefaultFormBuilder builder = ((DBConnectionInfoEditorView) connectionInfo.getView()).getBuilder();
        builder.append("Naming Strategy:", strategyCombo);
    }
    
    /**
     * @return combobox for naming strategy
     */
    public JComboBox getStrategyComboBox() {
        return strategyCombo;
    }
}
