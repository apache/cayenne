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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.ComboBoxAdapter;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.swing.components.JCayenneCheckBox;
import org.apache.cayenne.validation.ValidationException;

/**
 * @since 4.1
 */
public class CustomModePanel extends GeneratorControllerPanel {

    private ComboBoxAdapter<String> subclassTemplate;
    private ComboBoxAdapter<String> superclassTemplate;
    private ComboBoxAdapter<String> embeddableTemplate;
    private ComboBoxAdapter<String> embeddableSuperTemplate;
    private ComboBoxAdapter<String> queryTemplate;
    private ComboBoxAdapter<String> querySuperTemplate;
    private JCheckBox pairs;
    private JCheckBox overwrite;
    private JCheckBox usePackagePath;
    private TextAdapter outputPattern;
    private JCheckBox createPropertyNames;
    private JCheckBox pkProperties;
    private JCheckBox clientMode;
    private TextAdapter superPkg;

    private JButton manageTemplatesLink;

    CustomModePanel(ProjectController projectController, CodeGeneratorController codeGeneratorControllerBase) {
        super(projectController, codeGeneratorControllerBase);
        
        JComboBox<String> superclassField = new JComboBox<>();
        this.superclassTemplate = new ComboBoxAdapter<String>(superclassField) {
            @Override
            protected void updateModel(String item) throws ValidationException {
                CgenConfiguration cgenConfiguration = getCgenConfig();
                cgenConfiguration.setSuperTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(item,
                        cgenConfiguration.getDataMap().getConfigurationSource()));
                checkConfigDirty();
            }
        };

        JComboBox<String> subclassField = new JComboBox<>();
        this.subclassTemplate = new ComboBoxAdapter<String>(subclassField) {
            @Override
            protected void updateModel(String item) throws ValidationException {
                CgenConfiguration cgenConfiguration = getCgenConfig();
                cgenConfiguration.setTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(item,
                        cgenConfiguration.getDataMap().getConfigurationSource()));
                checkConfigDirty();
            }
        };
        
        JComboBox<String> embeddableField = new JComboBox<>();
        this.embeddableTemplate = new ComboBoxAdapter<String>(embeddableField) {
        	@Override
        	protected void updateModel(String item) throws ValidationException {
        		CgenConfiguration cgenConfiguration = getCgenConfig();
        		cgenConfiguration.setEmbeddableTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(item,
        				cgenConfiguration.getDataMap().getConfigurationSource()));
                checkConfigDirty();
        	}
        };
        
        JComboBox<String> embeddableSuperField = new JComboBox<>();
        this.embeddableSuperTemplate = new ComboBoxAdapter<String>(embeddableSuperField) {
        	@Override
        	protected void updateModel(String item) throws ValidationException {
        		CgenConfiguration cgenConfiguration = getCgenConfig();
        		cgenConfiguration.setEmbeddableSuperTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(item,
        				cgenConfiguration.getDataMap().getConfigurationSource()));
                checkConfigDirty();
        	}
        };
        
        JComboBox<String> queryField = new JComboBox<>();
        this.queryTemplate = new ComboBoxAdapter<String>(queryField) {
        	@Override
        	protected void updateModel(String item) throws ValidationException {
        		CgenConfiguration cgenConfiguration = getCgenConfig();
        		cgenConfiguration.setQueryTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(item,
        				cgenConfiguration.getDataMap().getConfigurationSource()));
                checkConfigDirty();
        	}
        };
        
        JComboBox<String> querySuperField = new JComboBox<>();
        this.querySuperTemplate = new ComboBoxAdapter<String>(querySuperField) {
        	@Override
        	protected void updateModel(String item) throws ValidationException {
        		CgenConfiguration cgenConfiguration = getCgenConfig();
        		cgenConfiguration.setQuerySuperTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(item,
        				cgenConfiguration.getDataMap().getConfigurationSource()));
                checkConfigDirty();
        	}
        };

        this.pairs = new JCayenneCheckBox();
        this.overwrite = new JCayenneCheckBox();
        this.usePackagePath = new JCayenneCheckBox();

        JTextField outputPatternField = new JTextField();
        this.outputPattern = new TextAdapter(outputPatternField) {
            protected void updateModel(String text) {
                getCgenConfig().setOutputPattern(text);
                checkConfigDirty();
            }
        };

        JTextField superPkgField = new JTextField();
        this.superPkg = new TextAdapter(superPkgField) {
            @Override
            protected void updateModel(String text) throws ValidationException {
                getCgenConfig().setSuperPkg(text);
                checkConfigDirty();
            }
        };

        this.createPropertyNames = new JCayenneCheckBox();
        this.pkProperties = new JCayenneCheckBox();
        this.clientMode= new JCayenneCheckBox();
        this.manageTemplatesLink = new JButton("Customize Templates...");
        this.manageTemplatesLink.setFont(manageTemplatesLink.getFont().deriveFont(10f));

        pairs.addChangeListener(e -> {
           setDisableSuperComboBoxes(pairs.isSelected());
            overwrite.setEnabled(!pairs.isSelected());
        });

        // assemble
        FormLayout layout = new FormLayout(
                "right:105dlu, 1dlu, fill:240:grow, 1dlu, left:100dlu, 100dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.append("Output Directory:", outputFolder.getComponent(), selectOutputFolder);
        builder.nextLine();

        builder.append("Subclass Template:", subclassTemplate.getComboBox());
        builder.nextLine();

        builder.append("Superclass Template:", superclassTemplate.getComboBox());
        builder.nextLine();
        
        builder.append("Embeddable Template:", embeddableTemplate.getComboBox());
        builder.nextLine();
        
        builder.append("Embeddable Superclass Template:", embeddableSuperTemplate.getComboBox());
        builder.nextLine();
        
        builder.append("DataMap Template:", queryTemplate.getComboBox());
        builder.nextLine();
        
        builder.append("DataMap Superclass Template:", querySuperTemplate.getComboBox());
        builder.nextLine();

        builder.append("Output Pattern:", outputPattern.getComponent());
        builder.nextLine();

        builder.append("Make Pairs:", pairs);
        builder.nextLine();

        builder.append("Use Package Path:", usePackagePath);
        builder.nextLine();

        builder.append("Overwrite Subclasses:", overwrite);
        builder.nextLine();

        builder.append("Create Property Names:", createPropertyNames);
        builder.nextLine();

        builder.append("Create PK properties:", pkProperties);
        builder.nextLine();

        builder.append("Client mode: ", clientMode);
        builder.nextLine();

        builder.append("Superclass package:", superPkg.getComponent());

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);

        JPanel links = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        links.add(manageTemplatesLink);
        add(links, BorderLayout.SOUTH);
    }

    public void setDisableSuperComboBoxes(boolean val){
        superclassTemplate.getComboBox().setEnabled(val);
        embeddableSuperTemplate.getComboBox().setEnabled(val);
        querySuperTemplate.getComboBox().setEnabled(val);
    }

    public JButton getManageTemplatesLink() {
        return manageTemplatesLink;
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

    public JCheckBox getOverwrite() {
        return overwrite;
    }

    public JCheckBox getPairs() {
        return pairs;
    }

    public JCheckBox getUsePackagePath() {
        return usePackagePath;
    }

    public TextAdapter getOutputPattern() {
        return outputPattern;
    }

    public JCheckBox getCreatePropertyNames() {
        return createPropertyNames;
    }

    public JCheckBox getPkProperties() {
        return pkProperties;
    }

    public JCheckBox getClientMode() {
        return clientMode;
    }

    public TextAdapter getSuperPkg() {
        return superPkg;
    }
}
