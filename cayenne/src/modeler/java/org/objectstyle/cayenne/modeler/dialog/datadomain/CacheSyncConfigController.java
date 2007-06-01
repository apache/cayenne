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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataRowStore;
import org.objectstyle.cayenne.map.event.DomainEvent;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.scopemvc.controller.basic.BasicController;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;
import org.scopemvc.core.ModelChangeEvent;
import org.scopemvc.core.ModelChangeListener;
import org.scopemvc.core.Selector;
import org.scopemvc.view.swing.SPanel;

/**
 * A controller for CacheSyncConfigDialog and its subviews. Builds a model out of a
 * DataDomain properties map, and on save updates DataDomain properties with configuration
 * changes made by the user. This controller manages one main dialog view, and its
 * subviews organized using CardLayout. Each subview as well as the main dialog have their
 * own independent models.
 * 
 * @author Andrei Adamchik
 */
public class CacheSyncConfigController extends BasicController implements
        ModelChangeListener {

    private static Logger logObj = Logger.getLogger(CacheSyncConfigController.class);

    // using strings instead of the actioal factory classes, since we
    // JMS and JavaGroups libraries may not be around, and Modeler
    // may throw CNFE
    private static final String JGROUPS_FACTORY_CLASS = "org.objectstyle.cayenne.event.JavaGroupsBridgeFactory";
    private static final String JMS_FACTORY_CLASS = "org.objectstyle.cayenne.event.JMSBridgeFactory";

    public static final String SAVE_CONFIG_CONTROL = "cayenne.modeler.cacheSyncConfig.save.button";
    public static final String CANCEL_CONFIG_CONTROL = "cayenne.modeler.cacheSyncConfig.cancel.button";

    public static final String JGROUPS_DEFAULT_CONTROL = "cayenne.modeler.jgroupConfig.radio1";

    public static final String JGROUPS_URL_CONTROL = "cayenne.modeler.jgroupConfig.radio2";

    protected Map existingCards;
    protected boolean modified;
    protected ProjectController eventController;

    public CacheSyncConfigController(ProjectController eventController) {
        this.eventController = eventController;
    }

    /**
     * Creates and shows a new modal dialog window. Registers as a listener for its own
     * model to update subviews on model changes.
     */
    public void startup() {
        DataDomain domain = eventController.getCurrentDataDomain();
        String factory = (String) domain.getProperties().get(
                DataRowStore.EVENT_BRIDGE_FACTORY_PROPERTY);

        CacheSyncTypesModel topModel = buildTypesModel(factory);
        setModel(topModel);
        setView(new CacheSyncConfigDialog());

        // build cards, showing the one corresponding to DataDomain state
        prepareChildren(factory);
        super.startup();
    }

    /**
     * ModelChangeListener implementation that updates "modified" status and changes
     * dialog subview on model changes.
     */
    public void modelChanged(ModelChangeEvent inEvent) {
        logObj.info("ModelChangeEvent: " + inEvent.getSelector());

        Selector selector = inEvent.getSelector();

        if (selector.startsWith(CacheSyncTypesModel.FACTORY_LABEL_SELECTOR)) {
            changeConfigView();
            modified = true;
            logObj.info("** Factory selection modified..");
        }
        else {
            modified = true;
            logObj.info("** Property modified modified..");
        }
    }

    /**
     * Overrides super implementation to process controls from this controller's view and
     * its subviews.
     */
    protected void doHandleControl(Control control) throws ControlException {
        logObj.info("Control: " + control);

        if (control.matchesID(CANCEL_CONFIG_CONTROL)) {
            shutdown();
        }
        else if (control.matchesID(SAVE_CONFIG_CONTROL)) {
            commitChanges();
        }
        else if (control.matchesID(JGROUPS_DEFAULT_CONTROL)) {
            jgroupsDefaultConfig();
        }
        else if (control.matchesID(JGROUPS_URL_CONTROL)) {
            jgroupsURLConfig();
        }
    }

    protected void jgroupsDefaultConfig() {
        JGroupsConfigPanel view = (JGroupsConfigPanel) existingCards
                .get(CacheSyncTypesModel.JGROUPS_FACTORY_LABEL);
        if (view != null) {
            view.showDefaultConfig();
        }
    }

    protected void jgroupsURLConfig() {
        JGroupsConfigPanel view = (JGroupsConfigPanel) existingCards
                .get(CacheSyncTypesModel.JGROUPS_FACTORY_LABEL);
        if (view != null) {
            view.showCustomConfig();
        }
    }

    /**
     * Stores configuration changes in the data domain properties.
     */
    protected void commitChanges() {
        logObj.info("Has changes?: " + modified);

        if (modified) {
            // extract model from current card
            CacheSyncTypesModel topModel = (CacheSyncTypesModel) getModel();
            SPanel card = (SPanel) existingCards.get(topModel.getFactoryLabel());
            CacheSyncConfigModel model = (CacheSyncConfigModel) card.getShownModel();

            DataDomain domain = eventController.getCurrentDataDomain();

            logObj.warn("domain properties BEFORE: " + domain.getProperties());
            model.storeProperties(domain.getProperties());

            logObj.warn("domain properties: " + domain.getProperties());

            eventController.fireDomainEvent(new DomainEvent(this, domain));
        }

        shutdown();
    }

    /**
     * Changes a subview to a panel specific for the currently selected configuration
     * type.
     */
    protected void changeConfigView() {
        CacheSyncTypesModel topModel = (CacheSyncTypesModel) getModel();
        CacheSyncConfigModel newModel = buildModel(topModel);

        // NOTE: card doesn't have a controller, since it does not need it
        String label = topModel.getFactoryLabel();
        SPanel card = (SPanel) existingCards.get(label);
        card.setBoundModel(newModel);
        ((CacheSyncConfigDialog) getView()).showCard(label);
    }

    protected CacheSyncTypesModel buildTypesModel(String factory) {

        if (factory == null) {
            factory = DataRowStore.EVENT_BRIDGE_FACTORY_DEFAULT;
        }

        String label;

        if (JGROUPS_FACTORY_CLASS.equals(factory)) {
            label = CacheSyncTypesModel.JGROUPS_FACTORY_LABEL;
        }
        else if (JMS_FACTORY_CLASS.equals(factory)) {
            label = CacheSyncTypesModel.JMS_FACTORY_LABEL;
        }
        else {
            label = CacheSyncTypesModel.CUSTOM_FACTORY_LABEL;
        }

        CacheSyncTypesModel model = new CacheSyncTypesModel();
        model.setFactoryLabel(label);
        model.addModelChangeListener(this);
        return model;
    }

    protected CacheSyncConfigModel buildModel(CacheSyncTypesModel topModel) {
        String label = topModel.getFactoryLabel();
        String factory;

        if (label.equals(CacheSyncTypesModel.JGROUPS_FACTORY_LABEL)) {
            factory = JGROUPS_FACTORY_CLASS;
        }
        else if (label.equals(CacheSyncTypesModel.JMS_FACTORY_LABEL)) {
            factory = JMS_FACTORY_CLASS;
        }
        else {
            // reset factory
            factory = null;
        }

        return buildModel(factory);
    }

    protected CacheSyncConfigModel buildModel(String factory) {

        CacheSyncConfigModel model;

        if (JGROUPS_FACTORY_CLASS.equals(factory)) {
            model = new JGroupsConfigModel();
        }
        else if (JMS_FACTORY_CLASS.equals(factory)) {
            model = new JMSConfigModel();
        }
        else {
            model = new CacheSyncConfigModel();
        }

        model.setMap(new HashMap(eventController.getCurrentDataDomain().getProperties()));
        model.setFactoryClass(factory);
        model.addModelChangeListener(this);

        return model;
    }

    protected void prepareChildren(String factory) {
        existingCards = new HashMap();
        CacheSyncConfigDialog topView = (CacheSyncConfigDialog) getView();

        // note that none of the panels need a controller
        // if they issue controls, they will use this object taken from parent

        JGroupsConfigPanel jgroupsPanel = new JGroupsConfigPanel();
        existingCards.put(CacheSyncTypesModel.JGROUPS_FACTORY_LABEL, jgroupsPanel);
        topView.addCard(jgroupsPanel, CacheSyncTypesModel.JGROUPS_FACTORY_LABEL);

        JMSConfigPanel jmsPanel = new JMSConfigPanel();
        existingCards.put(CacheSyncTypesModel.JMS_FACTORY_LABEL, jmsPanel);
        topView.addCard(jmsPanel, CacheSyncTypesModel.JMS_FACTORY_LABEL);

        CustomRemoteEventsConfigPanel customFactoryPanel = new CustomRemoteEventsConfigPanel();
        existingCards.put(CacheSyncTypesModel.CUSTOM_FACTORY_LABEL, customFactoryPanel);
        topView.addCard(customFactoryPanel, CacheSyncTypesModel.CUSTOM_FACTORY_LABEL);

        if (factory == null) {
            factory = DataRowStore.EVENT_BRIDGE_FACTORY_DEFAULT;
        }

        // display the right initial card
        // can't call "changeConfigView", since it will reset custom factories..
        Object model = buildModel(factory);

        if (JGROUPS_FACTORY_CLASS.equals(factory)) {
            jgroupsPanel.setBoundModel(model);
            ((CacheSyncConfigDialog) getView())
                    .showCard(CacheSyncTypesModel.JGROUPS_FACTORY_LABEL);
        }
        else if (JMS_FACTORY_CLASS.equals(factory)) {
            jmsPanel.setBoundModel(model);
            ((CacheSyncConfigDialog) getView())
                    .showCard(CacheSyncTypesModel.JMS_FACTORY_LABEL);
        }
        else {
            customFactoryPanel.setBoundModel(model);
            ((CacheSyncConfigDialog) getView())
                    .showCard(CacheSyncTypesModel.CUSTOM_FACTORY_LABEL);
        }
    }
}