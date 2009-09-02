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
package org.apache.cayenne.modeler.action;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.CayenneTransferable;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * Action for copying entities, queries etc. into system buffer
 */
public class CopyAction extends CayenneAction {
    public static String getActionName() {
        return "Copy";
    }

    /**
     * Constructor for CopyAction
     */
    public CopyAction(Application application) {
        this(getActionName(), application);
    }
    
    /**
     * Constructor for descendants
     */
    protected CopyAction(String name, Application application) {
        super(name, application);
    }

    @Override
    public String getIconName() {
        return "icon-copy.gif";
    }
    
    @Override
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    /**
     * Performs copying of items into system buffer
     */
    @Override
    public void performAction(ActionEvent e) {
        ProjectController mediator = getProjectController();
        
        Object content = copy(mediator);
        
        if (content != null) {
            Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
            sysClip.setContents(new CayenneTransferable(content), null);
        }
        
        //update paste button
        ((PasteAction) getApplication().getAction(PasteAction.getActionName())).updateState();
    }
    
    /**
     * Detects selected objects and returns them
     */
    public Object copy(ProjectController mediator) {
        return mediator.getCurrentObject();
    }
    
    /**
     * Prints an object in XML format to an output stream
     */
    protected void print(XMLEncoder encoder, XMLSerializable object) {
        object.encodeAsXML(encoder);
    }
    
    /**
     * Returns <code>true</code> if last object in the path contains a removable object.
     */
    @Override
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }
        
        Object last = path.getObject();
        if (last instanceof DataMap || last instanceof Query
                || last instanceof DbEntity || last instanceof ObjEntity
                || last instanceof Embeddable || last instanceof EmbeddableAttribute
                || last instanceof DbAttribute || last instanceof DbRelationship
                || last instanceof ObjAttribute || last instanceof ObjRelationship
                || last instanceof Procedure || last instanceof ProcedureParameter) {
            return true;
        }

        return false;
    }
}
