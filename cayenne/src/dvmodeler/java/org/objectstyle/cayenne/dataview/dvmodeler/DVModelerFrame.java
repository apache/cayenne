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

import javax.swing.*;
import javax.swing.plaf.basic.*;
import javax.swing.tree.*;



import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;


import com.jgoodies.plaf.Options;
import com.jgoodies.plaf.HeaderStyle;
import com.jgoodies.forms.factories.*;

import org.jdom.*;
import org.jdom.output.*;

import java.io.*;
import java.beans.*;

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