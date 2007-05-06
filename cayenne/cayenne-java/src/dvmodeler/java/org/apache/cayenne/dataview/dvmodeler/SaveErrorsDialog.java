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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 *
 * @author Nataliya Kholodna
 */
class SaveErrorsDialog extends JDialog{
  public static String CLOSE_DIALOG = "CLOSE";
  public static String SAVE_DIALOG = "SAVE";
  public static String EXIT_DIALOG = "EXIT";

  public static int SAVE_ANYWAY = 0;
  public static int CLOSE_WITHOUT_SAVING = 1;
  public static int EXIT_WITHOUT_SAVING = 2;
  public static int CANCEL = 3;

  private int selectedValue = -1;


  public static int showSaveErrorsDialog(Frame frame, java.util.List errors, String dialogType){
    SaveErrorsDialog saveErrorsDialog = new SaveErrorsDialog(frame, errors, dialogType);
    saveErrorsDialog.setVisible(true);
    return saveErrorsDialog.getSelectedValue();
  }

  private SaveErrorsDialog(Frame frame, java.util.List errors, String dialogType){
    super(frame, "DVModeler :: " + "SaveErrors", true);

    this.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        selectedValue = CANCEL;
        setVisible(false);
      }
    });

    ButtonBarBuilder builder = new ButtonBarBuilder();
    builder.addGlue();

    //buttons

    JButton saveAnywayButton = new JButton("Save Anyway");
    saveAnywayButton.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        selectedValue = SAVE_ANYWAY;
        setVisible(false);
      }
    });

    builder.addGridded(saveAnywayButton);


    JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e){
          selectedValue = CANCEL;
          setVisible(false);
        }
      });


     getRootPane().setDefaultButton(saveAnywayButton);

    if (dialogType.equals(SaveErrorsDialog.EXIT_DIALOG)){

      JButton exitWithoutSavingButton = new JButton("Exit Whithout Saving");
      exitWithoutSavingButton.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e){
          selectedValue = EXIT_WITHOUT_SAVING;
          setVisible(false);
        }
      });

      builder.addRelatedGap();
      builder.addGridded(exitWithoutSavingButton);
      builder.addRelatedGap();
      builder.addGridded(cancelButton);

    } else if (dialogType.equals(SaveErrorsDialog.CLOSE_DIALOG)){

      JButton closeWithoutSavingButton = new JButton("Close Whithout Saving");
      closeWithoutSavingButton.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e){
          selectedValue = CLOSE_WITHOUT_SAVING;
          setVisible(false);
        }
      });

      builder.addRelatedGap();
      builder.addGridded(closeWithoutSavingButton);
      builder.addRelatedGap();
      builder.addGridded(cancelButton);

    } else if (dialogType.equals(SaveErrorsDialog.SAVE_DIALOG)){

      builder.addRelatedGap();
      builder.addGridded(cancelButton);
    }

    JPanel buttonPane = new JPanel();

    buttonPane = builder.getPanel();


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
    mainPanelBuilder.add(buttonPane,   cc.xy(1, 3));
    contentPane.add(mainPanelBuilder.getPanel());
    pack();

    this.setLocationRelativeTo(null);
  }
  private int getSelectedValue(){
    return selectedValue;
  }
}
