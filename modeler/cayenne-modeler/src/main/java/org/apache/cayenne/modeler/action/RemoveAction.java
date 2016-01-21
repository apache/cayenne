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

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.event.DataMapEvent;
import org.apache.cayenne.configuration.event.DataNodeEvent;
import org.apache.cayenne.configuration.event.ProcedureEvent;
import org.apache.cayenne.configuration.event.ProcedureParameterEvent;
import org.apache.cayenne.configuration.event.QueryEvent;
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
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.EmbeddableAttributeEvent;
import org.apache.cayenne.map.event.EmbeddableEvent;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ConfirmRemoveDialog;
import org.apache.cayenne.modeler.editor.CallbackType;
import org.apache.cayenne.modeler.editor.ObjCallbackMethod;
import org.apache.cayenne.modeler.event.CallbackMethodEvent;
import org.apache.cayenne.modeler.undo.RemoveAttributeUndoableEdit;
import org.apache.cayenne.modeler.undo.RemoveCallbackMethodUndoableEdit;
import org.apache.cayenne.modeler.undo.RemoveCompoundUndoableEdit;
import org.apache.cayenne.modeler.undo.RemoveRelationshipUndoableEdit;
import org.apache.cayenne.modeler.undo.RemoveUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.map.QueryDescriptor;

import javax.swing.KeyStroke;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

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

    protected RemoveAction(String actionName, Application application) {
        super(actionName, application);
    }

    @Override
    public String getIconName() {
        return "icon-trash.gif";
    }

    @Override
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit
                .getDefaultToolkit()
                .getMenuShortcutKeyMask());
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

        ProjectController mediator = getProjectController();

        ConfirmRemoveDialog dialog = getConfirmDeleteDialog(allowAsking);

        if(mediator.getCurrentCallbackMethods().length > 0) {
            removeMethods(mediator, dialog, getProjectController().getCurrentCallbackMethods());
        }
        else if(mediator.getCurrentObjRelationships().length > 0) {
      		removeObjRelationships(mediator, dialog, getProjectController().getCurrentObjRelationships());
        }
        else if(mediator.getCurrentDbRelationships().length > 0) {
      		removeDBRelationships(mediator, dialog, getProjectController().getCurrentDbRelationships());
        }
        else if(mediator.getCurrentObjAttributes().length > 0) {
      		removeObjAttributes(mediator, dialog, getProjectController().getCurrentObjAttributes());
        }
        else if(mediator.getCurrentEmbAttributes().length > 0) {
      		removeEmbAttributes(mediator, dialog, getProjectController().getCurrentEmbAttributes());
        }
        else if(mediator.getCurrentDbAttributes().length > 0) {
        	removeDbAttributes(mediator, dialog, getProjectController().getCurrentDbAttributes());
        }
        else if(mediator.getCurrentProcedureParameters().length > 0) {
        	removeProcedureParameters(mediator.getCurrentProcedure(), mediator.getCurrentProcedureParameters());
        }
        else if (mediator.getCurrentObjEntity() != null) {
            if (dialog
                    .shouldDelete("ObjEntity", mediator.getCurrentObjEntity().getName())) {
                application.getUndoManager().addEdit(
                        new RemoveUndoableEdit(mediator.getCurrentDataMap(), mediator
                                .getCurrentObjEntity()));
                removeObjEntity(mediator.getCurrentDataMap(), mediator
                        .getCurrentObjEntity());
            }
        }
        else if (mediator.getCurrentDbEntity() != null) {
            if (dialog.shouldDelete("DbEntity", mediator.getCurrentDbEntity().getName())) {
                application.getUndoManager().addEdit(
                        new RemoveUndoableEdit(mediator.getCurrentDataMap(), mediator
                                .getCurrentDbEntity()));
                removeDbEntity(mediator.getCurrentDataMap(), mediator
                        .getCurrentDbEntity());
            }
        }
        else if (mediator.getCurrentQuery() != null) {
            if (dialog.shouldDelete("query", mediator.getCurrentQuery().getName())) {
                application.getUndoManager().addEdit(
                        new RemoveUndoableEdit(mediator.getCurrentDataMap(), mediator
                                .getCurrentQuery()));
                removeQuery(mediator.getCurrentDataMap(), mediator.getCurrentQuery());
            }
        }
        else if (mediator.getCurrentProcedure() != null) {
            if (dialog
                    .shouldDelete("procedure", mediator.getCurrentProcedure().getName())) {

                application.getUndoManager().addEdit(
                        new RemoveUndoableEdit(mediator.getCurrentDataMap(), mediator
                                .getCurrentProcedure()));

                removeProcedure(mediator.getCurrentDataMap(), mediator
                        .getCurrentProcedure());

            }
        }
        else if (mediator.getCurrentEmbeddable() != null) {
            if (dialog.shouldDelete("embeddable", mediator
                    .getCurrentEmbeddable()
                    .getClassName())) {

                application.getUndoManager().addEdit(
                        new RemoveUndoableEdit(mediator.getCurrentDataMap(), mediator
                                .getCurrentEmbeddable()));

                removeEmbeddable(mediator.getCurrentDataMap(), mediator
                        .getCurrentEmbeddable());
            }
        }
        else if (mediator.getCurrentDataMap() != null) {
            if (dialog.shouldDelete("data map", mediator.getCurrentDataMap().getName())) {

                // In context of Data node just remove from Data Node
                if (mediator.getCurrentDataNode() != null) {
                    application.getUndoManager().addEdit(
                            new RemoveUndoableEdit(application, mediator
                                    .getCurrentDataNode(), mediator.getCurrentDataMap()));

                    removeDataMapFromDataNode(mediator.getCurrentDataNode(), mediator
                            .getCurrentDataMap());

                }
                else {
                    // Not under Data Node, remove completely
                    application.getUndoManager().addEdit(
                            new RemoveUndoableEdit(application, mediator
                                    .getCurrentDataMap()));

                    removeDataMap(mediator.getCurrentDataMap());

                }
            }
        }
        else if (mediator.getCurrentDataNode() != null) {
            if (dialog.shouldDelete("data node", mediator.getCurrentDataNode().getName())) {
                application
                        .getUndoManager()
                        .addEdit(
                                new RemoveUndoableEdit(application, mediator
                                        .getCurrentDataNode()));

                removeDataNode(mediator.getCurrentDataNode());
            }
        }

        else if (mediator.getCurrentPaths() != null) { // multiple deletion
            if (dialog.shouldDelete("selected objects")) {
                Object[] paths = mediator.getCurrentPaths();
                Object parentPath = mediator.getCurrentParentPath();

                CompoundEdit compoundEdit = new RemoveCompoundUndoableEdit();

                for (Object path : paths) {
                    compoundEdit.addEdit(removeLastPathComponent(path, parentPath));
                }
                compoundEdit.end();

                application.getUndoManager().addEdit(compoundEdit);

            }
        }

    }

    private void removeProcedureParameters(
            Procedure procedure,
            ProcedureParameter[] parameters) {
        ProjectController mediator = getProjectController();

        for (ProcedureParameter parameter : parameters) {

            procedure.removeCallParameter(parameter.getName());

            ProcedureParameterEvent e = new ProcedureParameterEvent(Application
                    .getFrame(), parameter, MapEvent.REMOVE);

            mediator.fireProcedureParameterEvent(e);
        }
    }
    
    private void removeEmbAttributes(ProjectController mediator,
			ConfirmRemoveDialog dialog,
			EmbeddableAttribute[] embAttrs) {
    	if (embAttrs != null && embAttrs.length > 0) {
        	if ((embAttrs.length == 1 && dialog.shouldDelete("DbAttribute", embAttrs[0]
        			.getName()))
                    || (embAttrs.length > 1 && dialog.shouldDelete("selected DbAttributes"))) {

        		Embeddable embeddable = mediator.getCurrentEmbeddable();

                application.getUndoManager().addEdit(
                		new RemoveAttributeUndoableEdit(
                                embeddable,
                                embAttrs));

                for (EmbeddableAttribute attrib : embAttrs) {
                    embeddable.removeAttribute(attrib.getName());
                    EmbeddableAttributeEvent e = new EmbeddableAttributeEvent(Application
                            .getFrame(), attrib, embeddable, MapEvent.REMOVE);
                    mediator.fireEmbeddableAttributeEvent(e);
                }

                ProjectUtil.cleanObjMappings(mediator.getCurrentDataMap());
        	}
    	}
	}

	private void removeObjAttributes(ProjectController mediator,
			ConfirmRemoveDialog dialog, ObjAttribute[] objAttrs) {
    	if (objAttrs != null && objAttrs.length > 0) {
        	if ((objAttrs.length == 1 && dialog.shouldDelete("DbAttribute", objAttrs[0]
        			.getName()))
                    || (objAttrs.length > 1 && dialog.shouldDelete("selected DbAttributes"))) {

        		ObjEntity entity = mediator.getCurrentObjEntity();

                application.getUndoManager().addEdit(
                        new RemoveAttributeUndoableEdit(
                                (DataChannelDescriptor)mediator.getProject().getRootNode(),
                                mediator.getCurrentDataMap(),
                                entity,
                                objAttrs));

                for (ObjAttribute attrib : objAttrs) {
                    entity.removeAttribute(attrib.getName());

                    AttributeEvent e = new AttributeEvent(
                            Application.getFrame(),
                            attrib,
                            entity,
                            MapEvent.REMOVE);

                    mediator.fireObjAttributeEvent(e);
                }

                ProjectUtil.cleanObjMappings(mediator.getCurrentDataMap());
        	}
    	}
	}

	private void removeDbAttributes(ProjectController mediator,
			ConfirmRemoveDialog dialog,
			DbAttribute[] dbAttrs) {
    	if (dbAttrs != null && dbAttrs.length > 0) {
        	if ((dbAttrs.length == 1 && dialog.shouldDelete("DbAttribute", dbAttrs[0]
        			.getName()))
                    || (dbAttrs.length > 1 && dialog.shouldDelete("selected DbAttributes"))) {

        		DbEntity entity = mediator.getCurrentDbEntity();

                application.getUndoManager().addEdit(
                		new RemoveAttributeUndoableEdit(
                				(DataChannelDescriptor)mediator.getProject().getRootNode(),
                                mediator.getCurrentDataMap(),
                                entity,
                                dbAttrs));

                for (DbAttribute attrib : dbAttrs) {
                    entity.removeAttribute(attrib.getName());

                    AttributeEvent e = new AttributeEvent(
                            Application.getFrame(),
                            attrib,
                            entity,
                            MapEvent.REMOVE);

                    mediator.fireDbAttributeEvent(e);
                }

                ProjectUtil.cleanObjMappings(mediator.getCurrentDataMap());
        	}
    	}
    }
    
    private void removeDBRelationships(ProjectController mediator,
			ConfirmRemoveDialog dialog,
			DbRelationship[] dbRels) {
		if (dbRels != null && dbRels.length > 0) {
			if ((dbRels.length == 1 && dialog.shouldDelete(
					"DbRelationship", dbRels[0].getName()))
					|| (dbRels.length > 1 && dialog
							.shouldDelete("selected DbRelationships"))) {
				DbEntity entity = mediator.getCurrentDbEntity();
				
				for (DbRelationship rel : dbRels) {
					entity.removeRelationship(rel.getName());

					RelationshipEvent e = new RelationshipEvent(Application.getFrame(),
							rel, entity, MapEvent.REMOVE);
					mediator.fireDbRelationshipEvent(e);
				}

				ProjectUtil.cleanObjMappings(mediator.getCurrentDataMap());
				
				Application.getInstance().getUndoManager().addEdit(
						new RemoveRelationshipUndoableEdit(entity, dbRels));
			}
		}
	}

	private void removeObjRelationships(ProjectController mediator,
			ConfirmRemoveDialog dialog,
			ObjRelationship[] rels) {
		if ((rels.length == 1 && dialog.shouldDelete("ObjRelationship",
				rels[0].getName()))
				|| (rels.length > 1 && dialog
						.shouldDelete("selected ObjRelationships"))) {
			ObjEntity entity = mediator.getCurrentObjEntity();
			for (ObjRelationship rel : rels) {
				entity.removeRelationship(rel.getName());
				RelationshipEvent e = new RelationshipEvent(Application.getFrame(),
						rel, entity, MapEvent.REMOVE);
				mediator.fireObjRelationshipEvent(e);
			}
			Application.getInstance().getUndoManager().addEdit(
					new RemoveRelationshipUndoableEdit(entity, rels));
		}		
	}

	private void removeMethods(ProjectController mediator,
			ConfirmRemoveDialog dialog, ObjCallbackMethod[] methods) {
    	CallbackMap callbackMap = mediator.getCurrentObjEntity().getCallbackMap();
    	CallbackType callbackType = mediator.getCurrentCallbackType();

        if ((methods.length == 1 && dialog.shouldDelete("callback method", methods[0].getName()))
        	|| (methods.length > 1 && dialog.shouldDelete("selected callback methods"))) {
            for (ObjCallbackMethod callbackMethod : methods) {
            	callbackMap.getCallbackDescriptor(callbackType.getType()).removeCallbackMethod(callbackMethod.getName());
                    
                CallbackMethodEvent ce = new CallbackMethodEvent(
                        this,
                        null,
                        callbackMethod.getName(),
                        MapEvent.REMOVE);
                    
                mediator.fireCallbackMethodEvent(ce);
            }
            
            Application.getInstance().getUndoManager().addEdit( 
            		new RemoveCallbackMethodUndoableEdit(callbackType, methods));
        }		
	}

	public void removeDataMap(DataMap map) {
        ProjectController mediator = getProjectController();
        DataChannelDescriptor domain = (DataChannelDescriptor) mediator
                .getProject()
                .getRootNode();
        DataMapEvent e = new DataMapEvent(Application.getFrame(), map, MapEvent.REMOVE);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        domain.getDataMaps().remove(map);
        if (map.getConfigurationSource() != null) {
            URL mapURL = map.getConfigurationSource().getURL();
            Collection<URL> unusedResources = getCurrentProject().getUnusedResources();
            unusedResources.add(mapURL);
            if (map.getReverseEngineering() != null && map.getReverseEngineering().getConfigurationSource() != null) {
                URL reverseEngineeringURL = map.getReverseEngineering().getConfigurationSource().getURL();
                unusedResources.add(reverseEngineeringURL);
            }
        }
        
        Iterator<DataNodeDescriptor> iterator = domain.getNodeDescriptors().iterator();
        while(iterator.hasNext()){
            DataNodeDescriptor node = iterator.next();
            if(node.getDataMapNames().contains(map.getName())){
                removeDataMapFromDataNode(node, map);
            }
        }
       
        mediator.fireDataMapEvent(e);
    }

    public void removeDataNode(DataNodeDescriptor node) {
        ProjectController mediator = getProjectController();
        DataChannelDescriptor domain = (DataChannelDescriptor) mediator
                .getProject()
                .getRootNode();
        DataNodeEvent e = new DataNodeEvent(Application.getFrame(), node, MapEvent.REMOVE);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        domain.getNodeDescriptors().remove(node);
        mediator.fireDataNodeEvent(e);
    }

    /**
     * Removes current DbEntity from its DataMap and fires "remove" EntityEvent.
     */
    public void removeDbEntity(DataMap map, DbEntity ent) {
        ProjectController mediator = getProjectController();

        EntityEvent e = new EntityEvent(Application.getFrame(), ent, MapEvent.REMOVE);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        map.removeDbEntity(ent.getName(), true);
        mediator.fireDbEntityEvent(e);
    }

    /**
     * Removes current Query from its DataMap and fires "remove" QueryEvent.
     */
    public void removeQuery(DataMap map, QueryDescriptor query) {
        ProjectController mediator = getProjectController();

        QueryEvent e = new QueryEvent(Application.getFrame(), query, MapEvent.REMOVE, map);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        map.removeQueryDescriptor(query.getName());
        mediator.fireQueryEvent(e);
    }

    /**
     * Removes current Procedure from its DataMap and fires "remove" ProcedureEvent.
     */
    public void removeProcedure(DataMap map, Procedure procedure) {
        ProjectController mediator = getProjectController();

        ProcedureEvent e = new ProcedureEvent(
                Application.getFrame(),
                procedure,
                MapEvent.REMOVE);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        map.removeProcedure(procedure.getName());
        mediator.fireProcedureEvent(e);
    }

    /**
     * Removes current object entity from its DataMap.
     */
    public void removeObjEntity(DataMap map, ObjEntity entity) {
        ProjectController mediator = getProjectController();

        EntityEvent e = new EntityEvent(Application.getFrame(), entity, MapEvent.REMOVE);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        map.removeObjEntity(entity.getName(), true);
        mediator.fireObjEntityEvent(e);

        // remove queries that depend on entity
        // TODO: (Andrus, 09/09/2005) show warning dialog?

        // clone to be able to remove within iterator...
        for (QueryDescriptor query : new ArrayList<>(map.getQueryDescriptors())) {
            if (!QueryDescriptor.EJBQL_QUERY.equals(query.getType())) {
                Object root = query.getRoot();

                if (root == entity
                        || (root instanceof String && root.toString().equals(
                                entity.getName()))) {
                    removeQuery(map, query);
                }
            }
        }
    }

    public void removeEmbeddable(DataMap map, Embeddable embeddable) {
        ProjectController mediator = getProjectController();

        EmbeddableEvent e = new EmbeddableEvent(
                Application.getFrame(),
                embeddable,
                MapEvent.REMOVE);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        map.removeEmbeddable(embeddable.getClassName());
        mediator.fireEmbeddableEvent(e, map);
    }

    public void removeDataMapFromDataNode(DataNodeDescriptor node, DataMap map) {
        ProjectController mediator = getProjectController();

        DataNodeEvent e = new DataNodeEvent(Application.getFrame(), node);
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
        if (object == null) {
            return false;
        }

        if (object instanceof DataChannelDescriptor) {
            return true;
        }
        else if (object instanceof DataMap) {
            return true;
        }
        else if (object instanceof DataNodeDescriptor) {
            return true;
        }
        else if (object instanceof Entity) {
            return true;
        }
        else if (object instanceof Attribute) {
            return true;
        }
        else if (object instanceof Relationship) {
            return true;
        }
        else if (object instanceof Procedure) {
            return true;
        }
        else if (object instanceof ProcedureParameter) {
            return true;
        }
        else if (object instanceof Embeddable) {
            return true;
        }
        else if (object instanceof EmbeddableAttribute) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Removes an object, depending on its type
     */
    private UndoableEdit removeLastPathComponent(Object object, Object parentObject) {

        UndoableEdit undo = null;

        if (object instanceof DataMap) {
            if (parentObject != null && parentObject instanceof DataNodeDescriptor) {
                undo = new RemoveUndoableEdit(application, (DataNodeDescriptor) parentObject, (DataMap) object);
                removeDataMapFromDataNode((DataNodeDescriptor) parentObject, (DataMap) object);
            } else {
                // Not under Data Node, remove completely
                undo = new RemoveUndoableEdit(application, (DataMap) object);
                removeDataMap((DataMap) object);
            }
        }
        else if (object instanceof DataNodeDescriptor) {
            undo = new RemoveUndoableEdit(application, (DataNodeDescriptor) object);

            removeDataNode((DataNodeDescriptor) object);
        }
        else if (object instanceof DbEntity) {
            undo = new RemoveUndoableEdit(
                    ((DbEntity) object).getDataMap(),
                    (DbEntity) object);

            removeDbEntity(((DbEntity) object).getDataMap(), (DbEntity) object);
        }
        else if (object instanceof ObjEntity) {
            undo = new RemoveUndoableEdit(
                    ((ObjEntity) object).getDataMap(),
                    (ObjEntity) object);

            removeObjEntity(((ObjEntity) object).getDataMap(), (ObjEntity) object);
        }
        else if (object instanceof QueryDescriptor) {
            undo = new RemoveUndoableEdit(((Query) object).getDataMap(), (QueryDescriptor) object);

            removeQuery(((Query) object).getDataMap(), (QueryDescriptor) object);
        }
        else if (object instanceof Procedure) {
            undo = new RemoveUndoableEdit(
                    ((Procedure) object).getDataMap(),
                    (Procedure) object);

            removeProcedure(((Procedure) object).getDataMap(), (Procedure) object);
        }
        else if (object instanceof Embeddable) {
            undo = new RemoveUndoableEdit(
                    ((Embeddable) object).getDataMap(),
                    (Embeddable) object);
            removeEmbeddable(((Embeddable) object).getDataMap(), (Embeddable) object);
        }

        return undo;
    }
}
