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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.FindDialog;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.map.QueryDescriptor;

import javax.swing.JTextField;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FindAction extends CayenneAction {
    private java.util.List<Object> paths;

    public static String getActionName() {
        return "Find";
    }

    public FindAction(Application application) {
        super(getActionName(), application);
    }

    /**
     * All entities that contain a pattern substring (case-indifferent) in the name are produced.
     * @param e
     */
    public void performAction(ActionEvent e) {
        JTextField source = (JTextField) e.getSource();
        String sourceStr = source.getText().trim();

        paths = new ArrayList<>();
        if (sourceStr != null && !sourceStr.isEmpty()) {

            if (sourceStr.startsWith("*")) {
                sourceStr = sourceStr.substring(1);
            }

            Pattern pattern = Pattern.compile(sourceStr, Pattern.CASE_INSENSITIVE);

            Iterator<DataMap> it = ((DataChannelDescriptor)getProjectController().getProject().getRootNode()).getDataMaps().iterator();
            
            while(it.hasNext()) {
                
                  DataMap dm = it.next();
                 
                  Iterator<QueryDescriptor> querIterator = dm.getQueryDescriptors().iterator();
                  
                  while(querIterator.hasNext()) {
                      QueryDescriptor q = querIterator.next();
                      if(matchFound(q.getName(), pattern)){
                          paths.add(q);
                      }
                  }
                  
                  Iterator<Embeddable> embIterator = dm.getEmbeddables().iterator();
                  
                  while(embIterator.hasNext()) {
                      Embeddable emb = embIterator.next();
                      if(matchFound(emb.getClassName(), pattern)){
                          paths.add(emb);
                      }
                      
                      Iterator<EmbeddableAttribute> attrIterator = emb.getAttributes().iterator();
                      
                      while(attrIterator.hasNext()) {
                          EmbeddableAttribute attr = attrIterator.next();
                          if(matchFound(attr.getName(), pattern)){
                              paths.add(attr);
                          }
                      }
                  }
                  
                  
                  Iterator<DbEntity> dbEntIterator = dm.getDbEntities().iterator();
                  
                  while(dbEntIterator.hasNext()) {
                      DbEntity ent = dbEntIterator.next();
                      if(matchFound(ent.getName(), pattern)){
                          paths.add(ent);
                      }
                      
                      Iterator<DbAttribute> attrIterator = ent.getAttributes().iterator();
                      
                      while(attrIterator.hasNext()) {
                          DbAttribute attr = attrIterator.next();
                          if(matchFound(attr.getName(), pattern)){
                              paths.add(attr);
                          }
                      }
                      
                      Iterator<DbRelationship> relIterator = ent.getRelationships().iterator();
                      
                      while(relIterator.hasNext()) {
                          DbRelationship rel = relIterator.next();
                          if(matchFound(rel.getName(), pattern)){
                              paths.add(rel);
                          }
                      }

                      String catalog = ent.getCatalog();
                      if (catalog != null && !catalog.isEmpty()) {
                          if(matchFound(catalog, pattern) && !paths.contains(ent)){
                              paths.add(ent);
                          }
                      }

                      String schema = ent.getSchema();
                      if (schema != null && !schema.isEmpty()) {
                          if(matchFound(schema, pattern) && !paths.contains(ent)){
                              paths.add(ent);
                          }
                      }

                  }
                  
                  Iterator<ObjEntity> entIterator = dm.getObjEntities().iterator();
                  
                  while(entIterator.hasNext()) {
                      ObjEntity ent = entIterator.next();
                      if(matchFound(ent.getName(), pattern)){
                          paths.add(ent);
                      }
                      
                      Iterator<ObjAttribute> attrIterator = ent.getAttributes().iterator();
                      
                      while(attrIterator.hasNext()) {
                          ObjAttribute attr = attrIterator.next();
                          if(matchFound(attr.getName(), pattern)){
                              paths.add(attr);
                          }
                      }
                      
                      Iterator<ObjRelationship> relIterator = ent.getRelationships().iterator();
                      
                      while(relIterator.hasNext()) {
                          ObjRelationship rel = relIterator.next();
                          if(matchFound(rel.getName(), pattern)){
                              paths.add(rel);
                          }
                      }
                  }
            }
        }
     
        if(paths.size()==0){
            source.setBackground(Color.pink);
        } else if(paths.size()!=1){
            new FindDialog(getApplication().getFrameController(), paths).startupAction();
        } else {
           
            Iterator it = paths.iterator();
            if (it.hasNext()) {
                Object path = it.next();
                FindDialog.jumpToResult(path);
            }   
        }
    }

    private boolean matchFound(String entityName, Pattern pattern) {
        Matcher m = pattern.matcher(entityName);

        return m.find();
    }

}
