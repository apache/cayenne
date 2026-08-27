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

package org.apache.cayenne.modeler.toolkit.filechooser;

import org.apache.cayenne.modeler.pref.adapters.FileChooserPrefs;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Modal directory-picker dialog built around {@link JFileChooser} with custom Select / Cancel buttons and an extra
 * "New Folder" button (opening a directory may require creating a new one).
 */
public final class SwingDirChooserDialog extends JFileChooser {

    private static SwingDirChooserDialog instance;

    /**
     * Returns the lazily-initialized cached singleton. Constructing a {@link JFileChooser} is
     * expensive — on Windows it triggers a full L&amp;F UI delegate install and a Shell32 scan
     * that can take hundreds of milliseconds, making first-time dialog open feel sluggish.
     * Since Swing is single-threaded, one instance is safely reused across all callers; transient
     * state (listeners, current directory, selection) is reset on each {@link #open}.
     */
    public static SwingDirChooserDialog getInstance() {
        if (instance == null) {
            instance = new SwingDirChooserDialog();
        }
        return instance;
    }

    private final JButton selectButton = new JButton("Select");
    private final JButton cancelButton = new JButton("Cancel");
    private final JButton newFolderButton = new JButton("New Folder");

    private SwingDirChooserDialog() {
        setFileSelectionMode(DIRECTORIES_ONLY);

        // Using setControlButtonsAreShown(false) because on macOS Aqua L&F the built-in approve button is
        // continuously re-disabled when nothing is selected (e.g. after navigating into a directory).
        setControlButtonsAreShown(false);

        selectButton.addActionListener(e -> approveSelection());
        cancelButton.addActionListener(e -> cancelSelection());
        newFolderButton.addActionListener(e -> promptNewFolder());
    }

    /**
     * Shows the dialog modally and returns the selected directory, or {@code null} if the user cancelled.
     */
    public File open(Component parent, String title, File startDir, FileChooserPrefs prefs) {
        reset();
        if (startDir != null) {
            setCurrentDirectory(startDir);
        }
        setDialogTitle(title);
        if (prefs != null) {
            prefs.bind(this);
        }
        if (showOpenDialog(parent) != APPROVE_OPTION) {
            return null;
        }
        File selected = getSelectedFile();
        return selected != null ? selected : getCurrentDirectory();
    }

    @Override
    protected JDialog createDialog(Component parent) {
        JDialog dialog = super.createDialog(parent);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        right.add(cancelButton);
        right.add(selectButton);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEADING));
        left.add(newFolderButton);

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.add(left, BorderLayout.WEST);
        buttons.add(right, BorderLayout.EAST);

        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(selectButton);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        return dialog;
    }

    private void reset() {
        for (ActionListener l : getActionListeners()) {
            removeActionListener(l);
        }
        setSelectedFile(null);
    }

    private void promptNewFolder() {
        String name = JOptionPane.showInputDialog(this, "Folder name:", "New Folder", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) {
            return;
        }
        File newDir = new File(getCurrentDirectory(), name);
        if (newDir.mkdir()) {
            setCurrentDirectory(newDir);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Could not create folder \"" + name + "\".", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
