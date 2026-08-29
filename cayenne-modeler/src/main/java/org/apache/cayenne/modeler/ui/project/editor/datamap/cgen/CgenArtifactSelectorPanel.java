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

package org.apache.cayenne.modeler.ui.project.editor.datamap.cgen;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Per-class artifact selector for the cgen editor.
 */
public class CgenArtifactSelectorPanel extends JPanel {

    private static final Icon ERROR_ICON = IconFactory.buildIcon("icon-error.png");

    private static final int ROW_HEIGHT = 22;
    private static final Border ROW_BORDER = BorderFactory.createEmptyBorder(0, 8, 0, 8);

    private final CgenPanel cgen;
    private final JCheckBox selectAll;
    private final JPanel classList;
    private final List<ClassRow> rows;

    private ValidationResult lastValidationResult;

    public CgenArtifactSelectorPanel(CgenPanel cgen) {
        this.cgen = cgen;
        this.rows = new ArrayList<>();
        this.selectAll = new JCheckBox("Select All");
        this.classList = new JPanel();

        initLayout();
        selectAll.addActionListener(e -> selectAllAction());
    }

    /**
     * Rebuilds the class list from the current cgen configuration and syncs the "Select All" state.
     */
    public void startup() {
        refresh();
        classSelectedAction();
    }

    /**
     * Rebuilds the class list from the current cgen configuration. Callers that want the validation
     * icons to be up to date must run {@link #validate(Collection)} first.
     */
    public void refresh() {
        classList.removeAll();
        rows.clear();

        Map<ClassType, List<Object>> groups = new EnumMap<>(ClassType.class);
        for (Object item : cgen.getClasses()) {
            groups.computeIfAbsent(ClassType.of(item), t -> new ArrayList<>()).add(item);
        }

        boolean first = true;
        for (List<Object> group : groups.values()) {
            if (!first) {
                classList.add(buildSeparator());
            }
            first = false;

            for (Object item : group) {
                ClassRow row = new ClassRow(item);
                rows.add(row);
                classList.add(row);
            }
        }

        classList.revalidate();
        classList.repaint();
    }

    private void initLayout() {
        Color listBackground = UIManager.getColor("List.background");

        classList.setLayout(new BoxLayout(classList, BoxLayout.Y_AXIS));
        classList.setBackground(listBackground);

        // keep the rows at their preferred height instead of stretching them over the viewport
        JPanel listHolder = new JPanel(new BorderLayout());
        listHolder.setBackground(listBackground);
        listHolder.add(classList, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(
                listHolder,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setBackground(listBackground);

        // set some minimal preferred size, so that it is smaller than other forms used in
        // the dialog... this way we get the right automated overall size
        scrollPane.setPreferredSize(new Dimension(300, 200));

        // white like the list below it, with a grey line on top, so that it reads as a part of
        // the list rather than of the grey toolbar above
        selectAll.setOpaque(false);
        JPanel selectAllRow = new JPanel(new BorderLayout());
        selectAllRow.setBackground(listBackground);
        selectAllRow.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        selectAllRow.add(selectAll, BorderLayout.WEST);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(listBackground);
        header.add(buildSeparator(), BorderLayout.NORTH);
        header.add(selectAllRow, BorderLayout.CENTER);
        header.add(buildSeparator(), BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Called whenever the selection of a single class changes, either from this panel or from the
     * outside.
     */
    void classSelectedAction() {
        int selectedCount = cgen.getSelectedEntitiesSize()
                + cgen.getSelectedEmbeddablesSize()
                + (cgen.isDataMapSelected() ? 1 : 0);
        selectAll.setSelected(selectedCount >= cgen.getClasses().size());
        cgen.updateGenerateButton();
        cgen.updateSelectedEntities();
        cgen.getStandardModeController().updateTemplateEditorButtons();
        repaint();
    }

    private void selectAllAction() {
        if (cgen.updateSelection(selectAll.isSelected() ? o -> true : o -> false)) {
            rows.forEach(ClassRow::syncSelection);
            cgen.updateSelectedEntities();
            cgen.updateGenerateButton();
            cgen.getStandardModeController().updateTemplateEditorButtons();
        }
    }

    public void validate(Collection<? extends ConfigurationNode> classes) {
        this.lastValidationResult = new CgenValidator().getValidationResult(classes);
    }

    /**
     * Returns the code generation problem detected for a class with the given name, or null if the
     * class is valid.
     */
    public ValidationFailure getProblem(String name) {
        if (lastValidationResult == null || name == null) {
            return null;
        }

        List<ValidationFailure> failures = lastValidationResult.getFailures(name);
        return failures.isEmpty() ? null : failures.get(0);
    }

    /**
     * Creates a thin grey line that does not stretch vertically under the list's {@link BoxLayout}.
     */
    private static JSeparator buildSeparator() {
        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, separator.getPreferredSize().height));
        return separator;
    }

    private static String nameOf(Object item) {
        if (item instanceof Embeddable emb) {
            return emb.getClassName();
        } else if (item instanceof ObjEntity oe) {
            return oe.getName();
        } else {
            return ((DataMap) item).getName();
        }
    }

    private static Icon iconOf(Object item) {
        if (item instanceof Embeddable) {
            return IconFactory.iconForObject(new Embeddable());
        } else if (item instanceof ObjEntity) {
            return IconFactory.iconForObject(new ObjEntity());
        } else {
            return IconFactory.iconForObject(new DataMap());
        }
    }

    /**
     * Kinds of classes shown in the list. Rows are grouped by type in the order declared here,
     * regardless of the order in which the cgen configuration hands them over.
     */
    private enum ClassType {

        DATA_MAP, OBJ_ENTITY, EMBEDDABLE;

        static ClassType of(Object item) {
            if (item instanceof ObjEntity) {
                return OBJ_ENTITY;
            } else if (item instanceof Embeddable) {
                return EMBEDDABLE;
            } else {
                return DATA_MAP;
            }
        }
    }

    /**
     * A single class in the list — a real checkbox, the class icon and name, and an optional
     * validation problem icon. Clicking anywhere in the row toggles the checkbox.
     */
    private class ClassRow extends JPanel {

        private final Object item;
        private final JCheckBox checkBox;

        ClassRow(Object item) {
            super(new BorderLayout());

            this.item = item;
            this.checkBox = new JCheckBox();
            this.checkBox.setOpaque(false);
            this.checkBox.setSelected(cgen.isSelected(item));
            this.checkBox.addActionListener(e -> selectionChangedAction());

            JLabel name = new JLabel(nameOf(item), iconOf(item), JLabel.LEADING);
            name.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

            setBackground(UIManager.getColor("List.background"));
            setBorder(ROW_BORDER);

            add(checkBox, BorderLayout.WEST);
            add(name, BorderLayout.CENTER);
            add(buildProblemLabel(), BorderLayout.EAST);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    checkBox.doClick();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            // keep the width computed from the row contents, so that a narrow panel scrolls
            // horizontally instead of clipping long class names
            return new Dimension(super.getPreferredSize().width, ROW_HEIGHT);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, ROW_HEIGHT);
        }

        void syncSelection() {
            checkBox.setSelected(cgen.isSelected(item));
        }

        private void selectionChangedAction() {
            cgen.setSelected(item, checkBox.isSelected());
            classSelectedAction();
        }

        private JLabel buildProblemLabel() {
            JLabel label = new JLabel();
            ValidationFailure problem = getProblem(nameOf(item));
            if (problem != null) {
                label.setIcon(ERROR_ICON);
                label.setToolTipText(problem.getDescription());
            }
            return label;
        }
    }
}
