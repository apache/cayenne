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
package org.objectstyle.cayenne.modeler.dialog.pref;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.pref.PreferenceEditor;

/**
 * A controller for editing Modeler preferences.
 * 
 * @author Andrus Adamchik
 */
public class PreferenceDialog extends CayenneController {

    public static final String GENERAL_KEY = "General";
    public static final String DATA_SOURCES_KEY = "Local DataSources";
    public static final String CLASS_PATH_KEY = "ClassPath";
    public static final String TEMPLATES_KEY = "Templates";

    private static final String[] preferenceMenus = new String[] {
            GENERAL_KEY, DATA_SOURCES_KEY, CLASS_PATH_KEY, TEMPLATES_KEY
    };

    protected PreferenceDialogView view;
    protected Map detailControllers;
    protected PreferenceEditor editor;

    public PreferenceDialog(CayenneController parent) {
        super(parent);

        Window parentView = (Window) SwingUtilities.getAncestorOfClass(
                Window.class,
                parent.getView());
        this.view = (parentView instanceof Dialog) ? new PreferenceDialogView(
                (Dialog) parentView) : new PreferenceDialogView((Frame) parentView);
        this.detailControllers = new HashMap();

        // editor must be configured before startup for "showDetailViewAction()" to work
        this.editor = new CayenneModelerPreferenceEditor(application);

        initBindings();
    }

    protected void initBindings() {
        final JList list = view.getList();
        list.setListData(preferenceMenus);
        list.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                Object selection = list.getSelectedValue();
                if (selection != null) {
                    view.getDetailLayout().show(
                            view.getDetailPanel(),
                            selection.toString());
                }
            }
        });

        view.getCancelButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cancelAction();
            }
        });

        view.getSaveButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                savePreferencesAction();
            }
        });
    }

    public void cancelAction() {
        editor.revert();
        view.dispose();
    }

    public void savePreferencesAction() {
        editor.save();

        // update

        view.dispose();
    }

    /**
     * Configures preferences dialog to display an editor for a local DataSource with
     * specified name.
     */
    public void showDataSourceEditorAction(Object dataSourceKey) {
        configure();

        // this will install needed controller
        view.getDetailLayout().show(view.getDetailPanel(), DATA_SOURCES_KEY);

        DataSourcePreferences controller = (DataSourcePreferences) detailControllers
                .get(DATA_SOURCES_KEY);
        controller.editDataSourceAction(dataSourceKey);
        view.setVisible(true);
    }

    public void startupAction(String key) {

        if (key == null) {
            key = GENERAL_KEY;
        }

        configure();
        view.getList().setSelectedValue(key, true);
        view.setVisible(true);
    }

    protected void configure() {
        // init known panels
        registerPanel(GENERAL_KEY, new GeneralPreferences(this));
        registerPanel(DATA_SOURCES_KEY, new DataSourcePreferences(this));
        registerPanel(CLASS_PATH_KEY, new ClasspathPreferences(this));
        registerPanel(TEMPLATES_KEY, new TemplatePreferences(this));
        view.getDetailLayout().show(view.getDetailPanel(), GENERAL_KEY);
        // view.getSplit().setDividerLocation(150);
        view.pack();

        // show
        centerView();
        makeCloseableOnEscape();

        view.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        view.setModal(true);
    }

    protected void registerPanel(String name, CayenneController panelController) {
        detailControllers.put(name, panelController);
        view.getDetailPanel().add(panelController.getView(), name);
    }

    public Component getView() {
        return view;
    }

    public PreferenceEditor getEditor() {
        return editor;
    }
}