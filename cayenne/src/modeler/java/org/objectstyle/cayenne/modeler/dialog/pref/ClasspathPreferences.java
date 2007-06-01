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
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;

import org.objectstyle.cayenne.modeler.FileClassLoadingService;
import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.modeler.util.FileFilters;
import org.objectstyle.cayenne.pref.Domain;
import org.objectstyle.cayenne.pref.PreferenceDetail;
import org.objectstyle.cayenne.pref.PreferenceEditor;

/**
 * @author Andrei Adamchik
 */
public class ClasspathPreferences extends CayenneController {

    protected ClasspathPreferencesView view;
    protected PreferenceEditor editor;
    protected List classPathEntries;
    protected ClasspathTableModel tableModel;

    public ClasspathPreferences(PreferenceDialog parentController) {
        super(parentController);

        this.editor = parentController.getEditor();
        this.view = new ClasspathPreferencesView();
        this.classPathEntries = getClassLoaderDomain().getDetails();
        this.tableModel = new ClasspathTableModel();

        initBindings();
    }

    public Component getView() {
        return view;
    }

    protected Domain getClassLoaderDomain() {
        return editor
                .editableInstance(getApplication().getPreferenceDomain())
                .getSubdomain(FileClassLoadingService.class);
    }

    protected void initBindings() {
        view.getTable().setModel(tableModel);

        view.getAddDirButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                addClassDirectoryAction();
            }
        });

        view.getRemoveEntryButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removeEntryAction();
            }
        });

        view.getAddJarButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                addJarOrZipAction();
            }
        });
    }

    protected void addJarOrZipAction() {
        chooseClassEntry(
                FileFilters.getClassArchiveFilter(),
                "Select JAR or ZIP File.",
                JFileChooser.FILES_ONLY);
    }

    protected void addClassDirectoryAction() {
        chooseClassEntry(
                null,
                "Select Java Class Directory.",
                JFileChooser.DIRECTORIES_ONLY);
    }

    protected void removeEntryAction() {
        int selected = view.getTable().getSelectedRow();
        if (selected < 0) {
            return;
        }

        PreferenceDetail selection = (PreferenceDetail) classPathEntries.remove(selected);
        editor.deleteDetail(getClassLoaderDomain(), selection.getKey());
        tableModel.fireTableRowsDeleted(selected, selected);
    }

    protected void chooseClassEntry(FileFilter filter, String title, int selectionMode) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(selectionMode);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setAcceptAllFileFilterUsed(true);

        getLastDirectory().updateChooser(chooser);

        if (filter != null) {
            chooser.addChoosableFileFilter(filter);
        }

        chooser.setDialogTitle(title);

        File selected = null;
        int result = chooser.showOpenDialog(view);
        if (result == JFileChooser.APPROVE_OPTION) {
            selected = chooser.getSelectedFile();
        }

        if (selected != null) {
            // store last dir in preferences
            getLastDirectory().updateFromChooser(chooser);

            int len = classPathEntries.size();
            String key = selected.getAbsolutePath();
            classPathEntries.add(editor.createDetail(getClassLoaderDomain(), key));
            tableModel.fireTableRowsInserted(len, len);
        }
    }

    class ClasspathTableModel extends AbstractTableModel {

        public int getColumnCount() {
            return 1;
        }

        public int getRowCount() {
            return classPathEntries.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            PreferenceDetail preference = (PreferenceDetail) classPathEntries
                    .get(rowIndex);
            return preference.getKey();
        }

        public String getColumnName(int column) {
            return "Custom ClassPath";
        }
    }

}