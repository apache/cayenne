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

package org.apache.cayenne.modeler.pref;

import org.apache.cayenne.pref.CayennePreference;
import org.apache.cayenne.pref.PreferenceException;
import org.apache.cayenne.reflect.PropertyUtils;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;

public class ComponentGeometry extends CayennePreference {

    public static final String GEOMETRY_PREF_KEY = "geometry";
    
    public static final String HEIGHT_PROPERTY = "height";
    public static final String WIDTH_PROPERTY = "width";
    public static final String X_PROPERTY = "x";
    public static final String Y_PROPERTY = "y";
    
    public ComponentGeometry(Class<?> className, String path) {
        setCurrentNodeForPreference(className, path);
    };
    
    public Preferences getPreference() {
        if (getCurrentPreference() == null) {
            setCurrentNodeForPreference(this.getClass(), GEOMETRY_PREF_KEY);
        }
        return getCurrentPreference();
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
                setWidth(window.getWidth());
                setHeight(window.getHeight());
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                setX(window.getX());
                setY(window.getY());
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
                setWidth(window.getWidth());
                setHeight(window.getHeight());
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
                getPreference().put(property, value != null ? value.toString() : null);
            }
        });
    }

    void updateIntProperty(Component c, String property, int defaultValue) {
        int i = getPreference().getInt(property, defaultValue);
        try {
            PropertyUtils.setProperty(c, property, i);
        }
        catch (Throwable th) {
            throw new PreferenceException("Error setting property: " + property, th);
        }
    }

    void updateSize(Component c, int initialWidth, int initialHeight) {
        int w = getWidth(initialWidth);
        int h = getHeight(initialHeight);
        
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

        int x = getX(-1);
        int y = getY(-1);

        if (x > 0 && y > 0) {
            c.setLocation(x, y);
        }
    }

    public void changeX(int xOffset) {
        if (xOffset != 0) {
            setX(getX(0) + xOffset);
        }
    }

    public void changeY(int yOffset) {
        if (yOffset != 0) {
            setY(getY(0) + yOffset);
        }
    }

    private void setY(Integer y) {
            getPreference().putInt(Y_PROPERTY, y);
    }
    
    private void setX(Integer x) {
        getPreference().putInt(X_PROPERTY, x);
    }
    
    private void setHeight(Integer height) {
        getPreference().putInt(HEIGHT_PROPERTY, height);
    }
         
  
    private void setWidth(Integer width) {
        getPreference().putInt(WIDTH_PROPERTY, width);
    }
    
    public int getWidth(int defaultValue) {
        return getPreference().getInt(WIDTH_PROPERTY, defaultValue);
    }
         
    public int getHeight(int defaultValue) {
        return getPreference().getInt(HEIGHT_PROPERTY, defaultValue);
    }

    public int getX(int defaultValue) {
        return getPreference().getInt(X_PROPERTY, defaultValue);
    }
 
    public int getY(int defaultValue) {
        return getPreference().getInt(Y_PROPERTY, defaultValue);
    }
}

