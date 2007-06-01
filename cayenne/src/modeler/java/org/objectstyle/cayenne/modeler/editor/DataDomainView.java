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

package org.objectstyle.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataRowStore;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.map.event.DomainEvent;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.dialog.datadomain.CacheSyncConfigController;
import org.objectstyle.cayenne.modeler.event.DomainDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DomainDisplayListener;
import org.objectstyle.cayenne.modeler.util.ProjectUtil;
import org.objectstyle.cayenne.modeler.util.TextAdapter;
import org.objectstyle.cayenne.pref.Domain;
import org.objectstyle.cayenne.project.ApplicationProject;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.validation.ValidationException;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Panel for editing DataDomain.
 */
public class DataDomainView extends JPanel implements DomainDisplayListener {

    protected ProjectController projectController;

    protected TextAdapter name;
    protected TextAdapter cacheSize;
    protected JCheckBox objectValidation;
    protected JCheckBox externalTransactions;
    protected JCheckBox sharedCache;
    protected JCheckBox remoteUpdates;
    protected JButton configRemoteUpdates;

    public DataDomainView(ProjectController projectController) {
        this.projectController = projectController;

        // Create and layout components
        initView();

        // hook up listeners to widgets
        initController();
    }

    protected void initView() {

        // create widgets
        this.name = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setDomainName(text);
            }
        };

        this.cacheSize = new TextAdapter(new JTextField(10)) {

            protected void updateModel(String text) {
                setCacheSize(text);
            }
        };

        this.objectValidation = new JCheckBox();
        this.externalTransactions = new JCheckBox();

        this.sharedCache = new JCheckBox();
        this.remoteUpdates = new JCheckBox();
        this.configRemoteUpdates = new JButton("Configure");
        configRemoteUpdates.setEnabled(false);

        // assemble

        FormLayout layout = new FormLayout(
                "right:max(50dlu;pref), 3dlu, left:max(20dlu;pref), 3dlu, left:150",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("DataDomain Configuration");
        builder.append("DataDomain Name:", name.getComponent(), 3);
        builder.append("Child DataContexts Validate Objects:", objectValidation, 3);
        builder.append("Container-Managed Transactions:", externalTransactions, 3);

        builder.appendSeparator("Cache Configuration");
        builder.append("Max. Number of Objects:", cacheSize.getComponent(), 3);
        builder.append("Use Shared Cache:", sharedCache, 3);
        builder
                .append(
                        "Remote Change Notifications:",
                        remoteUpdates,
                        configRemoteUpdates);

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    protected void initController() {
        projectController.addDomainDisplayListener(this);

        // add action listener to checkboxes
        objectValidation.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String value = objectValidation.isSelected() ? "true" : "false";
                setDomainProperty(
                        DataDomain.VALIDATING_OBJECTS_ON_COMMIT_PROPERTY,
                        value,
                        Boolean.toString(DataDomain.VALIDATING_OBJECTS_ON_COMMIT_DEFAULT));
            }
        });

        externalTransactions.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String value = externalTransactions.isSelected() ? "true" : "false";
                setDomainProperty(
                        DataDomain.USING_EXTERNAL_TRANSACTIONS_PROPERTY,
                        value,
                        Boolean.toString(DataDomain.USING_EXTERNAL_TRANSACTIONS_DEFAULT));
            }
        });

        sharedCache.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String value = sharedCache.isSelected() ? "true" : "false";
                setDomainProperty(
                        DataDomain.SHARED_CACHE_ENABLED_PROPERTY,
                        value,
                        Boolean.toString(DataDomain.SHARED_CACHE_ENABLED_DEFAULT));

                // turning off shared cache should result in disabling remote events

                remoteUpdates.setEnabled(sharedCache.isSelected());

                if (!sharedCache.isSelected()) {
                    // uncheck remote updates...
                    remoteUpdates.setSelected(false);
                }

                // depending on final remote updates status change button status
                configRemoteUpdates.setEnabled(remoteUpdates.isSelected());
            }
        });

        remoteUpdates.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String value = remoteUpdates.isSelected() ? "true" : "false";

                // update config button state
                configRemoteUpdates.setEnabled(remoteUpdates.isSelected());

                setDomainProperty(
                        DataRowStore.REMOTE_NOTIFICATION_PROPERTY,
                        value,
                        Boolean.toString(DataRowStore.REMOTE_NOTIFICATION_DEFAULT));
            }
        });

        configRemoteUpdates.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new CacheSyncConfigController(projectController).startup();
            }
        });
    }

    /**
     * Helper method that updates domain properties. If a value equals to default, null
     * value is used instead.
     */
    protected void setDomainProperty(String property, String value, String defaultValue) {

        DataDomain domain = projectController.getCurrentDataDomain();
        if (domain == null) {
            return;
        }

        // no empty strings
        if ("".equals(value)) {
            value = null;
        }

        // use NULL for defaults
        if (value != null && value.equals(defaultValue)) {
            value = null;
        }

        Map properties = domain.getProperties();
        Object oldValue = properties.get(property);
        if (!Util.nullSafeEquals(value, oldValue)) {
            properties.put(property, value);

            DomainEvent e = new DomainEvent(this, domain);
            projectController.fireDomainEvent(e);
        }
    }

    public String getDomainProperty(String property, String defaultValue) {
        DataDomain domain = projectController.getCurrentDataDomain();
        if (domain == null) {
            return null;
        }

        String value = (String) domain.getProperties().get(property);
        return value != null ? value : defaultValue;
    }

    public boolean getDomainBooleanProperty(String property, String defaultValue) {
        return "true".equalsIgnoreCase(getDomainProperty(property, defaultValue));
    }

    /**
     * Invoked on domain selection event. Updates view with the values from the currently
     * selected domain.
     */
    public void currentDomainChanged(DomainDisplayEvent e) {
        DataDomain domain = e.getDomain();
        if (null == domain) {
            return;
        }

        // extract values from the new domain object
        name.setText(domain.getName());

        cacheSize.setText(getDomainProperty(
                DataRowStore.SNAPSHOT_CACHE_SIZE_PROPERTY,
                Integer.toString(DataRowStore.SNAPSHOT_CACHE_SIZE_DEFAULT)));

        objectValidation.setSelected(getDomainBooleanProperty(
                DataDomain.VALIDATING_OBJECTS_ON_COMMIT_PROPERTY,
                Boolean.toString(DataDomain.VALIDATING_OBJECTS_ON_COMMIT_DEFAULT)));

        externalTransactions.setSelected(getDomainBooleanProperty(
                DataDomain.USING_EXTERNAL_TRANSACTIONS_PROPERTY,
                Boolean.toString(DataDomain.USING_EXTERNAL_TRANSACTIONS_DEFAULT)));

        sharedCache.setSelected(getDomainBooleanProperty(
                DataDomain.SHARED_CACHE_ENABLED_PROPERTY,
                Boolean.toString(DataDomain.SHARED_CACHE_ENABLED_DEFAULT)));

        remoteUpdates.setSelected(getDomainBooleanProperty(
                DataRowStore.REMOTE_NOTIFICATION_PROPERTY,
                Boolean.toString(DataRowStore.REMOTE_NOTIFICATION_DEFAULT)));
        remoteUpdates.setEnabled(sharedCache.isSelected());
        configRemoteUpdates.setEnabled(remoteUpdates.isEnabled()
                && remoteUpdates.isSelected());
    }

    void setDomainName(String newName) {
        if (newName == null || newName.trim().length() == 0) {
            throw new ValidationException("Enter name for DataDomain");
        }

        Configuration configuration = ((ApplicationProject) Application.getProject())
                .getConfiguration();
        DataDomain domain = projectController.getCurrentDataDomain();

        DataDomain matchingDomain = configuration.getDomain(newName);

        if (matchingDomain == null) {
            Domain prefs = projectController.getPreferenceDomainForDataDomain();

            DomainEvent e = new DomainEvent(this, domain, domain.getName());
            ProjectUtil.setDataDomainName(configuration, domain, newName);
            prefs.rename(newName);
            projectController.fireDomainEvent(e);
        }
        else if (matchingDomain != domain) {
            throw new ValidationException("There is another DataDomain named '"
                    + newName
                    + "'. Use a different name.");
        }
    }

    void setCacheSize(String text) {
        if (text.length() > 0) {
            try {
                Integer.parseInt(text);
            }
            catch (NumberFormatException ex) {
                throw new ValidationException("Cache size must be an integer: " + text);
            }
        }

        setDomainProperty(DataRowStore.SNAPSHOT_CACHE_SIZE_PROPERTY, text, Integer
                .toString(DataRowStore.SNAPSHOT_CACHE_SIZE_DEFAULT));
    }
}