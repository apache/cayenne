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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ArtifactsGenerationMode;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.EmbeddedAttribute;
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
import org.apache.commons.collections.Predicate;

/**
 * A mode-specific part of the code generation dialog.
 * 
 */
public abstract class GeneratorController extends CayenneController {

    protected DataMapDefaults preferences;
    protected String mode = ArtifactsGenerationMode.ENTITY.getLabel();

    public GeneratorController(CodeGeneratorControllerBase parent) {
        super(parent);

        createDefaults();
        createView();
        initBindings(new BindingBuilder(getApplication().getBindingFactory(), this));
    }

    public String getOutputPath() {
        return preferences.getOutputPath();
    }

    public void setOutputPath(String path) {
        preferences.setOutputPath(path);
    }

    protected void initBindings(BindingBuilder bindingBuilder) {

        initOutputFolder();

        JTextField outputFolder = ((GeneratorControllerPanel) getView())
                .getOutputFolder();
        JButton outputSelect = ((GeneratorControllerPanel) getView())
                .getSelectOutputFolder();

        outputFolder.setText(getOutputPath());
        bindingBuilder.bindToAction(outputSelect, "selectOutputFolderAction()");
        bindingBuilder.bindToTextField(outputFolder, "outputPath");
    }

    protected CodeGeneratorControllerBase getParentController() {
        return (CodeGeneratorControllerBase) getParent();
    }

    protected abstract GeneratorControllerPanel createView();

    protected abstract DataMapDefaults createDefaults();

    /**
     * Creates an appropriate subclass of {@link ClassGenerationAction}, returning it in
     * an unconfigured state. Configuration is performed by {@link #createGenerator()}
     * method.
     */
    protected abstract ClassGenerationAction newGenerator();

    /**
     * Creates a class generator for provided selections.
     */
    public ClassGenerationAction createGenerator() {

        File outputDir = getOutputDir();

        // no destination folder
        if (outputDir == null) {
            JOptionPane.showMessageDialog(
                    this.getView(),
                    "Select directory for source files.");
            return null;
        }

        // no such folder
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            JOptionPane.showMessageDialog(this.getView(), "Can't create directory "
                    + outputDir
                    + ". Select a different one.");
            return null;
        }

        // not a directory
        if (!outputDir.isDirectory()) {
            JOptionPane.showMessageDialog(this.getView(), outputDir
                    + " is not a valid directory.");
            return null;
        }

        // remove generic entities...
        Collection<ObjEntity> entities = new ArrayList<ObjEntity>(getParentController()
                .getSelectedEntities());
        Iterator<ObjEntity> it = entities.iterator();
        while (it.hasNext()) {
            if (it.next().isGeneric()) {
                it.remove();
            }
        }

        ClassGenerationAction generator = newGenerator();
        generator.setArtifactsGenerationMode(mode);
        generator.setDataMap(getParentController().getDataMap());
        generator.addEntities(entities);
        generator.addEmbeddables(getParentController().getSelectedEmbeddables());
        generator.addQueries(getParentController().getDataMap().getQueries());

        // configure encoding from preferences
        Domain generatorPrefs = Application
                .getInstance()
                .getPreferenceDomain()
                .getSubdomain(ClassGenerationAction.class);

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

    public void validateEmbeddable(
            ValidationResult validationBuffer,
            Embeddable embeddable) {
        ValidationFailure embeddableFailure = validateEmbeddable(embeddable);
        if (embeddableFailure != null) {
            validationBuffer.addFailure(embeddableFailure);
            return;
        }

        for (EmbeddableAttribute attribute : embeddable.getAttributes()) {
            ValidationFailure failure = validateEmbeddableAttribute(attribute);
            if (failure != null) {
                validationBuffer.addFailure(failure);
                return;
            }
        }
    }

    private ValidationFailure validateEmbeddableAttribute(EmbeddableAttribute attribute) {
        String name = attribute.getEmbeddable().getClassName();

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

    protected ValidationFailure validateEmbeddable(Embeddable embeddable) {

        String name = embeddable.getClassName();

        ValidationFailure emptyClass = BeanValidationFailure.validateNotEmpty(
                name,
                "className",
                embeddable.getClassName());
        if (emptyClass != null) {
            return emptyClass;
        }

        ValidationFailure badClass = BeanValidationFailure.validateJavaClassName(
                name,
                "className",
                embeddable.getClassName());
        if (badClass != null) {
            return badClass;
        }

        return null;
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

        for (Attribute attribute : entity.getAttributes()) {
            if (attribute instanceof EmbeddedAttribute) {
                EmbeddedAttribute embeddedAttribute = (EmbeddedAttribute) attribute;
                for (ObjAttribute subAttribute : embeddedAttribute.getAttributes()) {
                    ValidationFailure failure = validateEmbeddedAttribute(subAttribute);
                    if (failure != null) {
                        validationBuffer.addFailure(failure);
                        return;
                    }
                }
            }
            else {

                ValidationFailure failure = validateAttribute((ObjAttribute) attribute);
                if (failure != null) {
                    validationBuffer.addFailure(failure);
                    return;
                }
            }
        }

        for (ObjRelationship rel : entity.getRelationships()) {
            ValidationFailure failure = validateRelationship(rel, clientValidation);
            if (failure != null) {
                validationBuffer.addFailure(failure);
                return;
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

    protected ValidationFailure validateEmbeddedAttribute(ObjAttribute attribute) {

        String name = attribute.getEntity().getName();

        // validate embeddedAttribute and attribute names
        // embeddedAttribute returned attibute as
        // [name_embeddedAttribute].[name_attribute]
        String[] attributes = attribute.getName().split("\\.");
        String nameEmbeddedAttribute = attributes[0];
        int beginIndex = attributes[0].length();
        String attr = attribute.getName().substring(beginIndex + 1);

        ValidationFailure emptyEmbeddedName = BeanValidationFailure.validateNotEmpty(
                name,
                "attribute.name",
                nameEmbeddedAttribute);
        if (emptyEmbeddedName != null) {
            return emptyEmbeddedName;
        }

        ValidationFailure badEmbeddedName = CodeValidationUtil.validateJavaIdentifier(
                name,
                "attribute.name",
                nameEmbeddedAttribute);
        if (badEmbeddedName != null) {
            return badEmbeddedName;
        }

        ValidationFailure emptyName = BeanValidationFailure.validateNotEmpty(
                name,
                "attribute.name",
                attr);
        if (emptyName != null) {
            return emptyName;
        }

        ValidationFailure badName = CodeValidationUtil.validateJavaIdentifier(
                name,
                "attribute.name",
                attr);
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
    public Predicate getDefaultClassFilter() {
        final ObjEntity selectedEntity = Application
                .getInstance()
                .getFrameController()
                .getProjectController()
                .getCurrentObjEntity();

        final Embeddable selectedEmbeddable = Application
                .getInstance()
                .getFrameController()
                .getProjectController()
                .getCurrentEmbeddable();

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
        // select a single embeddable
        else if (selectedEmbeddable != null) {
            final boolean hasProblem = getParentController().getProblem(
                    selectedEmbeddable.getClassName()) != null;

            return new Predicate() {

                public boolean evaluate(Object object) {
                    return !hasProblem && object == selectedEmbeddable;
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
                    
                    if (object instanceof Embeddable) {
                        return getParentController().getProblem(
                                ((Embeddable) object).getClassName()) == null;
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
            setOutputPath(path);
        }
    }

    private void initOutputFolder() {

        String path = null;
        if (preferences.getOutputPath() == null) {
            if (System.getProperty("cayenne.cgen.destdir") != null) {
                setOutputPath(System.getProperty("cayenne.cgen.destdir"));
            }
            else {
                // init default directory..
                FSPath lastPath = Application
                        .getInstance()
                        .getFrameController()
                        .getLastDirectory();

                path = checkDefaultMavenResourceDir(lastPath, "test");

                if (path != null
                        || (path = checkDefaultMavenResourceDir(lastPath, "main")) != null) {
                    setOutputPath(path);
                }
                else {
                    File lastDir = (lastPath != null) ? lastPath
                            .getExistingDirectory(false) : null;
                    setOutputPath(lastDir != null ? lastDir.getAbsolutePath() : null);
                }
            }
        }
    }

    private String checkDefaultMavenResourceDir(FSPath lastPath, String dirType) {
        String path = lastPath.getPath();
        String resourcePath = buildFilePath("src", dirType, "resources");
        int idx = path.indexOf(resourcePath);
        if (idx < 0) {
            return null;
        }
        return path.substring(0, idx)
                + buildFilePath("src", dirType, "java")
                + path.substring(idx + resourcePath.length());
    }

    private static final String buildFilePath(String... pathElements) {
        if (pathElements.length == 0) {
            return "";
        }
        StringBuilder path = new StringBuilder(pathElements[0]);
        for (int i = 1; i < pathElements.length; i++) {
            path.append(File.separator).append(pathElements[i]);
        }
        return path.toString();
    }
}
