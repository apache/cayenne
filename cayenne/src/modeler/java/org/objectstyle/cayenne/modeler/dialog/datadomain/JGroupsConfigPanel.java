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
package org.objectstyle.cayenne.modeler.dialog.datadomain;

import java.awt.BorderLayout;

import javax.swing.ButtonGroup;

import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.SRadioButton;
import org.scopemvc.view.swing.STextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Andrei Adamchik
 */
public class JGroupsConfigPanel extends SPanel {

    protected STextField multicastAddress;
    protected STextField multicastPort;
    protected STextField configURL;

    public JGroupsConfigPanel() {
        initView();
    }

    protected void initView() {
        setLayout(new BorderLayout());

        SRadioButton useDefaultConfig =
            new SRadioButton(
                CacheSyncConfigController.JGROUPS_DEFAULT_CONTROL,
                JGroupsConfigModel.USING_DEFAULT_CONFIG_SELECTOR);

        SRadioButton useConfigFile =
            new SRadioButton(
                CacheSyncConfigController.JGROUPS_URL_CONTROL,
                JGroupsConfigModel.USING_CONFIG_FILE_SELECTOR);

        ButtonGroup group = new ButtonGroup();
        group.add(useConfigFile);
        group.add(useDefaultConfig);

        multicastAddress = new STextField();
        multicastAddress.setSelector(JGroupsConfigModel.MCAST_ADDRESS_SELECTOR);

        multicastPort = new STextField(5);
        multicastPort.setSelector(JGroupsConfigModel.MCAST_PORT_SELECTOR);

        configURL = new STextField();
        configURL.setSelector(JGroupsConfigModel.JGROUPS_CONFIG_URL_SELECTOR);

        // type form
        FormLayout layout = new FormLayout("right:150, 3dlu, left:200", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.appendSeparator("JavaGroups Settings");

        builder.append(useDefaultConfig);
        builder.nextLine();

        // "1" at the end would enforce spanning the text field to
        // the full width
        builder.append("Multicast Address:", multicastAddress, 1);
        builder.append("Multicast Port:", multicastPort);

        builder.nextLine();
        builder.append(useConfigFile);
        builder.nextLine();
        builder.append("JGroups Config File:", configURL, 1);

        add(builder.getPanel(), BorderLayout.NORTH);
    }

    public void showDefaultConfig() {
        multicastAddress.setEditable(true);
        multicastPort.setEditable(true);
        configURL.setEditable(false);
    }

    public void showCustomConfig() {
        multicastAddress.setEditable(false);
        multicastPort.setEditable(false);
        configURL.setEditable(true);
    }
}