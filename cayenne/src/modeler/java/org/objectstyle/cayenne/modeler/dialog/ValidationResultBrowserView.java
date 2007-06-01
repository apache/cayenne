package org.objectstyle.cayenne.modeler.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Andrei Adamchik
 */
public class ValidationResultBrowserView extends JDialog {

    protected JTextArea messageLabel;
    protected JTextArea errorsDisplay;
    protected JButton closeButton;

    public ValidationResultBrowserView() {
        this.closeButton = new JButton("Close");

        this.messageLabel = new JTextArea();
        messageLabel.setEditable(false);
        messageLabel.setLineWrap(true);
        messageLabel.setWrapStyleWord(true);

        this.errorsDisplay = new JTextArea();
        errorsDisplay.setEditable(false);
        errorsDisplay.setLineWrap(true);
        errorsDisplay.setWrapStyleWord(true);

        // assemble
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:min(50dlu;pref):grow",
                "fill:20dlu, 9dlu, p, 3dlu, fill:40dlu:grow"));
        builder.setDefaultDialogBorder();
        builder.add(new JScrollPane(
                messageLabel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), cc.xy(1, 1));
        builder.addSeparator("Details", cc.xy(1, 3));
        builder.add(new JScrollPane(
                errorsDisplay,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), cc.xy(1, 5));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(closeButton);

        JComponent container = (JComponent) getContentPane();
        container.setLayout(new BorderLayout());
        container.add(builder.getPanel(), BorderLayout.CENTER);
        container.add(buttons, BorderLayout.SOUTH);

        // update top label bg
        messageLabel.setBackground(container.getBackground());

        // we need the right preferred size so that dialog "pack()" produces decent
        // default size...
        container.setPreferredSize(new Dimension(450, 270));
    }

    public JButton getCloseButton() {
        return closeButton;
    }

    public JTextArea getErrorsDisplay() {
        return errorsDisplay;
    }

    public JTextArea getMessageLabel() {
        return messageLabel;
    }
}