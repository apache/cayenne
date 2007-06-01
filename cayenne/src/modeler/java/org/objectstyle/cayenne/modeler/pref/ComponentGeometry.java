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
package org.objectstyle.cayenne.modeler.pref;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.apache.commons.beanutils.PropertyUtils;
import org.objectstyle.cayenne.pref.Domain;
import org.objectstyle.cayenne.pref.PreferenceException;

public class ComponentGeometry extends _ComponentGeometry {

    public static final String GEOMETRY_PREF_KEY = "geometry";

    public static ComponentGeometry getPreference(Domain domain) {
        return (ComponentGeometry) domain.getDetail(
                ComponentGeometry.GEOMETRY_PREF_KEY,
                ComponentGeometry.class,
                true);
    }

    /**
     * Binds this preference object to synchronize its state with a given component,
     * allowing to specify an initial offset compared to the stored position.
     */
    public void bind(
            final JFrame frame,
            int initialWidth,
            int initialHeight,
            int maxOffset) {

        updateSize(frame, initialWidth, initialHeight);
        updateLocation(frame, maxOffset);

        frame.addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent e) {
                setWidth(new Integer(frame.getWidth()));
                setHeight(new Integer(frame.getHeight()));
            }

            public void componentMoved(ComponentEvent e) {
                setX(new Integer(frame.getX()));
                setY(new Integer(frame.getY()));
            }
        });
    }

    /**
     * Binds this preference object to synchronize its state with a given component,
     * allowing to specify an initial offset compared to the stored position.
     */
    public void bind(final JDialog dialog, int initialWidth, int initialHeight) {
        updateSize(dialog, initialWidth, initialHeight);

        dialog.addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent e) {
                setWidth(new Integer(dialog.getWidth()));
                setHeight(new Integer(dialog.getHeight()));
            }
        });
    }

    /**
     * Binds this preference object to synchronize its state with a given component
     * property.
     */
    public void bindIntProperty(
            final Component component,
            final String property,
            int defaultValue) {

        updateIntProperty(component, property, defaultValue);

        component.addPropertyChangeListener(property, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent e) {
                Object value = e.getNewValue();
                setProperty(property, value != null ? value.toString() : null);
            }
        });
    }

    void updateIntProperty(Component c, String property, int defaultValue) {
        int i = getIntProperty(property, defaultValue);
        try {
            PropertyUtils.setProperty(c, property, new Integer(i));
        }
        catch (Throwable th) {
            throw new PreferenceException("Error setting property: " + property, th);
        }
    }

    void updateSize(Component c, int initialWidth, int initialHeight) {
        int w = getIntWidth(initialWidth);
        int h = getIntHeight(initialHeight);

        if (w > 0 && h > 0) {
            c.setSize(w, h);
        }
    }

    void updateLocation(Component c, int maxOffset) {
        if (maxOffset != 0) {
            int xOffset = (int) (Math.random() * maxOffset);
            int yOffset = (int) (Math.random() * maxOffset);
            changeX(xOffset);
            changeY(yOffset);
        }

        int x = getIntX(-1);
        int y = getIntY(-1);

        if (x > 0 && y > 0) {
            c.setLocation(x, y);
        }
    }

    public void changeX(int xOffset) {
        if (xOffset != 0) {
            setX(new Integer(getIntX(0) + xOffset));
        }
    }

    public void changeY(int yOffset) {
        if (yOffset != 0) {
            setY(new Integer(getIntY(0) + yOffset));
        }
    }

    public int getIntWidth(int defaultValue) {
        return (getWidth() != null) ? getWidth().intValue() : defaultValue;
    }

    public int getIntHeight(int defaultValue) {
        return (getHeight() != null) ? getHeight().intValue() : defaultValue;
    }

    public int getIntX(int defaultValue) {
        return (getX() != null) ? getX().intValue() : defaultValue;
    }

    public int getIntY(int defaultValue) {
        return (getY() != null) ? getY().intValue() : defaultValue;
    }
}

