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

package org.objectstyle.cayenne.modeler.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.objectstyle.cayenne.modeler.CayenneModelerFrame;
import org.objectstyle.cayenne.modeler.util.CayenneDialog;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.ModelerUtil;
import org.scopemvc.util.UIStrings;

/** 
 * Displays the Cayenne license and build information.
 */
// Implementation note - the data displayed here is 
// static and very simple, so there is no need to implement complex Scope MVC 
// triad, though it might be beneficial to use strings file
public class AboutDialog extends CayenneDialog {

    private static String licenseString;
    private static String infoString;
    private static ImageIcon logoImage;
    private static final Dimension infoAreaSize = new Dimension(450, 350);

    static synchronized ImageIcon getLogoImage() {
        if (logoImage == null) {
            logoImage = ModelerUtil.buildIcon("logo.jpg");
        }
        return logoImage;
    }

    /**
     * Builds and returns CayenneModeler info string.
     */
    static synchronized String getInfoString() {
        if (infoString == null) {
            StringBuffer buffer = new StringBuffer();

            buffer.append("<font size='-1' face='Arial,Helvetica'>");
            buffer.append(UIStrings.get("cayenne.modeler.about.info"));
            buffer.append("</font>");
            buffer.append("<font size='-2' face='Arial,Helvetica'>");

            String version = UIStrings.get("cayenne.version");
            if (version != null) {
                buffer.append("<br>Version: ").append(version);
            }

            String buildDate = UIStrings.get("cayenne.build.date");
            if (buildDate != null) {
                buffer.append(" (").append(buildDate).append(")");
            }

            buffer.append("</font>");
            infoString = buffer.toString();
        }

        return infoString;
    }

    /** 
     * Reads Cayenne license from cayenne.jar file and returns it as a string.
     */
    static synchronized String getLicenseString() {
        if (licenseString == null) {
            BufferedReader in = null;
            try {
                InputStream licenseIn =
                    AboutDialog.class.getClassLoader().getResourceAsStream(
                        "META-INF/LICENSE");

                if (licenseIn != null) {
                    in = new BufferedReader(new InputStreamReader(licenseIn));
                    String line = null;
                    StringBuffer buf = new StringBuffer();

                    while ((line = in.readLine()) != null) {
                        // strip comments
                        if (line.startsWith("/*") || line.startsWith(" */")) {
                            continue;
                        }

                        // strip separators 
                        if (line.indexOf("=================") >= 0) {
                            continue;
                        }

                        // strip beginning of the line
                        if (line.startsWith(" *")) {
                            line = line.substring(2);
                        }

                        buf.append(line).append('\n');
                    }

                    licenseString = buf.toString();
                }

            }
            catch (IOException ioex) {
                // ignoring
            }
            finally {
                if (in != null) {
                    try {
                        in.close();
                    }
                    catch (IOException ioex) {
                        // ignoring
                    }
                }
            }

            // if license is not initialized for whatever reason,
            // send them to the website
            if (licenseString == null) {
                licenseString =
                    "Latest Cayenne license can be found at http://objectstyle.org/cayenne/";
            }
        }

        return licenseString;
    }

    public AboutDialog(CayenneModelerFrame frame) {
        super(frame, "About CayenneModeler", true);
        init();

        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.pack();
        this.centerWindow();
        this.setVisible(true);
    }

    /** 
     * Sets up the graphical components. 
     */
    private void init() {

        // create widgets
        JButton okButton = CayenneWidgetFactory.createButton("Close");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AboutDialog.this.dispose();
            }
        });

        // assemble info section
        JTabbedPane tabPanel = new JTabbedPane() {
            public Dimension getMaximiumSize() {
                return infoAreaSize;
            }

            public Dimension getPreferredSize() {
                return infoAreaSize;
            }
        };

        tabPanel.setTabPlacement(JTabbedPane.TOP);
        tabPanel.addTab("About CayenneModeler", new JScrollPane(initInfoPanel()));
        tabPanel.addTab("License", new JScrollPane(initLicensePanel()));

        // assemble button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);

        // assemble dialog
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tabPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private JComponent initInfoPanel() {

        JLabel image = new JLabel(getLogoImage());
        image.setBounds(4, 4, 4, 4);

        JEditorPane infoPanel = new JEditorPane("text/html", getInfoString());
        infoPanel.setEditable(false);
        infoPanel.addHyperlinkListener(this);
        infoPanel.setBackground(getParent().getBackground());

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));

        panel.add(image, BorderLayout.NORTH);
        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    private JComponent initLicensePanel() {
        JTextArea licenseText = new JTextArea(getLicenseString());

        licenseText.setBackground(Color.WHITE);
        licenseText.setEditable(false);
        licenseText.setLineWrap(true);
        licenseText.setWrapStyleWord(true);

        return licenseText;
    }
}