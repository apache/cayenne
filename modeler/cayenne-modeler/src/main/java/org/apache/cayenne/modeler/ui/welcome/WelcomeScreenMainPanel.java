package org.apache.cayenne.modeler.ui.welcome;

import javax.swing.*;
import java.awt.*;

class WelcomeScreenMainPanel extends JPanel {

    private static final Color TOP_GRADIENT = new Color(153, 153, 153);
    private static final Color BOTTOM_GRADIENT = new Color(230, 230, 230);

    public WelcomeScreenMainPanel() {
        super(new GridBagLayout());
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Paint paint = new GradientPaint(0, 0, TOP_GRADIENT, 0, getHeight(), BOTTOM_GRADIENT);
        g2.setPaint(paint);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}
