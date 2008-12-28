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

import java.lang.reflect.Method;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.plaf.basic.BasicComboBoxEditor;

import org.apache.cayenne.modeler.util.CellRenderers;

/**
 * CustomTypeComboBoxEditor is used as an editor of a combobox, when
 * custom type (such as Entity) is to be used. BasicComboBoxEditor
 * cannot be used, because it converts String values to other types
 * incorrectly (in fact, only classes with valueOf(String) methods
 * are supported).
 * 
 */
public class CustomTypeComboBoxEditor extends BasicComboBoxEditor {
    /**
     * 'oldValue' property is private somewhy, so we make our local
     * copy
     */
    protected Object localOldValue;
    
    /**
     * The combobox being edited
     */
    protected final JComboBox combo;
    
    /**
     * Whether non-present items are allowed
     */
    protected final boolean allowsUserValues;
    
    /**
     * Creates new editor
     * @param combo ComboBox being edited
     */
    public CustomTypeComboBoxEditor(JComboBox combo, boolean allowsUserValues) {
        editor = new EditorTextField(combo);
        this.combo = combo;
        this.allowsUserValues = allowsUserValues;
    }
    
    /** 
     * Sets the item that should be edited. 
     *
     * @param anObject the displayed value of the editor
     */
    @Override
    public void setItem(Object anObject) {
        localOldValue = anObject;
        super.setItem(anObject == null ? null : CellRenderers.asString(anObject));
    }
    
    /**
     * @return edited item
     */
    @Override
    public Object getItem() {
        Object newValue = editor.getText();
        
        if (localOldValue != null && !(localOldValue instanceof String))  {
            // The original value is not a string. Should return the value in it's
            // original type.
            if (newValue.equals(localOldValue.toString())) {
                return localOldValue;
            }
            else {
                // Must take the value from the editor and get the value and cast it to the new type.
                Class cls = localOldValue.getClass();
                try {
                    newValue = convert((String)newValue, cls);
                }
                catch (Exception ignored) {}
            }
        }
        
        if (!allowsUserValues && newValue != null) {
            boolean contains = false;
            
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (newValue.equals(combo.getItemAt(i))) {
                    contains = true;
                    break;
                }
            }
                   
            if (!contains) {
                return null;
            }
        }
        
        return newValue;
    }
    
    /**
     * Converts String value to specified type
     *
     * @param value String value of textfield
     * @param classTo type of result item
     * 
     * @return value of classTo type, or null if conversion is impossible
     */
    protected Object convert(String value, Class classTo) {
        if (classTo == String.class) {
            return value;
        }
        
        /**
         * We still try to it in BasicComboBox's way, so that primary object
         * types (such as numbers) would still be supported 
         */
        try {
            Method method = classTo.getMethod("valueOf", new Class[]{String.class});
            return method.invoke(null, value);
        }
        catch (Exception ignored) {}
        
        /**
         * We could manually convert strings to dbentities, attrs and other, but
         * in this implementation we use reverse operation instead, and convert 
         * combobox model's items to String.
         * All string values are assumed unique is one model.
         */
        ComboBoxModel model = combo.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            if (value.equals(CellRenderers.asString(model.getElementAt(i)))) {
                return model.getElementAt(i);
            }
        }
        
        //we return null, since String will not be appreciated
        return null;
    }
}
