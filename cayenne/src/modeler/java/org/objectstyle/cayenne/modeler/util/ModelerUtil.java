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
package org.objectstyle.cayenne.modeler.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;

import org.apache.commons.beanutils.PropertyUtils;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.MapObject;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.ModelerConstants;

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
        else if (object instanceof MapObject) {
            return ((MapObject) object).getName();
        }
        else if (object instanceof String) {
            return (String) object;
        }
        else {
            try {
                // use reflection
                return (String) PropertyUtils.getSimpleProperty(object, "name");
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