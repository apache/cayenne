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

package org.apache.cayenne.modeler.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.modeler.ModelerConstants;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.property.PropertyUtils;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * Various unorganized utility methods used by CayenneModeler.
 * 
 * @author Andrei Adamchik
 */
public final class ModelerUtil {

    /**
     * Returns the "name" property of the object.
     * 
     * @since 1.1
     */
    public static String getObjectName(Object object) {
        if (object == null) {
            return null;
        }
        else if (object instanceof CayenneMapEntry) {
            return ((CayenneMapEntry) object).getName();
        }
        else if (object instanceof String) {
            return (String) object;
        }
        else {
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
    public static Collection getDbAttributeNames(
            ProjectController mediator,
            DbEntity entity) {

        Set keys = entity.getAttributeMap().keySet();
        List list = new ArrayList(keys.size());
        list.add("");
        list.addAll(keys);
        return list;
    }

    public static String[] getRegisteredTypeNames() {
        String[] srcList = new ExtendedTypeMap().getRegisteredTypeNames();
        Arrays.sort(srcList);

        String[] finalList = new String[srcList.length + 1];
        System.arraycopy(srcList, 0, finalList, 1, srcList.length);
        finalList[0] = "";

        return finalList;
    }

    public static DataNode getNodeLinkedToMap(DataDomain domain, DataMap map) {
        Collection nodes = domain.getDataNodes();

        // go via an iterator in an indexed loop, since
        // we already obtained the size
        // (and index is required to initialize array)
        Iterator nodesIt = nodes.iterator();
        while (nodesIt.hasNext()) {
            DataNode node = (DataNode) nodesIt.next();

            if (node.getDataMaps().contains(map)) {
                return node;
            }
        }

        return null;
    }
}
