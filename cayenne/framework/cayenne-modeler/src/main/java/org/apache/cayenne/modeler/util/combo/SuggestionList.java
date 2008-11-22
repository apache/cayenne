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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.basic.BasicComboPopup;

import org.apache.cayenne.modeler.util.CellRenderers;

/**
 * SuggestionList is a combo-popup displaying all items matching for
 * autocompletion.
 * 
 */
public class SuggestionList extends BasicComboPopup {
    /**
     * 'Strict' matching, i.e. whether 'startWith' or 'contains' function
     * should be used for checking match 
     */
    protected boolean strict;
    
    /**
     * Creates a strict suggestion-popup for a combobox
     */
    public SuggestionList(JComboBox cb) {
       this(cb, false); 
    }
    
    /**
     * Creates a suggestion-popup for a combobox
     */
    public SuggestionList(JComboBox cb, boolean strict) {
        super(cb);
        
        this.strict = strict;
        list.addMouseListener(new MouseHandler());
    }
    
    /**
     * 'Filters' the list, leaving only matching items
     * @param prefix user-typed string, used to filter
     */
    public void filter(String prefix) {
        ComboBoxModel model = comboBox.getModel();
        DefaultListModel lm = new DefaultListModel();
        
        for (int i = 0; i < model.getSize(); i++) {
            String item = CellRenderers.asString(model.getElementAt(i));
            
            if (matches(item, prefix)) {
                lm.addElement(model.getElementAt(i));
            }
        }
        
        list.setModel(lm);
    }
    
    /**
     * Checks if an item matches input pattern
     */
    protected boolean matches(String item, String pattern) {
        if (strict) {
            return item.toLowerCase().startsWith(pattern.toLowerCase());
        }
        else {
            return item.toLowerCase().contains(pattern.toLowerCase());
        }
    }
    
    /**
     * Retrieves the height of the popup based on the current
     * ListCellRenderer and the maximum row count.
     * 
     * Overrriden to count for local list size
     */
    @Override
    protected int getPopupHeightForRowCount(int maxRowCount) {
        int h = super.getPopupHeightForRowCount(Math.min(maxRowCount, list.getModel().getSize()));

        return h;
    }

    /**
     * @return selected index in popup
     */
    public int getSelectedIndex() {
        return list.getSelectedIndex();
    }
    
    /**
     * @return selected item in popup
     */
    public Object getSelectedValue() {
        return list.getSelectedValue();
    }
    
    /**
     * Selects an item in list
     */
    public void setSelectedIndex(int i) {
        list.setSelectedIndex(i);
        comboBox.setSelectedItem(list.getModel().getElementAt(i));
    }
    
    /**
     * @return current suggestions count
     */
    public int getItemCount() {
        return list.getModel().getSize();
    }
    
    /**
     * @return an item from the list
     */
    public Object getItemAt(int i) {
        return list.getModel().getElementAt(i);
    }
    
    @Override
    public MouseListener createListMouseListener() {
        return new MouseHandler();
    }
    
    /**
     * We don't want items in the list be automatically selected at all
     */
    @Override
    protected ItemListener createItemListener() {
        return 
          new ItemListener() {
            public void itemStateChanged(ItemEvent e) {}
          };
    }
    
    /**
     * @return Whether match-check is 'strict'
     */
    public boolean isStrict() {
        return strict;
    }
    
    protected class MouseHandler extends MouseInputAdapter {
        @Override
        public void mouseReleased(MouseEvent e) {
            comboBox.setSelectedItem(list.getSelectedValue());
            comboBox.setPopupVisible(false);

            // Workaround for cancelling an edited item (JVM bug 4530953).
            if (comboBox.isEditable() && comboBox.getEditor() != null) {
                comboBox.configureEditor(comboBox.getEditor(), comboBox.getSelectedItem());
            }
            
            hide();
        }
    }
}
