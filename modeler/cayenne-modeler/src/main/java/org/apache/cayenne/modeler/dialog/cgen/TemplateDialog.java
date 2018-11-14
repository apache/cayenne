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
package org.apache.cayenne.modeler.dialog.cgen;

import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.modeler.editor.cgen.CustomModeController;
import org.apache.cayenne.modeler.util.CayenneController;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

/**
 * @since 4.1
 */
public class TemplateDialog extends CayenneController {

    protected TemplateDialogView view;
    private CgenConfiguration cgenConfiguration;
    private String template;
    private String superTemplate;

    public TemplateDialog(final CayenneController parent, CgenConfiguration cgenConfiguration, String template, String superTemplate) {
        super(parent);
        this.cgenConfiguration = cgenConfiguration;
        this.template = template;
        this.superTemplate = superTemplate;
        final Window parentView = parent.getView() instanceof Window
                ? (Window) parent.getView()
                : SwingUtilities.getWindowAncestor(parent.getView());
        this.view = (parentView instanceof Dialog)
                ? new TemplateDialogView((Dialog) parentView, template, superTemplate)
                : new TemplateDialogView((Frame) parentView, template, superTemplate);
        initListeners();
    }

    public void startupAction() {
        view.pack();
        // show
        centerView();
        makeCloseableOnEscape();
        view.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        view.setModal(true);
        view.setVisible(true);
    }

    @Override
    public Component getView() {
        return view;
    }

    private void initListeners() {
        view.getUseDefault().addActionListener(action -> {
            if(template != null) {
                cgenConfiguration.setTemplate(ClassGenerationAction.SUBCLASS_TEMPLATE);
            }
            if(superTemplate != null) {
                cgenConfiguration.setSuperTemplate(ClassGenerationAction.SUPERCLASS_TEMPLATE);
            }
            view.dispose();
        });
        view.getAddTemplate().addActionListener(action -> {
            ((CustomModeController)parent).addTemplateAction(template, superTemplate);
            view.dispose();
        });
    }
}
