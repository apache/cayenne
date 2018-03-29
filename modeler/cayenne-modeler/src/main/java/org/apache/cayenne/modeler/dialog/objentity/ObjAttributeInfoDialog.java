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
package org.apache.cayenne.modeler.dialog.objentity;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.editor.ObjAttributeTableModel;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.EntityTreeAttributeRelationshipFilter;
import org.apache.cayenne.modeler.util.EntityTreeModel;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.util.CayenneMapEntry;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ObjAttributeInfoDialog extends CayenneController implements TreeSelectionListener {

	private ObjAttributeTableModel model;
	private OverrideEmbeddableAttributeTableModel embeddableModel;
	private int row;
	protected ObjAttributeInfoDialogView view;
	protected ObjAttribute attribute;
	protected ObjAttribute attributeSaved;

	protected List<DbEntity> relTargets;

	protected Map<String, Embeddable> stringToEmbeddables;
	protected List<String> embeddableNames;

	protected ProjectController mediator;
	private Object lastObjectType;

	public ObjAttributeInfoDialog(ProjectController mediator, int row, ObjAttributeTableModel model) {
		super(mediator);
		this.view = new ObjAttributeInfoDialogView(mediator);
		this.mediator = mediator;
		this.model = model;
		this.row = row;
		this.stringToEmbeddables = new HashMap<>();
		this.embeddableNames = new ArrayList<>();

		for (Embeddable emb : mediator.getEmbeddablesInCurrentDataDomain()) {
			stringToEmbeddables.put(emb.getClassName(), emb);
			embeddableNames.add(emb.getClassName());
		}
		initController(model.getAttribute(row).getValue());
	}

	@Override
	public Component getView() {
		return view;
	}

	/**
	 * Starts options dialog.
	 */
	public void startupAction() {
		view.pack();
		view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		view.setModal(true);
		makeCloseableOnEscape();
		centerView();
		view.setVisible(true);
	}

	private void initController(ObjAttribute attr) {

		for (String embeddableName : embeddableNames) {
			((DefaultComboBoxModel) view.getTypeComboBox().getModel()).addElement(embeddableName);
		}

		this.attribute = attr;

		if (attribute instanceof EmbeddedAttribute || embeddableNames.contains(attribute.getType())) {
			this.attributeSaved = new EmbeddedAttribute();
		} else {
			this.attributeSaved = new ObjAttribute();
		}

		copyObjAttribute(attributeSaved, attribute);

		relTargets = new ArrayList<DbEntity>(attribute.getEntity().getDataMap().getDbEntities());

		/*
		 * Register auto-selection of the target
		 */
		view.getPathBrowser().addTreeSelectionListener(this);

		view.getAttributeName().setText(attribute.getName());
		if (attribute.getDbAttributePath() != null) {
			if (attribute.getDbAttributePath().contains(".")) {
				String path = attribute.getDbAttributePath();
				view.getCurrentPathLabel().setText(path.replace(".", " -> "));
			} else {
				view.getCurrentPathLabel().setText(attribute.getDbAttributePath());
			}
		} else {
			view.getCurrentPathLabel().setText("");
		}
		view.getSourceEntityLabel().setText(attribute.getEntity().getName());

		view.getTypeComboBox().setSelectedItem(attribute.getType());

		BindingBuilder builder = new BindingBuilder(getApplication().getBindingFactory(), this);
		builder.bindToAction(view.getCancelButton(), "closeAction()");
		builder.bindToAction(view.getSelectPathButton(), "setPath(true)");
		builder.bindToAction(view.getSaveButton(), "saveMapping()");

		/*
		 * set filter for ObjAttributePathBrowser
		 */
		if (view.getPathBrowser().getModel() == null) {
			Entity firstEntity = null;
			if (attribute.getDbAttribute() == null) {

				if (attribute.getParent() instanceof ObjEntity) {
					DbEntity dbEnt = ((ObjEntity) attribute.getParent()).getDbEntity();

					if (dbEnt != null) {
						Collection<DbAttribute> attrib = dbEnt.getAttributes();
						Collection<DbRelationship> rel = dbEnt.getRelationships();

						if (attrib.size() > 0) {
							Iterator<DbAttribute> iter = attrib.iterator();
							firstEntity = iter.next().getEntity();
						} else if (rel.size() > 0) {
							Iterator<DbRelationship> iter = rel.iterator();
							firstEntity = iter.next().getSourceEntity();
						}
					}
				}
			} else {
				firstEntity = getFirstEntity();
			}

			if (firstEntity != null) {
				EntityTreeModel treeModel = new EntityTreeModel(firstEntity);
				treeModel.setFilter(new EntityTreeAttributeRelationshipFilter());
				view.getPathBrowser().setModel(treeModel);
			}
		}

		if (attribute.getDbAttribute() != null) {
			setSelectionPath();
		}

		view.getTypeComboBox().addItemListener(e -> {
            if (lastObjectType != null) {
                if (!lastObjectType.equals(e.getItemSelectable())) {

                    if (embeddableNames.contains(e.getItemSelectable().getSelectedObjects()[0].toString())) {
                        if (attributeSaved instanceof ObjAttribute) {
                            EmbeddedAttribute copyAttrSaved = new EmbeddedAttribute();
                            copyObjAttribute(copyAttrSaved, attributeSaved);
                            attributeSaved = copyAttrSaved;
                        }
                    } else {
                        if (attributeSaved instanceof EmbeddedAttribute) {
                            ObjAttribute copyAttrSaved = new ObjAttribute();
                            copyObjAttribute(copyAttrSaved, attributeSaved);
                            attributeSaved = copyAttrSaved;
                        }
                    }

                    attributeSaved.setType(e.getItemSelectable().getSelectedObjects()[0].toString());
                    rebuildTable();
                    setEnabledSaveButton();
                }
            }
        });

		view.getAttributeName().addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent e) {
				if (!view.getAttributeName().getText().equals(attribute.getName())) {
					setEnabledSaveButton();
				}
			}

			public void keyReleased(KeyEvent e) {
				if (!view.getAttributeName().getText().equals(attribute.getName())) {
					setEnabledSaveButton();
				}
			}

			public void keyTyped(KeyEvent e) {
			}
		});

		rebuildTable();

	}

	private void setEnabledSaveButton() {
		if (!attribute.getDbPathIterator().hasNext()) {
			view.getSaveButton().setEnabled(true);
		} else {
			boolean isAttributeLast = false;
			Iterator<CayenneMapEntry> it = attribute.getDbPathIterator();
			while (it.hasNext()) {
				Object obj = it.next();
				if (obj instanceof DbAttribute && !it.hasNext()) {
					isAttributeLast = true;
				}
			}
			view.getSaveButton().setEnabled(isAttributeLast);
		}
	}

	private void setUpTableStructure() {

		DefaultTableCellRenderer renderer = new CellRenderer();

		TableColumn nameColumn = view.getOverrideAttributeTable().getColumnModel()
				.getColumn(OverrideEmbeddableAttributeTableModel.OBJ_ATTRIBUTE);
		nameColumn.setCellRenderer(renderer);

		TableColumn typeColumn = view.getOverrideAttributeTable().getColumnModel()
				.getColumn(OverrideEmbeddableAttributeTableModel.OBJ_ATTRIBUTE_TYPE);
		typeColumn.setCellRenderer(renderer);

		TableColumn dbAttrColumn = view.getOverrideAttributeTable().getColumnModel()
				.getColumn(OverrideEmbeddableAttributeTableModel.DB_ATTRIBUTE);
		dbAttrColumn.setCellRenderer(renderer);

		TableColumn dbAttrTypeColumn = view.getOverrideAttributeTable().getColumnModel()
				.getColumn(OverrideEmbeddableAttributeTableModel.DB_ATTRIBUTE_TYPE);
		dbAttrTypeColumn.setCellRenderer(renderer);

		view.getTablePreferences().bind(view.getOverrideAttributeTable(), null, null, null,
				OverrideEmbeddableAttributeTableModel.OBJ_ATTRIBUTE, true);

		initComboBoxes();

	}

	private void initComboBoxes() {
		Collection<String> nameAttr = null;
		if (attributeSaved != null) {
			DbEntity currentEnt = ((ObjEntity) attributeSaved.getEntity()).getDbEntity();

			if (currentEnt != null) {
				nameAttr = ModelerUtil.getDbAttributeNames(mediator, currentEnt);
				embeddableModel.setCellEditor(nameAttr, view.getOverrideAttributeTable());
				embeddableModel.setComboBoxes(
						nameAttr,
						view.getOverrideAttributeTable().convertColumnIndexToView(
								OverrideEmbeddableAttributeTableModel.DB_ATTRIBUTE));
			}
		}
	}

	private void rebuildTable() {
		String typeName = null;
		Collection<EmbeddableAttribute> embAttrTempCopy = new ArrayList<>();

		if (attributeSaved.getType() != null) {
			typeName = attributeSaved.getType();
		}
		if (embeddableNames.contains(typeName)) {

			Collection<EmbeddableAttribute> embAttrTemp = stringToEmbeddables.get(typeName).getAttributes();
			for (EmbeddableAttribute temp : embAttrTemp) {
				EmbeddableAttribute at = new EmbeddableAttribute();
				at.setDbAttributeName(temp.getDbAttributeName());
				at.setName(temp.getName());
				at.setType(temp.getType());
				at.setEmbeddable(temp.getEmbeddable());
				embAttrTempCopy.add(at);
			}
		}

		embeddableModel = new OverrideEmbeddableAttributeTableModel(mediator, this, embAttrTempCopy, attributeSaved);

		view.getOverrideAttributeTable().setModel(embeddableModel);
		view.getOverrideAttributeTable().setRowHeight(25);
		view.getOverrideAttributeTable().setRowMargin(3);

		setUpTableStructure();

		if (view.getTypeComboBox().getSelectedItem() == null) {
			lastObjectType = "";
		} else {
			lastObjectType = view.getTypeComboBox().getSelectedItem();
		}
	}

	public void closeAction() {
		view.dispose();
	}

	public boolean setPath(boolean isChange) {

		if (isChange()) {
			attributeSaved.setType(view.getTypeComboBox().getSelectedItem().toString());
			attributeSaved.setName(view.getAttributeName().getText());
		}

		if (!(attributeSaved instanceof EmbeddedAttribute) || isRegistredType(attributeSaved.getType())) {

			StringBuilder attributePath = new StringBuilder();
			StringBuilder pathStr = new StringBuilder();
			if (attribute.getEntity().getDbEntity() != null) {
				TreePath path = view.getPathBrowser().getSelectionPath();

				if (path.getLastPathComponent() instanceof DbAttribute) {
					Object[] pathComponents = path.getPath();
					for (int i = 0; i < pathComponents.length; i++) {
						boolean attrOrRel = true;
						if (pathComponents[i] instanceof DbAttribute) {
							pathStr.append(((DbAttribute) pathComponents[i]).getName());
							attributePath.append(((DbAttribute) pathComponents[i]).getName());
						} else if (pathComponents[i] instanceof DbRelationship) {
							pathStr.append(((DbRelationship) pathComponents[i]).getName());
							attributePath.append(((DbRelationship) pathComponents[i]).getName());
						} else {
							attrOrRel = false;
						}

						if (i != pathComponents.length - 1 && attrOrRel) {
							pathStr.append(" -> ");
							attributePath.append(".");
						}
					}
				}
			} else {
				view.getCurrentPathLabel().setText("");
			}

			view.getCurrentPathLabel().setText(pathStr.toString());

			if (attribute.getDbAttributePath() != null
					&& !embeddableNames.contains(view.getTypeComboBox().getSelectedItem().toString())) {
				if (!attribute.getDbAttributePath().equals(attributePath.toString())) {
					attributeSaved.setDbAttributePath(attributePath.toString());

					if (!attribute.getDbAttributePath().equals(attributePath.toString()) && isChange) {
						model.setUpdatedValueAt(attributeSaved.getDbAttributePath(), row, 3);
					}
					return true;
				}
			} else {
				if (attributePath.length() > 0
						|| (attribute instanceof EmbeddedAttribute && !(attributeSaved instanceof EmbeddedAttribute))) {

					attributeSaved.setDbAttributePath(attributePath.toString());
					if (attributePath.length() == 0) {
						model.setUpdatedValueAt(attributeSaved.getDbAttributePath(), row, 3);
						return false;
					}
					return true;
				}
			}
		}
		return false;
	}

	public boolean isChange() {

		boolean isOverrideTableChange = ((OverrideEmbeddableAttributeTableModel) view.getOverrideAttributeTable()
				.getModel()).isAttributeOverrideChange();
		return isOverrideTableChange || !attribute.getName().equals(view.getAttributeName().getText())
				|| (attribute.getType() == null && view.getTypeComboBox().getSelectedItem().toString() != null)
				|| !attribute.getType().equals(view.getTypeComboBox().getSelectedItem().toString());
	}

	public void saveMapping() {

		if (setPath(false)) {

			if (JOptionPane.showConfirmDialog(getView(),
					"You have changed Db Attribute path. Do you want it to be saved?", "Save ObjAttribute",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

				if (attribute instanceof EmbeddedAttribute) {
					changeAttributeObject();
				} else {
					model.setUpdatedValueAt(attributeSaved.getName(), row, 1);
					model.setUpdatedValueAt(attributeSaved.getType(), row, 2);
				}

				model.setUpdatedValueAt(attributeSaved.getDbAttributePath(), row, 3);
			} else {
				model.setUpdatedValueAt(attributeSaved.getName(), row, 1);
				model.setUpdatedValueAt(attributeSaved.getType(), row, 2);
			}
		} else {
			if ((attributeSaved instanceof EmbeddedAttribute && !(attribute instanceof EmbeddedAttribute))
					|| (!(attributeSaved instanceof EmbeddedAttribute) && attribute instanceof EmbeddedAttribute)) {
				changeAttributeObject();
			} else {
				if (attributeSaved instanceof EmbeddedAttribute && embeddableModel.isAttributeOverrideChange()) {
					Map<String, String> overrides = ((EmbeddedAttribute) attributeSaved).getAttributeOverrides();
					Map<String, String> currentOverrAttr = getCurrentOverrideAttribute();

					compareAndSetOverrideInEmbeddedAttribute(attributeSaved, overrides, currentOverrAttr);
				}

				model.setUpdatedValueAt(attributeSaved.getName(), row, 1);
				model.setUpdatedValueAt(attributeSaved.getType(), row, 2);
				model.setUpdatedValueAt(attributeSaved.getDbAttributePath(), row, 3);
			}

			if (attributeSaved instanceof EmbeddedAttribute && attribute instanceof EmbeddedAttribute) {

				model.setUpdatedValueAt(attributeSaved.getDbAttributePath(), row, 3);
				if (embeddableModel.isAttributeOverrideChange()) {
					Map<String, String> overrides;
					overrides = ((EmbeddedAttribute) attribute).getAttributeOverrides();
					Map<String, String> currentOverrAttr = ((EmbeddedAttribute) attributeSaved).getAttributeOverrides();

					compareAndSetOverrideInEmbeddedAttribute(attribute, overrides, currentOverrAttr);
				}
			}
		}
		closeAction();
	}

	private void changeAttributeObject() {

		if (attributeSaved instanceof EmbeddedAttribute && embeddableModel.isAttributeOverrideChange()) {
			Map<String, String> overrides = ((EmbeddedAttribute) attributeSaved).getAttributeOverrides();
			Map<String, String> currentOverrAttr = getCurrentOverrideAttribute();
			compareAndSetOverrideInEmbeddedAttribute(attributeSaved, overrides, currentOverrAttr);
		}
		if (attributeSaved instanceof EmbeddedAttribute) {
			attributeSaved.setDbAttributePath(null);
			model.setUpdatedValueAt(attributeSaved.getDbAttributePath(), row, 3);
		}

		model.getEntity().removeAttribute(attribute.getName());
		model.getEntity().addAttribute(attributeSaved);

		mediator.fireObjEntityEvent(new EntityEvent(this, model.getEntity(), MapEvent.CHANGE));

		EntityDisplayEvent event = new EntityDisplayEvent(this, mediator.getCurrentObjEntity(),
				mediator.getCurrentDataMap(), (DataChannelDescriptor) mediator.getProject().getRootNode());

		mediator.fireObjEntityDisplayEvent(event);

		mediator.fireObjAttributeEvent(new AttributeEvent(this, attributeSaved, model.getEntity(), MapEvent.CHANGE));

		AttributeDisplayEvent eventAttr = new AttributeDisplayEvent(this, attributeSaved,
				mediator.getCurrentObjEntity(), mediator.getCurrentDataMap(), (DataChannelDescriptor) mediator
						.getProject().getRootNode());

		mediator.fireObjAttributeDisplayEvent(eventAttr);

	}

	public Map<String, String> getCurrentOverrideAttribute() {
		Map<String, String> currentEmbeddableOverrite = new HashMap<>();
		Collection<EmbeddableAttribute> embList = embeddableModel.getEmbeddableList();
		Embeddable emb = stringToEmbeddables.get(attributeSaved.getType());
		for (EmbeddableAttribute e : embList) {
			if ((emb.getAttribute(e.getName()).getDbAttributeName() == null && e.getDbAttributeName() != null)
					|| (emb.getAttribute(e.getName()).getDbAttributeName() != null && !emb.getAttribute(e.getName())
					.getDbAttributeName().equals(e.getDbAttributeName()))) {
				currentEmbeddableOverrite.put(e.getName(), e.getDbAttributeName());
			}
		}
		return currentEmbeddableOverrite;
	}

	public void valueChanged(TreeSelectionEvent e) {
	}

	private Entity getFirstEntity() {
		Iterator<CayenneMapEntry> it = attribute.getDbPathIterator();
		Entity firstEnt = attribute.getDbAttribute().getEntity();
		boolean setEnt = false;

		while (it.hasNext()) {
			Object ob = it.next();
			if (ob instanceof DbRelationship) {
				if (!setEnt) {
					firstEnt = ((DbRelationship) ob).getSourceEntity();
					setEnt = true;
				}
			} else if (ob instanceof DbAttribute) {
				if (!setEnt) {
					firstEnt = ((DbAttribute) ob).getEntity();
				}
			}
		}

		return firstEnt;
	}

	/**
	 * Selects path in browser
	 */
	public void setSelectionPath() {
		List<CayenneMapEntry> list = new ArrayList<>();
		boolean isAttributeLast = false;
		Iterator<CayenneMapEntry> it = attribute.getDbPathIterator();
		while (it.hasNext()) {
			CayenneMapEntry obj = it.next();
			list.add(obj);
			if (obj instanceof DbAttribute && !it.hasNext()) {
				isAttributeLast = true;
			}
		}
		if (isAttributeLast) {
			Object[] path = new Object[list.size() + 1];
			path[0] = getFirstEntity();
			System.arraycopy(list.toArray(), 0, path, 1, list.size());
			view.getPathBrowser().setSelectionPath(new TreePath(path));
			view.getSaveButton().setEnabled(true);
		}
	}

	public boolean isRegistredType(String typeName) {
		boolean isType = false;
		String[] typeNames = ModelerUtil.getRegisteredTypeNames();
		for (String nextTypeName : typeNames) {
			if (nextTypeName.equals(typeName)) {
				isType = true;
			}
		}
		return isType;
	}

	// custom renderer used for inherited attributes highlighting
	final class CellRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {

			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			OverrideEmbeddableAttributeTableModel model = (OverrideEmbeddableAttributeTableModel) table.getModel();

			if (!model.isCellEditable(row, column)) {
				setForeground(Color.GRAY);
			} else {
				setForeground(isSelected && !hasFocus ? table.getSelectionForeground() : table.getForeground());
			}
			setBackground(isSelected && !hasFocus ? table.getSelectionBackground() : table.getBackground());

			return this;
		}
	}

	private void copyObjAttribute(ObjAttribute attributeSaved, ObjAttribute attribute) {
		attributeSaved.setDbAttributePath(attribute.getDbAttributePath());
		attributeSaved.setName(attribute.getName());
		attributeSaved.setEntity(attribute.getEntity());
		attributeSaved.setParent(attribute.getParent());
		attributeSaved.setType(attribute.getType());
		attributeSaved.setUsedForLocking(attribute.isUsedForLocking());

		if (attributeSaved instanceof EmbeddedAttribute) {
			Map<String, String> attrOverrides;
			if (attribute instanceof EmbeddedAttribute) {
				attrOverrides = ((EmbeddedAttribute) attribute).getAttributeOverrides();
			} else {
				attrOverrides = new HashMap<>();
			}
			if (attrOverrides.size() > 0) {
				for (Map.Entry<String, String> attrOv : attrOverrides.entrySet()) {
					((EmbeddedAttribute) attributeSaved).addAttributeOverride(attrOv.getKey(), attrOv.getValue());
				}
			}
		}
	}

	private void compareAndSetOverrideInEmbeddedAttribute(ObjAttribute attribute, Map<String, String> overrides,
			Map<String, String> currentOverrAttr) {
		ArrayList<String> keysForDelete = new ArrayList<>();
		ArrayList<String> keysForAdd = new ArrayList<>();

		for (Object o : overrides.entrySet()) {
			Map.Entry obj = (Map.Entry) o;

			String key = (String) obj.getKey();
			if (currentOverrAttr.get(key) == null || !(obj.getValue().equals(currentOverrAttr.get(key)))) {
				keysForDelete.add(key);
			}
		}

		for (Object o : currentOverrAttr.entrySet()) {
			Map.Entry obj = (Map.Entry) o;
			String key = (String) obj.getKey();
			if (overrides.get(key) == null || !(obj.getValue().equals(overrides.get(key)))) {
				keysForAdd.add(key);
			}
		}

		for (String aKeysForDelete : keysForDelete) {
			((EmbeddedAttribute) attribute).removeAttributeOverride(aKeysForDelete);
		}
		for (String key : keysForAdd) {
			((EmbeddedAttribute) attribute).addAttributeOverride(key, currentOverrAttr.get(key));
		}
	}
}
