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

package org.apache.cayenne.modeler.dialog.codegen;

import java.awt.Component;

import javax.swing.JOptionPane;

import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.dialog.ErrorDebugDialog;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A controller for the class generator dialog.
 * 
 */
public class CodeGeneratorController extends CodeGeneratorControllerBase {
    /**
     * Logger to print stack traces
     */
    private static Log logObj = LogFactory.getLog(ErrorDebugDialog.class);

    protected CodeGeneratorDialog view;

    protected ClassesTabController clessSelector;
    protected GeneratorTabController generatorSelector;

    public CodeGeneratorController(CayenneController parent, DataMap dataMap) {
        super(parent, dataMap);

        this.clessSelector = new ClassesTabController(this);
        this.generatorSelector = new GeneratorTabController(this);
    }

    @Override
    public Component getView() {
        return view;
    }

    public void startup() {
        // show dialog even on empty DataMap, as custom generation may still take
        // advantage of it

        view = new CodeGeneratorDialog(generatorSelector.getView(), clessSelector
                .getView());
        initBindings();

        view.pack();
        view.setModal(true);
        centerView();
        makeCloseableOnEscape();
        view.setVisible(true);
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        builder.bindToAction(view.getCancelButton(), "cancelAction()");
        builder.bindToAction(view.getGenerateButton(), "generateAction()");
        builder.bindToAction(this, "classesSelectedAction()", SELECTED_PROPERTY);
        builder.bindToAction(
                generatorSelector,
                "generatorSelectedAction()",
                GeneratorTabController.GENERATOR_PROPERTY);

        generatorSelectedAction();
    }

    public void generatorSelectedAction() {
        GeneratorController controller = generatorSelector.getGeneratorController();
        validate(controller);

        Predicate predicate = controller != null
                ? controller.getDefaultClassFilter()
                : PredicateUtils.falsePredicate();

        updateSelection(predicate);
        clessSelector.classSelectedAction();
    }

    public void classesSelectedAction() {
        int size = getSelectedEntitiesSize();
        String label;

        if (size == 0) {
            label = "No entities selected";
        }
        else if (size == 1) {
            label = "One entity selected";
        }
        else {
            label = size + " entities selected";
        }

        label = label.concat("; ");
        
        int sizeEmb = getSelectedEmbeddablesSize();

        if (sizeEmb == 0) {
            label = label + "No embeddables selected";
        }
        else if (sizeEmb == 1) {
            label = label + "One embeddable selected";
        }
        else {
            label =label + sizeEmb + " embeddables selected";
        }
        
        view.getClassesCount().setText(label);
    }

    public void cancelAction() {
        view.dispose();
    }

    public void generateAction() {
        ClassGenerationAction generator = generatorSelector.getGenerator();

        if (generator != null) {
            try {
                generator.execute();
                JOptionPane.showMessageDialog(
                        this.getView(),
                        "Class generation finished");
            }
            catch (Exception e) {
                logObj.error("Error generating classes", e);
                JOptionPane.showMessageDialog(
                        this.getView(),
                        "Error generating classes - " + e.getMessage());
            }
        }

        view.dispose();
    }
}
