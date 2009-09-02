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
package org.apache.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.util.EventObject;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.event.EmbeddableEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.event.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableDisplayListener;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.util.Util;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class EmbeddableTab extends JPanel implements EmbeddableDisplayListener {

    protected ProjectController mediator;
    protected TextAdapter className;

    public EmbeddableTab(ProjectController mediator) {
        this.mediator = mediator;
        initView();
        initController();
    }

    private void initController() {
        mediator.addEmbeddableDisplayListener(this);

    }

    private void initView() {
        this.setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        Application app = Application.getInstance();
        toolBar.add(app.getAction(CreateAttributeAction.getActionName()).buildButton());

        add(toolBar, BorderLayout.NORTH);

        className = new TextAdapter(new JTextField()) {

            @Override
            protected void updateModel(String text) {
                setClassName(text);
            }
        };

        FormLayout layout = new FormLayout(
                "right:50dlu, 3dlu, fill:150dlu, 3dlu, fill:100",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.append("Class Name:", className.getComponent(), 3);

        add(builder.getPanel(), BorderLayout.CENTER);
    }

    public void processExistingSelection(EventObject e) {
        EmbeddableDisplayEvent ede = new EmbeddableDisplayEvent(this, mediator
                .getCurrentEmbeddable(), mediator.getCurrentDataMap(), mediator
                .getCurrentDataDomain());
        mediator.fireEmbeddableDisplayEvent(ede);
    }

    void setClassName(String newClassName) {
        if (newClassName != null && newClassName.trim().length() == 0) {
            newClassName = null;
        }

        Embeddable embeddable = mediator.getCurrentEmbeddable();
        
        if (embeddable == null) {
            return;
        }
        
        if (Util.nullSafeEquals(newClassName, embeddable.getClassName())) {
            return;
        }

        // completely new name, set new name for entity
        EmbeddableEvent e = new EmbeddableEvent(this, embeddable, embeddable.getClassName());
        embeddable.setClassName(newClassName);
        
        mediator.fireEmbeddableEvent(e, mediator.getCurrentDataMap());
    }

    public void currentEmbeddableChanged(EmbeddableDisplayEvent e) {
        Embeddable embeddable = e.getEmbeddable();
        if (embeddable == null || !e.isEmbeddableChanged()) {
            return;
        }
        initFromModel(embeddable);
    }

    private void initFromModel(Embeddable embeddable) {
        className.setText(embeddable.getClassName());
    }
}
