package org.apache.cayenne.modeler.editor;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.ModelerUtil;

import javax.swing.*;
import java.awt.*;

public class TabPanel extends JPanel {

    private JCheckBox checkConfig;
    private JLabel dataMapLabel;
    private JButton toConfigButton;
    private DataMap dataMap;

    public TabPanel(DataMap dataMap, String icon) {
        setLayout(new BorderLayout());
        FormLayout layout = new FormLayout(
                "left:pref, 4dlu, fill:50dlu, 3dlu, fill:120", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        this.dataMap = dataMap;
        this.checkConfig = new JCheckBox();
        this.dataMapLabel = new JLabel(dataMap.getName());
        DataChannelMetaData metaData = Application.getInstance().getMetaData();
        this.toConfigButton = new JButton();
        if(metaData.get(dataMap, CgenConfiguration.class) != null) {
            this.toConfigButton.setText("Edit Config");
        } else {
            this.toConfigButton.setText("Create Config");
        }
        this.toConfigButton.setIcon(ModelerUtil.buildIcon(icon));

        builder.append(checkConfig, dataMapLabel, toConfigButton);
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    public JCheckBox getCheckConfig() {
        return checkConfig;
    }

    public JButton getToConfigButton() {
        return toConfigButton;
    }

    public JLabel getDataMapLabel() {
        return dataMapLabel;
    }

    public DataMap getDataMap() {
        return dataMap;
    }
}
