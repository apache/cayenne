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
package org.apache.cayenne.modeler.ui.project.editor.datadomain;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.event.model.DataMapEvent;
import org.apache.cayenne.modeler.event.model.DataMapListener;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Base for the per-DataMap generator tabs of the DataDomain editor (Db Import,
 * Class Generation). Merges layout, selection state, and listener wiring that previously
 * lived in two parallel hierarchies. Subclasses implement {@link #runGenerators(Set)} and
 * {@link #showConfig(DataMap)}.
 */
public abstract class DataDomainGeneratorsTab<T> extends ProjectPanel implements DataMapListener {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DataDomainGeneratorsTab.class);

    private final ConcurrentMap<DataMap, DataDomainGeneratorsPanel> generatorsPanels;
    private final Set<DataMap> selectedDataMaps;
    private final Class<T> type;
    private final boolean selectAllByDefault;
    private final TopGeneratorPanel generationPanel;

    protected DataDomainGeneratorsTab(
            ProjectSession session,
            Class<T> type, boolean selectAllByDefault,
            String icon,
            String runTooltip) {

        super(session);

        this.type = type;
        this.selectAllByDefault = selectAllByDefault;
        this.generatorsPanels = new ConcurrentHashMap<>();
        this.selectedDataMaps = new HashSet<>();
        this.generationPanel = new TopGeneratorPanel(icon, runTooltip);

        setLayout(new BorderLayout());
        session.addDataMapListener(this);
        generationPanel.generateAll.addActionListener(action -> runGenerators(getSelectedDataMaps()));
    }

    public abstract void runGenerators(Set<DataMap> dataMaps);

    public abstract void showConfig(DataMap dataMap);

    public void initView() {
        removeAll();
        createPanels();

        FormLayout layout = new FormLayout("left:pref, $rgap", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        if (generatorsPanels.isEmpty()) {
            this.add(new JLabel("There are no datamaps."), BorderLayout.NORTH);
            return;
        }

        builder.append(generationPanel);
        builder.nextLine();
        SortedSet<DataMap> keys = new TreeSet<>(generatorsPanels.keySet());
        for (DataMap dataMap : keys) {
            builder.append(generatorsPanels.get(dataMap));
            builder.nextLine();
        }
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    public void showEmptyMessage() {
        JOptionPane.showMessageDialog(this, "Nothing to generate");
    }

    private void createPanels() {
        Collection<DataMap> dataMaps = getDataMaps();
        refreshSelectedMaps(dataMaps);
        generatorsPanels.clear();
        for (DataMap dataMap : dataMaps) {
            DataDomainGeneratorsPanel generatorPanel = new DataDomainGeneratorsPanel(
                    app, dataMap, "icon-datamap.png", type);
            initListenersForPanel(generatorPanel);
            generatorsPanels.put(dataMap, generatorPanel);
        }
        selectedDataMaps.forEach(dataMap -> {
            DataDomainGeneratorsPanel currPanel = generatorsPanels.get(dataMap);
            if (currPanel != null) {
                currPanel.getCheckConfig().setSelected(true);
            }
        });
        if (selectedDataMaps.isEmpty() && selectAllByDefault) {
            generationPanel.selectAll.setSelected(true);
            generationPanel.generateAll.setEnabled(true);
            for (Map.Entry<DataMap, DataDomainGeneratorsPanel> entry : generatorsPanels.entrySet()) {
                entry.getValue().getCheckConfig().setSelected(true);
            }
        }
    }

    private void initListenersForPanel(DataDomainGeneratorsPanel panel) {
        panel.getCheckConfig().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectedDataMaps.add(panel.getDataMap());
                if (selectedDataMaps.size() == generatorsPanels.size()) {
                    generationPanel.selectAll.setSelected(true);
                }
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                selectedDataMaps.remove(panel.getDataMap());
                generationPanel.selectAll.setSelected(false);
            }
            updateGenerateButton();
        });

        panel.getToConfigButton().addActionListener(action -> showConfig(panel.getDataMap()));

        generationPanel.selectAll.addActionListener(e -> {
            boolean isSelected = generationPanel.selectAll.isSelected();
            generatorsPanels.forEach((key, value) -> {
                if (value.getCheckConfig().isEnabled()) {
                    value.getCheckConfig().setSelected(isSelected);
                }
            });
            updateGenerateButton();
        });
    }

    private void updateGenerateButton() {
        generationPanel.generateAll.setEnabled(!selectedDataMaps.isEmpty());
    }

    private Collection<DataMap> getDataMaps() {
        Project project = session.project();
        return ((DataChannelDescriptor) project.getRootNode()).getDataMaps();
    }

    private void refreshSelectedMaps(Collection<DataMap> dataMaps) {
        selectedDataMaps.removeIf(dataMap -> !dataMaps.contains(dataMap));
    }

    protected Set<DataMap> getSelectedDataMaps() {
        return selectedDataMaps;
    }

    public ProjectSession getController() {
        return session;
    }

    @Override
    public void dataMapAdded(DataMapEvent e) {
        DataDomainGeneratorsPanel generatorPanel = new DataDomainGeneratorsPanel(
                app, e.getDataMap(), "icon-datamap.png", type);
        initListenersForPanel(generatorPanel);
        generatorsPanels.put(e.getDataMap(), generatorPanel);
        if (generationPanel.selectAll.isSelected()) {
            generatorPanel.getCheckConfig().setSelected(true);
            selectedDataMaps.add(e.getDataMap());
        }
    }

    @Override
    public void dataMapRemoved(DataMapEvent e) {
        selectedDataMaps.remove(e.getDataMap());
        generatorsPanels.remove(e.getDataMap());
    }

    @Override
    public void dataMapChanged(DataMapEvent e) {
    }

    private static class TopGeneratorPanel extends JPanel {

        final JCheckBox selectAll;
        final JButton generateAll;

        TopGeneratorPanel(String icon, String runTooltip) {
            setLayout(new BorderLayout());
            FormLayout layout = new FormLayout(
                    "left:pref, $rgap, fill:70dlu, $lcgap, fill:120, $lcgap, fill:120", "");
            DefaultFormBuilder builder = new DefaultFormBuilder(layout);
            this.selectAll = new JCheckBox();
            this.generateAll = new JButton("Run");
            this.generateAll.setEnabled(false);
            this.generateAll.setIcon(IconFactory.buildIcon(icon));
            this.generateAll.setToolTipText(runTooltip);
            builder.append(selectAll, new JLabel("Select All"), generateAll);
            this.add(builder.getPanel(), BorderLayout.CENTER);
        }
    }

}
