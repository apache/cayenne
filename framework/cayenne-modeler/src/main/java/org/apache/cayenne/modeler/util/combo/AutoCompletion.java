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

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

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
        
        comboBox.setEditor(new CustomTypeComboBoxEditor(comboBox, allowsUserValues));
        
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
        handleKeyPressed(comboBox, e);
    }

    public void keyReleased(KeyEvent e) {}

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
    private void handleKeyPressed(JComboBox comboBox, KeyEvent e) {
        boolean suggest = suggestionList.isVisible();
        
        int sel, next, max;
        
        if (suggest) {
            sel = suggestionList.getSelectedIndex();
            max = suggestionList.getItemCount() - 1;
        }
        else {
            sel = comboBox.getSelectedIndex();
            max = comboBox.getItemCount() - 1;
        }
        
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
                if (suggest) {
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
                return;
                
            case KeyEvent.VK_ESCAPE:
                if (suggest) {
                    suggestionList.hide();
                }
                return;
                
            case KeyEvent.VK_CONTROL:
            case KeyEvent.VK_ALT:
            case KeyEvent.VK_SHIFT:
                return;
                
            default:
                //invoke in end of AWT thread so that information in textEditor would update
                SwingUtilities.invokeLater(this);
                return;
        }
        
        /**
         * Handle navigation keys
         */

        e.consume();
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
                }
                else {
                   comboBox.setPopupVisible(true);
                   comboBox.setSelectedIndex(next);
                }
            }
            
            textEditor.requestFocus();
        }
    }
}
