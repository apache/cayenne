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

package org.objectstyle.cayenne.modeler.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.Attribute;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.ProcedureParameter;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.map.event.AttributeEvent;
import org.objectstyle.cayenne.map.event.DataMapEvent;
import org.objectstyle.cayenne.map.event.DataNodeEvent;
import org.objectstyle.cayenne.map.event.DomainEvent;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.map.event.MapEvent;
import org.objectstyle.cayenne.map.event.ProcedureEvent;
import org.objectstyle.cayenne.map.event.ProcedureParameterEvent;
import org.objectstyle.cayenne.map.event.QueryEvent;
import org.objectstyle.cayenne.map.event.RelationshipEvent;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.util.CayenneAction;
import org.objectstyle.cayenne.modeler.util.ProjectUtil;
import org.objectstyle.cayenne.project.ApplicationProject;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.query.Query;

/**
 * Removes currently selected object from the project. This can be Domain, DataNode,
 * Entity, Attribute or Relationship.
 */
public class RemoveAction extends CayenneAction {

    public static String getActionName() {
        return "Remove";
    }

    public RemoveAction(Application application) {
        super(getActionName(), application);
    }

    public String getIconName() {
        return "icon-trash.gif";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK);
    }

    public void performAction(ActionEvent e) {
        remove();
    }

    private void remove() {
        ProjectController mediator = getProjectController();

        if (mediator.getCurrentObjAttribute() != null) {
            removeObjAttribute();
        }
        else if (mediator.getCurrentDbAttribute() != null) {
            removeDbAttribute();
        }
        else if (mediator.getCurrentObjRelationship() != null) {
            removeObjRelationship();
        }
        else if (mediator.getCurrentDbRelationship() != null) {
            removeDbRelationship();
        }
        else if (mediator.getCurrentProcedureParameter() != null) {
            removeProcedureParameter();
        }
        else if (mediator.getCurrentObjEntity() != null) {
            removeObjEntity();
        }
        else if (mediator.getCurrentDbEntity() != null) {
            removeDbEntity();
        }
        else if (mediator.getCurrentQuery() != null) {
            removeQuery();
        }
        else if (mediator.getCurrentProcedure() != null) {
            removeProcedure();
        }
        else if (mediator.getCurrentDataMap() != null) {
            // In context of Data node just remove from Data Node
            if (mediator.getCurrentDataNode() != null) {
                removeDataMapFromDataNode();
            }
            else {
                // Not under Data Node, remove completely
                removeDataMap();
            }
        }
        else if (mediator.getCurrentDataNode() != null) {
            removeDataNode();
        }
        else if (mediator.getCurrentDataDomain() != null) {
            removeDomain();
        }
    }

    protected void removeDomain() {
        ApplicationProject project = (ApplicationProject) getCurrentProject();
        ProjectController mediator = getProjectController();
        DataDomain domain = mediator.getCurrentDataDomain();
        project.getConfiguration().removeDomain(domain.getName());
        mediator.fireDomainEvent(new DomainEvent(
                Application.getFrame(),
                domain,
                MapEvent.REMOVE));
    }

    protected void removeDataMap() {
        ProjectController mediator = getProjectController();
        DataMap map = mediator.getCurrentDataMap();
        DataDomain domain = mediator.getCurrentDataDomain();
        domain.removeMap(map.getName());
        mediator.fireDataMapEvent(new DataMapEvent(
                Application.getFrame(),
                map,
                MapEvent.REMOVE));
    }

    protected void removeDataNode() {
        ProjectController mediator = getProjectController();
        DataNode node = mediator.getCurrentDataNode();
        DataDomain domain = mediator.getCurrentDataDomain();
        domain.removeDataNode(node.getName());
        mediator.fireDataNodeEvent(new DataNodeEvent(
                Application.getFrame(),
                node,
                MapEvent.REMOVE));
    }

    /**
     * Removes current DbEntity from its DataMap and fires "remove" EntityEvent.
     */
    protected void removeDbEntity() {
        ProjectController mediator = getProjectController();
        DbEntity ent = mediator.getCurrentDbEntity();
        DataMap map = mediator.getCurrentDataMap();
        map.removeDbEntity(ent.getName(), true);
        mediator.fireDbEntityEvent(new EntityEvent(
                Application.getFrame(),
                ent,
                MapEvent.REMOVE));
    }

    /**
     * Removes current Query from its DataMap and fires "remove" QueryEvent.
     */
    protected void removeQuery() {
        ProjectController mediator = getProjectController();
        Query query = mediator.getCurrentQuery();
        DataMap map = mediator.getCurrentDataMap();
        map.removeQuery(query.getName());
        mediator.fireQueryEvent(new QueryEvent(
                Application.getFrame(),
                query,
                MapEvent.REMOVE));
    }

    /**
     * Removes current Procedure from its DataMap and fires "remove" ProcedureEvent.
     */
    protected void removeProcedure() {
        ProjectController mediator = getProjectController();
        Procedure procedure = mediator.getCurrentProcedure();
        DataMap map = mediator.getCurrentDataMap();
        map.removeProcedure(procedure.getName());
        mediator.fireProcedureEvent(new ProcedureEvent(
                Application.getFrame(),
                procedure,
                MapEvent.REMOVE));
    }

    /**
     * Removes current object entity from its DataMap.
     */
    protected void removeObjEntity() {
        ProjectController mediator = getProjectController();
        ObjEntity ent = mediator.getCurrentObjEntity();

        DataMap map = mediator.getCurrentDataMap();
        map.removeObjEntity(ent.getName(), true);
        mediator.fireObjEntityEvent(new EntityEvent(
                Application.getFrame(),
                ent,
                MapEvent.REMOVE));
    }

    protected void removeProcedureParameter() {
        ProjectController mediator = getProjectController();
        ProcedureParameter parameter = mediator.getCurrentProcedureParameter();
        mediator.getCurrentProcedure().removeCallParameter(parameter.getName());
        ProcedureParameterEvent e = new ProcedureParameterEvent(
                Application.getFrame(),
                parameter,
                MapEvent.REMOVE);
        mediator.fireProcedureParameterEvent(e);
    }

    protected void removeObjAttribute() {
        ProjectController mediator = getProjectController();
        ObjEntity entity = mediator.getCurrentObjEntity();
        ObjAttribute attrib = mediator.getCurrentObjAttribute();

        entity.removeAttribute(attrib.getName());
        AttributeEvent e = new AttributeEvent(
                Application.getFrame(),
                attrib,
                entity,
                MapEvent.REMOVE);
        mediator.fireObjAttributeEvent(e);
    }

    protected void removeDbAttribute() {
        ProjectController mediator = getProjectController();
        DbEntity entity = mediator.getCurrentDbEntity();
        DbAttribute attrib = mediator.getCurrentDbAttribute();
        entity.removeAttribute(attrib.getName());
        ProjectUtil.cleanObjMappings(mediator.getCurrentDataMap());

        AttributeEvent e = new AttributeEvent(
                Application.getFrame(),
                attrib,
                entity,
                MapEvent.REMOVE);
        mediator.fireDbAttributeEvent(e);
    }

    protected void removeObjRelationship() {
        ProjectController mediator = getProjectController();
        ObjEntity entity = mediator.getCurrentObjEntity();
        ObjRelationship rel = mediator.getCurrentObjRelationship();
        entity.removeRelationship(rel.getName());
        RelationshipEvent e = new RelationshipEvent(
                Application.getFrame(),
                rel,
                entity,
                RelationshipEvent.REMOVE);
        mediator.fireObjRelationshipEvent(e);
    }

    protected void removeDbRelationship() {
        ProjectController mediator = getProjectController();
        DbEntity entity = mediator.getCurrentDbEntity();
        DbRelationship rel = mediator.getCurrentDbRelationship();
        entity.removeRelationship(rel.getName());
        ProjectUtil.cleanObjMappings(mediator.getCurrentDataMap());

        RelationshipEvent e = new RelationshipEvent(
                Application.getFrame(),
                rel,
                entity,
                RelationshipEvent.REMOVE);
        mediator.fireDbRelationshipEvent(e);
    }

    protected void removeDataMapFromDataNode() {
        ProjectController mediator = getProjectController();
        DataNode node = mediator.getCurrentDataNode();
        DataMap map = mediator.getCurrentDataMap();
        node.removeDataMap(map.getName());

        // Force reloading of the data node in the browse view
        mediator.fireDataNodeEvent(new DataNodeEvent(Application.getFrame(), node));
    }

    /**
     * Returns <code>true</code> if last object in the path contains a removable object.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        Object lastObject = path.getObject();

        if (lastObject instanceof DataDomain) {
            return true;
        }
        else if (lastObject instanceof DataMap) {
            return true;
        }
        else if (lastObject instanceof DataNode) {
            return true;
        }
        else if (lastObject instanceof Entity) {
            return true;
        }
        else if (lastObject instanceof Attribute) {
            return true;
        }
        else if (lastObject instanceof Relationship) {
            return true;
        }
        else {
            return false;
        }
    }
}