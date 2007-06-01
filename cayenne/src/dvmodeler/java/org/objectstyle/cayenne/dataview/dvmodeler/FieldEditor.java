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

import java.awt.BorderLayout;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.plaf.Options;

/**
 *
 * @author Nataliya Kholodna
 * @author Andriy Shapochka
 * @version 1.0
 */

public class FieldEditor extends JPanel {
  ObjEntityViewField objEntityViewField;
  private BorderLayout borderLayout = new BorderLayout();

  private JTextField viewNameField;
  private JComboBox objEntityCombo;
  private JTextField fieldNameField;
  private JComboBox dataTypeCombo;
  private JComboBox calcTypeCombo;
  private JComboBox objAttributeCombo;
  private JTextField defaultValueField;
  private JTextField captionField;
  private JCheckBox editableCheckBox;
  private JCheckBox visibleCheckBox;
  private JTextField displayPatternField;
  private JTextField displayClassField;
  private JTextField editPatternField;
  private JTextField editClassField;
  private JSpinner preferredIndexField;
  private JComboBox  lookupViewCombo;
  private JComboBox  lookupFieldCombo;
  private JComboBox objRelationshipCombo;

  private FieldEditorHelper fieldEditorHelper;


  public FieldEditor() {
    try {
      jbInit();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
    fieldEditorHelper = new FieldEditorHelper(this);
  }
  void jbInit() throws Exception {
    this.setLayout(borderLayout);
    this.add(buildPanel(), BorderLayout.CENTER);
  }

  private void initComponents() {
    viewNameField = new JTextField();
    objEntityCombo  = new JComboBox();
    fieldNameField = new JTextField();
    dataTypeCombo = new JComboBox(new String[] {
      "String", "Money", "Integer", "Double", "Percent",
      "Date", "Datetime", "Boolean", "Object" });
    calcTypeCombo = new JComboBox(new String[] {"No Calculation", "Lookup"});
    lookupViewCombo = new JComboBox();
    lookupFieldCombo = new JComboBox();
    objRelationshipCombo = new JComboBox();
    objAttributeCombo = new JComboBox();
    defaultValueField = new JTextField();
    captionField = new JTextField();
    editableCheckBox = new JCheckBox("", true);
    visibleCheckBox = new JCheckBox("", true);
    displayClassField = new JTextField();
    displayPatternField = new JTextField();
    editClassField = new JTextField();
    editPatternField = new JTextField();
    SpinnerNumberModel preferredIndexNumberModel = new SpinnerNumberModel(-1, -1, 1024, 1);
    preferredIndexField = new JSpinner(preferredIndexNumberModel);
  }


  private JComponent buildPanel() {
    initComponents();

    FormLayout layout = new FormLayout(
        "right:55dlu, 3dlu, 170dlu, 3dlu, " +
        "right:25dlu, 3dlu, 55dlu",
        "");
    DefaultFormBuilder generalBuilder = new DefaultFormBuilder(layout);
    generalBuilder.setDefaultDialogBorder();

    generalBuilder.append("Name:", fieldNameField);
    generalBuilder.append("Type:", dataTypeCombo);
    generalBuilder.append("Default:", defaultValueField);
    generalBuilder.appendUnrelatedComponentsGapRow();
    generalBuilder.nextLine(2);

    generalBuilder.append("Calc Type:", calcTypeCombo);
    generalBuilder.nextLine();
    generalBuilder.append("Attribute:", objAttributeCombo);
    generalBuilder.nextLine();

    generalBuilder.appendSeparator("Value Lookup");

    generalBuilder.append("Relationship:",   objRelationshipCombo);
    generalBuilder.nextLine();
    generalBuilder.append("Lookup View:",  lookupViewCombo);
    generalBuilder.nextLine();
    generalBuilder.append("Lookup Field:",   lookupFieldCombo);

    layout = new FormLayout(
        "right:55dlu, 3dlu, 170dlu, 3dlu, " +
        "right:25dlu, 3dlu, pref",
        "");
    DefaultFormBuilder displayBuilder = new DefaultFormBuilder(layout);
    displayBuilder.setDefaultDialogBorder();

    displayBuilder.append("Caption:", captionField);
    displayBuilder.append("Index:", preferredIndexField);
    displayBuilder.append("Visible:", visibleCheckBox);
    displayBuilder.nextLine();
    displayBuilder.append("Editable:", editableCheckBox);
    displayBuilder.appendUnrelatedComponentsGapRow();
    displayBuilder.nextLine(2);

    displayBuilder.append("Display Format:", displayClassField);
    displayBuilder.nextLine();
    displayBuilder.append("Display Pattern:", displayPatternField);
    displayBuilder.appendUnrelatedComponentsGapRow();
    displayBuilder.nextLine(2);

    displayBuilder.append("Edit Format:", editClassField);
    displayBuilder.nextLine();
    displayBuilder.append("Edit Pattern:", editPatternField);

    JScrollPane generalScrollPane = new JScrollPane(generalBuilder.getPanel());
    generalScrollPane.setBorder(Borders.EMPTY_BORDER);

    JScrollPane displayScrollPane = new JScrollPane(displayBuilder.getPanel());
    displayScrollPane.setBorder(Borders.EMPTY_BORDER);

    JTabbedPane fieldPane = new JTabbedPane(JTabbedPane.BOTTOM);
    fieldPane.putClientProperty(Options.NO_CONTENT_BORDER_KEY, Boolean.TRUE);

    fieldPane.addTab("General", generalScrollPane);
    fieldPane.addTab("Display", displayScrollPane);

    return fieldPane;
  }

  public void setFieldProperties(ObjEntityViewField field){
    fieldEditorHelper.setObjEntityViewField(field);

    objEntityViewField = field;
    ObjEntityView objEntityView = field.getObjEntityView();
    viewNameField.setText(objEntityView.getName());

    fieldNameField.setText(field.getName());
    dataTypeCombo.setSelectedItem(field.getDataType());
    if (field.getCalcType().equals("nocalc")){
      calcTypeCombo.setSelectedItem("No Calculation");
      objRelationshipCombo.setModel(new DefaultComboBoxModel());
      objRelationshipCombo.setEnabled(false);
      lookupViewCombo.setModel(new DefaultComboBoxModel());
      lookupViewCombo.setEnabled(false);
      lookupFieldCombo.setModel(new DefaultComboBoxModel());
      lookupFieldCombo.setEnabled(false);
    }
    if (field.getCalcType().equals("lookup")){
      calcTypeCombo.setSelectedItem("Lookup");
      objAttributeCombo.setModel(new DefaultComboBoxModel());
      objAttributeCombo.setEnabled(false);
    }

    defaultValueField.setText(field.getDefaultValue());
    captionField.setText(field.getCaption());
    editableCheckBox.setSelected(field.getEditable());
    visibleCheckBox.setSelected(field.getVisible());
    displayClassField.setText(field.getDisplayFormat().getClassName());
    displayPatternField.setText(field.getDisplayFormat().getPattern());
    editClassField.setText(field.getEditFormat().getClassName());
    editPatternField.setText(field.getEditFormat().getPattern());
    preferredIndexField.setValue(new Integer(field.getPrefIndex()));
  }

  public ObjEntityViewField getObjEntityViewField(){
    return objEntityViewField;
  }

  public JTextField getViewNameField(){
    return viewNameField;
  }
  public JComboBox getObjEntityCombo(){
    return objEntityCombo;
  }

  public JTextField getFieldNameField(){
    return fieldNameField;
  }
  public JComboBox getDataTypeCombo(){
    return dataTypeCombo;
  }
  public JComboBox getCalcTypeCombo(){
    return calcTypeCombo;
  }
  public JComboBox getObjAttributeCombo(){
    return objAttributeCombo;
  }
  public JTextField getDefaultValueField(){
    return defaultValueField;
  }
  public JTextField getCaptionField(){
    return captionField;
  }
  public JCheckBox getEditableCheckBox(){
    return editableCheckBox;
  }
  public JCheckBox getVisibleCheckBox(){
    return visibleCheckBox;
  }
  public JTextField getDisplayClassField(){
    return displayClassField;
  }
  public JTextField getDisplayPatternField(){
    return displayPatternField;
  }
  public JTextField getEditClassField(){
    return editClassField;
  }
  public JTextField getEditPatternField(){
    return editPatternField;
  }
  public JSpinner getPreferredIndexField(){
    return preferredIndexField;
  }
  public JComboBox  getLookupViewCombo(){
    return lookupViewCombo;
  }
  public JComboBox  getLookupFieldCombo(){
    return lookupFieldCombo;
  }
  public JComboBox getObjRelationshipCombo(){
    return objRelationshipCombo;
  }

  public void setModels(DataMapTreeModel mapModel,
                        DataViewTreeModel viewModel,
                        FieldsTableModel tableModel){
    fieldEditorHelper.setModels(mapModel, viewModel, tableModel);
  }
}

