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
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.File;
import java.util.function.Consumer;

/**
 * {@link CMFileChooser} implementation backed by Swing's {@link JFileChooser}.
 */
public class SwingFileChooser implements CMFileChooser {

    private final Component parent;
    private final String title;

    public SwingFileChooser(Component parent, String title) {
        this.parent = parent;
        this.title = title;
    }

    @Override
    public File openFile(File initialDir, FileFilter filter) {
        return showOpen(
                JFileChooser.FILES_ONLY,
                c -> { if (initialDir != null) c.setCurrentDirectory(initialDir); },
                filter);
    }

    @Override
    public File openFile(FileChooserPrefs prefs, FileFilter filter) {
        return showOpen(JFileChooser.FILES_ONLY, prefs::bind, filter);
    }

    @Override
    public File openDir(File initialDir) {
        return showOpenDir(parent, title, c -> { if (initialDir != null) c.setCurrentDirectory(initialDir); });
    }

    @Override
    public File openDir(FileChooserPrefs prefs) {
        return showOpenDir(parent, title, prefs::bind);
    }

    @Override
    public File saveFile(FileChooserPrefs prefs, String defaultName) {
        return showSave(prefs::bind, defaultName);
    }

    @Override
    public File saveDir(File initialDir) {
        return showOpenDir(parent, title, c -> { if (initialDir != null) c.setCurrentDirectory(initialDir); });
    }

    // setControlButtonsAreShown(false) + custom buttons: on macOS Aqua L&F the built-in
    // approve button is continuously re-disabled when nothing is selected (e.g. after
    // navigating into a directory). The custom dialog owns its own always-enabled "Select"
    // button and a "New Folder" button, and is used on all platforms for consistency.
    public static File showOpenDir(Component parent, String title, Consumer<JFileChooser> init) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setControlButtonsAreShown(false);
        init.accept(chooser);

        boolean[] approved = {false};
        JButton selectBtn = new JButton("Select");
        JButton cancelBtn = new JButton("Cancel");
        JButton newFolderBtn = new JButton("New Folder");

        selectBtn.addActionListener(e -> {
            approved[0] = true;
            SwingUtilities.getWindowAncestor(chooser).dispose();
        });
        cancelBtn.addActionListener(e -> SwingUtilities.getWindowAncestor(chooser).dispose());
        newFolderBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(SwingUtilities.getWindowAncestor(chooser),
                    "Folder name:", "New Folder", JOptionPane.PLAIN_MESSAGE);
            if (name != null && !name.isBlank()) {
                File newDir = new File(chooser.getCurrentDirectory(), name);
                if (newDir.mkdir()) {
                    chooser.setCurrentDirectory(newDir);
                } else {
                    JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(chooser),
                            "Could not create folder \"" + name + "\".", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        chooser.addActionListener(e -> {
            if (JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand())) {
                approved[0] = true;
            }
            SwingUtilities.getWindowAncestor(chooser).dispose();
        });

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEADING));
        left.add(newFolderBtn);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        right.add(cancelBtn);
        right.add(selectBtn);
        JPanel buttons = new JPanel(new BorderLayout());
        buttons.add(left, BorderLayout.WEST);
        buttons.add(right, BorderLayout.EAST);

        Window owner = (parent instanceof Window w) ? w : (parent != null ? SwingUtilities.getWindowAncestor(parent) : null);
        JDialog dialog = new JDialog(owner, title != null ? title : "", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.add(chooser, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(selectBtn);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        if (!approved[0]) {
            return null;
        }
        File selected = chooser.getSelectedFile();
        return selected != null ? selected : chooser.getCurrentDirectory();
    }

    private File showOpen(int mode, Consumer<JFileChooser> init, FileFilter filter) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(mode);
        init.accept(chooser);
        if (title != null) {
            chooser.setDialogTitle(title);
        }
        if (filter != null) {
            chooser.addChoosableFileFilter(filter);
            chooser.setFileFilter(filter);
        }
        return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile()
                : null;
    }

    private File showSave(Consumer<JFileChooser> init, String defaultName) {
        JFileChooser chooser = new JFileChooser();
        init.accept(chooser);
        if (title != null) {
            chooser.setDialogTitle(title);
        }
        if (defaultName != null) {
            chooser.setSelectedFile(new File(defaultName));
        }
        return chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile()
                : null;
    }
}
