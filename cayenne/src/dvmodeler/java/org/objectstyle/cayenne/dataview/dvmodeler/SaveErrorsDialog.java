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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


import com.jgoodies.forms.builder.ButtonBarBuilder;

/**
 *
 * @author Nataliya Kholodna
 * @version 1.0
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
