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


package org.apache.cayenne.dataview.dvmodeler;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Insets;

import java.awt.event.KeyEvent;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.looks.HeaderStyle;
import com.jgoodies.looks.Options;

/**
 *
 * @author Nataliya Kholodna
 * @author Andriy Shapochka
 * @version 1.0
 */

public class DVModelerFrame extends JFrame {
 private DVModelerFrameHelper dvModelerFrameHelper;

  private Action saveAction;
  private Action openAction;
  private Action addNewObjEntityViewAction;
  private Action addNewFieldAction;
  private Action addNewDataViewAction;
  private Action deleteAction;
  private JMenuItem close;
  private JMenuItem loadErrors;
  private JMenuItem saveErrors;

  private JSplitPane fieldsSplitPane;
  private JSplitPane splitPane;

  private JTile browserTile;
  private JTile topTile;
  private JTile bottomTile;

  private JTree dataViewTree;
  private JTree dataMapTree;

  private CardPanel topCardPanel;
  private JPanel bottomCardPanel;

  private JTabbedPane dataTabbedPane;

  private JTable fieldsTable;
  private JTable relationshipsTable;

  protected DVModelerFrame(){
    super(" Data View Modeler ");

    dvModelerFrameHelper = new DVModelerFrameHelper(this);

    saveAction = dvModelerFrameHelper.getSaveAction();
    openAction = dvModelerFrameHelper.getOpenAction();
    addNewObjEntityViewAction = dvModelerFrameHelper.getAddNewObjEntityViewAction();
    addNewFieldAction = dvModelerFrameHelper.getAddNewFieldAction();
    addNewDataViewAction = dvModelerFrameHelper.getAddNewDataViewAction();
    deleteAction = dvModelerFrameHelper.getDeleteAction();


    getContentPane().setLayout(new BorderLayout());

    buildMenuBar();
    buildToolBar();
    buildMainPanel();
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);


    dvModelerFrameHelper.browserTabChanged();

    this.addWindowListener(dvModelerFrameHelper.getDVModelerFrameWindowListener());

  }

  private void buildMenuBar(){
    JMenuBar menuBar = new JMenuBar();
    menuBar.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.BOTH);

    JMenu menuFile = new JMenu("File");
    menuFile.setMnemonic(KeyEvent.VK_F);
    menuBar.add(menuFile);
    menuFile.add(openAction);
    saveAction.setEnabled(false);
    menuFile.add(saveAction);
    close = new JMenuItem("Close Cayenne Project");
    close.addActionListener(
        dvModelerFrameHelper.getMenuItemCloseActionListener());
    close.setEnabled(false);
    menuFile.add(close);

    menuFile.addSeparator();

    loadErrors = new JMenuItem("Show Load Errors");
    loadErrors.addActionListener(dvModelerFrameHelper.getMenuItemLoadErrorsActionListenr());
    loadErrors.setEnabled(false);
    menuFile.add(loadErrors);

    saveErrors = new JMenuItem("Show Save Errors");
    saveErrors.addActionListener(dvModelerFrameHelper.getMenuItemSaveErrorsActionListener());
    saveErrors.setEnabled(false);
    menuFile.add(saveErrors);

    menuFile.addSeparator();

    JMenuItem exit = new JMenuItem("Exit");
    exit.addActionListener(
      dvModelerFrameHelper.getMenuItemExitActionListener());
    menuFile.add(exit);

    JMenu menuEdit = new JMenu("Edit");
    menuEdit.setMnemonic(KeyEvent.VK_E);
    menuBar.add(menuEdit);
    menuEdit.add(addNewDataViewAction);
    menuEdit.add(addNewObjEntityViewAction);
    menuEdit.add(addNewFieldAction);
    menuEdit.addSeparator();
    menuEdit.add(deleteAction);

    setJMenuBar(menuBar);
  }


  private void buildToolBar(){
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(false);
    toolBar.setOrientation(JToolBar.HORIZONTAL);
    toolBar.setRollover(true);
    toolBar.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.BOTH);

    toolBar.add(dvModelerFrameHelper.getOpenAction());
    toolBar.add(dvModelerFrameHelper.getSaveAction());
    toolBar.addSeparator();
    toolBar.add(dvModelerFrameHelper.getAddNewDataViewAction());
    toolBar.add(dvModelerFrameHelper.getAddNewObjEntityViewAction());
    toolBar.add(dvModelerFrameHelper.getAddNewFieldAction());
    toolBar.addSeparator();
    toolBar.add(dvModelerFrameHelper.getDeleteAction());


    Insets buttonMargin = new Insets(0, 0, 0, 0);
    int count = toolBar.getComponentCount();
    for (int i = 0; i < count; i++) {
      Component c = toolBar.getComponentAtIndex(i);
      if (c instanceof AbstractButton) {
        ((AbstractButton)c).setMargin(buttonMargin);
      }
    }

    getContentPane().add(toolBar, BorderLayout.NORTH);
  }

  private void buildMainPanel(){
    JScrollPane mapsScrollPane = new JScrollPane(buildDataMapTree());
    mapsScrollPane.setBorder(Borders.EMPTY_BORDER);
    JScrollPane viewsScrollPane = new JScrollPane(buildDataViewTree());
    viewsScrollPane.setBorder(Borders.EMPTY_BORDER);
    dataTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
    dataTabbedPane.putClientProperty(Options.NO_CONTENT_BORDER_KEY, Boolean.TRUE);
    dataTabbedPane.putClientProperty(Options.EMBEDDED_TABS_KEY, Boolean.TRUE);
    dataTabbedPane.addTab(
        "Views",
        new ImageIcon(DVModelerFrame.class.getResource("images/dataview-node.gif")),
        viewsScrollPane);
    dataTabbedPane.addTab(
        "Entities",
        new ImageIcon(DVModelerFrame.class.getResource("images/datamap-node.gif")),
        mapsScrollPane);
    dataTabbedPane.addChangeListener(
        dvModelerFrameHelper.getDataTabbedPaneChangeListener());
    topCardPanel = new CardPanel();
    bottomCardPanel = buildBottomCardPanel();


    fieldsSplitPane = new JSplitPane(
        JSplitPane.VERTICAL_SPLIT,
        topTile = new JTile(topCardPanel),
        bottomTile = new JTile(bottomCardPanel));
    fieldsSplitPane.setDividerLocation(300);
    fieldsSplitPane.setBorder(Borders.EMPTY_BORDER);
    BasicSplitPaneDivider divider = ((BasicSplitPaneUI)fieldsSplitPane.getUI()).getDivider();
    divider.setBorder(Borders.EMPTY_BORDER);

    splitPane = new JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT,
        browserTile = new JTile(dataTabbedPane),
        fieldsSplitPane);

    splitPane.setDividerLocation(250);

    divider = ((BasicSplitPaneUI)splitPane.getUI()).getDivider();
    divider.setBorder(Borders.EMPTY_BORDER);
    splitPane.setBorder(Borders.DIALOG_BORDER);

    getContentPane().add(splitPane, BorderLayout.CENTER);

    CardLayout cardLayout = (CardLayout)bottomCardPanel.getLayout();
    cardLayout.show(bottomCardPanel, "emptyPanel");
  }

  private JPanel buildBottomCardPanel(){
    JPanel bottomPanel = new JPanel();
    CardLayout bottomCardLayout = new CardLayout();
    bottomPanel.setLayout( bottomCardLayout );
    JPanel emptyPanel = new JPanel();
    bottomPanel.add(emptyPanel, "emptyPanel");

    fieldsTable = new JTable();
    fieldsTable.setModel(new FieldsTableModel());
    fieldsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane scrollPane = new JScrollPane(fieldsTable);
    scrollPane.setBorder(Borders.EMPTY_BORDER);
    bottomPanel.add(scrollPane, "fieldsTable");

    relationshipsTable = new JTable(new RelationshipsTableModel());
    relationshipsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    scrollPane = new JScrollPane(relationshipsTable);
    scrollPane.setBorder(Borders.EMPTY_BORDER);
    bottomPanel.add(scrollPane, "relationshipsTable");

    return bottomPanel;
  }

  private JTree buildDataMapTree(){
    dataMapTree = new JTree(new DataMapTreeModel());
    dataMapTree.setRootVisible(false);
    dataMapTree.setShowsRootHandles(true);
    dataMapTree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
    DefaultTreeCellRenderer renderer = new DVTreeCellRenderer();

    dataMapTree.setCellRenderer(renderer);

    dataMapTree.addTreeSelectionListener(
      dvModelerFrameHelper.getDataMapTreeTreeSelectionListener());
    return dataMapTree;
  }

  private JTree buildDataViewTree(){
    dataViewTree = new JTree(new DataViewTreeModel());
    dataViewTree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
    dataViewTree.setRootVisible(false);
    dataViewTree.setShowsRootHandles(true);
    DefaultTreeCellRenderer renderer = new DVTreeCellRenderer();

    dataViewTree.setCellRenderer(renderer);

    dataViewTree.addTreeSelectionListener(dvModelerFrameHelper.getDataViewTreeTreeSelectionListener());
    return dataViewTree;
  }

  public JTree getDataViewTree(){
    return dataViewTree;
  }

  public JTree getDataMapTree(){
    return dataMapTree;
  }

  public CardPanel getTopCardPanel(){
    return topCardPanel;
  }

  public JPanel getBottomCardPanel(){
    return bottomCardPanel;
  }

  public JTabbedPane getDataTabbedPane(){
    return dataTabbedPane;
  }

  public JTable getFieldsTable(){
    return fieldsTable;
  }

  public JTable getRelationshipsTable(){
    return relationshipsTable;
  }

  public JTile getBrowserTile(){
    return browserTile;
  }

  public JTile getTopTile(){
    return topTile;
  }

  public JTile getBottomTile(){
    return bottomTile;
  }

  public JMenuItem getMenuItemClose(){
    return close;
  }
  public JMenuItem getMenuItemLoadErrors(){
    return loadErrors;
  }
  public JMenuItem getMenuItemSaveErrors(){
    return saveErrors;
  }






}
