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

package org.apache.cayenne.modeler.ui.action;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.CallbackMap;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.event.model.CallbackMethodEvent;
import org.apache.cayenne.modeler.event.model.DataMapEvent;
import org.apache.cayenne.modeler.event.model.DataNodeEvent;
import org.apache.cayenne.modeler.event.model.DbAttributeEvent;
import org.apache.cayenne.modeler.event.model.DbEntityEvent;
import org.apache.cayenne.modeler.event.model.DbRelationshipEvent;
import org.apache.cayenne.modeler.event.model.EmbeddableAttributeEvent;
import org.apache.cayenne.modeler.event.model.EmbeddableEvent;
import org.apache.cayenne.modeler.event.model.ObjAttributeEvent;
import org.apache.cayenne.modeler.event.model.ObjEntityEvent;
import org.apache.cayenne.modeler.event.model.ObjRelationshipEvent;
import org.apache.cayenne.modeler.event.model.ProcedureEvent;
import org.apache.cayenne.modeler.event.model.ProcedureParameterEvent;
import org.apache.cayenne.modeler.event.model.QueryEvent;
import org.apache.cayenne.modeler.project.DataMapOps;
import org.apache.cayenne.modeler.ui.confirmremove.ConfirmRemoveDialog;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.editor.objentity.callbacks.CallbackType;
import org.apache.cayenne.modeler.ui.project.editor.objentity.callbacks.ObjCallbackMethod;
import org.apache.cayenne.modeler.undo.RemoveAttributeUndoableEdit;
import org.apache.cayenne.modeler.undo.RemoveCallbackMethodUndoableEdit;
import org.apache.cayenne.modeler.undo.RemoveCompoundUndoableEdit;
import org.apache.cayenne.modeler.undo.RemoveRelationshipUndoableEdit;
import org.apache.cayenne.modeler.undo.RemoveUndoableEdit;

import javax.swing.*;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Removes currently selected object from the project. This can be Domain, DataNode,
 * Entity, Attribute or Relationship.
 */
public class RemoveAction extends ModelerAbstractAction {

    public RemoveAction(Application application) {
        super("Remove", application);
    }

    protected RemoveAction(String actionName, Application application) {
        super(actionName, application);
    }

    @Override
    public String getIconName() {
        return "icon-trash.png";
    }

    @Override
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    /**
     * Creates and returns dialog for delete prompt
     * 
     * @param allowAsking If false, no question will be asked no matter what settings are
     */
    public ConfirmRemoveDialog getConfirmDeleteDialog(boolean allowAsking) {
        return new ConfirmRemoveDialog(app, allowAsking);
    }

    @Override
    public void performAction(ActionEvent e) {
        performAction(e, true);
    }

    /**
     * Performs delete action
     * 
     * @param allowAsking If false, no question will be asked no matter what settings are
     */
    public void performAction(ActionEvent e, boolean allowAsking) {

        ProjectSession session = getProjectSession();
        ConfirmRemoveDialog dialog = getConfirmDeleteDialog(allowAsking);
        
        if (session.getSelectedObjEntity() != null) {
            if (dialog.shouldDelete("ObjEntity", session.getSelectedObjEntity().getName())) {

                app.getUndoManager()
                        .addEdit(new RemoveUndoableEdit(session, session.getSelectedDataMap(), session.getSelectedObjEntity()));
                removeObjEntity(session.getSelectedDataMap(), session.getSelectedObjEntity());
            }
        } else if (session.getSelectedDbEntity() != null) {
            if (dialog.shouldDelete("DbEntity", session.getSelectedDbEntity().getName())) {

                app.getUndoManager()
                        .addEdit(new RemoveUndoableEdit(session, session.getSelectedDataMap(), session.getSelectedDbEntity()));
                removeDbEntity(session.getSelectedDataMap(), session.getSelectedDbEntity());
            }
        } else if (session.getSelectedQuery() != null) {
            if (dialog.shouldDelete("query", session.getSelectedQuery().getName())) {

                app.getUndoManager()
                        .addEdit(new RemoveUndoableEdit(session, session.getSelectedDataMap(), session.getSelectedQuery()));
                removeQuery(session.getSelectedDataMap(), session.getSelectedQuery());
            }
        } else if (session.getSelectedProcedure() != null) {
            if (dialog.shouldDelete("procedure", session.getSelectedProcedure().getName())) {

                app.getUndoManager()
                        .addEdit(new RemoveUndoableEdit(session, session.getSelectedDataMap(), session.getSelectedProcedure()));
                removeProcedure(session.getSelectedDataMap(), session.getSelectedProcedure());
            }
        } else if (session.getSelectedEmbeddable() != null) {
            if (dialog.shouldDelete("embeddable", session.getSelectedEmbeddable().getClassName())) {

                app.getUndoManager()
                        .addEdit(new RemoveUndoableEdit(session, session.getSelectedDataMap(), session.getSelectedEmbeddable()));
                removeEmbeddable(session.getSelectedDataMap(), session.getSelectedEmbeddable());
            }
        } else if (session.getSelectedDataMap() != null) {
            if (dialog.shouldDelete("data map", session.getSelectedDataMap().getName())) {

                // In context of Data node just remove from Data Node
                if (session.getSelectedDataNode() != null) {
                    app.getUndoManager()
                            .addEdit(new RemoveUndoableEdit(session, session.getSelectedDataNode(),
                                    session.getSelectedDataMap()));
                    removeDataMapFromDataNode(session.getSelectedDataNode(), session.getSelectedDataMap());
                } else {
                    // Not under Data Node, remove completely
                    app.getUndoManager()
                            .addEdit(new RemoveUndoableEdit(session, session.getSelectedDataMap()));
                    removeDataMap(session.getSelectedDataMap());
                }
            }
        } else if (session.getSelectedDataNode() != null) {
            if (dialog.shouldDelete("data node", session.getSelectedDataNode().getName())) {

                app.getUndoManager()
                        .addEdit(new RemoveUndoableEdit(session, session.getSelectedDataNode()));
                removeDataNode(session.getSelectedDataNode());
            }
        } else if (session.getSelectedPaths() != null) { // multiple deletion
            if (dialog.shouldDelete("selected objects")) {

                ConfigurationNode[] paths = session.getSelectedPaths();
                ConfigurationNode parentPath = session.getSelectedParentPath();

                CompoundEdit compoundEdit = new RemoveCompoundUndoableEdit();
                for (ConfigurationNode path : paths) {
                    compoundEdit.addEdit(removeLastPathComponent(path, parentPath));
                }
                compoundEdit.end();

                app.getUndoManager().addEdit(compoundEdit);
            }
        } else if(session.getSelectedCallbackMethods().length > 0) {
            removeMethods(session, dialog, getProjectSession().getSelectedCallbackMethods());
        } else if(session.getSelectedObjRelationships().length > 0) {
      		removeObjRelationships(session, dialog, getProjectSession().getSelectedObjRelationships());
        } else if(session.getSelectedDbRelationships().length > 0) {
      		removeDBRelationships(session, dialog, getProjectSession().getSelectedDbRelationships());
        } else if(session.getSelectedObjAttributes().length > 0) {
      		removeObjAttributes(session, dialog, getProjectSession().getSelectedObjAttributes());
        } else if(session.getSelectedEmbeddableAttributes().length > 0) {
      		removeEmbAttributes(session, dialog, getProjectSession().getSelectedEmbeddableAttributes());
        } else if(session.getSelectedDbAttributes().length > 0) {
        	removeDbAttributes(session, dialog, getProjectSession().getSelectedDbAttributes());
        } else if(session.getSelectedProcedureParameters().length > 0) {
        	removeProcedureParameters(session.getSelectedProcedure(), session.getSelectedProcedureParameters());
        }

    }

    private void removeProcedureParameters(Procedure procedure, ProcedureParameter[] parameters) {
        ProjectSession session = getProjectSession();
        for (ProcedureParameter parameter : parameters) {
            procedure.removeCallParameter(parameter.getName());
            ProcedureParameterEvent e = ProcedureParameterEvent.ofRemove(app.getFrame(), parameter);
            session.fireProcedureParameterEvent(e);
        }
    }
    
    private void removeEmbAttributes(ProjectSession session, ConfirmRemoveDialog dialog, EmbeddableAttribute[] embAttrs) {
    	if (embAttrs != null && embAttrs.length > 0) {
        	if ((embAttrs.length == 1 && dialog.shouldDelete("DbAttribute", embAttrs[0].getName()))
                    || (embAttrs.length > 1 && dialog.shouldDelete("selected DbAttributes"))) {

        		Embeddable embeddable = session.getSelectedEmbeddable();

                app.getUndoManager()
                        .addEdit(new RemoveAttributeUndoableEdit(session,embeddable, embAttrs));

                for (EmbeddableAttribute attrib : embAttrs) {
                    embeddable.removeAttribute(attrib.getName());
                    EmbeddableAttributeEvent e = EmbeddableAttributeEvent.ofRemove(app.getFrame(),
                            attrib, embeddable);
                    session.fireEmbeddableAttributeEvent(e);
                }

                DataMapOps.removeBrokenObjToDbMappings(session.getSelectedDataMap());
        	}
    	}
	}

	private void removeObjAttributes(ProjectSession session, ConfirmRemoveDialog dialog, ObjAttribute[] objAttrs) {
    	if (objAttrs != null && objAttrs.length > 0) {
        	if ((objAttrs.length == 1 && dialog.shouldDelete("DbAttribute", objAttrs[0].getName()))
                    || (objAttrs.length > 1 && dialog.shouldDelete("selected DbAttributes"))) {

        		ObjEntity entity = session.getSelectedObjEntity();

                app.getUndoManager().addEdit(new RemoveAttributeUndoableEdit(session,entity, objAttrs));

                for (ObjAttribute attrib : objAttrs) {
                    entity.removeAttribute(attrib.getName());
                    ObjAttributeEvent e = ObjAttributeEvent.ofRemove(app.getFrame(), attrib, entity);
                    session.fireObjAttributeEvent(e);
                }

                DataMapOps.removeBrokenObjToDbMappings(session.getSelectedDataMap());
        	}
    	}
	}

	private void removeDbAttributes(ProjectSession session, ConfirmRemoveDialog dialog, DbAttribute[] dbAttrs) {
    	if (dbAttrs != null && dbAttrs.length > 0) {
        	if ((dbAttrs.length == 1 && dialog.shouldDelete("DbAttribute", dbAttrs[0].getName()))
                    || (dbAttrs.length > 1 && dialog.shouldDelete("selected DbAttributes"))) {

        		DbEntity entity = session.getSelectedDbEntity();

                app.getUndoManager()
                        .addEdit(new RemoveAttributeUndoableEdit(session,entity, dbAttrs));

                for (DbAttribute attrib : dbAttrs) {
                    entity.removeAttribute(attrib.getName());
                DbAttributeEvent e = DbAttributeEvent.ofRemove(app.getFrame(), attrib, entity);
                    session.fireDbAttributeEvent(e);
                }

                DataMapOps.removeBrokenObjToDbMappings(session.getSelectedDataMap());
        	}
    	}
    }
    
    private void removeDBRelationships(ProjectSession session, ConfirmRemoveDialog dialog, DbRelationship[] dbRels) {
		if (dbRels != null && dbRels.length > 0) {
			if ((dbRels.length == 1 && dialog.shouldDelete("DbRelationship", dbRels[0].getName()))
					|| (dbRels.length > 1 && dialog.shouldDelete("selected DbRelationships"))) {
				DbEntity entity = session.getSelectedDbEntity();
				
				for (DbRelationship rel : dbRels) {
					entity.removeRelationship(rel.getName());
					DbRelationshipEvent e = DbRelationshipEvent.ofRemove(app.getFrame(), rel, entity);
					session.fireDbRelationshipEvent(e);
				}

                DataMapOps.removeBrokenObjToDbMappings(session.getSelectedDataMap());
				app.getUndoManager().addEdit(new RemoveRelationshipUndoableEdit(session,entity, dbRels));
			}
		}
	}

	private void removeObjRelationships(ProjectSession session, ConfirmRemoveDialog dialog, ObjRelationship[] rels) {
		if ((rels.length == 1 && dialog.shouldDelete("ObjRelationship", rels[0].getName()))
				|| (rels.length > 1 && dialog.shouldDelete("selected ObjRelationships"))) {
			ObjEntity entity = session.getSelectedObjEntity();
			for (ObjRelationship rel : rels) {
				entity.removeRelationship(rel.getName());
ObjRelationshipEvent e = ObjRelationshipEvent.ofRemove(app.getFrame(), rel, entity);
				session.fireObjRelationshipEvent(e);
			}
			app.getUndoManager().addEdit(new RemoveRelationshipUndoableEdit(session,entity, rels));
		}		
	}

	private void removeMethods(ProjectSession session, ConfirmRemoveDialog dialog, ObjCallbackMethod[] methods) {
    	CallbackMap callbackMap = session.getSelectedObjEntity().getCallbackMap();
    	CallbackType callbackType = session.getSelectedCallbackType();

        if ((methods.length == 1 && dialog.shouldDelete("callback method", methods[0].getName()))
        	|| (methods.length > 1 && dialog.shouldDelete("selected callback methods"))) {
            for (ObjCallbackMethod callbackMethod : methods) {
            	callbackMap.getCallbackDescriptor(callbackType.getType()).removeCallbackMethod(callbackMethod.getName());
                    
                CallbackMethodEvent ce = CallbackMethodEvent.ofRemove(this, callbackMethod.getName());
                    
                session.fireCallbackMethodEvent(ce);
            }
            
            app.getUndoManager().addEdit(new RemoveCallbackMethodUndoableEdit(getProjectSession(), callbackType, methods));
        }		
	}

	public void removeDataMap(DataMap map) {
        ProjectSession session = getProjectSession();
        DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();
        DataMapEvent e = DataMapEvent.ofRemove(app.getFrame(), map);

        domain.getDataMaps().remove(map);
        if (map.getConfigurationSource() != null) {
            URL mapURL = map.getConfigurationSource().getURL();
            Collection<URL> unusedResources = getCurrentProject().getUnusedResources();
            unusedResources.add(mapURL);
        }

        for (DataNodeDescriptor node : domain.getNodeDescriptors()) {
            if (node.getDataMapNames().contains(map.getName())) {
                removeDataMapFromDataNode(node, map);
            }
        }
       
        session.fireDataMapEvent(e);
    }

    public void removeDataNode(DataNodeDescriptor node) {
        ProjectSession session = getProjectSession();
        DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();
        DataNodeEvent e = DataNodeEvent.ofRemove(app.getFrame(), node);

        domain.getNodeDescriptors().remove(node);
        session.fireDataNodeEvent(e);
    }

    /**
     * Removes current DbEntity from its DataMap and fires "remove" EntityEvent.
     */
    public void removeDbEntity(DataMap map, DbEntity ent) {
        ProjectSession session = getProjectSession();

        DbEntityEvent e = DbEntityEvent.ofRemove(app.getFrame(), ent);

        map.removeDbEntity(ent.getName(), true);
        session.fireDbEntityEvent(e);
    }

    /**
     * Removes current Query from its DataMap and fires "remove" QueryEvent.
     */
    public void removeQuery(DataMap map, QueryDescriptor query) {
        ProjectSession session = getProjectSession();

        QueryEvent e = QueryEvent.ofRemove(app.getFrame(), query, map);

        map.removeQueryDescriptor(query.getName());
        session.fireQueryEvent(e);
    }

    /**
     * Removes current Procedure from its DataMap and fires "remove" ProcedureEvent.
     */
    public void removeProcedure(DataMap map, Procedure procedure) {
        ProjectSession session = getProjectSession();

        ProcedureEvent e = ProcedureEvent.ofRemove(app.getFrame(), procedure);

        map.removeProcedure(procedure.getName());
        session.fireProcedureEvent(e);
    }

    /**
     * Removes current object entity from its DataMap.
     */
    public void removeObjEntity(DataMap map, ObjEntity entity) {
        ProjectSession session = getProjectSession();

    ObjEntityEvent e = ObjEntityEvent.ofRemove(app.getFrame(), entity);

        map.removeObjEntity(entity.getName(), true);
        session.fireObjEntityEvent(e);

        // remove queries that depend on entity
        // TODO: (Andrus, 09/09/2005) show warning dialog?

        // clone to be able to remove within iterator...
        for (QueryDescriptor query : new ArrayList<>(map.getQueryDescriptors())) {
            if (!QueryDescriptor.EJBQL_QUERY.equals(query.getType())) {
                Object root = query.getRoot();

                if (root == entity || (root instanceof String && root.toString().equals(entity.getName()))) {
                    removeQuery(map, query);
                }
            }
        }
    }

    public void removeEmbeddable(DataMap map, Embeddable embeddable) {
        ProjectSession session = getProjectSession();

        EmbeddableEvent e = EmbeddableEvent.ofRemove(app.getFrame(), embeddable);

        map.removeEmbeddable(embeddable.getClassName());
        session.fireEmbeddableEvent(e, map);
    }

    public void removeDataMapFromDataNode(DataNodeDescriptor node, DataMap map) {
        ProjectSession session = getProjectSession();

        DataNodeEvent e = DataNodeEvent.ofChange(app.getFrame(), node);

        node.getDataMapNames().remove(map.getName());

        // Force reloading of the data node in the browse view
        session.fireDataNodeEvent(e);
    }

    /**
     * Returns <code>true</code> if last object in the path contains a removable object.
     */
    @Override
    public boolean enableForPath(ConfigurationNode object) {
        return (object instanceof DataChannelDescriptor)
                || (object instanceof DataMap)
                || (object instanceof DataNodeDescriptor)
                || (object instanceof Entity)
                || (object instanceof Attribute)
                || (object instanceof Relationship)
                || (object instanceof Procedure)
                || (object instanceof ProcedureParameter)
                || (object instanceof QueryDescriptor)
                || (object instanceof Embeddable)
                || (object instanceof EmbeddableAttribute);
    }

    /**
     * Removes an object, depending on its type
     */
    private UndoableEdit removeLastPathComponent(ConfigurationNode object, ConfigurationNode parentObject) {

        UndoableEdit undo = null;

        ProjectSession session = getProjectSession();
        if (object instanceof DataMap) {
            if (parentObject instanceof DataNodeDescriptor) {
                undo = new RemoveUndoableEdit(session, (DataNodeDescriptor) parentObject, (DataMap) object);
                removeDataMapFromDataNode((DataNodeDescriptor) parentObject, (DataMap) object);
            } else {
                // Not under Data Node, remove completely
                undo = new RemoveUndoableEdit(session, (DataMap) object);
                removeDataMap((DataMap) object);
            }
        } else if (object instanceof DataNodeDescriptor) {
            undo = new RemoveUndoableEdit(session, (DataNodeDescriptor) object);
            removeDataNode((DataNodeDescriptor) object);
        } else if (object instanceof DbEntity) {
            undo = new RemoveUndoableEdit(session, ((DbEntity) object).getDataMap(), (DbEntity) object);
            removeDbEntity(((DbEntity) object).getDataMap(), (DbEntity) object);
        } else if (object instanceof ObjEntity) {
            undo = new RemoveUndoableEdit(session, ((ObjEntity) object).getDataMap(), (ObjEntity) object);
            removeObjEntity(((ObjEntity) object).getDataMap(), (ObjEntity) object);
        } else if (object instanceof QueryDescriptor) {
            undo = new RemoveUndoableEdit(session, ((QueryDescriptor) object).getDataMap(), (QueryDescriptor) object);
            removeQuery(((QueryDescriptor) object).getDataMap(), (QueryDescriptor) object);
        } else if (object instanceof Procedure) {
            undo = new RemoveUndoableEdit(session, ((Procedure) object).getDataMap(), (Procedure) object);
            removeProcedure(((Procedure) object).getDataMap(), (Procedure) object);
        } else if (object instanceof Embeddable) {
            undo = new RemoveUndoableEdit(session, ((Embeddable) object).getDataMap(), (Embeddable) object);
            removeEmbeddable(((Embeddable) object).getDataMap(), (Embeddable) object);
        }

        return undo;
    }
}
