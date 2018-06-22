package org.apache.cayenne.modeler.dialog.codegen.cgen;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.swing.components.TopBorder;

import javax.swing.*;
import java.awt.*;

public class CgenDialog extends JDialog {

    protected JPanel panel;
    protected JButton cancelButton;

    public CgenDialog(Component generatorPanel) {
        super(Application.getFrame());

        this.panel = new JPanel();
        this.panel.setFocusable(false);

        this.cancelButton = new JButton("Cancel");
        JScrollPane scrollPane = new JScrollPane(
                generatorPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(900, 550));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(TopBorder.create());
        buttons.add(Box.createHorizontalStrut(50));
        buttons.add(cancelButton);

        panel.add(scrollPane);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(panel, BorderLayout.CENTER);
        contentPane.add(buttons, BorderLayout.SOUTH);

        setTitle("Cgen Global Config");
    }

    public JButton getCancelButton() {
        return cancelButton;
    }
}
