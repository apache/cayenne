/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.pref;

import org.apache.cayenne.pref.Domain;
import org.apache.cayenne.pref.PreferenceException;
import org.apache.cayenne.reflect.PropertyUtils;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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
            final Window window,
            int initialWidth,
            int initialHeight,
            int maxOffset) {

        updateSize(window, initialWidth, initialHeight);
        updateLocation(window, maxOffset);

        window.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                setWidth(new Integer(window.getWidth()));
                setHeight(new Integer(window.getHeight()));
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                setX(new Integer(window.getX()));
                setY(new Integer(window.getY()));
            }
        });
    }

    /**
     * Binds this preference object to synchronize its state with a given component,
     * allowing to specify an initial offset compared to the stored position.
     */
    public void bindSize(final Window window, int initialWidth, int initialHeight) {
        updateSize(window, initialWidth, initialHeight);

        window.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                setWidth(new Integer(window.getWidth()));
                setHeight(new Integer(window.getHeight()));
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

