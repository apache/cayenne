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

package org.apache.cayenne.modeler.action;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.modeler.event.model.DataMapEvent;
import org.apache.cayenne.modeler.event.model.DataNodeEvent;
import org.apache.cayenne.modeler.event.model.ProcedureEvent;
import org.apache.cayenne.modeler.event.model.ProcedureParameterEvent;
import org.apache.cayenne.modeler.event.model.QueryEvent;
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
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.EmbeddableAttributeEvent;
import org.apache.cayenne.map.event.EmbeddableEvent;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.ui.confirmremove.ConfirmRemoveDialog;
import org.apache.cayenne.modeler.ui.project.editor.objentity.callbacks.CallbackType;
import org.apache.cayenne.modeler.ui.project.editor.objentity.callbacks.ObjCallbackMethod;
import org.apache.cayenne.modeler.event.model.CallbackMethodEvent;
import org.apache.cayenne.modeler.undo.RemoveAttributeUndoableEdit;
import org.apache.cayenne.modeler.undo.RemoveCallbackMethodUndoableEdit;
import org.apache.cayenne.modeler.undo.RemoveCompoundUndoableEdit;
import org.apache.cayenne.modeler.undo.RemoveRelationshipUndoableEdit;
import org.apache.cayenne.modeler.undo.RemoveUndoableEdit;
import org.apache.cayenne.modeler.util.ProjectUtil;

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
        return new ConfirmRemoveDialog(allowAsking);
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

        ProjectController controller = getProjectController();
        ConfirmRemoveDialog dialog = getConfirmDeleteDialog(allowAsking);
        
        if (controller.getSelectedObjEntity() != null) {
            if (dialog.shouldDelete("ObjEntity", controller.getSelectedObjEntity().getName())) {

                application.getUndoManager()
                        .addEdit(new RemoveUndoableEdit(controller.getSelectedDataMap(), controller.getSelectedObjEntity()));
                removeObjEntity(controller.getSelectedDataMap(), controller.getSelectedObjEntity());
            }
        } else if (controller.getSelectedDbEntity() != null) {
            if (dialog.shouldDelete("DbEntity", controller.getSelectedDbEntity().getName())) {

                application.getUndoManager()
                        .addEdit(new RemoveUndoableEdit(controller.getSelectedDataMap(), controller.getSelectedDbEntity()));
                removeDbEntity(controller.getSelectedDataMap(), controller.getSelectedDbEntity());
            }
        } else if (controller.getSelectedQuery() != null) {
            if (dialog.shouldDelete("query", controller.getSelectedQuery().getName())) {

                application.getUndoManager()
                        .addEdit(new RemoveUndoableEdit(controller.getSelectedDataMap(), controller.getSelectedQuery()));
                removeQuery(controller.getSelectedDataMap(), controller.getSelectedQuery());
            }
        } else if (controller.getSelectedProcedure() != null) {
            if (dialog.shouldDelete("procedure", controller.getSelectedProcedure().getName())) {

                application.getUndoManager()
                        .addEdit(new RemoveUndoableEdit(controller.getSelectedDataMap(), controller.getSelectedProcedure()));
                removeProcedure(controller.getSelectedDataMap(), controller.getSelectedProcedure());
            }
        } else if (controller.getSelectedEmbeddable() != null) {
            if (dialog.shouldDelete("embeddable", controller.getSelectedEmbeddable().getClassName())) {

                application.getUndoManager()
                        .addEdit(new RemoveUndoableEdit(controller.getSelectedDataMap(), controller.getSelectedEmbeddable()));
                removeEmbeddable(controller.getSelectedDataMap(), controller.getSelectedEmbeddable());
            }
        } else if (controller.getSelectedDataMap() != null) {
            if (dialog.shouldDelete("data map", controller.getSelectedDataMap().getName())) {

                // In context of Data node just remove from Data Node
                if (controller.getSelectedDataNode() != null) {
                    application.getUndoManager()
                            .addEdit(new RemoveUndoableEdit(application, controller.getSelectedDataNode(),
                                    controller.getSelectedDataMap()));
                    removeDataMapFromDataNode(controller.getSelectedDataNode(), controller.getSelectedDataMap());
                } else {
                    // Not under Data Node, remove completely
                    application.getUndoManager()
                            .addEdit(new RemoveUndoableEdit(application, controller.getSelectedDataMap()));
                    removeDataMap(controller.getSelectedDataMap());
                }
            }
        } else if (controller.getSelectedDataNode() != null) {
            if (dialog.shouldDelete("data node", controller.getSelectedDataNode().getName())) {

                application.getUndoManager()
                        .addEdit(new RemoveUndoableEdit(application, controller.getSelectedDataNode()));
                removeDataNode(controller.getSelectedDataNode());
            }
        } else if (controller.getSelectedPaths() != null) { // multiple deletion
            if (dialog.shouldDelete("selected objects")) {

                ConfigurationNode[] paths = controller.getSelectedPaths();
                ConfigurationNode parentPath = controller.getSelectedParentPath();

                CompoundEdit compoundEdit = new RemoveCompoundUndoableEdit();
                for (ConfigurationNode path : paths) {
                    compoundEdit.addEdit(removeLastPathComponent(path, parentPath));
                }
                compoundEdit.end();

                application.getUndoManager().addEdit(compoundEdit);
            }
        } else if(controller.getSelectedCallbackMethods().length > 0) {
            removeMethods(controller, dialog, getProjectController().getSelectedCallbackMethods());
        } else if(controller.getSelectedObjRelationships().length > 0) {
      		removeObjRelationships(controller, dialog, getProjectController().getSelectedObjRelationships());
        } else if(controller.getSelectedDbRelationships().length > 0) {
      		removeDBRelationships(controller, dialog, getProjectController().getSelectedDbRelationships());
        } else if(controller.getSelectedObjAttributes().length > 0) {
      		removeObjAttributes(controller, dialog, getProjectController().getSelectedObjAttributes());
        } else if(controller.getSelectedEmbeddableAttributes().length > 0) {
      		removeEmbAttributes(controller, dialog, getProjectController().getSelectedEmbeddableAttributes());
        } else if(controller.getSelectedDbAttributes().length > 0) {
        	removeDbAttributes(controller, dialog, getProjectController().getSelectedDbAttributes());
        } else if(controller.getSelectedProcedureParameters().length > 0) {
        	removeProcedureParameters(controller.getSelectedProcedure(), controller.getSelectedProcedureParameters());
        }

    }

    private void removeProcedureParameters(Procedure procedure, ProcedureParameter[] parameters) {
        ProjectController mediator = getProjectController();
        for (ProcedureParameter parameter : parameters) {
            procedure.removeCallParameter(parameter.getName());
            ProcedureParameterEvent e = new ProcedureParameterEvent(application.getFrameController().getView(), parameter, MapEvent.REMOVE);
            mediator.fireProcedureParameterEvent(e);
        }
    }
    
    private void removeEmbAttributes(ProjectController mediator, ConfirmRemoveDialog dialog,
                                     EmbeddableAttribute[] embAttrs) {
    	if (embAttrs != null && embAttrs.length > 0) {
        	if ((embAttrs.length == 1 && dialog.shouldDelete("DbAttribute", embAttrs[0].getName()))
                    || (embAttrs.length > 1 && dialog.shouldDelete("selected DbAttributes"))) {

        		Embeddable embeddable = mediator.getSelectedEmbeddable();

                application.getUndoManager()
                        .addEdit(new RemoveAttributeUndoableEdit(embeddable, embAttrs));

                for (EmbeddableAttribute attrib : embAttrs) {
                    embeddable.removeAttribute(attrib.getName());
                    EmbeddableAttributeEvent e = new EmbeddableAttributeEvent(application.getFrameController().getView(),
                            attrib, embeddable, MapEvent.REMOVE);
                    mediator.fireEmbeddableAttributeEvent(e);
                }

                ProjectUtil.cleanObjMappings(mediator.getSelectedDataMap());
        	}
    	}
	}

	private void removeObjAttributes(ProjectController mediator,
			ConfirmRemoveDialog dialog, ObjAttribute[] objAttrs) {
    	if (objAttrs != null && objAttrs.length > 0) {
        	if ((objAttrs.length == 1 && dialog.shouldDelete("DbAttribute", objAttrs[0].getName()))
                    || (objAttrs.length > 1 && dialog.shouldDelete("selected DbAttributes"))) {

        		ObjEntity entity = mediator.getSelectedObjEntity();

                application.getUndoManager()
                        .addEdit(new RemoveAttributeUndoableEdit(entity, objAttrs));

                for (ObjAttribute attrib : objAttrs) {
                    entity.removeAttribute(attrib.getName());
                    AttributeEvent e = new AttributeEvent(application.getFrameController().getView(), attrib, entity, MapEvent.REMOVE);
                    mediator.fireObjAttributeEvent(e);
                }

                ProjectUtil.cleanObjMappings(mediator.getSelectedDataMap());
        	}
    	}
	}

	private void removeDbAttributes(ProjectController controller, ConfirmRemoveDialog dialog, DbAttribute[] dbAttrs) {
    	if (dbAttrs != null && dbAttrs.length > 0) {
        	if ((dbAttrs.length == 1 && dialog.shouldDelete("DbAttribute", dbAttrs[0].getName()))
                    || (dbAttrs.length > 1 && dialog.shouldDelete("selected DbAttributes"))) {

        		DbEntity entity = controller.getSelectedDbEntity();

                application.getUndoManager()
                        .addEdit(new RemoveAttributeUndoableEdit(entity, dbAttrs));

                for (DbAttribute attrib : dbAttrs) {
                    entity.removeAttribute(attrib.getName());
                    AttributeEvent e = new AttributeEvent(application.getFrameController().getView(), attrib, entity, MapEvent.REMOVE);
                    controller.fireDbAttributeEvent(e);
                }

                ProjectUtil.cleanObjMappings(controller.getSelectedDataMap());
        	}
    	}
    }
    
    private void removeDBRelationships(ProjectController controller, ConfirmRemoveDialog dialog, DbRelationship[] dbRels) {
		if (dbRels != null && dbRels.length > 0) {
			if ((dbRels.length == 1 && dialog.shouldDelete("DbRelationship", dbRels[0].getName()))
					|| (dbRels.length > 1 && dialog.shouldDelete("selected DbRelationships"))) {
				DbEntity entity = controller.getSelectedDbEntity();
				
				for (DbRelationship rel : dbRels) {
					entity.removeRelationship(rel.getName());
					RelationshipEvent e = new RelationshipEvent(application.getFrameController().getView(), rel, entity, MapEvent.REMOVE);
					controller.fireDbRelationshipEvent(e);
				}

				ProjectUtil.cleanObjMappings(controller.getSelectedDataMap());
				Application.getInstance().getUndoManager().addEdit(new RemoveRelationshipUndoableEdit(entity, dbRels));
			}
		}
	}

	private void removeObjRelationships(ProjectController controller, ConfirmRemoveDialog dialog, ObjRelationship[] rels) {
		if ((rels.length == 1 && dialog.shouldDelete("ObjRelationship", rels[0].getName()))
				|| (rels.length > 1 && dialog.shouldDelete("selected ObjRelationships"))) {
			ObjEntity entity = controller.getSelectedObjEntity();
			for (ObjRelationship rel : rels) {
				entity.removeRelationship(rel.getName());
				RelationshipEvent e = new RelationshipEvent(application.getFrameController().getView(), rel, entity, MapEvent.REMOVE);
				controller.fireObjRelationshipEvent(e);
			}
			Application.getInstance().getUndoManager().addEdit(new RemoveRelationshipUndoableEdit(entity, rels));
		}		
	}

	private void removeMethods(ProjectController mediator, ConfirmRemoveDialog dialog, ObjCallbackMethod[] methods) {
    	CallbackMap callbackMap = mediator.getSelectedObjEntity().getCallbackMap();
    	CallbackType callbackType = mediator.getSelectedCallbackType();

        if ((methods.length == 1 && dialog.shouldDelete("callback method", methods[0].getName()))
        	|| (methods.length > 1 && dialog.shouldDelete("selected callback methods"))) {
            for (ObjCallbackMethod callbackMethod : methods) {
            	callbackMap.getCallbackDescriptor(callbackType.getType()).removeCallbackMethod(callbackMethod.getName());
                    
                CallbackMethodEvent ce = new CallbackMethodEvent(this, null,
                        callbackMethod.getName(),
                        MapEvent.REMOVE);
                    
                mediator.fireCallbackMethodEvent(ce);
            }
            
            Application.getInstance().getUndoManager()
                    .addEdit(new RemoveCallbackMethodUndoableEdit(callbackType, methods));
        }		
	}

	public void removeDataMap(DataMap map) {
        ProjectController mediator = getProjectController();
        DataChannelDescriptor domain = (DataChannelDescriptor) mediator.getProject().getRootNode();
        DataMapEvent e = new DataMapEvent(application.getFrameController().getView(), map, MapEvent.REMOVE);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

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
       
        mediator.fireDataMapEvent(e);
    }

    public void removeDataNode(DataNodeDescriptor node) {
        ProjectController mediator = getProjectController();
        DataChannelDescriptor domain = (DataChannelDescriptor) mediator.getProject().getRootNode();
        DataNodeEvent e = new DataNodeEvent(application.getFrameController().getView(), node, MapEvent.REMOVE);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        domain.getNodeDescriptors().remove(node);
        mediator.fireDataNodeEvent(e);
    }

    /**
     * Removes current DbEntity from its DataMap and fires "remove" EntityEvent.
     */
    public void removeDbEntity(DataMap map, DbEntity ent) {
        ProjectController mediator = getProjectController();

        EntityEvent e = new EntityEvent(application.getFrameController().getView(), ent, MapEvent.REMOVE);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        map.removeDbEntity(ent.getName(), true);
        mediator.fireDbEntityEvent(e);
    }

    /**
     * Removes current Query from its DataMap and fires "remove" QueryEvent.
     */
    public void removeQuery(DataMap map, QueryDescriptor query) {
        ProjectController mediator = getProjectController();

        QueryEvent e = new QueryEvent(application.getFrameController().getView(), query, MapEvent.REMOVE, map);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        map.removeQueryDescriptor(query.getName());
        mediator.fireQueryEvent(e);
    }

    /**
     * Removes current Procedure from its DataMap and fires "remove" ProcedureEvent.
     */
    public void removeProcedure(DataMap map, Procedure procedure) {
        ProjectController mediator = getProjectController();

        ProcedureEvent e = new ProcedureEvent(application.getFrameController().getView(), procedure, MapEvent.REMOVE);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        map.removeProcedure(procedure.getName());
        mediator.fireProcedureEvent(e);
    }

    /**
     * Removes current object entity from its DataMap.
     */
    public void removeObjEntity(DataMap map, ObjEntity entity) {
        ProjectController mediator = getProjectController();

        EntityEvent e = new EntityEvent(application.getFrameController().getView(), entity, MapEvent.REMOVE);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        map.removeObjEntity(entity.getName(), true);
        mediator.fireObjEntityEvent(e);

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
        ProjectController mediator = getProjectController();

        EmbeddableEvent e = new EmbeddableEvent(application.getFrameController().getView(), embeddable, MapEvent.REMOVE);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        map.removeEmbeddable(embeddable.getClassName());
        mediator.fireEmbeddableEvent(e, map);
    }

    public void removeDataMapFromDataNode(DataNodeDescriptor node, DataMap map) {
        ProjectController mediator = getProjectController();

        DataNodeEvent e = new DataNodeEvent(application.getFrameController().getView(), node);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        node.getDataMapNames().remove(map.getName());

        // Force reloading of the data node in the browse view
        mediator.fireDataNodeEvent(e);
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

        if (object instanceof DataMap) {
            if (parentObject instanceof DataNodeDescriptor) {
                undo = new RemoveUndoableEdit(application, (DataNodeDescriptor) parentObject, (DataMap) object);
                removeDataMapFromDataNode((DataNodeDescriptor) parentObject, (DataMap) object);
            } else {
                // Not under Data Node, remove completely
                undo = new RemoveUndoableEdit(application, (DataMap) object);
                removeDataMap((DataMap) object);
            }
        } else if (object instanceof DataNodeDescriptor) {
            undo = new RemoveUndoableEdit(application, (DataNodeDescriptor) object);
            removeDataNode((DataNodeDescriptor) object);
        } else if (object instanceof DbEntity) {
            undo = new RemoveUndoableEdit(((DbEntity) object).getDataMap(), (DbEntity) object);
            removeDbEntity(((DbEntity) object).getDataMap(), (DbEntity) object);
        } else if (object instanceof ObjEntity) {
            undo = new RemoveUndoableEdit(((ObjEntity) object).getDataMap(), (ObjEntity) object);
            removeObjEntity(((ObjEntity) object).getDataMap(), (ObjEntity) object);
        } else if (object instanceof QueryDescriptor) {
            undo = new RemoveUndoableEdit(((QueryDescriptor) object).getDataMap(), (QueryDescriptor) object);
            removeQuery(((QueryDescriptor) object).getDataMap(), (QueryDescriptor) object);
        } else if (object instanceof Procedure) {
            undo = new RemoveUndoableEdit(((Procedure) object).getDataMap(), (Procedure) object);
            removeProcedure(((Procedure) object).getDataMap(), (Procedure) object);
        } else if (object instanceof Embeddable) {
            undo = new RemoveUndoableEdit(((Embeddable) object).getDataMap(), (Embeddable) object);
            removeEmbeddable(((Embeddable) object).getDataMap(), (Embeddable) object);
        }

        return undo;
    }
}
