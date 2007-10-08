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
import java.util.HashMap;
import java.util.Map;

import org.objectstyle.cayenne.gen.DefaultClassGenerator;
import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.pref.PreferenceDetail;
import org.objectstyle.cayenne.swing.BindingBuilder;
import org.objectstyle.cayenne.util.Util;

/**
 * @author Andrus Adamchik
 */
public class GeneratorTabController extends CayenneController {

    private static final String STANDARD_OBJECTS_MODE = "Standard Persistent Objects";
    private static final String CLIENT_OBJECTS_MODE = "Client Persistent Objects";
    private static final String ADVANCED_MODE = "Advanced";

    public static final String GENERATOR_PROPERTY = "generator";

    private static final String[] GENERATION_MODES = new String[] {
            STANDARD_OBJECTS_MODE, CLIENT_OBJECTS_MODE, ADVANCED_MODE
    };

    protected GeneratorTabPanel view;
    protected Map controllers;
    protected PreferenceDetail preferences;

    public GeneratorTabController(CodeGeneratorControllerBase parent) {
        super(parent);

        this.controllers = new HashMap(5);
        controllers.put(STANDARD_OBJECTS_MODE, new StandardModeController(parent));
        controllers.put(CLIENT_OBJECTS_MODE, new ClientModeController(parent));
        controllers.put(ADVANCED_MODE, new CustomModeController(parent));

        Component[] modePanels = new Component[GENERATION_MODES.length];
        for (int i = 0; i < GENERATION_MODES.length; i++) {
            modePanels[i] = ((GeneratorController) controllers.get(GENERATION_MODES[i]))
                    .getView();
        }

        this.view = new GeneratorTabPanel(GENERATION_MODES, modePanels);
        initBindings();
    }

    public Component getView() {
        return view;
    }

    protected CodeGeneratorControllerBase getParentController() {
        return (CodeGeneratorControllerBase) getParent();
    }

    protected void initBindings() {

        // bind actions
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        builder.bindToAction(view.getGenerationMode(), "updateModeAction()");

        this.preferences = getViewDomain().getDetail("controller", true);

        if (Util.isEmptyString(preferences.getProperty("mode"))) {
            preferences.setProperty("mode", STANDARD_OBJECTS_MODE);
        }

        builder.bindToComboSelection(
                view.getGenerationMode(),
                "preferences.property['mode']").updateView();

        updateModeAction();
    }

    /**
     * Resets selection to default values for a given controller.
     */
    public void updateModeAction() {
        firePropertyChange(GENERATOR_PROPERTY, null, getGeneratorController());
    }

    public GeneratorController getGeneratorController() {
        Object name = view.getGenerationMode().getSelectedItem();
        return (GeneratorController) controllers.get(name);
    }

    public DefaultClassGenerator getGenerator() {
        GeneratorController modeController = getGeneratorController();
        return (modeController != null) ? modeController.createGenerator() : null;
    }

    public PreferenceDetail getPreferences() {
        return preferences;
    }
}
