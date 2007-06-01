/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.modeler.dialog.classgen;

import java.awt.Component;
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.objectstyle.cayenne.gen.DefaultClassGenerator;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.pref.DataMapDefaults;
import org.objectstyle.cayenne.modeler.pref.FSPath;
import org.objectstyle.cayenne.modeler.util.FileFilters;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.project.validator.Validator;
import org.scopemvc.controller.basic.BasicController;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;
import org.scopemvc.view.swing.STable;

/**
 * @author Andrei Adamchik
 */
public class ClassGeneratorController extends BasicController {

    public static final String CANCEL_CONTROL = "cayenne.modeler.classgenerator.cancel.button";
    public static final String GENERATE_CLASSES_CONTROL = "cayenne.modeler.classgenerator.generate.button";
    public static final String SELECT_ALL_CONTROL = "cayenne.modeler.classgenerator.selectall.button";
    public static final String CHOOSE_LOCATION_CONTROL = "cayenne.modeler.classgenerator.choose.button";
    public static final String CHOOSE_TEMPLATE_CONTROL = "cayenne.modeler.classgenerator.choosetemplate.button";
    public static final String CHOOSE_SUPERTEMPLATE_CONTROL = "cayenne.modeler.classgenerator.choosesupertemplate.button";

    public ClassGeneratorController(ProjectController parent) {
        setModel(prepareModel(parent));
    }

    protected Object prepareModel(ProjectController parent) {
        Project project = parent.getProject();
        DataMap map = parent.getCurrentDataMap();
        DataMapDefaults preferences = parent.getDataMapPreferences();
        ObjEntity selectedEntity = parent.getCurrentObjEntity();

        // validate entities
        Validator validator = project.getValidator();
        validator.validate();

        ClassGeneratorModel model = new ClassGeneratorModel(
                map,
                preferences,
                selectedEntity,
                validator.validationResults());

        return model;
    }

    /**
     * Creates and runs the class generation dialog.
     */
    public void startup() {
        setView(new ClassGeneratorDialog());
        super.startup();
    }

    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(CANCEL_CONTROL)) {
            shutdown();
        }
        else if (control.matchesID(GENERATE_CLASSES_CONTROL)) {
            generateClasses();
        }
        else if (control.matchesID(CHOOSE_LOCATION_CONTROL)) {
            chooseLocation();
        }
        else if (control.matchesID(CHOOSE_TEMPLATE_CONTROL)) {
            chooseClassTemplate();
        }
        else if (control.matchesID(CHOOSE_SUPERTEMPLATE_CONTROL)) {
            chooseSuperclassTemplate();
        }
        else if (control.matchesID(SELECT_ALL_CONTROL)) {
            selectAllClasses();
        }
    }

    protected void selectAllClasses() {
        ClassGeneratorModel model = (ClassGeneratorModel) getModel();
        if (model.selectAllEnabled()) {
            // force model update
            STable table = ((ClassGeneratorDialog) getView()).getTable();
            table.refresh();
        }
    }

    protected void generateClasses() {
        ClassGeneratorModel model = (ClassGeneratorModel) getModel();
        File outputDir = model.getOutputDirectory();

        // no destination folder
        if (outputDir == null) {
            JOptionPane.showMessageDialog(
                    (Component) this.getView(),
                    "Select directory for source files.");
            return;
        }

        // no such folder
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            JOptionPane.showMessageDialog(
                    (Component) this.getView(),
                    "Can't create directory " + outputDir + ". Select a different one.");
            return;
        }

        // not a directory
        if (!outputDir.isDirectory()) {
            JOptionPane.showMessageDialog((Component) this.getView(), outputDir
                    + " is not a valid directory.");
            return;
        }

        File classTemplate = null;
        if (model.getCustomClassTemplate() != null) {
            classTemplate = new File(model.getCustomClassTemplate());

            if (!classTemplate.canRead()) {
                JOptionPane.showMessageDialog((Component) this.getView(), model
                        .getCustomClassTemplate()
                        + " is not a valid template file.");
                return;
            }
        }

        File superClassTemplate = null;
        if (model.getCustomSuperclassTemplate() != null) {
            superClassTemplate = new File(model.getCustomSuperclassTemplate());

            if (!superClassTemplate.canRead()) {
                JOptionPane.showMessageDialog((Component) this.getView(), model
                        .getCustomClassTemplate()
                        + " is not a valid template file.");
                return;
            }
        }

        List selected = model.getSelectedEntities();
        DefaultClassGenerator generator = new DefaultClassGenerator(selected);
        generator.setDestDir(outputDir);
        generator.setMakePairs(model.isPairs());
        generator.setSuperPkg(model.getSuperClassPackage());
        generator.setSuperTemplate(superClassTemplate);
        generator.setTemplate(classTemplate);

        try {
            generator.execute();
            JOptionPane.showMessageDialog(
                    (Component) this.getView(),
                    "Class generation finished");
            shutdown();
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    (Component) this.getView(),
                    "Error generating classes - " + e.getMessage());
        }
    }

    protected void chooseLocation() {
        ClassGeneratorModel model = (ClassGeneratorModel) getModel();
        File startDir = model.getOutputDirectory();

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);

        // guess start directory
        if (startDir != null) {
            chooser.setCurrentDirectory(startDir);
        }
        else {
            FSPath lastDir = Application
                    .getInstance()
                    .getFrameController()
                    .getLastDirectory();
            lastDir.updateChooser(chooser);
        }

        int result = chooser.showOpenDialog((Component) this.getView());
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();

            // update model
            model.setOutputDir(selected.getAbsolutePath());
        }
    }

    protected void chooseSuperclassTemplate() {
        ClassGeneratorModel model = (ClassGeneratorModel) getModel();
        String template = chooseTemplate(
                model.getCustomSuperclassTemplate(),
                "Select Custom Superclass Template");

        if (template != null) {
            model.setCustomSuperclassTemplate(template);
        }
    }

    protected void chooseClassTemplate() {
        ClassGeneratorModel model = (ClassGeneratorModel) getModel();
        String template = chooseTemplate(
                model.getCustomClassTemplate(),
                "Select Custom Class Template");

        if (template != null) {
            model.setCustomClassTemplate(template);
        }
    }

    /**
     * Picks and returns class generation velocity template.
     */
    private String chooseTemplate(String oldTemplate, String title) {

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.addChoosableFileFilter(FileFilters.getVelotemplateFilter());

        chooser.setDialogTitle(title);

        File startDir = (oldTemplate != null)
                ? new File(oldTemplate).getParentFile()
                : null;
        if (startDir != null) {
            chooser.setCurrentDirectory(startDir);
        }
        else {
            FSPath lastDir = Application
                    .getInstance()
                    .getFrameController()
                    .getLastDirectory();
            lastDir.updateChooser(chooser);
        }

        File selected = null;
        int result = chooser.showOpenDialog((Component) this.getView());
        if (result == JFileChooser.APPROVE_OPTION) {
            selected = chooser.getSelectedFile();

            FSPath lastDir = Application
                    .getInstance()
                    .getFrameController()
                    .getLastDirectory();
            lastDir.updateFromChooser(chooser);

        }

        return (selected != null) ? selected.getAbsolutePath() : null;
    }
}