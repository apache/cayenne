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

package org.apache.cayenne.modeler.editor.cgen;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.ComboBoxAdapter;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.function.BiConsumer;

/**
 * @since 4.1
 */
public class CustomModePanel extends StandardModePanel {

    private static final String TEMPLATE_LAYOUT = "pref:grow, 1dlu, fill:240:grow, 1dlu, left:150dlu, 100dlu";
    private ComboBoxAdapter<String> subclassTemplate;
    private ComboBoxAdapter<String> superclassTemplate;
    private ComboBoxAdapter<String> embeddableTemplate;
    private ComboBoxAdapter<String> embeddableSuperTemplate;
    private ComboBoxAdapter<String> queryTemplate;
    private ComboBoxAdapter<String> querySuperTemplate;
    private JButton templateManagerButton;

    CustomModePanel(CodeGeneratorController codeGeneratorControllerBase) {
        super(codeGeneratorControllerBase);
    }


    @Override
    protected void addCustomModeFields() {
        checkBoxBuilder.append(clientMode);
        checkBoxBuilder.append("Client mode");
        dataFieldsBuilder.append(" Output Pattern");
        dataFieldsBuilder.nextLine();
        dataFieldsBuilder.append(outputPattern.getComponent());
        dataFieldsBuilder.nextLine();
    }

    @Override
    protected void addTemplatePanel() {
        checkBoxPanel.setVisible(true);
        dataFieldsPanel.setVisible(true);
        initTemplatesSelector();
        add(createTemplatePanel(), BorderLayout.CENTER);
    }

    class StringComboBoxAdapter extends ComboBoxAdapter<String> {
        private final BiConsumer<CgenConfiguration, String> setTemplate;

        public StringComboBoxAdapter(JComboBox<String> subclassField, BiConsumer<CgenConfiguration, String> setter) {
            super(subclassField);
            this.setTemplate = setter;
        }

        @Override
        protected void updateModel(String item) throws ValidationException {
            CgenConfiguration cgenConfiguration = getCgenConfig();
            String templatePath = Application.getInstance()
                    .getCodeTemplateManager()
                    .getTemplatePath(item, cgenConfiguration.getDataMap().getConfigurationSource());
            setTemplate.accept(cgenConfiguration, templatePath);
            checkConfigDirty();
        }
    }

    private void initTemplatesSelector() {
        JComboBox<String> subclassField = new JComboBox<>();
        this.subclassTemplate = new StringComboBoxAdapter(subclassField, CgenConfiguration::setTemplate);
        JComboBox<String> superclassField = new JComboBox<>();
        this.superclassTemplate = new StringComboBoxAdapter(superclassField, CgenConfiguration::setSuperTemplate);
        JComboBox<String> embeddableField = new JComboBox<>();
        this.embeddableTemplate = new StringComboBoxAdapter(embeddableField, CgenConfiguration::setEmbeddableTemplate);
        JComboBox<String> embeddableSuperField = new JComboBox<>();
        this.embeddableSuperTemplate = new StringComboBoxAdapter(embeddableSuperField, CgenConfiguration::setEmbeddableSuperTemplate);
        JComboBox<String> queryField = new JComboBox<>();
        this.queryTemplate = new StringComboBoxAdapter(queryField, CgenConfiguration::setQueryTemplate);
        JComboBox<String> querySuper = new JComboBox<>();
        this.querySuperTemplate = new StringComboBoxAdapter(querySuper, CgenConfiguration::setQuerySuperTemplate);
    }


    private JPanel createTemplatePanel() {

        FormLayout templateLayout = new FormLayout(TEMPLATE_LAYOUT, "");
        DefaultFormBuilder templateBuilder = new DefaultFormBuilder(templateLayout);
        templateBuilder.setDefaultDialogBorder();

        templateBuilder.append(new JLabel(" Templates"));
        templateBuilder.nextLine();
        templateBuilder.append(subclassTemplate.getComboBox());
        templateBuilder.append("Subclass");
        templateBuilder.nextLine();

        templateBuilder.append(superclassTemplate.getComboBox());
        templateBuilder.append("Superclass");
        templateBuilder.nextLine();

        templateBuilder.append(embeddableTemplate.getComboBox());
        templateBuilder.append("Embeddable");
        templateBuilder.nextLine();

        templateBuilder.append(embeddableSuperTemplate.getComboBox());
        templateBuilder.append("Embeddable Superclass");
        templateBuilder.nextLine();

        templateBuilder.append(queryTemplate.getComboBox());
        templateBuilder.append("DataMap");
        templateBuilder.nextLine();

        templateBuilder.append(querySuperTemplate.getComboBox());
        templateBuilder.append("DataMap Superclass");
        templateBuilder.nextLine();
        templateBuilder.append("");
        templateBuilder.nextLine();

        this.templateManagerButton = new JButton("Template manager");
        this.templateManagerButton.setFont(templateManagerButton.getFont().deriveFont(14f));

        templateBuilder.append(templateManagerButton);

        return templateBuilder.getPanel();
    }


    public void setDisableSuperComboBoxes(boolean val) {
        superclassTemplate.getComboBox().setEnabled(val);
        embeddableSuperTemplate.getComboBox().setEnabled(val);
        querySuperTemplate.getComboBox().setEnabled(val);
    }

    public JButton getTemplateManagerButton() {
        return templateManagerButton;
    }

    public ComboBoxAdapter<String> getSubclassTemplate() {
        return subclassTemplate;
    }

    public ComboBoxAdapter<String> getSuperclassTemplate() {
        return superclassTemplate;
    }

    public ComboBoxAdapter<String> getEmbeddableTemplate() {
        return embeddableTemplate;
    }

    public ComboBoxAdapter<String> getEmbeddableSuperTemplate() {
        return embeddableSuperTemplate;
    }

    public ComboBoxAdapter<String> getQueryTemplate() {
        return queryTemplate;
    }

    public ComboBoxAdapter<String> getQuerySuperTemplate() {
        return querySuperTemplate;
    }


}
