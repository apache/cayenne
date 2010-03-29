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
package org.apache.cayenne.modeler.editor.datanode;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.cayenne.conf.PasswordEncoding;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class PasswordEncoderView extends JPanel{
    
    protected JComboBox      passwordEncoder;
    protected JComboBox      passwordLocation;
    protected JTextField     passwordKey;
    protected JTextField     passwordSource;
    protected JLabel         passwordSourceLabel;
    
    private static final String PASSWORD_CLASSPATH  = "Classpath Search (File System)";
    private static final String PASSWORD_EXECUTABLE = "Executable Program";
    private static final String PASSWORD_MODEL      = "Cayenne Model";
    private static final String PASSWORD_URL        = "URL (file:, http:, etc)";
    
    private static final Object[] PASSWORD_LOCATIONS = new Object[] {
        DataSourceInfo.PASSWORD_LOCATION_MODEL,
        DataSourceInfo.PASSWORD_LOCATION_CLASSPATH,
        DataSourceInfo.PASSWORD_LOCATION_EXECUTABLE,
        DataSourceInfo.PASSWORD_LOCATION_URL };
    
    private static final Map<String, String> passwordSourceLabels = new TreeMap<String, String>();

    static
    {
      passwordSourceLabels.put(DataSourceInfo.PASSWORD_LOCATION_MODEL, PASSWORD_MODEL);
      passwordSourceLabels.put(DataSourceInfo.PASSWORD_LOCATION_CLASSPATH, PASSWORD_CLASSPATH);
      passwordSourceLabels.put(DataSourceInfo.PASSWORD_LOCATION_EXECUTABLE, PASSWORD_EXECUTABLE);
      passwordSourceLabels.put(DataSourceInfo.PASSWORD_LOCATION_URL, PASSWORD_URL);
    }
    
    final class PasswordLocationRenderer extends DefaultListCellRenderer
    {
      public Component getListCellRendererComponent(JList list,
                                                    Object object,
                                                    int arg2,
                                                    boolean arg3,
                                                    boolean arg4)
      {
        if (object != null)
          object = passwordSourceLabels.get(object);
        else
          object = PASSWORD_MODEL;

        return super.getListCellRendererComponent(list, object, arg2, arg3, arg4);
      }
    }
    
    public PasswordEncoderView(){
        
        this.passwordEncoder  = CayenneWidgetFactory.createUndoableComboBox();
        this.passwordLocation = CayenneWidgetFactory.createUndoableComboBox();
        this.passwordSource   = CayenneWidgetFactory.createUndoableTextField();
        this.passwordKey      = CayenneWidgetFactory.createUndoableTextField();
        

        
        // init combo box choices                                                                                                                                                                
        passwordEncoder.setModel(new DefaultComboBoxModel(PasswordEncoding.standardEncoders));
        passwordEncoder.setEditable(true);

        passwordLocation = CayenneWidgetFactory.createUndoableComboBox();
        passwordLocation.setRenderer(new PasswordLocationRenderer());

        DefaultComboBoxModel passwordLocationModel = new DefaultComboBoxModel(PASSWORD_LOCATIONS);

        passwordLocation.setModel(passwordLocationModel);

        
        CellConstraints cc = new CellConstraints();

        FormLayout layout =
            new FormLayout("right:80dlu, 3dlu, fill:50dlu, 3dlu, fill:74dlu, 3dlu, fill:70dlu", // Columns
                   "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p"); // Rows
        
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        
        builder.addSeparator("Encoder", cc.xywh(1, 1, 7, 1));
        
        builder.addLabel("Password Encoder:", cc.xy(1, 11));
        builder.add(passwordEncoder, cc.xywh(3, 11, 5, 1));

        builder.addLabel("Password Encoder Key:", cc.xy(1, 13));
        builder.add(passwordKey, cc.xywh(3, 13, 5, 1));

        builder.addLabel("Note: Cayenne supplied encoders do not use a key.", cc.xywh(3, 15, 5, 1));

        builder.addLabel("Password Location:", cc.xy(1, 17));
        builder.add(passwordLocation, cc.xywh(3, 17, 5, 1));

        passwordSourceLabel = builder.addLabel("Password Source:", cc.xy(1, 19));
        builder.add(passwordSource, cc.xywh(3, 19, 5, 1));


        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);

    }
    
    /**
     * @return the passwordEncoder
     */
    public JComboBox getPasswordEncoder()
    {
      return passwordEncoder;
    }

    /**
     * @return the passwordLocation
     */
    public JComboBox getPasswordLocation()
    {
      return passwordLocation;
    }

    /**
     * @return the passwordKey
     */
    public JTextField getPasswordKey()
    {
      return passwordKey;
    }

    /**
     * @return the passwordSource
     */
    public JTextField getPasswordSource()
    {
      return passwordSource;
    }

    /**
     * @return the passwordLocationLabel
     */
    public JLabel getPasswordSourceLabel()
    {
      return passwordSourceLabel;
    }

}
