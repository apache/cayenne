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
import java.io.File;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.apache.commons.collections.Predicate;
import org.objectstyle.cayenne.gen.DefaultClassGenerator;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.dialog.pref.GeneralPreferences;
import org.objectstyle.cayenne.modeler.pref.DataMapDefaults;
import org.objectstyle.cayenne.modeler.pref.FSPath;
import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.modeler.util.CodeValidationUtil;
import org.objectstyle.cayenne.pref.Domain;
import org.objectstyle.cayenne.pref.PreferenceDetail;
import org.objectstyle.cayenne.swing.BindingBuilder;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.validation.BeanValidationFailure;
import org.objectstyle.cayenne.validation.SimpleValidationFailure;
import org.objectstyle.cayenne.validation.ValidationFailure;
import org.objectstyle.cayenne.validation.ValidationResult;

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
