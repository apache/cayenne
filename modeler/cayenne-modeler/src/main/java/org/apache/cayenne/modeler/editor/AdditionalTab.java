package org.apache.cayenne.modeler.editor;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.ModelerUtil;

import javax.swing.*;
import java.awt.*;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;

public class AdditionalTab extends JPanel {

    protected ProjectController projectController;
    private AdditionalTabController additionalTabController;

    private JCheckBox selectAll;
    private JButton generateAll;

    public AdditionalTab(ProjectController projectController, AdditionalTabController additionalTabController, String icon) {
        this.projectController = projectController;
        this.additionalTabController = additionalTabController;
        this.selectAll = new JCheckBox();
        generateAll = new JButton("Run");
        generateAll.setEnabled(false);
        generateAll.setIcon(ModelerUtil.buildIcon(icon));
        generateAll.setPreferredSize(new Dimension(120, 30));
        generateAll.addActionListener(action -> additionalTabController.runGenerators(additionalTabController.getSelectedDataMaps()));
        setLayout(new BorderLayout());
    }

    public void initView() {
        removeAll();
        additionalTabController.createPanels();
        FormLayout layout = new FormLayout(
                "left:pref, 4dlu, 50dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        ConcurrentMap<DataMap, TabPanel> panels = additionalTabController.getGeneratorsPanels();

        if(panels.isEmpty()) {
            this.add(new JLabel("There are no configs."), BorderLayout.NORTH);
            return;
        }

        JPanel selectAllPanel = new JPanel(new FlowLayout());
        selectAllPanel.add(new JLabel("Select All"), FlowLayout.LEFT);
        selectAllPanel.add(selectAll, FlowLayout.CENTER);
        builder.append(selectAllPanel);
        builder.nextLine();

        SortedSet<DataMap> keys = new TreeSet<>(panels.keySet());
        for(DataMap dataMap : keys) {
            builder.append(panels.get(dataMap));
            builder.nextLine();
        }
        builder.append(generateAll);
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    public JCheckBox getSelectAll() {
        return selectAll;
    }

    public JButton getGenerateAll() {
        return generateAll;
    }
}
