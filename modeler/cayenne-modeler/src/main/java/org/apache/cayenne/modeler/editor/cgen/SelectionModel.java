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

package org.apache.cayenne.modeler.editor.cgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;

/**
 * @since 4.2
 */
class SelectionModel {
    private Set<String> selectedEntities;
    private Set<String> selectedEmbeddables;
    private Set<String> selectedDataMaps;

    private Map<DataMap, Set<String>> selectedEntitiesForDataMap;
    private Map<DataMap, Set<String>> selectedEmbeddablesForDataMap;
    private Map<DataMap, Set<String>> selectedDataMapsForDataMap;

    SelectionModel() {
        selectedEntitiesForDataMap = new HashMap<>();
        selectedEmbeddablesForDataMap = new HashMap<>();
        selectedDataMapsForDataMap = new HashMap<>();
        selectedEntities = new HashSet<>();
        selectedEmbeddables = new HashSet<>();
        selectedDataMaps = new HashSet<>();
    }

    void initCollectionsForSelection(DataMap dataMap) {
        selectedEntities = selectedEntitiesForDataMap.computeIfAbsent(dataMap, dm -> new HashSet<>());
        selectedEmbeddables = selectedEmbeddablesForDataMap.computeIfAbsent(dataMap, dm -> new HashSet<>());
        selectedDataMaps = selectedDataMapsForDataMap.computeIfAbsent(dataMap, dm -> new HashSet<>());
    }

    boolean updateSelection(Predicate<Object> predicate, List<Object> classes) {
        boolean modified = false;
        for (Object classObj : classes) {
            boolean select = predicate.test(classObj);
            if (classObj instanceof ObjEntity) {
                if (select) {
                    if (selectedEntities.add(((ObjEntity) classObj).getName())) {
                        modified = true;
                    }
                } else {
                    if (selectedEntities.remove(((ObjEntity) classObj).getName())) {
                        modified = true;
                    }
                }
            } else if (classObj instanceof Embeddable) {
                if (select) {
                    if (selectedEmbeddables.add(((Embeddable) classObj).getClassName())) {
                        modified = true;
                    }
                } else {
                    if (selectedEmbeddables.remove(((Embeddable) classObj).getClassName())) {
                        modified = true;
                    }
                }
            } else if (classObj instanceof DataMap) {
                if (select) {
                    if (selectedDataMaps.add(((DataMap) classObj).getName())) {
                        modified = true;
                    }
                } else {
                    if (selectedDataMaps.remove(((DataMap) classObj).getName())) {
                        modified = true;
                    }
                }
            }
        }
        return modified;
    }

    List<Embeddable> getSelectedEmbeddables(List<Object> classes) {
        List<Embeddable> selected = new ArrayList<>(selectedEmbeddables.size());
        for (Object classObj : classes) {
            if (classObj instanceof Embeddable) {
                String name = ((Embeddable) classObj).getClassName();
                if (selectedEmbeddables.contains(name)) {
                    selected.add((Embeddable) classObj);
                }
            }
        }

        return selected;
    }

    List<ObjEntity> getSelectedEntities(List<Object> classes) {
        List<ObjEntity> selected = new ArrayList<>(selectedEntities.size());
        for (Object classObj : classes) {
            if (classObj instanceof ObjEntity) {
                String name = ((ObjEntity) classObj).getName();
                if (selectedEntities.contains(name)) {
                    selected.add(((ObjEntity) classObj));
                }
            }
        }

        return selected;
    }

    boolean isSelected(Object currentClass) {
        if (currentClass instanceof ObjEntity) {
            return selectedEntities.contains(((ObjEntity) currentClass).getName());
        } else if (currentClass instanceof Embeddable) {
            return selectedEmbeddables.contains(((Embeddable) currentClass).getClassName());
        } else if (currentClass instanceof DataMap) {
            return selectedDataMaps.contains(((DataMap) currentClass).getName());
        }
        return false;
    }

    boolean setSelected(Object currentClass, boolean selectedFlag) {
        if (currentClass instanceof ObjEntity) {
            if (selectedFlag) {
                return selectedEntities.add(((ObjEntity) currentClass).getName());
            } else {
                return selectedEntities.remove(((ObjEntity) currentClass).getName());
            }
        } else if (currentClass instanceof Embeddable) {
            if (selectedFlag) {
                return selectedEmbeddables.add(((Embeddable) currentClass).getClassName());
            } else {
                return selectedEmbeddables.remove(((Embeddable) currentClass).getClassName());
            }
        } else if (currentClass instanceof DataMap) {
            if (selectedFlag) {
                return selectedDataMaps.add(((DataMap) currentClass).getName());
            } else {
                return selectedDataMaps.remove(((DataMap) currentClass).getName());
            }
        }
        return false;
    }

    void removeFromSelectedEntities(ObjEntity objEntity) {
        initCollectionsForSelection(objEntity.getDataMap());
        selectedEntities.remove(objEntity.getName());
    }

    void removeFromSelectedEmbeddables(Embeddable embeddable) {
        initCollectionsForSelection(embeddable.getDataMap());
        selectedEmbeddables.remove(embeddable.getClassName());
    }

    void addSelectedEntities(Collection<String> entities) {
        selectedEntities.addAll(entities);
    }

    void addSelectedEntity(String entity) {
        selectedEntities.add(entity);
    }

    void addSelectedEmbeddables(Collection<String> entities) {
        selectedEmbeddables.addAll(entities);
    }

    void addSelectedEmbeddable(String entity) {
        selectedEmbeddables.add(entity);
    }

    int getSelectedEntitiesCount() {
        return selectedEntities.size();
    }

    int getSelecetedEmbeddablesCount() {
        return selectedEmbeddables.size();
    }

    int getSelectedDataMapsCount() {
        return selectedDataMaps.size();
    }

}
