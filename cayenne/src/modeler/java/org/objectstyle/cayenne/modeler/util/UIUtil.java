package org.objectstyle.cayenne.modeler.util;

import java.awt.Rectangle;

import javax.swing.JTable;
import javax.swing.JViewport;

/**
 * @author Andrei Adamchik
 * @since 1.1
 */
public class UIUtil {

    /**
     * Scrolls table within JViewport to the selected row if there is one.
     */
    public static void scrollToSelectedRow(JTable table) {
        int row = table.getSelectedRow();
        if (row >= 0) {
            scroll(table, row, 0);
        }
    }

    /**
    * Scrolls view if it is located in a JViewport, so that the specified cell
    * is displayed in the center.
    */
    public static void scroll(JTable table, int rowIndex, int vColIndex) {
        if (!(table.getParent() instanceof JViewport)) {
            return;
        }

        JViewport viewport = (JViewport) table.getParent();
        Rectangle rect = table.getCellRect(rowIndex, vColIndex, true);
        Rectangle viewRect = viewport.getViewRect();

        if (viewRect.intersects(rect)) {
            return;
        }

        // Translate the cell location so that it is relative
        // to the view, assuming the northwest corner of the
        // view is (0,0).
        rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);

        // Calculate location of rect if it were at the center of view
        int centerX = (viewRect.width - rect.width) / 2;
        int centerY = (viewRect.height - rect.height) / 2;

        // Fake the location of the cell so that scrollRectToVisible
        // will move the cell to the center
        if (rect.x < centerX) {
            centerX = -centerX;
        }
        if (rect.y < centerY) {
            centerY = -centerY;
        }
        rect.translate(centerX, centerY);

        // Scroll the area into view.
        viewport.scrollRectToVisible(rect);
    }
}
