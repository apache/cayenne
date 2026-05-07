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

package org.apache.cayenne.modeler.toolkit.combobox;

import org.apache.cayenne.map.MappingNamespace;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Supplier;

/**
 * AutoCompletion class handles user input and suggests matching variants (see CAY-911)
 *
 */
public class AutoCompletion implements FocusListener, KeyListener, Runnable {

    public static final String AUTOCOMPLETION_PROPERTY = "JComboBox.autoCompletion";

    private final SuggestionList suggestions;

    private final JComboBox comboBox;
    private final JTextComponent textEditor;
    private final boolean allowsUserValues;

    protected AutoCompletion(JComboBox comboBox, boolean strict, boolean allowsUserValues, Supplier<MappingNamespace> namespaceSupplier) {
        this.comboBox = comboBox;
        this.textEditor = ((JTextComponent) comboBox.getEditor().getEditorComponent());
        this.allowsUserValues = allowsUserValues;
        this.suggestions = new SuggestionList(comboBox, strict, namespaceSupplier);

        comboBox.putClientProperty(AUTOCOMPLETION_PROPERTY, Boolean.TRUE);
    }

    /**
     * Enables auto-completion for specified combobox
     */
    public static void enable(JComboBox comboBox, boolean strict, boolean allowsUserValues, Supplier<MappingNamespace> namespaceSupplier) {
        comboBox.setEditable(true);
        KeyListener[] listeners = comboBox.getEditor().getEditorComponent().getListeners(KeyListener.class);
        comboBox.setEditor(new CustomTypeComboBoxEditor(comboBox, allowsUserValues, namespaceSupplier));
        for (KeyListener listener : listeners) {
            comboBox.getEditor().getEditorComponent().addKeyListener(listener);
        }


        AutoCompletion ac = new AutoCompletion(comboBox, strict, allowsUserValues, namespaceSupplier);
        comboBox.addFocusListener(ac);
        ac.textEditor.addKeyListener(ac);

        //original keys would not work properly
        SwingUtilities.replaceUIActionMap(comboBox, null);
    }

    /**
     * Enables auto-completion for specified combobox
     */
    public static void enable(JComboBox comboBox, Supplier<MappingNamespace> namespaceSupplier) {
        enable(comboBox, true, false, namespaceSupplier);
    }

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
        suggestions.hide();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        handleKeyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
            String text = textEditor.getText();
            if (comboBox.isShowing()) {
                suggestions.hide();
                suggestions.filter(text);
                suggestions.show();
            }
        }
    }

    public void keyTyped(KeyEvent e) {
    }

    public void run() {
        String text = textEditor.getText();

        //need to hide first because Swing incorrectly updates popups (getSize() returns
        //dimension not the same as seen on the screen)
        suggestions.hide();

        if (comboBox.isShowing()) {
            suggestions.filter(text);

            if (suggestions.getItemCount() > 0) {
                suggestions.show();
            }
        }
    }

    /**
     * Calculates next selection row, according to a pressed key and selects it.
     * This might affect either suggestion list or original popup
     */
    private void handleKeyPressed(KeyEvent e) {
        boolean suggest = suggestions.isVisible();

        if (suggest) {
            processKeyPressedWhenSuggestionListIsVisible(e);
        } else {
            processKeyPressedWhenSuggestionListIsInvisible(e);
        }

        //scroll doesn't work in suggestionList..so we will scroll manually
        suggestionListScrolling();
    }

    private void processKeyPressedWhenSuggestionListIsInvisible(KeyEvent e) {
        int sel = comboBox.getSelectedIndex();
        int max = comboBox.getItemCount() - 1;

        int next;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_NUMPAD8:
                next = sel - 1;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_NUMPAD2:
                next = sel + 1;
                break;
            case KeyEvent.VK_PAGE_UP:
                next = sel - comboBox.getMaximumRowCount();
                break;
            case KeyEvent.VK_PAGE_DOWN:
                next = sel + comboBox.getMaximumRowCount();
                break;
            case KeyEvent.VK_HOME:
                next = 0;
                break;
            case KeyEvent.VK_END:
                next = max;
                break;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_ESCAPE:
                return;
            default:
                //invoke in end of AWT thread so that information in textEditor would update
                SwingUtilities.invokeLater(this);
                return;
        }
        e.consume();
        handleNavigationKeys(false, next, sel, max);
    }

    private void processKeyPressedWhenSuggestionListIsVisible(KeyEvent e) {
        int sel = suggestions.getSelectedIndex();
        int max = suggestions.getItemCount() - 1;
        int next;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_NUMPAD8:
                next = sel - 1;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_NUMPAD2:
                next = sel + 1;
                break;
            case KeyEvent.VK_PAGE_UP:
                next = sel - comboBox.getMaximumRowCount();
                break;
            case KeyEvent.VK_PAGE_DOWN:
                next = sel + comboBox.getMaximumRowCount();
                break;
            case KeyEvent.VK_HOME:
                next = 0;
                break;
            case KeyEvent.VK_END:
                next = max;
                break;
            case KeyEvent.VK_ENTER:
                processEnterPressed();
                return;
            case KeyEvent.VK_ESCAPE:
                suggestions.hide();
                return;
            default:
                //invoke in end of AWT thread so that information in textEditor would update
                SwingUtilities.invokeLater(this);
                return;
        }
        e.consume();
        handleNavigationKeys(true, next, sel, max);
    }

    private void processEnterPressed() {
        Object value = suggestions.getSelectedValue();
        if (!allowsUserValues && value == null && suggestions.getItemCount() > 0) {
            value = suggestions.getItemAt(0);
        }
        //reset the item (value == null) only if user values are not supported
        if (value != null || !allowsUserValues) {
            comboBox.setSelectedItem(value);
        }
        suggestions.hide();
    }

    private void handleNavigationKeys(boolean suggest, int next, int sel, int max) {
        if (!suggest && !comboBox.isPopupVisible()) {
            comboBox.setPopupVisible(true);
            return;
        }

        if (comboBox.getItemCount() > 0) {
            if (next < 0) {
                next = 0;
            }

            if (next > max) {
                next = max;
            }

            if (next != sel) {
                if (suggest) {
                    suggestions.setSelectedIndex(next);
                } else {
                    comboBox.setPopupVisible(true);
                    comboBox.setSelectedIndex(next);
                }
            }
            textEditor.requestFocus();
        }
    }

    private void suggestionListScrolling() {
        JList list = suggestions.getList();
        int selectedIndex = suggestions.getSelectedIndex();
        list.ensureIndexIsVisible(selectedIndex);
    }
}

