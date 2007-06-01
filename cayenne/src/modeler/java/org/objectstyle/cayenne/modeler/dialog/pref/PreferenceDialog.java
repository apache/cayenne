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
package org.objectstyle.cayenne.modeler.dialog.pref;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.objectstyle.cayenne.modeler.pref.ComponentGeometry;
import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.pref.Domain;
import org.objectstyle.cayenne.pref.PreferenceEditor;
import org.objectstyle.cayenne.pref.PreferenceException;

/**
 * A controller for editing Modeler preferences.
 * 
 * @author Andrei Adamchik
 */
public class PreferenceDialog extends CayenneController {

    public static final String GENERAL_KEY = "General";
    public static final String DATA_SOURCES_KEY = "Local DataSources";
    public static final String CLASS_PATH_KEY = "ClassPath";

    private static final String[] preferenceMenus = new String[] {
            GENERAL_KEY, DATA_SOURCES_KEY, CLASS_PATH_KEY
    };

    protected PreferenceDialogView view;
    protected Map detailControllers;
    protected PreferenceEditor editor;

    public PreferenceDialog(CayenneController parent) {
        super(parent);
        this.view = new PreferenceDialogView();
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
                    showDetailViewAction(selection.toString());
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
        // this will install needed controller
        showDetailViewAction(DATA_SOURCES_KEY);

        DataSourcePreferences controller = (DataSourcePreferences) detailControllers
                .get(DATA_SOURCES_KEY);
        controller.editDataSourceAction(dataSourceKey);
    }

    /**
     * Switches preference detail view to the editor identified by provided name.
     */
    public void showDetailViewAction(String name) {

        if (!detailControllers.containsKey(name)) {
            CayenneController c;

            if (GENERAL_KEY.equals(name)) {
                c = new GeneralPreferences(this);
            }
            else if (DATA_SOURCES_KEY.equals(name)) {
                c = new DataSourcePreferences(this);
            }
            else if (CLASS_PATH_KEY.equals(name)) {
                c = new ClasspathPreferences(this);
            }
            else {
                throw new PreferenceException("Unknown detail key: " + name);
            }

            detailControllers.put(name, c);
            view.getDetailPanel().add(c.getView(), name);

            // this is needed to display freshly added panel...
            view.getDetailPanel().getParent().validate();
        }

        view.getDetailLayout().show(view.getDetailPanel(), name);
    }

    public void startupAction() {

        // bind own view preferences
        Domain prefDomain = application.getPreferenceDomain().getSubdomain(
                view.getClass());
        ComponentGeometry geometry = ComponentGeometry.getPreference(prefDomain);
        geometry.bind(view, 650, 350);
        geometry.bindIntProperty(view.getSplit(), "dividerLocation", 220);

        // show
        centerView();
        makeCloseableOnEscape();

        view.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        view.setModal(true);
        view.show();

    }

    public Component getView() {
        return view;
    }

    public PreferenceEditor getEditor() {
        return editor;
    }
}