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
package org.objectstyle.cayenne.modeler.editor.datanode;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Andrei Adamchik
 */
public class JDBCDataSourceView extends JPanel {

    protected JTextField driver;
    protected JTextField url;
    protected JTextField userName;
    protected JPasswordField password;
    protected JTextField minConnections;
    protected JTextField maxConnections;
    protected JButton syncWithLocal;

    public JDBCDataSourceView() {

        driver = new JTextField();
        url = new JTextField();
        userName = new JTextField();
        password = new JPasswordField();
        minConnections = new JTextField(6);
        maxConnections = new JTextField(6);
        syncWithLocal = new JButton("Sync with Local");
        syncWithLocal.setToolTipText("Update from local DataSource");

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:80dlu, 3dlu, fill:50dlu, 3dlu, fill:74dlu, 3dlu, fill:70dlu",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("JDBC Configuration", cc.xywh(1, 1, 7, 1));
        builder.addLabel("JDBC Driver:", cc.xy(1, 3));
        builder.add(driver, cc.xywh(3, 3, 5, 1));
        builder.addLabel("DB URL:", cc.xy(1, 5));
        builder.add(url, cc.xywh(3, 5, 5, 1));
        builder.addLabel("User Name:", cc.xy(1, 7));
        builder.add(userName, cc.xywh(3, 7, 5, 1));
        builder.addLabel("Password:", cc.xy(1, 9));
        builder.add(password, cc.xywh(3, 9, 5, 1));
        builder.addLabel("Min Connections:", cc.xy(1, 11));
        builder.add(minConnections, cc.xy(3, 11));
        builder.addLabel("Max Connections:", cc.xy(1, 13));
        builder.add(maxConnections, cc.xy(3, 13));
        builder.add(syncWithLocal, cc.xy(7, 13));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    public JTextField getDriver() {
        return driver;
    }

    public JPasswordField getPassword() {
        return password;
    }

    public JTextField getUrl() {
        return url;
    }

    public JTextField getUserName() {
        return userName;
    }

    public JTextField getMaxConnections() {
        return maxConnections;
    }

    public JTextField getMinConnections() {
        return minConnections;
    }

    public JButton getSyncWithLocal() {
        return syncWithLocal;
    }
}