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

package org.objectstyle.cayenne.dataview.dvmodeler;

import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 *
 * @author Nataliya Kholodna
 * @version 1.0
 */

class FieldEditorHelper {
  private JTextField fieldNameField;
  private JComboBox dataTypeCombo;
  private JComboBox calcTypeCombo;
  private JComboBox objAttributeCombo;
  private JTextField defaultValueField;
  private JTextField captionField;
  private JCheckBox editableCheckBox;
  private JCheckBox visibleCheckBox;
  private JTextField displayClassField;
  private JTextField displayPatternField;
  private JTextField editClassField;
  private JTextField editPatternField;
  private JSpinner preferredIndexField;
  private JComboBox  lookupViewCombo;
  private JComboBox  lookupFieldCombo;
  private JComboBox objRelationshipCombo;

  private DataMapTreeModel dataMapTreeModel;
  private DataViewTreeModel dataViewTreeModel;
  private FieldsTableModel fieldsTableModel;

  private FieldEditor fieldEditor;
  private ObjEntityViewField objEntityViewField;

  FieldEditorHelper(FieldEditor fieldEditor){
    this.fieldEditor = fieldEditor;

    fieldNameField = fieldEditor.getFieldNameField();
    fieldNameField.getDocument().addDocumentListener(
      new FieldNameChangeListener());

    dataTypeCombo = fieldEditor.getDataTypeCombo();
    dataTypeCombo.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        dataTypeComboAction(e);
      }
    });
    calcTypeCombo = fieldEditor.getCalcTypeCombo();
    calcTypeCombo.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        calcTypeComboAction(e);
      }
    });
    lookupViewCombo = fieldEditor.getLookupViewCombo();
    lookupViewCombo.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        lookupViewComboAction(e);
      }
    });
    lookupFieldCombo = fieldEditor.getLookupFieldCombo();
    lookupFieldCombo.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        lookupFieldComboAction(e);
      }
    });
    objRelationshipCombo = fieldEditor.getObjRelationshipCombo();
    objRelationshipCombo.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        objRelationshipComboAction(e);
      }
    });

    objAttributeCombo = fieldEditor.getObjAttributeCombo();
    objAttributeCombo.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        objAttributeComboAction(e);
      }
    });
    defaultValueField = fieldEditor.getDefaultValueField();
    defaultValueField.getDocument().addDocumentListener(
      new DefaultValueChangeListener());

    captionField = fieldEditor.getCaptionField();
    captionField.getDocument().addDocumentListener(
      new CaptionChangeListener());

    editableCheckBox = fieldEditor.getEditableCheckBox();
    editableCheckBox.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        editableCheckBoxAction(e);
      }
    });
    visibleCheckBox = fieldEditor.getVisibleCheckBox();
    visibleCheckBox.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        visibleCheckBoxAction(e);
      }
    });
    displayClassField = fieldEditor.getDisplayClassField();
    displayClassField.getDocument().addDocumentListener(
      new DisplayClassChangeListener());

    displayPatternField = fieldEditor.getDisplayPatternField();
    displayPatternField.getDocument().addDocumentListener(
      new DisplayPatternChangeListener());

    editClassField = fieldEditor.getEditClassField();
    editClassField.getDocument().addDocumentListener(
      new EditClassChangeListener());

    editPatternField = fieldEditor.getEditPatternField();
    editPatternField.getDocument().addDocumentListener(
      new EditPatternChangeListener());


    preferredIndexField = fieldEditor.getPreferredIndexField();
    preferredIndexField.addChangeListener(new ChangeListener(){
      public void stateChanged(ChangeEvent e){
        preferredIndexFieldChanged(e);
      }
    });
  }

  public void setObjEntityViewField(ObjEntityViewField field){
    objEntityViewField = field;
  }

  private class FieldNameChangeListener extends BasicDocumentListener{
    public void documentUpdated(String text) {
      fieldNameChangeAction(text);
    }
  }
  private void fieldNameChangeAction(String fieldName){
    if (!objEntityViewField.getName().equals(fieldName.trim())){
      objEntityViewField.setName(fieldName.trim());
    }
  }

  private void dataTypeComboAction(ActionEvent e){
    String comboDataType = (String)dataTypeCombo.getSelectedItem();
    String dataType = objEntityViewField.getDataType();
    if ( dataType != comboDataType){
      objEntityViewField.setDataType(comboDataType);
    }
  }

  private void calcTypeComboAction(ActionEvent e){
    String comboCalcType = (String)calcTypeCombo.getSelectedItem();
    ObjEntity objEntity = objEntityViewField.getObjEntityView().getObjEntity();

    if (comboCalcType.equals("No Calculation")){
      objEntityViewField.setCalcType("nocalc");

      objRelationshipCombo.setEnabled(false);
      lookupViewCombo.setEnabled(false);
      lookupFieldCombo.setEnabled(false);


      ObjAttribute nullAttribute = null;
      java.util.List attributes = new ArrayList();
      attributes.add(nullAttribute);
      if (objEntity != null){
        attributes.addAll(objEntity.getObjAttributes());
      }

      DefaultComboBoxModel attributesDefaultModel = new DefaultComboBoxModel(attributes.toArray());
      objAttributeCombo.setModel(attributesDefaultModel);
      objAttributeCombo.setEnabled(true);
      if (objEntity != null){
        if (objEntityViewField.getObjAttribute() != null){
          objAttributeCombo.setSelectedItem(objEntityViewField.getObjAttribute());
        }else {
          objAttributeCombo.setSelectedIndex(0);
        }
      } else {
        objAttributeCombo.setSelectedIndex(0);
      }

    }
    if (comboCalcType.equals("Lookup")){
      objEntityViewField.setCalcType("lookup");

      objAttributeCombo.setEnabled(false);
      objRelationshipCombo.setEnabled(true);
      lookupViewCombo.setEnabled(true);
      lookupFieldCombo.setEnabled(true);

      ObjRelationship nullRelationship = null;
      java.util.List relationships = new ArrayList();
      relationships.add(nullRelationship);
      if (objEntity != null){
        relationships.addAll(objEntity.getDataMap().getObjRelationshipsBySourceToOne(objEntity));
      }

      DefaultComboBoxModel relationshipsDefaultModel = new DefaultComboBoxModel(relationships.toArray());
      objRelationshipCombo.setModel(relationshipsDefaultModel);

      ObjRelationship fieldRelationship = objEntityViewField.getObjRelationship();
      if(fieldRelationship != null){
        boolean flagSetSelectedItem = false;
        for (Iterator itr = relationships.iterator();itr.hasNext();){
          ObjRelationship relationship = (ObjRelationship)itr.next();
          if ((relationship != null) && (fieldRelationship.getName().equals(relationship.getName()))){
            objRelationshipCombo.setSelectedItem(fieldRelationship);
            flagSetSelectedItem = true;
            break;
          }
        }
        if (!flagSetSelectedItem){
          objRelationshipCombo.setSelectedIndex(0);
        }
      }else{
        objRelationshipCombo.setSelectedIndex(0);
      }
    }
  }

  private void lookupViewComboAction(ActionEvent e){
    ObjEntityView fieldLookupView = objEntityViewField.getLookup().getLookupObjEntityView();
    ObjEntityView selectedLookupView = (ObjEntityView)lookupViewCombo.getSelectedItem();

    if (selectedLookupView != fieldLookupView){
      objEntityViewField.getLookup().setLookupObjEntityView(selectedLookupView);

      dataViewTreeModel.fieldChanged(objEntityViewField);
      dataMapTreeModel.fieldChanged(objEntityViewField);
      fieldsTableModel.fireTableCellUpdated(objEntityViewField.
         getObjEntityView().getIndexOfObjEntityViewField(objEntityViewField), 4);
    }

    if (selectedLookupView != null){
      ObjEntityViewField nullField = null;
      java.util.List lookupFields = new ArrayList();
      lookupFields.add(nullField);
      lookupFields.addAll(selectedLookupView.getObjEntityViewFields());

      DefaultComboBoxModel lookupFieldsDefaultModel = new DefaultComboBoxModel(lookupFields.toArray());
      lookupFieldCombo.setModel(lookupFieldsDefaultModel);
      ObjEntityViewField fieldLookupField = objEntityViewField.getLookup().getLookupField();
      if(fieldLookupField != null){
        boolean flagSetSelectedItem = false;
        for (Iterator itr = lookupFields.iterator();itr.hasNext();){
          ObjEntityViewField field = (ObjEntityViewField)itr.next();
          if ((field != null) && (fieldLookupField.getName().equals(field.getName()))){
            lookupFieldCombo.setSelectedItem(fieldLookupField);
            flagSetSelectedItem = true;
            break;
          }
        }
        if (!flagSetSelectedItem){
          lookupFieldCombo.setSelectedIndex(0);
        }
      }else{
        lookupFieldCombo.setSelectedIndex(0);
      }
    }else{
      ObjEntityViewField nullField = null;
      ObjEntityViewField[] fields = new ObjEntityViewField[]{nullField};

      DefaultComboBoxModel fieldsModel = new DefaultComboBoxModel(fields);
      lookupFieldCombo.setModel(fieldsModel);
      lookupFieldCombo.setSelectedIndex(0);

    }
  }

  private void lookupFieldComboAction(ActionEvent e){
    ObjEntityViewField fieldLookupField = objEntityViewField.getLookup().getLookupField();
    ObjEntityViewField selectedLookupField = (ObjEntityViewField)lookupFieldCombo.getSelectedItem();

    if (selectedLookupField != fieldLookupField){
      objEntityViewField.getLookup().setLookupField(selectedLookupField);

      dataViewTreeModel.fieldChanged(objEntityViewField);
      dataMapTreeModel.fieldChanged(objEntityViewField);
      fieldsTableModel.fireTableCellUpdated(objEntityViewField.getObjEntityView().
        getIndexOfObjEntityViewField(objEntityViewField), 4);
    }
  }

  private void objRelationshipComboAction(ActionEvent e){
    ObjRelationship selectedObjRelationship = (ObjRelationship)objRelationshipCombo.getSelectedItem();
    ObjRelationship fieldObjRelationship = objEntityViewField.getObjRelationship();

    if (selectedObjRelationship != fieldObjRelationship){
      objEntityViewField.setObjRelationship(selectedObjRelationship);
    }

    if (selectedObjRelationship != null){
      ObjEntity targetObjEntity = selectedObjRelationship.getTargetObjEntity();

      ObjEntityView nullView = null;
      java.util.List lookupViews = new ArrayList();
      lookupViews.add(nullView);
      lookupViews.addAll(targetObjEntity.getObjEntityViews());

      DefaultComboBoxModel lookupViewModel = new DefaultComboBoxModel(lookupViews.toArray());
      lookupViewCombo.setModel(lookupViewModel);
      ObjEntityView fieldLookupView = objEntityViewField.getLookup().getLookupObjEntityView();
      if(fieldLookupView != null){
        boolean flagSetSelectedItem = false;
        for (Iterator itr = lookupViews.iterator();itr.hasNext();){
          ObjEntityView view = (ObjEntityView)itr.next();
          if (fieldLookupView == view){
            lookupViewCombo.setSelectedItem(fieldLookupView);
            flagSetSelectedItem = true;
            break;
          }
        }
        if (!flagSetSelectedItem){
          lookupViewCombo.setSelectedIndex(0);
        }
      }else{
        lookupViewCombo.setSelectedIndex(0);
      }
    }else{
      ObjEntityView nullView = null;
      ObjEntityView[] views = new ObjEntityView[]{nullView};
      DefaultComboBoxModel viewsModel = new DefaultComboBoxModel(views);
      lookupViewCombo.setModel(viewsModel);
      lookupViewCombo.setSelectedIndex(0);
    }
  }


  private void objAttributeComboAction(ActionEvent e){

    ObjAttribute selectedObjAttribute = (ObjAttribute)objAttributeCombo.getSelectedItem();
    ObjAttribute fieldObjAttribute = objEntityViewField.getObjAttribute();
    if (selectedObjAttribute != fieldObjAttribute){
      objEntityViewField.setObjAttribute(selectedObjAttribute);
    }
  }

  private class DefaultValueChangeListener extends BasicDocumentListener{
    public void documentUpdated(String text) {
      defaultValueChangeAction(text);
    }
  }

  private void defaultValueChangeAction(String defaultValue){
    String fieldDefaultValue = objEntityViewField.getDefaultValue();
    if ( !defaultValue.trim().equals(fieldDefaultValue)){
      objEntityViewField.setDefaultValue(defaultValue.trim());
    }
  }

  private class CaptionChangeListener extends BasicDocumentListener{
    public void documentUpdated(String text) {
      captionChangeAction(text);
    }
  }

  private void captionChangeAction(String caption){
    String fieldCaption = objEntityViewField.getCaption();
    if ( !caption.trim().equals(fieldCaption)){
      objEntityViewField.setCaption(caption.trim());
    }
  }

  private void editableCheckBoxAction(ActionEvent e){
    boolean editable = editableCheckBox.isSelected();
    if ( editable != objEntityViewField.getEditable()){
      objEntityViewField.setEditable(editable);
    }
  }

  private void visibleCheckBoxAction(ActionEvent e){
    boolean visible = visibleCheckBox.isSelected();
    if ( visible != objEntityViewField.getVisible()){
      objEntityViewField.setVisible(visible);
    }
  }

  private class DisplayClassChangeListener extends BasicDocumentListener{
    public void documentUpdated(String text) {
      displayClassChangeAction(text);
    }
  }

  private void displayClassChangeAction(String displayClass){
    String fieldDisplayClass = objEntityViewField.getDisplayFormat().getClassName();
    if ( !displayClass.trim().equals(fieldDisplayClass)){
      objEntityViewField.getDisplayFormat().setClassName(displayClass.trim());
    }
  }

  private class DisplayPatternChangeListener extends BasicDocumentListener{
    public void documentUpdated(String text) {
      displayPatternChangeAction(text);
    }
  }

  private void displayPatternChangeAction(String displayPattern){
    String fieldDisplayPattern = objEntityViewField.getDisplayFormat().getPattern();
    if ( !displayPattern.trim().equals(fieldDisplayPattern)){
      objEntityViewField.getDisplayFormat().setPattern(displayPattern.trim());
    }
  }

   private class EditClassChangeListener extends BasicDocumentListener{
    public void documentUpdated(String text) {
      editClassChangeAction(text);
    }
  }

  private void editClassChangeAction(String editClass){
    String fieldEditClass = objEntityViewField.getEditFormat().getClassName();
    if ( !editClass.trim().equals(fieldEditClass)){
      objEntityViewField.getEditFormat().setClassName(editClass.trim());
    }
  }

  private class EditPatternChangeListener extends BasicDocumentListener{
    public void documentUpdated(String text) {
      editPatternChangeAction(text);
    }
  }

  private void editPatternChangeAction(String editPattern){
    String fieldEditPattern = objEntityViewField.getEditFormat().getPattern();
    if ( !editPattern.trim().equals(fieldEditPattern)){
      objEntityViewField.getEditFormat().setPattern(editPattern.trim());
    }
  }

  private void preferredIndexFieldChanged(ChangeEvent e){
    Integer indx = (Integer)(preferredIndexField.getValue());
    int index = indx.intValue();
    if ( index != objEntityViewField.getPrefIndex()){
      objEntityViewField.setPrefIndex(index);
    }
  }

  public void setModels(DataMapTreeModel mapModel,
                        DataViewTreeModel viewModel,
                        FieldsTableModel tableModel){
    dataMapTreeModel = mapModel;
    dataViewTreeModel = viewModel;
    fieldsTableModel = tableModel;
  }
}
