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

package org.apache.cayenne.modeler.util;

import javax.swing.Action;
import javax.swing.ImageIcon;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ModelerConstants;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.MultipleObjectsAction;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.reflect.PropertyUtils;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.value.GeoJson;
import org.apache.cayenne.value.Json;
import org.apache.cayenne.value.Wkt;

/**
 * Various unorganized utility methods used by CayenneModeler.
 * 
 */
public final class ModelerUtil {

    static final String[] REGISTERED_TYPE_NAMES;
    static {
        String[] nonPrimitivesNames = {
                String.class.getName(),
                BigDecimal.class.getName(),
                BigInteger.class.getName(),
                Boolean.class.getName(),
                Byte.class.getName(),
                Character.class.getName(),
                Date.class.getName(),
                java.util.Date.class.getName(),
                Double.class.getName(),
                Float.class.getName(),
                Integer.class.getName(),
                Long.class.getName(),
                Short.class.getName(),
                Time.class.getName(),
                Timestamp.class.getName(),
                GregorianCalendar.class.getName(),
                Calendar.class.getName(),
                UUID.class.getName(),
                Serializable.class.getName(),
                Json.class.getName(),
                Wkt.class.getName(),
                GeoJson.class.getName(),
                "java.lang.Character[]",
                "java.lang.Byte[]",
                "java.time.LocalDate",
                "java.time.LocalTime",
                "java.time.LocalDateTime",
                "java.time.Duration",
                "java.time.Period"
        };
        Arrays.sort(nonPrimitivesNames);

        String[] primitivesNames = {
                "boolean", "byte", "byte[]", "char", "char[]", "double", "float", "int", "long", "short"
        };

        REGISTERED_TYPE_NAMES = new String[primitivesNames.length + nonPrimitivesNames.length + 1];

        REGISTERED_TYPE_NAMES[0] = "";
        System.arraycopy(primitivesNames, 0, REGISTERED_TYPE_NAMES, 1, primitivesNames.length);
        System.arraycopy(
                nonPrimitivesNames,
                0,
                REGISTERED_TYPE_NAMES,
                primitivesNames.length + 1,
                nonPrimitivesNames.length);
    }

    /**
     * Returns the "name" property of the object.
     * 
     * @since 1.1
     */
    public static String getObjectName(Object object) {
        if (object == null) {
            return null;
        } else if (object instanceof CayenneMapEntry) {
            return ((CayenneMapEntry) object).getName();
        } else if (object instanceof String) {
            return (String) object;
        } else {
            try {
                // use reflection
                return (String) PropertyUtils.getProperty(object, "name");
            }
            catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * Returns an icon, building it from an image file located at the shared resources
     * folder for the modeler.
     */
    public static ImageIcon buildIcon(String path) {
        ClassLoader cl = ModelerUtil.class.getClassLoader();
        URL url = cl.getResource(ModelerConstants.RESOURCE_PATH + path);
        return new ImageIcon(url);
    }

    /**
     * Returns array of db attribute names for DbEntity mapped to current ObjEntity.
     */
    public static Collection<String> getDbAttributeNames(DbEntity entity) {

        Set<String> keys = entity.getAttributeMap().keySet();
        List<String> list = new ArrayList<>(keys.size() + 1);
        list.add("");
        list.addAll(keys);
        return list;
    }

    public static String[] getRegisteredTypeNames() {
        return REGISTERED_TYPE_NAMES;
    }

    public static DataNodeDescriptor getNodeLinkedToMap(DataChannelDescriptor domain, DataMap map) {
        Collection<DataNodeDescriptor> nodes = domain.getNodeDescriptors();

        // go via an iterator in an indexed loop, since
        // we already obtained the size
        // (and index is required to initialize array)
        for (DataNodeDescriptor node : nodes) {
            if (node.getDataMapNames().contains(map.getName())) {
                return node;
            }
        }

        return null;
    }

    /**
     * Updates MultipleObjectActions' state, depending on number of selected objects
     * (attributes, rel etc.)
     */
    public static void updateActions(int numSelected, Class<? extends Action>... actions) {
        ActionManager actionManager = Application.getInstance().getActionManager();

        for (Class<? extends Action> actionType : actions) {
            Action action = actionManager.getAction(actionType);

            if (action instanceof MultipleObjectsAction) {
                MultipleObjectsAction multiObjectAction = (MultipleObjectsAction) action;
                multiObjectAction.setEnabled(numSelected > 0);
                ((CayenneAction) multiObjectAction).setName(multiObjectAction.getActionName(numSelected > 1));
            }
        }
    }
    
    /**
     * Retrieves strings from .properties file
     */
    public static String getProperty(String key) {
        ResourceBundle properties = ResourceBundle.getBundle(Application.DEFAULT_MESSAGE_BUNDLE);
        return properties == null ? "" : properties.getString(key);
    }
    
    /**
     * Center a window on a parent window
     */
    public static void centerWindow(Window parent, Window child) {
        Dimension parentSize = parent.getSize();
        Dimension childSize = child.getSize();
        
        Point parentLocation = new Point(0, 0);
        if (parent.isShowing()) {
            parentLocation = parent.getLocationOnScreen();
        }

        int x = parentLocation.x + parentSize.width / 2 - childSize.width / 2;
        int y = parentLocation.y + parentSize.height / 2 - childSize.height / 2;

        child.setLocation(x, y);
    }

    /**
     * @since 4.1
     */
    public static String initOutputFolder() {
        String path;
        if (System.getProperty("cayenne.cgen.destdir") != null) {
            return System.getProperty("cayenne.cgen.destdir");
        } else {
            // init default directory..
            FSPath lastPath = Application.getInstance().getFrameController().getLastDirectory();

            path = checkDefaultMavenResourceDir(lastPath, "test");

            if (path != null || (path = checkDefaultMavenResourceDir(lastPath, "main")) != null) {
                return path;
            } else {
                File lastDir = lastPath.getExistingDirectory(false);
                return lastDir != null ? lastDir.getAbsolutePath() : ".";
            }
        }
    }

    private static String checkDefaultMavenResourceDir(FSPath lastPath, String dirType) {
        String path = lastPath.getPath();
        String resourcePath = buildFilePath("src", dirType, "resources");
        int idx = path.indexOf(resourcePath);
        if (idx < 0) {
            return null;
        }
        return path.substring(0, idx) + buildFilePath("src", dirType, "java");
    }

    private static String buildFilePath(String... pathElements) {
        if (pathElements.length == 0) {
            return "";
        }
        StringBuilder path = new StringBuilder(pathElements[0]);
        for (int i = 1; i < pathElements.length; i++) {
            path.append(File.separator).append(pathElements[i]);
        }
        return path.toString();
    }

}
