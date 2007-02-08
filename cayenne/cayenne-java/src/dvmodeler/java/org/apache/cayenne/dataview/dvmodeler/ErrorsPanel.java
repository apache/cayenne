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

import java.util.*;
import java.awt.*;
import javax.swing.*;

/**
 *
 * @author Nataliya Kholodna
 * @version 1.0
 */

class ErrorsPanel extends JPanel{
  public ErrorsPanel(java.util.List errors, String labelText){
    super();

    String htmlErrors = "<html><body><font size=-1 >";
    for (Iterator j = errors.iterator(); j.hasNext();){
      htmlErrors += (String)j.next();
    }
    htmlErrors += "</font></body></html>";

    JEditorPane editorPane = new JEditorPane();
    editorPane.setEditable(false);
    editorPane.setContentType("text/html");

    editorPane.setText(htmlErrors);

    JScrollPane scrollPane = new JScrollPane(editorPane);
    scrollPane.setPreferredSize(new Dimension(500, 280));
    scrollPane.setMinimumSize(new Dimension(5, 200));
    scrollPane.setAlignmentX(LEFT_ALIGNMENT);

    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    JLabel label = new JLabel(labelText);
    label.setLabelFor(editorPane);
    label.setAlignmentX(LEFT_ALIGNMENT);
    this.add(label);
    this.add(Box.createRigidArea(new Dimension(0,5)));
    this.add(scrollPane);
  }
}
