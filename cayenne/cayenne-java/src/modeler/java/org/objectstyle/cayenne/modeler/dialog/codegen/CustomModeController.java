/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.dialog.codegen;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;

import org.objectstyle.cayenne.gen.ClassGenerator;
import org.objectstyle.cayenne.gen.DefaultClassGenerator;
import org.objectstyle.cayenne.modeler.CodeTemplateManager;
import org.objectstyle.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.objectstyle.cayenne.modeler.pref.DataMapDefaults;
import org.objectstyle.cayenne.swing.BindingBuilder;
import org.objectstyle.cayenne.swing.ObjectBinding;
import org.objectstyle.cayenne.util.Util;

/**
 * A controller for the custom generation mode.
 * 
 * @author Andrus Adamchik
 */
public class CustomModeController extends GeneratorController {

    // correspond to non-public constants on MapClassGenerator.
    static final String MODE_DATAMAP = "datamap";
    static final String MODE_ENTITY = "entity";

    static final String DATA_MAP_MODE_LABEL = "One run per DataMap";
    static final String ENTITY_MODE_LABEL = "One run per each selected Entity";

    static final Map modesByLabel = new HashMap();
    static {
        modesByLabel.put(DATA_MAP_MODE_LABEL, MODE_DATAMAP);
        modesByLabel.put(ENTITY_MODE_LABEL, MODE_ENTITY);
    }

    protected CustomModePanel view;
    protected CodeTemplateManager templateManager;

    protected ObjectBinding superTemplate;
    protected ObjectBinding subTemplate;

    public CustomModeController(CodeGeneratorControllerBase parent) {
        super(parent);

        Object[] modeChoices = new Object[] {
                ENTITY_MODE_LABEL, DATA_MAP_MODE_LABEL
        };
        view.getGenerationMode().setModel(new DefaultComboBoxModel(modeChoices));

        Object[] versionChoices = new Object[] {
                ClassGenerator.VERSION_1_1, ClassGenerator.VERSION_1_2
        };
        view.getGeneratorVersion().setModel(new DefaultComboBoxModel(versionChoices));

        // bind preferences and init defaults...

        if (Util.isEmptyString(preferences.getSuperclassTemplate())) {
            preferences
                    .setSuperclassTemplate(CodeTemplateManager.STANDARD_SERVER_SUPERCLASS);
        }

        if (Util.isEmptyString(preferences.getSubclassTemplate())) {
            preferences.setSubclassTemplate(CodeTemplateManager.STANDARD_SERVER_SUBCLASS);
        }

        if (Util.isEmptyString(preferences.getProperty("mode"))) {
            preferences.setProperty("mode", MODE_ENTITY);
        }

        if (Util.isEmptyString(preferences.getProperty("version"))) {
            preferences.setProperty("version", ClassGenerator.VERSION_1_1);
        }

        if (Util.isEmptyString(preferences.getProperty("overwrite"))) {
            preferences.setBooleanProperty("overwrite", false);
        }

        if (Util.isEmptyString(preferences.getProperty("pairs"))) {
            preferences.setBooleanProperty("pairs", true);
        }

        if (Util.isEmptyString(preferences.getProperty("usePackagePath"))) {
            preferences.setBooleanProperty("usePackagePath", true);
        }

        if (Util.isEmptyString(preferences.getProperty("outputPattern"))) {
            preferences.setProperty("outputPattern", "*.java");
        }

        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        builder.bindToAction(view.getManageTemplatesLink(), "popPreferencesAction()");

        builder.bindToTextField(
                view.getSuperclassPackage(),
                "preferences.superclassPackage").updateView();

        builder.bindToComboSelection(
                view.getGenerationMode(),
                "preferences.property['mode']").updateView();

        builder.bindToComboSelection(
                view.getGeneratorVersion(),
                "preferences.property['version']").updateView();

        builder.bindToStateChange(
                view.getOverwrite(),
                "preferences.booleanProperty['overwrite']").updateView();
        builder
                .bindToStateChange(
                        view.getPairs(),
                        "preferences.booleanProperty['pairs']")
                .updateView();

        builder.bindToStateChange(
                view.getUsePackagePath(),
                "preferences.booleanProperty['usePackagePath']").updateView();

        subTemplate = builder.bindToComboSelection(
                view.getSubclassTemplate(),
                "preferences.subclassTemplate");

        superTemplate = builder.bindToComboSelection(
                view.getSuperclassTemplate(),
                "preferences.superclassTemplate");

        builder.bindToTextField(
                view.getOutputPattern(),
                "preferences.property['outputPattern']").updateView();

        updateTemplates();
    }

    protected DataMapDefaults createDefaults() {
        DataMapDefaults prefs = getApplication()
                .getFrameController()
                .getProjectController()
                .getDataMapPreferences("__custom");

        prefs.updateSuperclassPackage(getParentController().getDataMap(), false);
        this.preferences = prefs;
        return prefs;
    }

    protected void updateTemplates() {
        this.templateManager = getApplication().getCodeTemplateManager();

        List customTemplates = new ArrayList(templateManager
                .getCustomTemplates()
                .keySet());
        Collections.sort(customTemplates);

        List superTemplates = new ArrayList(templateManager
                .getStandardSuperclassTemplates()
                .keySet());
        Collections.sort(superTemplates);
        superTemplates.addAll(customTemplates);

        List subTemplates = new ArrayList(templateManager
                .getStandardSubclassTemplates()
                .keySet());
        Collections.sort(subTemplates);
        subTemplates.addAll(customTemplates);

        this.view.getSubclassTemplate().setModel(
                new DefaultComboBoxModel(subTemplates.toArray()));
        this.view.getSuperclassTemplate().setModel(
                new DefaultComboBoxModel(superTemplates.toArray()));

        superTemplate.updateView();
        subTemplate.updateView();
    }

    protected GeneratorControllerPanel createView() {
        this.view = new CustomModePanel();
        return view;
    }

    public Component getView() {
        return view;
    }

    public DefaultClassGenerator createGenerator() {

        DefaultClassGenerator generator = super.createGenerator();

        String mode = modesByLabel
                .get(view.getGenerationMode().getSelectedItem())
                .toString();
        generator.setMode(mode);

        String superKey = view.getSuperclassTemplate().getSelectedItem().toString();
        String superTemplate = templateManager.getTemplatePath(superKey);
        generator.setSuperTemplate(superTemplate);

        String subKey = view.getSubclassTemplate().getSelectedItem().toString();
        String subTemplate = templateManager.getTemplatePath(subKey);
        generator.setTemplate(subTemplate);

        if (view.getGeneratorVersion().getSelectedItem() != null) {
            generator.setVersionString(view
                    .getGeneratorVersion()
                    .getSelectedItem()
                    .toString());
        }

        generator.setOverwrite(view.getOverwrite().isSelected());
        generator.setUsePkgPath(view.getUsePackagePath().isSelected());
        generator.setMakePairs(view.getPairs().isSelected());

        if (!Util.isEmptyString(view.getOutputPattern().getText())) {
            generator.setOutputPattern(view.getOutputPattern().getText());
        }

        return generator;
    }

    public void popPreferencesAction() {
        new PreferenceDialog(getApplication().getFrameController())
                .startupAction(PreferenceDialog.TEMPLATES_KEY);
        updateTemplates();
    }
}
