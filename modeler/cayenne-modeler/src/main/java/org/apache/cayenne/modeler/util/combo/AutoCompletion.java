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

package org.apache.cayenne.modeler.util.combo;

import javax.swing.JComboBox;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * AutoCompletion class handles user input and suggests matching variants (see CAY-911)
 *
 */
public class AutoCompletion implements FocusListener, KeyListener, Runnable {
    /**
     * Property to mark combobox as 'auto-completing'
     */
    public static final String AUTOCOMPLETION_PROPERTY = "JComboBox.autoCompletion";
    
    /**
     * A list with matching items
     */
    private final SuggestionList suggestionList; 

    /**
     * Combo with auto-completion
     */
    private final JComboBox comboBox;
    
    private final JTextComponent textEditor;
    
    private final boolean allowsUserValues;
    
    protected AutoCompletion(final JComboBox comboBox, boolean strict, boolean allowsUserValues) {
        this.comboBox = comboBox;
        textEditor = ((JTextComponent)comboBox.getEditor().getEditorComponent());

        this.allowsUserValues = allowsUserValues;
        
        suggestionList = new SuggestionList(comboBox, strict);
        
        /**
         * Marking combobox as auto-completing
         */
        comboBox.putClientProperty(AUTOCOMPLETION_PROPERTY, Boolean.TRUE);
    }

    /**
     * Enables auto-completion for specified combobox
     *
     * @param comboBox Combo to be featured
     * @param strict Whether strict matching (check 'startWith' or 'contains') should be used
     * @param allowsUserValues Whether non-present items are allowed 
     */
    public static void enable(JComboBox comboBox, boolean strict, boolean allowsUserValues) {
        comboBox.setEditable(true);
        KeyListener[] listeners = comboBox.getEditor().getEditorComponent().getListeners(KeyListener.class);
        comboBox.setEditor(new CustomTypeComboBoxEditor(comboBox, allowsUserValues));
        for (KeyListener listener : listeners) {
            comboBox.getEditor().getEditorComponent().addKeyListener(listener);
        }


        AutoCompletion ac = new AutoCompletion(comboBox, strict, allowsUserValues);
        comboBox.addFocusListener(ac);
        ac.textEditor.addKeyListener(ac);

        //original keys would not work properly
        SwingUtilities.replaceUIActionMap(comboBox, null);
    }
    
    /**
     * Enables auto-completion for specified combobox
     */
    public static void enable(JComboBox comboBox) {
        enable(comboBox, true, false);
    }

    public void focusGained(FocusEvent e) {}

    public void focusLost(FocusEvent e) {
        suggestionList.hide();
    }

    public void keyPressed(KeyEvent e) {
        handleKeyPressed(e);
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE ||
                e.getKeyCode() == KeyEvent.VK_ENTER) {

            String text = textEditor.getText();
            if (comboBox.isShowing()) {
                suggestionList.hide();
                suggestionList.filter(text);
                suggestionList.show();
            }
        }
    }

    public void keyTyped(KeyEvent e) {}

    public void run() {
        String text = textEditor.getText();

        //need to hide first because Swing incorrectly updates popups (getSize() returns
        //dimension not the same as seen on the screen)
        suggestionList.hide();

        if (comboBox.isShowing()) {
            suggestionList.filter(text);

            if (suggestionList.getItemCount() > 0) {
                suggestionList.show();
            }
        }
    }
    
    /**
     * Calculates next selection row, according to a pressed key and selects it.
     * This might affect either suggestion list or original popup
     */
    private void handleKeyPressed(KeyEvent e) {
        boolean suggest = suggestionList.isVisible();

        if (suggest) {
            processKeyPressedWhenSuggestionListIsVisible(e);
        }
        else {
            processKeyPressedWhenSuggestionListIsInvisible(e);
        }

        //scroll doesn't work in suggestionList..so we will scroll manually
        suggestionListScrolling();

        textEditor.requestFocus();
    }

    private void   processKeyPressedWhenSuggestionListIsInvisible(KeyEvent e){
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
                return;
            case KeyEvent.VK_ESCAPE:
                return;
            default:
                //invoke in end of AWT thread so that information in textEditor would update
                SwingUtilities.invokeLater(this);
                return;
        }
        e.consume();
        handleNavigationKeys(false,next,sel,max);
    }

    private void processKeyPressedWhenSuggestionListIsVisible(KeyEvent e){
        int sel = suggestionList.getSelectedIndex();
        int max = suggestionList.getItemCount() - 1;
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
                suggestionList.hide();
                return;
            default:
                //invoke in end of AWT thread so that information in textEditor would update
                SwingUtilities.invokeLater(this);
                return;
        }
        e.consume();
        handleNavigationKeys(true,next,sel,max);
    }

    private void processEnterPressed(){
        Object value = suggestionList.getSelectedValue();
        if (!allowsUserValues && value == null && suggestionList.getItemCount() > 0) {
            value = suggestionList.getItemAt(0);
        }
        //reset the item (value == null) only if user values are not supported
        if (value != null || !allowsUserValues) {
            comboBox.setSelectedItem(value);
        }
        suggestionList.hide();
    }

    private void handleNavigationKeys(boolean suggest, int next, int sel, int max){
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
                    suggestionList.setSelectedIndex(next);
                } else {
                    comboBox.setPopupVisible(true);
                    comboBox.setSelectedIndex(next);
                }
            }
        }
    }

    private void suggestionListScrolling(){
        Component c = suggestionList.getComponent(0);
        if (c instanceof JScrollPane) {
            double height = suggestionList.getPreferredSize().getHeight();
            int itemCount = suggestionList.getItemCount();
            int selectedIndex = suggestionList.getSelectedIndex();
            double scrollValue = Math.ceil(height*selectedIndex/itemCount);
            JScrollPane scrollPane = (JScrollPane) c;
            JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
            scrollBar.setValue((int) scrollValue);
        }
    }
}

