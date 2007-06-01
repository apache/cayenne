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

import java.awt.*;
import java.awt.event.WindowListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import java.io.*;
import java.beans.*;



/**
 * This class is helper for DVModelerFrame class.
 *
 * @author Nataliya Kholodna
 * @version 1.0
 */


class DVModelerFrameHelper {
  private DVModelerFrame dvModelerFrame;
  private CayenneProject cayenneProject;

  /*This is actions for menu items from dvModelerFrame*/
  private Action saveAction = new SaveAction();
  private Action openAction = new OpenAction();
  private Action addNewObjEntityViewAction = new AddNewObjEntityViewAction();
  private Action addNewFieldAction = new AddNewFieldAction();
  private Action addNewDataViewAction = new AddNewDataViewAction();
  private Action deleteAction = new DeleteAction();

  private JFileChooser openProjectFileChooser = new JFileChooser();

  private PropertyChangeMediator propertyChangeMediator =
      new PropertyChangeMediator();

  private ErrorsDialog loadErrorsDialog;
  private ErrorsDialog saveErrorsDialog;

  public DVModelerFrameHelper(DVModelerFrame dvModelerFrame) {
    this.dvModelerFrame = dvModelerFrame;
    configureFileChoosers();
  }

  public Action getSaveAction(){
    return saveAction;
  }

  public Action getOpenAction(){
    return openAction;
  }

  public Action getAddNewObjEntityViewAction(){
    return addNewObjEntityViewAction;
  }

  public Action getAddNewFieldAction(){
    return addNewFieldAction;
  }

  public Action getAddNewDataViewAction(){
    return addNewDataViewAction;
  }
  public Action getDeleteAction(){
    return deleteAction;
  }


  public WindowListener getDVModelerFrameWindowListener(){
    return new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        exitAction(e);
      }
    };
  }

  public ActionListener getMenuItemExitActionListener(){
    return new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        exitAction(null);
      }
    };
  }

  private void exitAction(WindowEvent e){
    if (cayenneProject != null){
      int returnVal = JOptionPane.showConfirmDialog(
          dvModelerFrame,
          "Save current project before exit?",
          "DVModeler :: Exit Confirmation",
          JOptionPane.YES_NO_CANCEL_OPTION,
          JOptionPane.QUESTION_MESSAGE);
      if (returnVal == JOptionPane.YES_OPTION){
        int selectedValue = saveCayenneProjectFileAction(null, SaveErrorsDialog.EXIT_DIALOG);
        if ((selectedValue == -1)||(selectedValue == SaveErrorsDialog.EXIT_WITHOUT_SAVING)){
          System.exit(0);
        }
      } else if (returnVal == JOptionPane.NO_OPTION){
        System.exit(0);
      }
    } else {
      System.exit(0);
    }
  }

  public ActionListener getMenuItemCloseActionListener(){
    return new ActionListener(){
      public void actionPerformed(ActionEvent e){
        closeAction(e);
      }
    };
  }

  private void closeAction(ActionEvent e){
    if (cayenneProject != null){
      int returnVal = JOptionPane.showConfirmDialog(
          dvModelerFrame,
          "Save current project before closing?",
          "DVModeler :: Close Confirmation",
          JOptionPane.YES_NO_CANCEL_OPTION,
          JOptionPane.QUESTION_MESSAGE);
      if (returnVal == JOptionPane.YES_OPTION){
        int selectedValue = saveCayenneProjectFileAction(null, SaveErrorsDialog.CLOSE_DIALOG);
        if ((selectedValue == -1)
           || (selectedValue == SaveErrorsDialog.CLOSE_WITHOUT_SAVING)){
          closeCayenneProject();
        }

      } else if (returnVal == JOptionPane.NO_OPTION){
        closeCayenneProject();
      }
    }
  }

  private void closeCayenneProject(){
    JTree dataMapTree = dvModelerFrame.getDataMapTree();
    JTree dataViewTree = dvModelerFrame.getDataViewTree();
    cayenneProject = null;
    DataMapTreeModel dataMapTreeModel =
      (DataMapTreeModel)dataMapTree.getModel();
    dataMapTreeModel.setDataMaps(null);

    DataViewTreeModel dataViewTreeModel =
      (DataViewTreeModel)dataViewTree.getModel();
    dataViewTreeModel.setDataViews(null);

    dvModelerFrame.getTopCardPanel().showPanel(null);
    showBottomEmptyPanel();

    addNewDataViewAction.setEnabled(false);
    saveAction.setEnabled(false);
    dvModelerFrame.getMenuItemClose().setEnabled(false);
    dvModelerFrame.getMenuItemLoadErrors().setEnabled(false);
    dvModelerFrame.getMenuItemSaveErrors().setEnabled(false);
  }


  public ChangeListener getDataTabbedPaneChangeListener(){
    return new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        browserTabChanged();
      }
    };
  }

  public void browserTabChanged() {
    JTabbedPane dataTabbedPane = dvModelerFrame.getDataTabbedPane();
    int selectedIndex = dataTabbedPane.getSelectedIndex();
    String title = "";
    if (selectedIndex == 0) {
      title = "View Browser";
      dataViewTreeNodeSelected();
    } else if (selectedIndex == 1) {
      title = "Entity Browser";
      dataMapTreeNodeSelected();
    }
    dvModelerFrame.getBrowserTile().setCaption(title);
    enableControls();
  }

  private void enableControls() {
    JTree dataMapTree = dvModelerFrame.getDataMapTree();
    JTree dataViewTree = dvModelerFrame.getDataViewTree();

    JTabbedPane dataTabbedPane = dvModelerFrame.getDataTabbedPane();
    boolean dataViewTabSelected = (dataTabbedPane.getSelectedIndex() == 0);
    boolean openProject = (cayenneProject != null);
    Object node = (dataViewTabSelected ?
                   dataViewTree.getLastSelectedPathComponent() :
                   dataMapTree.getLastSelectedPathComponent());
    addNewDataViewAction.setEnabled(
        openProject && dataViewTabSelected);
    addNewObjEntityViewAction.setEnabled(
        node instanceof DataView ||
        node instanceof ObjEntity);
    addNewFieldAction.setEnabled(
        node instanceof ObjEntityView);
    deleteAction.setEnabled(
        node instanceof DataView ||
        node instanceof ObjEntityView ||
        node instanceof ObjEntityViewField);
  }


  private class OpenAction extends AbstractAction {
    private OpenAction() {
      super(
          "Open Cayenne Project...",
          new ImageIcon(SaveAction.class.getResource("images/open.gif")));
      setEnabled(true);
      putValue(Action.ACTION_COMMAND_KEY, "open-project");
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control O"));
      putValue(Action.SHORT_DESCRIPTION, "Open Cayenne Project");
    }
    public void actionPerformed(ActionEvent e) {
      openCayenneProjectFileAction(e);
    }
  }

  private void openCayenneProjectFileAction(ActionEvent  event){
    JTree dataMapTree = dvModelerFrame.getDataMapTree();
    JTree dataViewTree = dvModelerFrame.getDataViewTree();

    int returnVal = openProjectFileChooser.showDialog(dvModelerFrame, "Open Project");

    String fileName = "";
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      if (cayenneProject != null) {
        int result = JOptionPane.showConfirmDialog(
            dvModelerFrame,
            "Save current project before its closing?",
            "DVModeler :: Confirmation",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.CANCEL_OPTION)
          return;
        else if (result == JOptionPane.YES_OPTION)
          result = saveCayenneProjectFileAction(null, SaveErrorsDialog.CLOSE_DIALOG);
        if ((result == -1) || (result == SaveErrorsDialog.CLOSE_WITHOUT_SAVING)){
          cayenneProject = null;
          dvModelerFrame.getMenuItemLoadErrors().setEnabled(false);
          dvModelerFrame.getMenuItemSaveErrors().setEnabled(false);
        } else return;
      }

      File file = openProjectFileChooser.getSelectedFile();

      try {
        cayenneProject = new CayenneProject(file);
        java.util.List loadErrorsList = cayenneProject.getLoadErrors();

        if (loadErrorsList.size() != 0){
          loadErrorsDialog = new ErrorsDialog(dvModelerFrame, loadErrorsList, "Load Errors");
          loadErrorsDialog.setVisible(true);
          dvModelerFrame.getMenuItemLoadErrors().setEnabled(true);
        }

        DataViewTreeModel dataViewTreeModel = (DataViewTreeModel)dataViewTree.getModel();
        dataViewTreeModel.setDataViews(cayenneProject.getDataViews());
        DataMapTreeModel dataMapTreeModel = (DataMapTreeModel)dataMapTree.getModel();
        dataMapTreeModel.setDataMaps(cayenneProject.getDataMaps());

        JTable fieldsTable = dvModelerFrame.getFieldsTable();
        FieldsTableModel tableModel = (FieldsTableModel)fieldsTable.getModel();
        dvModelerFrame.getTopCardPanel().setModels(dataMapTreeModel,dataViewTreeModel,tableModel);
        for (Iterator i = cayenneProject.getDataViews().iterator(); i.hasNext(); ) {
          DataView dataView = (DataView)i.next();
          dataView.addPropertyChangeListener(propertyChangeMediator);
          for (Iterator j = dataView.getObjEntityViews().iterator(); j.hasNext(); ) {
            ObjEntityView view = (ObjEntityView)j.next();
            view.addPropertyChangeListener(propertyChangeMediator);
            for (Iterator k = view.getObjEntityViewFields().iterator(); k.hasNext(); ) {
              ObjEntityViewField field = (ObjEntityViewField)k.next();
              field.addPropertyChangeListener(propertyChangeMediator);
            }
          }
        }
      }
      catch (DVModelerException ex) {
        showExceptionDialog(
            "Could not open selected Cayenne project", ex);
      } finally {
        dvModelerFrame.getTopCardPanel().setProject(cayenneProject);
        enableControls();
        saveAction.setEnabled(true);
        dvModelerFrame.getMenuItemClose().setEnabled(true);
      }
    }
  }

    private class AddNewDataViewAction extends AbstractAction {
    private AddNewDataViewAction() {
      super(
          "Add Data View",
          new ImageIcon(DVModelerFrame.class.getResource("images/add-dataview.gif")));
      setEnabled(false);
      putValue(Action.ACTION_COMMAND_KEY, "create-new-data-view");
      putValue(Action.SHORT_DESCRIPTION, "Create New Data View");
    }
    public void actionPerformed(ActionEvent e) {
      addNewDataViewAction();
    }
  }

  private void addNewDataViewAction() {
    JTree dataViewTree = dvModelerFrame.getDataViewTree();

    DataView dataView = cayenneProject.createDataView();
    DataViewTreeModel dataViewTreeModel =
        (DataViewTreeModel)dataViewTree.getModel();
    TreePath path = dataViewTreeModel.dataViewAdded(dataView);
    dataViewTree.makeVisible(path);
    dataView.addPropertyChangeListener(propertyChangeMediator);
  }

    private class AddNewObjEntityViewAction extends AbstractAction {
    private AddNewObjEntityViewAction() {
      super(
          "Add ObjEntityView",
          new ImageIcon(AddNewObjEntityViewAction.class.getResource("images/add-objentityview.gif")));
      setEnabled(false);
      putValue(Action.ACTION_COMMAND_KEY, "create-new-view");
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control W"));
      putValue(Action.SHORT_DESCRIPTION, "Create New ObjEntityView");
    }
    public void actionPerformed(ActionEvent e) {
      addNewObjEntityViewAction(e);
    }
  }

  private void addNewObjEntityViewAction(ActionEvent  event){
    JTabbedPane dataTabbedPane = dvModelerFrame.getDataTabbedPane();
    JTree dataMapTree = dvModelerFrame.getDataMapTree();
    JTree dataViewTree = dvModelerFrame.getDataViewTree();

    int tabIndex = dataTabbedPane.getSelectedIndex();
    ObjEntityView view = null;
    if (tabIndex == 0) {
      TreePath path = dataViewTree.getSelectionPath();
      DataView dataView = (DataView)path.getLastPathComponent();
      ObjEntity selectedObjEntity = (ObjEntity)JOptionPane.showInputDialog(
          dvModelerFrame,
          "Select ObjEntity",
          "DVModeler :: New Entity View",
          JOptionPane.PLAIN_MESSAGE,
          null,
          cayenneProject.getObjEntities(),
          cayenneProject.getObjEntities()[0]);
       if (selectedObjEntity != null){
         view = new ObjEntityView(dataView);
         view.setObjEntity(selectedObjEntity);
       } else return;


    } else if (tabIndex == 1) {
      TreePath path = dataMapTree.getSelectionPath();

      ObjEntity objEntity = (ObjEntity)path.getLastPathComponent();
      /* DataView choice */
      Object[] dataViews = cayenneProject.getDataViews().toArray();
      if (dataViews.length == 0) {
        JOptionPane.showMessageDialog(
            dvModelerFrame,
            "First, create a data view (Tab \"Views\")",
            "DVModeler :: Warning",
            JOptionPane.WARNING_MESSAGE);
        return;
      }
      DataView selectedDataView = (DataView)JOptionPane.showInputDialog(
          dvModelerFrame,
          "Select Data View",
          "DVModeler :: New Entity View",
          JOptionPane.PLAIN_MESSAGE,
          null,
          dataViews,
          dataViews[0]);
      if (selectedDataView == null)
      return;
      java.util.List allObjEntityViews = new ArrayList();

      for (Iterator j = cayenneProject.getDataViews().iterator(); j.hasNext();){
        DataView dataView = (DataView)j.next();
        allObjEntityViews.addAll(dataView.getObjEntityViews());
      }
      view = selectedDataView.createObjEntityView(allObjEntityViews);
      view.setObjEntity(objEntity);
    }

    DataViewTreeModel dataViewTreeModel =
        (DataViewTreeModel)dataViewTree.getModel();
    TreePath path = dataViewTreeModel.objEntityViewAdded(view);
    dataViewTree.makeVisible(path);
    DataMapTreeModel dataMapTreeModel =
        (DataMapTreeModel)dataMapTree.getModel();
    path = dataMapTreeModel.objEntityViewAdded(view);
    if (path != null)
      dataMapTree.makeVisible(path);

    view.addPropertyChangeListener(propertyChangeMediator);
  }

  private class AddNewFieldAction extends AbstractAction {
    private AddNewFieldAction() {
      super(
          "Add Field",
          new ImageIcon(AddNewFieldAction.class.getResource("images/add-objentityviewfield.gif")));
      setEnabled(false);
      putValue(Action.ACTION_COMMAND_KEY, "create-new-field");
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control F"));
      putValue(Action.SHORT_DESCRIPTION, "Create New Field in ObjEntityView");
    }
    public void actionPerformed(ActionEvent e) {
      addNewFieldAction(e);
    }
  }

  private void addNewFieldAction(ActionEvent  event){
    JTabbedPane dataTabbedPane = dvModelerFrame.getDataTabbedPane();
    JTree dataMapTree = dvModelerFrame.getDataMapTree();
    JTree dataViewTree = dvModelerFrame.getDataViewTree();

    int tabIndex = dataTabbedPane.getSelectedIndex();
    TreePath selectionPath = null;
    if (tabIndex == 0) {
      selectionPath = dataViewTree.getSelectionPath();
    } else if (tabIndex == 1) {
      selectionPath = dataMapTree.getSelectionPath();
    }
    ObjEntityView objEntityView = (ObjEntityView)selectionPath.getLastPathComponent();
    ObjEntityViewField newField = objEntityView.createObjEntityViewField();
    DataViewTreeModel dataViewTreeModel =
        (DataViewTreeModel)dataViewTree.getModel();
    TreePath path = dataViewTreeModel.fieldAdded(newField);
    dataViewTree.makeVisible(path);
    DataMapTreeModel dataMapTreeModel =
        (DataMapTreeModel)dataMapTree.getModel();
    path = dataMapTreeModel.fieldAdded(newField);
    if (path != null)
      dataMapTree.makeVisible(path);

    int fieldIndex = objEntityView.getIndexOfObjEntityViewField(newField);

    JTable fieldsTable = dvModelerFrame.getFieldsTable();
    FieldsTableModel tableModel = (FieldsTableModel)fieldsTable.getModel();
      tableModel.fireTableRowsInserted(fieldIndex,fieldIndex);


    newField.addPropertyChangeListener(propertyChangeMediator);
  }



  private class SaveAction extends AbstractAction {
    private SaveAction() {
      super(
          "Save Cayenne Project",
          new ImageIcon(SaveAction.class.getResource("images/save_edit.gif")));
      setEnabled(true);
      putValue(Action.ACTION_COMMAND_KEY, "save-project");
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control S"));
      putValue(Action.SHORT_DESCRIPTION, "Save Cayenne Project");
    }
    public void actionPerformed(ActionEvent e) {
      saveCayenneProjectFileAction(e, SaveErrorsDialog.SAVE_DIALOG);
    }
  }
 /**
 * It return -1 if
 */
  private int saveCayenneProjectFileAction(ActionEvent  event, String dialogType){
    if (cayenneProject == null)
      return -1;
    try {
      cayenneProject.buildDataViewsElements();
      java.util.List saveErrorsList = cayenneProject.getSaveErrors();

      if (saveErrorsList.size() != 0){
        saveErrorsDialog = new ErrorsDialog(dvModelerFrame, saveErrorsList, "Save Errors");
        saveErrorsDialog.setVisible(false);
        dvModelerFrame.getMenuItemSaveErrors().setEnabled(true);

        int selectedValue = SaveErrorsDialog.showSaveErrorsDialog(dvModelerFrame, saveErrorsList, dialogType);

        if (selectedValue == SaveErrorsDialog.SAVE_ANYWAY){
          cayenneProject.save();
        }else return selectedValue;
      }else {
        cayenneProject.save();
      }
    }
    catch (IOException ex) {

      showExceptionDialog(
          "Could not save Cayenne project", ex);
    }
    return -1;
  }

  private class DeleteAction extends AbstractAction {
    private DeleteAction() {
      super(
          "Delete",
          new ImageIcon(AddNewObjEntityViewAction.class.getResource("images/delete.gif")));
      setEnabled(false);
      putValue(Action.ACTION_COMMAND_KEY, "delete");
      putValue(Action.SHORT_DESCRIPTION, "Remove Selected Item");

    }
    public void actionPerformed(ActionEvent e) {
      deleteAction(e);
    }
  }

  private void deleteAction(ActionEvent e){
    JTabbedPane dataTabbedPane = dvModelerFrame.getDataTabbedPane();
    JTree dataMapTree = dvModelerFrame.getDataMapTree();
    JTree dataViewTree = dvModelerFrame.getDataViewTree();

    boolean dataViewTabSelected = (dataTabbedPane.getSelectedIndex() == 0);
    boolean openProject = (cayenneProject != null);
    Object node = (dataViewTabSelected ?
                   dataViewTree.getLastSelectedPathComponent() :
                   dataMapTree.getLastSelectedPathComponent());

    if (node instanceof DataView){
      DataView dataView = (DataView)node;
      if (dataView.getObjEntityViewCount() != 0){
        int option = JOptionPane.showConfirmDialog(
          dvModelerFrame,
          "Do you really whant to delete the selected DataView?",
          "DVModeler :: Deleting DataView",
          JOptionPane.OK_CANCEL_OPTION
        );

        if (option == JOptionPane.OK_OPTION){
          int index = cayenneProject.getDataViews().indexOf(dataView);
          Map removingViews = cayenneProject.removeDataView(dataView);

          DataViewTreeModel dataViewTreeModel =
            (DataViewTreeModel)dataViewTree.getModel();
          dataViewTreeModel.dataViewRemoved(dataView, index);

          DataMapTreeModel dataMapTreeModel =
            (DataMapTreeModel)dataMapTree.getModel();
          dataMapTreeModel.objEntityViewsRemoved(removingViews);

          if (cayenneProject.getDataViews().size() == 0){

            dvModelerFrame.getTopCardPanel().showPanel(null);
            showBottomEmptyPanel();
          }else{
            if (index == 0){
              selectDataView((DataView)cayenneProject.getDataViews().get(0));
            } else {
              selectDataView((DataView)cayenneProject.getDataViews().get(index - 1));
            }
          }
        }
      } else {
        int index = cayenneProject.getDataViews().indexOf(dataView);
        Map removingViews = cayenneProject.removeDataView(dataView);

        DataViewTreeModel dataViewTreeModel =
          (DataViewTreeModel)dataViewTree.getModel();
        dataViewTreeModel.dataViewRemoved(dataView, index);

        if (cayenneProject.getDataViews().size() == 0){

          dvModelerFrame.getTopCardPanel().showPanel(null);
          showBottomEmptyPanel();
        }else{
          if (index == 0){
            selectDataView((DataView)cayenneProject.getDataViews().get(0));
          } else {
            selectDataView((DataView)cayenneProject.getDataViews().get(index - 1));
          }
        }
      }


    } else if (node instanceof ObjEntityView){
      ObjEntityView view = (ObjEntityView)node;
      DataView dataView = view.getDataView();
      int indexInDataView = dataView.getIndexOfObjEntityView(view);
      ObjEntity entity = view.getObjEntity();

      int indexInDataMap = -1;

      if (entity != null){
        indexInDataMap = entity.getIndexOfObjEntityView(view);
        dataView.removeObjEntityView(view);

        DataMapTreeModel dataMapTreeModel =
          (DataMapTreeModel)dataMapTree.getModel();
        dataMapTreeModel.objEntityViewRemoved(entity, view, indexInDataMap);

      } else {
        dataView.removeObjEntityView(view);
      }

      DataViewTreeModel dataViewTreeModel =
        (DataViewTreeModel)dataViewTree.getModel();
      dataViewTreeModel.objEntityViewRemoved(view, indexInDataView);


      if (dataViewTabSelected){
        if (dataView.getObjEntityViewCount() == 0){
          selectDataView(dataView);
        }else{
          if (indexInDataView == 0){
            selectObjEntityView(dataViewTabSelected, dataView.getObjEntityView(0));
          } else {
            selectObjEntityView(dataViewTabSelected, dataView.getObjEntityView(indexInDataView - 1));
          }
        }
      }else{
        if (entity.getObjEntityViewCount() == 0){
          selectObjEntity(entity);
        }else{
          if (indexInDataMap == 0){
            selectObjEntityView(dataViewTabSelected, entity.getObjEntityView(0));
          } else {
            selectObjEntityView(dataViewTabSelected, entity.getObjEntityView(indexInDataMap - 1));
          }
        }
      }
    } else if (node instanceof ObjEntityViewField){
      ObjEntityViewField field = (ObjEntityViewField)node;
      ObjEntityView view = field.getObjEntityView();

      DataView dataView = view.getDataView();
      int index = view.getIndexOfObjEntityViewField(field);
      view.removeObjEntityViewField(field);

      DataViewTreeModel dataViewTreeModel =
        (DataViewTreeModel)dataViewTree.getModel();
      dataViewTreeModel.fieldRemoved(field, index);

      DataMapTreeModel dataMapTreeModel =
        (DataMapTreeModel)dataMapTree.getModel();
      dataMapTreeModel.fieldRemoved(field, index);

      JTable fieldsTable = dvModelerFrame.getFieldsTable();
      FieldsTableModel tableModel = (FieldsTableModel)fieldsTable.getModel();
      tableModel.fireTableRowsDeleted(index,index);

      ObjEntity entity = view.getObjEntity();
      DataMap dataMap = entity.getDataMap();
      if (view.getObjEntityViewFieldCount() == 0){
        if (dataViewTabSelected){
          selectObjEntityViewInDataViewTree(view);
        } else {
          selectObjEntityViewInDataMapTree(view);
        }

      } else {
        if (index == 0){
          selectField(dataViewTabSelected, view.getObjEntityViewField(0));
        } else {
          selectField(dataViewTabSelected, view.getObjEntityViewField(index-1));

        }
      }
    }
  }


  private void selectDataView(DataView dataView){
    JTree dataViewTree = dvModelerFrame.getDataViewTree();

    DataViewTreeModel dataViewTreeModel = (DataViewTreeModel)dataViewTree.getModel();
    Object[] path = new Object[] {dataViewTreeModel.getRoot(),
                                  dataView
                                  };

    dataViewTree.setSelectionPath(new TreePath(path));
  }

  private void selectObjEntity(ObjEntity entity){
    JTree dataMapTree = dvModelerFrame.getDataMapTree();

    DataMapTreeModel dataMapTreeModel = (DataMapTreeModel)dataMapTree.getModel();
    DataMap dataMap = entity.getDataMap();
    Object[] path = new Object[] {dataMapTreeModel.getRoot(),
                                  dataMap,
                                  entity
                                 };

    dataMapTree.setSelectionPath(new TreePath(path));
  }

  private void selectObjEntityView(boolean dataViewTabSelected, ObjEntityView view){
    DataView dataView = view.getDataView();
    JTree dataMapTree = dvModelerFrame.getDataMapTree();
    JTree dataViewTree = dvModelerFrame.getDataViewTree();

    DataViewTreeModel dataViewTreeModel = (DataViewTreeModel)dataViewTree.getModel();
    if (dataViewTabSelected){
      Object[] path = new Object[]{dataViewTreeModel.getRoot(),
                                   dataView,
                                   view
                                  };

      dataViewTree.setSelectionPath(new TreePath(path));
    } else {
      ObjEntity entity = view.getObjEntity();
      DataMap dataMap = entity.getDataMap();
      DataMapTreeModel dataMapTreeModel = (DataMapTreeModel)dataMapTree.getModel();

      Object[] path = new Object[]{dataMapTreeModel.getRoot(),
                                   dataMap,
                                   entity,
                                   view
                                  };
      dataMapTree.setSelectionPath(new TreePath(path));
    }
  }

  private void selectObjEntityViewInDataViewTree(ObjEntityView view){
    JTree dataViewTree = dvModelerFrame.getDataViewTree();

    DataViewTreeModel dataViewTreeModel = (DataViewTreeModel)dataViewTree.getModel();
    DataView dataView = view.getDataView();
    Object[] path = new Object[] {dataViewTreeModel.getRoot(),
                                        dataView,
                                        view
                                       };

    dataViewTree.setSelectionPath(new TreePath(path));
  }

  private void selectObjEntityViewInDataMapTree(ObjEntityView view){
    JTree dataMapTree = dvModelerFrame.getDataMapTree();

    DataMapTreeModel dataMapTreeModel = (DataMapTreeModel)dataMapTree.getModel();
    ObjEntity entity = view.getObjEntity();
    DataMap dataMap = entity.getDataMap();
    Object[] path = new Object[] {dataMapTreeModel.getRoot(),
                                        dataMap,
                                        entity,
                                        view
                                       };

    dataMapTree.setSelectionPath(new TreePath(path));
  }
  private void selectField(boolean dataViewTabSelected, ObjEntityViewField field){
    JTree dataMapTree = dvModelerFrame.getDataMapTree();
    JTree dataViewTree = dvModelerFrame.getDataViewTree();

    ObjEntityView view = field.getObjEntityView();
    DataView dataView = view.getDataView();
    DataViewTreeModel dataViewTreeModel = (DataViewTreeModel)dataViewTree.getModel();
    if (dataViewTabSelected){
      Object[] path = new Object[]{dataViewTreeModel.getRoot(),
                                   dataView,
                                   view,
                                   field
                                  };

      dataViewTree.setSelectionPath(new TreePath(path));
    } else {
      ObjEntity entity = view.getObjEntity();
      DataMap dataMap = entity.getDataMap();
      DataMapTreeModel dataMapTreeModel = (DataMapTreeModel)dataMapTree.getModel();

      Object[] path = new Object[]{dataMapTreeModel.getRoot(),
                                   dataMap,
                                   entity,
                                   view,
                                   field
                                  };
      dataMapTree.setSelectionPath(new TreePath(path));
    }
  }

  public ActionListener getMenuItemLoadErrorsActionListenr(){
    return new ActionListener(){
      public void actionPerformed(ActionEvent e){
        loadErrorsAction(e);
      }
    };
  }

  private void loadErrorsAction(ActionEvent e){
    loadErrorsDialog.setVisible(true);
  }

  public ActionListener getMenuItemSaveErrorsActionListener(){
    return new ActionListener(){
      public void actionPerformed(ActionEvent e){
        saveErrorsAction(e);
      }
    };
  }

  private void saveErrorsAction(ActionEvent e){
    saveErrorsDialog.setVisible(true);
  }

  public TreeSelectionListener getDataMapTreeTreeSelectionListener(){
    return new TreeSelectionListener(){
      public void valueChanged(TreeSelectionEvent e){
        dataMapTreeNodeSelected();
      }
    };
  }

  private void dataMapTreeNodeSelected() {
    JTree dataMapTree = dvModelerFrame.getDataMapTree();

    Object node = dataMapTree.getLastSelectedPathComponent();
    String topTitle = "";
    String bottomTitle = "";
    if (node == null){
      dvModelerFrame.getTopCardPanel().showPanel(null);
      showBottomEmptyPanel();
    }else if (node instanceof DataMap){
      dvModelerFrame.getTopCardPanel().showPanel((DVObject)node);
      showBottomEmptyPanel();
      topTitle = "Data View \"" +
                 node +
                 "\" :: Properties";
    } else if (node instanceof ObjEntity){
      dvModelerFrame.getTopCardPanel().showPanel((DVObject)node);
      ObjEntity objEntity = (ObjEntity)node;
      showRelatioshipsTable(objEntity);
      topTitle = "Entity \"" +
                 node +
                 "\" :: Attributes";
      bottomTitle = "Entity \"" +
                    node +
                    "\" :: Relationships";
    } else if (node instanceof ObjEntityView){
      dvModelerFrame.getTopCardPanel().showPanel((DVObject)node);
      showFieldsTable((ObjEntityView)node);
      topTitle = "View \"" +
                 node +
                 "\" :: Properties";
      bottomTitle = "View \"" +
                    node +
                    "\" :: Fields";
    } else if (node instanceof ObjEntityViewField){
      dvModelerFrame.getTopCardPanel().showPanel((DVObject)node);
      ObjEntityViewField field = (ObjEntityViewField)node;

      showFieldsTable(field.getObjEntityView());
      int index = field.getObjEntityView().getIndexOfObjEntityViewField(field);
      JTable fieldsTable = dvModelerFrame.getFieldsTable();
      fieldsTable.setRowSelectionInterval(index,index);

      topTitle = "Field \"" + node + "\" :: Properties";
      bottomTitle = "View \"" +
                    ((ObjEntityViewField)node).getObjEntityView() +
                    "\" :: Fields";
    }

    dvModelerFrame.getTopTile().setCaption(topTitle);
    dvModelerFrame.getBottomTile().setCaption(bottomTitle);
    enableControls();
  }

  public TreeSelectionListener getDataViewTreeTreeSelectionListener(){
    return new TreeSelectionListener(){
      public void valueChanged(TreeSelectionEvent e){
        dataViewTreeNodeSelected();
      }
    };
  }

  private void dataViewTreeNodeSelected() {
    JTree dataViewTree = dvModelerFrame.getDataViewTree();

    Object node = dataViewTree.getLastSelectedPathComponent();
    String topTitle = "";
    String bottomTitle = "";

    if (node == null){
      dvModelerFrame.getTopCardPanel().showPanel(null);
      showBottomEmptyPanel();
    }else if (node instanceof DataView){
      dvModelerFrame.getTopCardPanel().showPanel((DVObject)node);
      showBottomEmptyPanel();
      topTitle = "Data View \"" +
                 node +
                 "\" :: Properties";
    } else if (node instanceof ObjEntityView){
      dvModelerFrame.getTopCardPanel().showPanel((DVObject)node);
      showFieldsTable((ObjEntityView)node);
      topTitle = "View \"" +
                 node +
                 "\" :: Properties";
      bottomTitle = "View \"" +
                    node +
                    "\" :: Fields";
    } else if (node instanceof ObjEntityViewField){
      dvModelerFrame.getTopCardPanel().showPanel((DVObject)node);
      ObjEntityViewField field = (ObjEntityViewField)node;
      showFieldsTable(field.getObjEntityView());
      int index = field.getObjEntityView().getIndexOfObjEntityViewField(field);

      JTable fieldsTable = dvModelerFrame.getFieldsTable();
      fieldsTable.setRowSelectionInterval(index,index);
      topTitle = "Field \"" + node + "\" :: Properties";
      bottomTitle = "View \"" +
                    ((ObjEntityViewField)node).getObjEntityView() +
                    "\" :: Fields";
    }
    dvModelerFrame.getTopTile().setCaption(topTitle);
    dvModelerFrame.getBottomTile().setCaption(bottomTitle);
    enableControls();
  }


    private void showBottomEmptyPanel(){
    JPanel bottomCardPanel = dvModelerFrame.getBottomCardPanel();
    CardLayout cardLayout = (CardLayout)bottomCardPanel.getLayout();
    cardLayout.show(bottomCardPanel, "emptyPanel");
  }

  private void showFieldsTable(ObjEntityView view){
    JTable fieldsTable = dvModelerFrame.getFieldsTable();
    FieldsTableModel fieldsTableModel = (FieldsTableModel)fieldsTable.getModel();
    fieldsTableModel.setObjEntityView(view);

    JPanel bottomCardPanel = dvModelerFrame.getBottomCardPanel();
    CardLayout cardLayout = (CardLayout)bottomCardPanel.getLayout();
    cardLayout.show(bottomCardPanel, "fieldsTable");
  }

  private void showRelatioshipsTable(ObjEntity objEntity){
    java.util.List relationships = new ArrayList();
    relationships.addAll(objEntity.getDataMap().getObjRelationshipsBySource(objEntity));
    relationships.addAll(objEntity.getDataMap().getObjRelationshipsByTarget(objEntity));

    JTable relationshipsTable = dvModelerFrame.getRelationshipsTable();
    RelationshipsTableModel model = (RelationshipsTableModel)relationshipsTable.getModel();
    model.setObjRelationships(relationships);

    JPanel bottomCardPanel = dvModelerFrame.getBottomCardPanel();
    CardLayout cardLayout = (CardLayout)bottomCardPanel.getLayout();
    cardLayout.show(bottomCardPanel, "relationshipsTable");
  }

    private void configureFileChoosers() {
    openProjectFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
    openProjectFileChooser.setApproveButtonMnemonic('O');
    openProjectFileChooser.setApproveButtonText("Open Project");
    openProjectFileChooser.setApproveButtonToolTipText("Open Selected Cayenne Project");
    openProjectFileChooser.setDialogTitle("DVModeler :: Open Cayenne Project");
    openProjectFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    openProjectFileChooser.setMultiSelectionEnabled(false);
    openProjectFileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
      public String getDescription() {
        return "(*.xml) Cayenne Project Files";
      }
      public boolean accept(File file) {
        return (file.isDirectory() ||
                file.getName().toLowerCase().endsWith(".xml"));
      }
    });
  }

    private class PropertyChangeMediator implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent e) {
      Object source = e.getSource();
      String propertyName = e.getPropertyName();

      JTree dataMapTree = dvModelerFrame.getDataMapTree();
      JTree dataViewTree = dvModelerFrame.getDataViewTree();

      DataViewTreeModel dataViewTreeModel =
          (DataViewTreeModel)dataViewTree.getModel();
      DataMapTreeModel dataMapTreeModel =
          (DataMapTreeModel)dataMapTree.getModel();

      JTable fieldsTable = dvModelerFrame.getFieldsTable();


      if (source instanceof DataView) {
        if ("name".equals(propertyName)) {
          DataView dataView = (DataView)source;
          String topTitle = "Data View \"" +
                             source +
                             "\" :: Properties";
          dvModelerFrame.getTopTile().setCaption(topTitle);
          dataViewTreeModel.dataViewChanged((DataView)source);
        }
      } else if (source instanceof ObjEntityView) {
        if ("name".equals(propertyName)) {
          String topTitle = "View \"" +
                            source +
                            "\" :: Properties";
          String bottomTitle = "View \"" +
                               source +
                               "\" :: Fields";
          dvModelerFrame.getTopTile().setCaption(topTitle);
          dvModelerFrame.getBottomTile().setCaption(bottomTitle);
          dataViewTreeModel.objEntityViewChanged((ObjEntityView)source);
          dataMapTreeModel.objEntityViewChanged((ObjEntityView)source);
        }
      } else if (source instanceof ObjEntityViewField) {

        FieldsTableModel fieldsTableModel = (FieldsTableModel)fieldsTable.getModel();
        ObjEntityViewField field = (ObjEntityViewField)source;

        if ("name".equals(propertyName)) {
          String topTitle = "Field \"" + source + "\" :: Properties";
          dvModelerFrame.getTopTile().setCaption(topTitle);
          dataViewTreeModel.fieldChanged(field);
          dataMapTreeModel.fieldChanged(field);
          fieldsTableModel.fireTableCellUpdated(field.getObjEntityView().getIndexOfObjEntityViewField(field), 0);
        }
        if ("dataType".equals(propertyName)){
          fieldsTableModel.fireTableCellUpdated(field.getObjEntityView().getIndexOfObjEntityViewField(field), 1);
        }
        if ("calcType".equals(propertyName)){
          fieldsTableModel.fireTableCellUpdated(field.getObjEntityView().getIndexOfObjEntityViewField(field), 2);
        }
        if ("objRelationship".equals(propertyName)){

          dataViewTreeModel.fieldChanged(field);
          dataMapTreeModel.fieldChanged(field);
          fieldsTableModel.fireTableCellUpdated(field.getObjEntityView().getIndexOfObjEntityViewField(field), 3);
        }
        if ("objAttribute".equals(propertyName)){
          dataViewTreeModel.fieldChanged(field);
          dataMapTreeModel.fieldChanged(field);
          fieldsTableModel.fireTableCellUpdated(field.getObjEntityView().getIndexOfObjEntityViewField(field), 3);
        }
        if ("lookup".equals(propertyName)){
          dataViewTreeModel.fieldChanged(field);
          dataMapTreeModel.fieldChanged(field);
          fieldsTableModel.fireTableCellUpdated(field.getObjEntityView().getIndexOfObjEntityViewField(field), 4);
        }
      }
    }
  }

  private void showExceptionDialog(String description, Exception ex) {
    String message = "<html>" + description + "<br>";
    message += ex.getClass() + ": " + ex.getMessage() + "<br>";
    Exception cause = (Exception)ex.getCause();
    if (cause != null) {
      message += "caused by " + cause.getClass() + ": " + cause.getMessage();
    }
    message += "</html>";
    JOptionPane.showMessageDialog(
        dvModelerFrame, message, "DVModeler :: Error", JOptionPane.ERROR_MESSAGE);
  }



}
