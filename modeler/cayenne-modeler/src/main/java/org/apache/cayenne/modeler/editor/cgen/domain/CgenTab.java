package org.apache.cayenne.modeler.editor.cgen.domain;

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

public class CgenTab extends JPanel {

    protected ProjectController projectController;
    private CgenTabController cgenTabController;

    private JCheckBox selectAll;
    private JButton generateAll;

    public CgenTab(ProjectController projectController, CgenTabController cgenTabController) {
        this.projectController = projectController;
        this.cgenTabController = cgenTabController;
        this.selectAll = new JCheckBox();
        generateAll = new JButton("Generate");
        generateAll.setEnabled(false);
        generateAll.setIcon(ModelerUtil.buildIcon("icon-gen_java.png"));
        generateAll.setPreferredSize(new Dimension(120, 30));
        generateAll.addActionListener(action -> cgenTabController.runGenerators(cgenTabController.getSelectedDataMaps()));
        setLayout(new BorderLayout());
    }

    public void initView() {
        removeAll();
        cgenTabController.createPanels();
        FormLayout layout = new FormLayout(
                "left:pref, 4dlu, 50dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        ConcurrentMap<DataMap, CgenPanel> panels = cgenTabController.getGeneratorsPanels();

        if(panels.isEmpty()) {
            this.add(new JLabel("There are no cgen configs."), BorderLayout.NORTH);
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

    void showSuccessMessage() {
        JOptionPane.showMessageDialog(
                this,
                "Class generation finished");
    }

    void showErrorMessage(String msg) {
        JOptionPane.showMessageDialog(
                this,
                "Error generating classes - " + msg);
    }

    void showEmptyMessage() {
        JOptionPane.showMessageDialog(
                this,
                "Nothing to generate - ");
    }

    public JCheckBox getSelectAll() {
        return selectAll;
    }

    public JButton getGenerateAll() {
        return generateAll;
    }
}
