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

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


/**
 *
 * @author Nataliya Kholodna
 * @version 1.0
 */

class ErrorsDialog extends JDialog{
  public ErrorsDialog(Frame frame, java.util.List errors, String titleText){
    super(frame, "DVModeler :: " + titleText, false);

    ButtonBarBuilder buttonPanelBuilder = new ButtonBarBuilder();
    buttonPanelBuilder.addGlue();

    //button
    JButton okButton = new JButton("Ok");
    okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setVisible(false);
      }
    });

    buttonPanelBuilder.addGridded(okButton);
    getRootPane().setDefaultButton(okButton);


    //Put everything together, using the content pane's BorderLayout.
    Container contentPane = getContentPane();
        FormLayout layout = new FormLayout(
                        "fill:pref:grow",
                        "fill:p:grow, 5dlu, p");

    PanelBuilder mainPanelBuilder = new PanelBuilder(layout);
    CellConstraints cc = new CellConstraints();
    mainPanelBuilder.setDefaultDialogBorder();

    ErrorsPanel errorsPanel = new ErrorsPanel(errors, "SaveErrors" + ":");

    mainPanelBuilder.add(errorsPanel,    cc.xy(1, 1));
    mainPanelBuilder.add(buttonPanelBuilder.getPanel(),   cc.xy(1, 3));
    contentPane.add(mainPanelBuilder.getPanel());
    pack();

    this.setLocationRelativeTo(null);


  }
}
