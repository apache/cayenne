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
import java.io.File;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.apache.commons.collections.Predicate;
import org.apache.cayenne.gen.DefaultClassGenerator;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.pref.GeneralPreferences;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.CodeValidationUtil;
import org.apache.cayenne.pref.Domain;
import org.apache.cayenne.pref.PreferenceDetail;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.BeanValidationFailure;
import org.apache.cayenne.validation.SimpleValidationFailure;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

/**
 * A mode-specific part of the code generation dialog.
 * 
 * @author Andrus Adamchik
 */
public abstract class GeneratorController extends CayenneController {

    protected DataMapDefaults preferences;

    public GeneratorController(CodeGeneratorControllerBase parent) {
        super(parent);

        createDefaults();
        createView();
        initBindings(new BindingBuilder(getApplication().getBindingFactory(), this));
    }

    protected void initBindings(BindingBuilder bindingBuilder) {
        if (preferences.getOutputPath() == null) {
            // init default directory..
            FSPath lastPath = Application
                    .getInstance()
                    .getFrameController()
                    .getLastDirectory();
            File lastDir = (lastPath != null)
                    ? lastPath.getExistingDirectory(false)
                    : null;
            preferences.setOutputPath(lastDir != null ? lastDir.getAbsolutePath() : null);
        }

        JTextField outputFolder = ((GeneratorControllerPanel) getView())
                .getOutputFolder();
        JButton outputSelect = ((GeneratorControllerPanel) getView())
                .getSelectOutputFolder();

        outputFolder.setText(preferences.getOutputPath());
        bindingBuilder.bindToAction(outputSelect, "selectOutputFolderAction()");
    }

    protected CodeGeneratorControllerBase getParentController() {
        return (CodeGeneratorControllerBase) getParent();
    }

    protected abstract GeneratorControllerPanel createView();

    protected abstract DataMapDefaults createDefaults();

    /**
     * Creates a class generator for provided selections.
     */
    public DefaultClassGenerator createGenerator() {

        File outputDir = getOutputDir();

        // no destination folder
        if (outputDir == null) {
            JOptionPane.showMessageDialog(
                    (Component) this.getView(),
                    "Select directory for source files.");
            return null;
        }

        // no such folder
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            JOptionPane.showMessageDialog(
                    (Component) this.getView(),
                    "Can't create directory " + outputDir + ". Select a different one.");
            return null;
        }

        // not a directory
        if (!outputDir.isDirectory()) {
            JOptionPane.showMessageDialog((Component) this.getView(), outputDir
                    + " is not a valid directory.");
            return null;
        }

        DefaultClassGenerator generator = new DefaultClassGenerator(getParentController()
                .getDataMap(), getParentController().getSelectedEntities());

        // configure encoding from preferences
        Domain generatorPrefs = Application
                .getInstance()
                .getPreferenceDomain()
                .getSubdomain(DefaultClassGenerator.class);

        PreferenceDetail detail = generatorPrefs.getDetail(
                GeneralPreferences.ENCODING_PREFERENCE,
                false);
        if (detail != null) {
            generator.setEncoding(detail
                    .getProperty(GeneralPreferences.ENCODING_PREFERENCE));
        }

        generator.setDestDir(outputDir);
        generator.setMakePairs(true);

        String superPackage = ((GeneratorControllerPanel) getView())
                .getSuperclassPackage()
                .getText();
        if (!Util.isEmptyString(superPackage)) {
            generator.setSuperPkg(superPackage);
        }

        return generator;
    }

    public void validateEntity(
            ValidationResult validationBuffer,
            ObjEntity entity,
            boolean clientValidation) {

        ValidationFailure entityFailure = validateEntity(clientValidation ? entity
                .getClientEntity() : entity);
        if (entityFailure != null) {
            validationBuffer.addFailure(entityFailure);
            return;
        }

        {
            Iterator it = entity.getAttributes().iterator();
            while (it.hasNext()) {
                ValidationFailure failure = validateAttribute((ObjAttribute) it.next());
                if (failure != null) {
                    validationBuffer.addFailure(failure);
                    return;
                }
            }
        }

        {
            Iterator it = entity.getRelationships().iterator();
            while (it.hasNext()) {

                ValidationFailure failure = validateRelationship((ObjRelationship) it
                        .next(), clientValidation);
                if (failure != null) {
                    validationBuffer.addFailure(failure);
                    return;
                }
            }
        }
    }

    protected ValidationFailure validateEntity(ObjEntity entity) {

        String name = entity.getName();

        if (entity.isGeneric()) {
            return new SimpleValidationFailure(name, "Generic class");
        }

        ValidationFailure emptyClass = BeanValidationFailure.validateNotEmpty(
                name,
                "className",
                entity.getClassName());
        if (emptyClass != null) {
            return emptyClass;
        }

        ValidationFailure badClass = BeanValidationFailure.validateJavaClassName(
                name,
                "className",
                entity.getClassName());
        if (badClass != null) {
            return badClass;
        }

        if (entity.getSuperClassName() != null) {
            ValidationFailure badSuperClass = BeanValidationFailure
                    .validateJavaClassName(name, "superClassName", entity
                            .getSuperClassName());
            if (badSuperClass != null) {
                return badSuperClass;
            }
        }

        return null;
    }

    protected ValidationFailure validateAttribute(ObjAttribute attribute) {

        String name = attribute.getEntity().getName();

        ValidationFailure emptyName = BeanValidationFailure.validateNotEmpty(
                name,
                "attribute.name",
                attribute.getName());
        if (emptyName != null) {
            return emptyName;
        }

        ValidationFailure badName = CodeValidationUtil.validateJavaIdentifier(
                name,
                "attribute.name",
                attribute.getName());
        if (badName != null) {
            return badName;
        }

        ValidationFailure emptyType = BeanValidationFailure.validateNotEmpty(
                name,
                "attribute.type",
                attribute.getType());
        if (emptyType != null) {
            return emptyType;
        }

        ValidationFailure badType = BeanValidationFailure.validateJavaClassName(
                name,
                "attribute.type",
                attribute.getType());
        if (badType != null) {
            return badType;
        }

        return null;
    }

    protected ValidationFailure validateRelationship(
            ObjRelationship relationship,
            boolean clientValidation) {

        String name = relationship.getSourceEntity().getName();

        ValidationFailure emptyName = BeanValidationFailure.validateNotEmpty(
                name,
                "relationship.name",
                relationship.getName());
        if (emptyName != null) {
            return emptyName;
        }

        ValidationFailure badName = CodeValidationUtil.validateJavaIdentifier(
                name,
                "relationship.name",
                relationship.getName());
        if (badName != null) {
            return badName;
        }

        if (!relationship.isToMany()) {

            ObjEntity targetEntity = (ObjEntity) relationship.getTargetEntity();

            if (clientValidation && targetEntity != null) {
                targetEntity = targetEntity.getClientEntity();
            }

            if (targetEntity == null) {

                return new BeanValidationFailure(
                        name,
                        "relationship.targetEntity",
                        "No target entity");
            }
            else if (!targetEntity.isGeneric()) {
                ValidationFailure emptyClass = BeanValidationFailure.validateNotEmpty(
                        name,
                        "relationship.targetEntity.className",
                        targetEntity.getClassName());
                if (emptyClass != null) {
                    return emptyClass;
                }

                ValidationFailure badClass = BeanValidationFailure.validateJavaClassName(
                        name,
                        "relationship.targetEntity.className",
                        targetEntity.getClassName());
                if (badClass != null) {
                    return badClass;
                }
            }
        }

        return null;
    }

    /**
     * Returns a predicate for default entity selection in a given mode.
     */
    public Predicate getDefaultEntityFilter() {
        final ObjEntity selectedEntity = Application
                .getInstance()
                .getFrameController()
                .getProjectController()
                .getCurrentObjEntity();

        // select a single entity
        if (selectedEntity != null) {
            final boolean hasProblem = getParentController().getProblem(
                    selectedEntity.getName()) != null;

            return new Predicate() {

                public boolean evaluate(Object object) {
                    return !hasProblem && object == selectedEntity;
                }
            };
        }
        // select all entities
        else {

            return new Predicate() {

                public boolean evaluate(Object object) {
                    if (object instanceof ObjEntity) {
                        return getParentController().getProblem(
                                ((ObjEntity) object).getName()) == null;
                    }

                    return false;
                }
            };
        }
    }

    public File getOutputDir() {
        String dir = ((GeneratorControllerPanel) getView()).getOutputFolder().getText();
        return dir != null ? new File(dir) : new File(System.getProperty("user.dir"));
    }

    public DataMapDefaults getPreferences() {
        return preferences;
    }

    /**
     * An action method that pops up a file chooser dialog to pick the generation
     * directory.
     */
    public void selectOutputFolderAction() {

        JTextField outputFolder = ((GeneratorControllerPanel) getView())
                .getOutputFolder();

        String currentDir = outputFolder.getText();

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);

        // guess start directory
        if (!Util.isEmptyString(currentDir)) {
            chooser.setCurrentDirectory(new File(currentDir));
        }
        else {
            FSPath lastDir = Application
                    .getInstance()
                    .getFrameController()
                    .getLastDirectory();
            lastDir.updateChooser(chooser);
        }

        int result = chooser.showOpenDialog(getView());
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();

            // update model
            String path = selected.getAbsolutePath();
            outputFolder.setText(path);
            preferences.setOutputPath(path);
        }
    }
}
