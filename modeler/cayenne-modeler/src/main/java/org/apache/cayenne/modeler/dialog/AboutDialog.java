/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.dialog;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.LocalDate;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.util.LocalizedStringsHandler;
import org.apache.cayenne.util.Util;

/**
 * Displays the Cayenne license and build information.
 */
// Implementation note - the data displayed here is
// static and very simple, so there is no need to implement complex Scope MVC
// triad, though it might be beneficial to use strings file
public class AboutDialog extends JFrame implements FocusListener, KeyListener, MouseListener {

    private static String infoString;
    private static ImageIcon logoImage;

    static ImageIcon getLogoImage() {
        if (logoImage == null) {
            logoImage = ModelerUtil.buildIcon("logo.jpg");
        }
        return logoImage;
    }

    /**
     * Builds and returns CayenneModeler info string.
     */
    static String getInfoString() {
        if (infoString == null) {

            double maxMemory = (double) Runtime.getRuntime().maxMemory() / 1024 / 1024;
            double totalMemory = (double) Runtime.getRuntime().totalMemory() / 1024 / 1024;
            double freeMemory = (double) Runtime.getRuntime().freeMemory() / 1024 / 1024;

            StringBuilder buffer = new StringBuilder();
            buffer.append("<html>");
            buffer.append("<font size='-1' face='Arial,Helvetica'>");
            buffer.append(String.format(ModelerUtil.getProperty("cayenne.modeler.about.info"), LocalDate.now().getYear()));
            buffer.append("</font>");

            buffer.append("<font size='-2' face='Arial,Helvetica'>");
            buffer.append("<br>JVM: ").append(System.getProperty("java.vm.name")).append(" ").append(System.getProperty("java.version"));
            buffer.append(String.format("<br>Memory: used %.2f MB, max %.2f MB", totalMemory - freeMemory, maxMemory));

            String version = LocalizedStringsHandler.getString("cayenne.version");
            buffer.append("<br>Version: ").append(version);

            String buildDate = LocalizedStringsHandler.getString("cayenne.build.date");
            if (!Util.isEmptyString(buildDate)) {
                buffer.append(" (").append(buildDate).append(")");
            }

            buffer.append("</font>");
            buffer.append("</html>");
            infoString = buffer.toString();
        }

        return infoString;
    }

    public AboutDialog() {
        super();
        final FlowLayout flowLayout = new FlowLayout();
        getContentPane().setLayout(flowLayout);
        getContentPane().setBackground(Color.WHITE);
        this.setUndecorated(true);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        addMouseListener(this);
        addFocusListener(this);
        addKeyListener(this);
        setLocationRelativeTo(null); // centre on screen

        final JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        getContentPane().add(panel);

        JLabel image = new JLabel(getLogoImage());
        panel.add(image, new GridBagConstraints());

        JLabel license = new JLabel();
        final GridBagConstraints gridBagConstraints_1 = new GridBagConstraints();
        gridBagConstraints_1.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints_1.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints_1.gridx = 0;
        gridBagConstraints_1.gridy = 1;
        gridBagConstraints_1.insets = new Insets(0, 12, 0, 0);
        panel.add(license, gridBagConstraints_1);
        license.setText("<html><font size='-1' face='Arial,Helvetica'>Available under the Apache license.</font></html>");

        JLabel info = new JLabel();
        final GridBagConstraints gridBagConstraints_2 = new GridBagConstraints();
        gridBagConstraints_2.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints_2.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints_2.gridx = 0;
        gridBagConstraints_2.gridy = 2;
        gridBagConstraints_2.insets = new Insets(6, 12, 12, 12);
        panel.add(info, gridBagConstraints_2);
        info.setText(getInfoString());

        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    public void keyPressed(KeyEvent e) {
        dispose();
    }

    public void focusLost(FocusEvent e) {
        dispose();
    }

    public void focusGained(FocusEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
        dispose();
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
}
