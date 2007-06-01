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

import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This class defines contents for part of DVModelerFrame frame.
 *
 * @author Nataliya Kholodna
 * @version 1.0
 */

public class CardPanel extends JPanel{
  private FieldEditor fieldPanel;
  private JTable attributesTable;
  private JPanel objEntityViewPanel;

  private JPanel dataMapPanel;
  private JTextField dataMapNameField;
  private JTextField dataMapFileField;

  private JPanel dataViewPanel;
  private DataView dataView;
  private JTextField dataViewNameField;
  private JTextField dataViewFileField;
  private DataMapTreeModel dataMapTreeModel;

  private JTextField dataViewField;
  private JTextField viewNameField;
  private ObjEntityView objEntityView;
  private JComboBox objEntityCombo;

  private JPanel emptyPanel;
  private CardLayout cardLayout;


  private DVObject selectedObject;

  public CardPanel() {
    super();
    cardLayout = new CardLayout();
    this.setLayout( cardLayout );

    /*empty panel*/
    emptyPanel = new JPanel();
    add(emptyPanel, "emptyPanel");

    /*panel for field editing */
    fieldPanel = new FieldEditor();
    add(fieldPanel, "objEntityViewFieldPanel");

    /*table for ObjEntity's attributes*/
    attributesTable = new JTable();
    attributesTable.setModel(new AttributesTableModel());
    attributesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane scrollPane = new JScrollPane(attributesTable);
    scrollPane.setBorder(Borders.EMPTY_BORDER);
    add(scrollPane, "attributesTable");

    /*building panel for objEntityView properties*/
    FormLayout layout = new FormLayout(
        "right:55dlu, 3dlu, 200dlu",
        "");
    DefaultFormBuilder builderView = new DefaultFormBuilder(layout);
    builderView.setDefaultDialogBorder();

    viewNameField = new JTextField();
    viewNameField.getDocument().addDocumentListener(
        new NameChangeListener());
    builderView.append("Name:", viewNameField);

    objEntityCombo = new JComboBox();
    objEntityCombo.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        objEntityComboAction(e);
      }
    });
    builderView.append("ObjEntity:",   objEntityCombo);

    dataViewField = new JTextField();

    dataViewField.setEditable(false);
    dataViewField.setBorder(Borders.EMPTY_BORDER);
    builderView.append("Data View:", dataViewField);

    objEntityViewPanel = builderView.getPanel();
    scrollPane = new JScrollPane(objEntityViewPanel);
    scrollPane.setBorder(Borders.EMPTY_BORDER);
    add(scrollPane, "objEntityViewPanel");

    /*Building panel for dataMap properties*/
    FormLayout layout1 = new FormLayout(
        "right:55dlu, 3dlu, 200dlu",
        "");

    DefaultFormBuilder builderMap = new DefaultFormBuilder(layout1);
    builderMap.setDefaultDialogBorder();

    dataMapNameField = new JTextField();
    dataMapNameField.setEditable(false);
    dataMapNameField.setBorder(Borders.EMPTY_BORDER);
    builderMap.append("Name:", dataMapNameField);

    dataMapFileField = new JTextField();
    dataMapFileField.setEditable(false);
    dataMapFileField.setBorder(Borders.EMPTY_BORDER);
    builderMap.append("Location:", dataMapFileField);

    dataMapPanel = builderMap.getPanel();
    scrollPane = new JScrollPane(dataMapPanel);
    scrollPane.setBorder(Borders.EMPTY_BORDER);
    add(scrollPane, "dataMapPanel");

    /*Building panel for dataView properties*/
    FormLayout layout2 = new FormLayout(
        "right:55dlu, 3dlu, 200dlu",
        "");

    DefaultFormBuilder builderDataView = new DefaultFormBuilder(layout2);
    builderDataView.setDefaultDialogBorder();

    dataViewNameField = new JTextField();
    dataViewNameField.getDocument().addDocumentListener(
        new NameChangeListener());

    builderDataView.append("Name:", dataViewNameField);

    dataViewFileField = new JTextField();
    dataViewFileField.setEditable(false);
    dataViewFileField.setBorder(Borders.EMPTY_BORDER);
    builderDataView.append("Location:", dataViewFileField);

    dataViewPanel = builderDataView.getPanel();
    scrollPane = new JScrollPane(dataViewPanel);
    scrollPane.setBorder(Borders.EMPTY_BORDER);
    add(scrollPane, "dataViewPanel");
    /*setting default panel*/
    cardLayout.show(this, "emptyPanel");
  }

  /*shows  definning by selectedObject panel */
  public void showPanel(DVObject selectedObject){
    this.selectedObject = null;
    if (selectedObject instanceof DataMap){
      DataMap dataMap = (DataMap)selectedObject;
      dataMapNameField.setText(dataMap.getName());
      dataMapFileField.setText(dataMap.getFile().getName());
      cardLayout.show(this, "dataMapPanel");
    } else if (selectedObject instanceof DataView){
      dataView = (DataView)selectedObject;
      dataViewNameField.setText(dataView.getName());
      dataViewFileField.setText(
          (dataView.getFile() != null ?
          dataView.getFile().getName() :
          "New Data View"));

      cardLayout.show(this, "dataViewPanel");
    } else if (selectedObject instanceof ObjEntity){
      ObjEntity objEntity = (ObjEntity)selectedObject;
      AttributesTableModel attributesTableModel =
          (AttributesTableModel)attributesTable.getModel();
      attributesTableModel.setObjEntity(objEntity);
      cardLayout.show(this, "attributesTable");
    } else if (selectedObject instanceof ObjEntityView){
      objEntityView = (ObjEntityView)selectedObject;
      dataViewField.setText(objEntityView.getDataView().getName());
      viewNameField.setText(objEntityView.getName());

      ObjEntity objEntity = objEntityView.getObjEntity();
      if (objEntity == null){
        objEntityCombo.setSelectedIndex(0);
      } else {
        objEntityCombo.setSelectedItem(objEntity);
      }

      cardLayout.show(this, "objEntityViewPanel");
    } else if (selectedObject instanceof ObjEntityViewField){
      ObjEntityViewField objEntityViewField = (ObjEntityViewField)selectedObject;
      fieldPanel.setFieldProperties(objEntityViewField);
      cardLayout.show(this, "objEntityViewFieldPanel");
    } else if (selectedObject == null ){
      cardLayout.show(this, "emptyPanel");
    }
    this.selectedObject = selectedObject;
  }

  /*sets models which can be changed*/
  public void setModels(DataMapTreeModel mapModel,
                        DataViewTreeModel viewModel,
                        FieldsTableModel tableModel){
    dataMapTreeModel = mapModel;
    fieldPanel.setModels(mapModel, viewModel, tableModel);
  }

  /*sets another CayenneProject*/
  public void setProject(CayenneProject project) {
    if (project == null) {
      objEntityCombo.setModel(new DefaultComboBoxModel());
      return;
    }

    ObjEntity[] projectEntities = project.getObjEntities();
    ObjEntity[] entities = new ObjEntity[projectEntities.length + 1];
    ObjEntity nullEntity = null;
    entities[0] = nullEntity;

    for (int j = 0; j < projectEntities.length; j++){
      entities[j+1] = projectEntities[j];
    }
    DefaultComboBoxModel objEntitiesDefaultModel =
        new DefaultComboBoxModel(entities);
    objEntityCombo.setModel(objEntitiesDefaultModel);
  }

  private class NameChangeListener extends BasicDocumentListener {
    public void documentUpdated(String text) {
      if (selectedObject != null){
        selectedObject.setName(text.trim());
        if (selectedObject instanceof DataView){
          File file = dataView.getFile();
          File newFile = new File(file.getParentFile(), dataView.getName() + ".view.xml");
          dataView.setFile(newFile);
          dataViewFileField.setText(dataView.getFile().getName());
        }
      }
    }
  }

  private void objEntityComboAction(ActionEvent e){
    ObjEntity selectedObjEntity = (ObjEntity)objEntityCombo.getSelectedItem();
    ObjEntity objEntity = objEntityView.getObjEntity();
    if ((objEntity != selectedObjEntity)){
      if ((objEntity != null) && (selectedObjEntity != null)){
        java.util.List relationships = selectedObjEntity.getDataMap().getObjRelationshipsBySourceToOne(selectedObjEntity);
        int oldIndex = objEntity.getIndexOfObjEntityView(objEntityView);
        objEntityView.setObjEntity(selectedObjEntity);

        /*refrashing dataMapTreeModel: replace ObjEntityView
          to new ObjEntity*/
        dataMapTreeModel.replaceObjEntityView(objEntity, oldIndex, selectedObjEntity, objEntityView);

        /*changing fields properties, which depend on ObjEntity */
        java.util.List fields = objEntityView.getObjEntityViewFields();
        for (Iterator itr = fields.iterator();itr.hasNext();){
          ObjEntityViewField field = (ObjEntityViewField)itr.next();
          if(field.getCalcType().equals("nocalc")){
            if (field.getObjAttribute() != null){
              String fieldAttributeName = field.getObjAttribute().getName();
              if (selectedObjEntity.getObjAttribute(fieldAttributeName) != null){
                field.setObjAttribute(selectedObjEntity.getObjAttribute(fieldAttributeName));
              }else{
                field.setObjAttribute(null);
              }
            }
          }
          if(field.getCalcType().equals("lookup")){
            ObjRelationship fieldRelationship = field.getObjRelationship();
            if (fieldRelationship != null){
              String relationshipName = fieldRelationship.getName();
              ObjEntity targetObjEntity = fieldRelationship.getTargetObjEntity();
              Lookup lookup = field.getLookup();
              Lookup nullLookup = new Lookup(field);
              nullLookup.setLookupField(null);
              nullLookup.setLookupObjEntityView(null);

              field.setObjRelationship(null);
              field.setLookup(nullLookup);
              for (Iterator j = relationships.iterator(); j.hasNext();){
                ObjRelationship relationship = (ObjRelationship)j.next();
                if ((relationship.getName().equals(relationshipName))&&
                    (relationship.getTargetObjEntity() == targetObjEntity)){
                  field.setObjRelationship(relationship);
                  field.setLookup(lookup);
                }
              }
            }
          }
        }
      } else if ((objEntity == null) && (selectedObjEntity != null)){
        objEntityView.setObjEntity(selectedObjEntity);

        /*refreshing dataMapTreeModel: adding objEntityView to objEntity*/
        dataMapTreeModel.objEntityViewAdded(objEntityView);
      } else if ((objEntity != null) && (selectedObjEntity == null)){
        int index = objEntity.getIndexOfObjEntityView(objEntityView);
        objEntityView.setObjEntity(null);

        /*refreshing dataMapTreeModel: removing objEntityView from objEntity*/
        dataMapTreeModel.objEntityViewRemoved(objEntity, objEntityView, index);

        /*changing fields properties, which depend on ObjEntity */
        java.util.List fields = objEntityView.getObjEntityViewFields();

        for (Iterator itr = fields.iterator();itr.hasNext();){
          ObjEntityViewField field = (ObjEntityViewField)itr.next();
          if(field.getCalcType().equals("nocalc")){
            field.setObjAttribute(null);
          }
          if(field.getCalcType().equals("lookup")){
            Lookup nullLookup = new Lookup(field);
            nullLookup.setLookupField(null);
            nullLookup.setLookupObjEntityView(null);

            field.setObjRelationship(null);
            field.setLookup(nullLookup);

          }
        }
      }
    }
  }
}
