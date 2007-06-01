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

package org.objectstyle.cayenne.dataview.dvmodeler;

import java.io.*;
import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;
import java.util.*;

/**
 * This class defines cayenne project.
 *
 * @author Andriy Shapochka
 * @author Nataliya Kholodna
 * @version 1.0
 */

public class CayenneProject {
  public static String DATAVIEW_FILE_SUFFIX = ".view.xml";
  private File projectFile;
  private File projectDirectory;
  private Document projectDocument;
  private List dataMaps = new ArrayList();
  private List readOnlyDataMaps = Collections.unmodifiableList(dataMaps);
  private List dataViews = new ArrayList();
  private Map objEntityMap = new TreeMap();
  private Map fieldLookupsTemp = new HashMap();
  private ObjEntity[] objEntities;
  private List loadErrors = new ArrayList();
  private List saveErrors = new ArrayList();
  private Map dataViewsElements = new HashMap();
  private Set objEntitiesNames = new HashSet();
  private Set objEntityViewsNames = new HashSet();


  public CayenneProject(File projectFile) throws DVModelerException {
    if (projectFile == null ||
        !projectFile.isFile() ||
        !projectFile.canRead())
      throw new IllegalArgumentException(projectFile + " is invalid.");

    this.projectFile = projectFile;
    projectDirectory = projectFile.getParentFile();

    load();
  }

  /*reterns project directory*/
  public File getProjectDirectory(){
    return projectDirectory;
  }

  public List getLoadErrors(){
    return Collections.unmodifiableList(loadErrors);
  }

  public List getSaveErrors(){
    return Collections.unmodifiableList(saveErrors);
  }

  public List getDataMaps() {
    return readOnlyDataMaps;
  }

  public List getDataViews() {
    return dataViews;
  }

  public ObjEntity[] getObjEntities() {
    return objEntities;
  }

  public void buildDataViewsElements(){
    if (saveErrors.size() != 0){
      saveErrors.clear();
    }
    for (Iterator i = dataViews.iterator(); i.hasNext(); ) {
      DataView dataView = (DataView)i.next();
      String dataViewName = dataView.getName();
      String location = dataView.getFile().getName();
      List builtDataViews = dataViews.subList(0, dataViews.indexOf(dataView));
      for (Iterator itr = builtDataViews.iterator(); itr.hasNext();){
        DataView nextDataView = (DataView)itr.next();
        if (nextDataView.getName().equalsIgnoreCase(dataViewName)){
          if (!nextDataView.getFile().getName().equalsIgnoreCase(location)){
            saveErrors.add("<b>" + dataViewName + "</b><br>"
                + "Data View names must be unique<br><br>");
            break;
          }else {
            saveErrors.add("<font color = \"red\" ><b>" + dataViewName + "</b><br>"
                + "Data View names and file locations must be unique</font><br><br>");
            break;
          }
        }else {
          if (nextDataView.getFile().getName().equalsIgnoreCase(location)){
                        saveErrors.add("<font color = \"red\" ><b>" + dataViewName + "</b><br>"
                + "and \"" + nextDataView + "\" have the same file location.</font><br><br>");
          }
        }
      }
      Element dataViewRoot = dataView.getDataViewElement();
      dataViewsElements.put(dataView, dataViewRoot);

      List views = dataView.getObjEntityViews();
      for(Iterator j = views.iterator(); j.hasNext();){
        ObjEntityView view = (ObjEntityView)j.next();
        if (objEntityViewsNames.add(view.getName()) == false){
            String path = "<b>" + dataView + "." + view + "</b><br>";
            saveErrors.add(path + " ObjEntity View \"" + view
                + "\" already exists in the Cayenne project<br><br>");
          }

      }

      saveErrors.addAll(dataView.getSaveErrors());
    }
    objEntityViewsNames.clear();
  }

  public void save() throws IOException {
//    if (dataViewsElements.size() != 0){
      Element root = projectDocument.getRootElement();
      List views = root.getChildren("view");
      views.clear();
      XMLOutputter out = new XMLOutputter();
      out.setIndent("    ");
      out.setNewlines(true);
      out.setTextNormalize(true);

      Set dataViewsElementsKeys = dataViewsElements.keySet();
      for (Iterator i = dataViewsElementsKeys.iterator(); i.hasNext(); ) {
        DataView dataView = (DataView)i.next();
        String dataViewName = dataView.getName();
        File dataViewFile = dataView.getFile();
        Element dataViewRoot = (Element)dataViewsElements.get(dataView);
        Document dataViewDocument = new Document(dataViewRoot);
        if (dataViewFile == null) {
          dataViewFile = new File(
            projectDirectory,
            dataViewName + CayenneProject.DATAVIEW_FILE_SUFFIX);
          dataView.setFile(dataViewFile);
        }
        if (!dataViewFile.exists())
          dataViewFile.createNewFile();
        if (dataViewFile.isFile() && dataViewFile.canWrite()) {
          FileWriter dataViewFileWriter = new FileWriter(dataViewFile);
          BufferedWriter dataViewWriter = new BufferedWriter(dataViewFileWriter);
          out.output(dataViewDocument, dataViewWriter);
          dataViewWriter.close();
          dataViewFileWriter.close();
          Element viewElement = new Element("view");
          viewElement.setAttribute("name", dataViewName);
          viewElement.setAttribute("location", dataViewFile.getName());
          views.add(viewElement);
        } else {
          throw new IOException(
            "Cannot save data view " +
            dataViewName +
            "to file " +
            dataViewFile);
        }
      }
      FileWriter projectWriter = new FileWriter(projectFile);
      out.output(projectDocument, projectWriter);
      projectWriter.close();
//    } else {
//      throw new IllegalStateException("You must call buildDataViewsElements() from class CayenneProject at first.");
//    }
  }

  public ObjEntity getObjEntity(String name) {
    return (ObjEntity)objEntityMap.get(name);
  }

  public DataView createDataView() {
    String nameRoot = "dataview";
    String name = nameRoot;
    for (int i = 0; i < Integer.MAX_VALUE; i++) {
      name = nameRoot + i;
      boolean nameExists = false;
      for (int j = 0; j < dataViews.size(); j++) {
        DataView dataView = (DataView)dataViews.get(j);
        nameExists = name.equalsIgnoreCase(dataView.getName());
        if (nameExists)
          break;
      }
      if (!nameExists)
        break;
    }
    DataView dataView = new DataView(this);
    dataView.setName(name);
    File file = dataView.getFile();
    File newFile = new File(file.getParentFile(), name + ".view.xml");
    dataView.setFile(newFile);

    dataViews.add(dataView);
    return dataView;
  }

  public Map removeDataView(DataView dataView){
    Map removingObjEntityViews = new HashMap();
    for (Iterator j = dataView.getObjEntityViews().iterator(); j.hasNext();){
      ObjEntityView view = (ObjEntityView)j.next();
      ObjEntity entity = view.getObjEntity();
      if (entity != null){
        removingObjEntityViews.put(view, new Integer(entity.getIndexOfObjEntityView(view)));

        entity.removeObjEntityView(view);
      }
    }
    dataViews.remove(dataView);
    return removingObjEntityViews;
  }

  private void load() throws DVModelerException {
    SAXBuilder builder = new SAXBuilder();
    try {
      projectDocument = builder.build(projectFile);
    } catch (JDOMException ex) {
      throw new DVModelerException(
          "Failed to parse Cayenne project file: " + projectFile,
          ex);
    } catch (IOException ex) {
      throw new DVModelerException(
          "Failed to parse Cayenne project file: " + projectFile,
          ex);
    }

    Element root = projectDocument.getRootElement();
    if (!"domains".equals(root.getName()))
      throw new DVModelerException(
          "Root element " +
          root.getName() +
          " is invalid, file: " +
          projectFile);

    List domains = root.getChildren("domain");
    for (Iterator i = domains.iterator(); i.hasNext(); ) {
      Element domain = (Element)i.next();

      List maps = domain.getChildren("map");
      for (Iterator j = maps.iterator(); j.hasNext(); ) {
        Element map = (Element)j.next();
        String name = map.getAttributeValue("name");
        String location = map.getAttributeValue("location");
        boolean loading = true;
        for (Iterator itr = dataMaps.iterator(); itr.hasNext();){
          DataMap nextDataMap = (DataMap)itr.next();
          if (nextDataMap.getName().equalsIgnoreCase(name)){
            if (!nextDataMap.getFile().getName().equalsIgnoreCase(location)){
              loadErrors.add("<b>" + name + "</b><br>"
                  + "Data Map with this name already exists"
                  + " in the Cayenne project<br><br>");
              break;
            }else {
              loading = false;
              break;
            }
          }
        }
        if(loading == true){
          loadDataMap(name, location);
        }
      }
      objEntities = (ObjEntity[])objEntityMap.values().toArray(new ObjEntity[] {});
    }
    objEntitiesNames.clear();

    List views = root.getChildren("view");
    for (Iterator j = views.iterator(); j.hasNext(); ) {
      Element view = (Element)j.next();
      String name = view.getAttributeValue("name");
      String location = view.getAttributeValue("location");
      boolean loading = true;
      for (Iterator itr = dataViews.iterator(); itr.hasNext();){
        DataView nextDataView = (DataView)itr.next();
        if (nextDataView.getName().equalsIgnoreCase(name)){
          if (!nextDataView.getFile().getName().equalsIgnoreCase(location)){
            loadErrors.add("<b>" + name + "</b><br>"
                + "Data View with this name already exists"
                + " in the Cayenne project<br><br>");
            break;
          }else {
            loading = false;
            break;
          }
        }
      }
      if(loading == true){
        loadDataView(name, location);
      }
    }
    objEntityViewsNames.clear();

    Set keysTemp = fieldLookupsTemp.keySet();
    for (Iterator j = keysTemp.iterator(); j.hasNext();){
      ObjEntityViewField field = (ObjEntityViewField)j.next();
      ObjEntityView fieldView = field.getObjEntityView();
      DataView fieldDataView = fieldView.getDataView();
      String fieldPath = "<b>" + fieldDataView + "." + fieldView + "</b><br>";

      String[] lookupNames = (String[])fieldLookupsTemp.get(field);
      String lookupViewName = lookupNames[0];
      String lookupFieldName = lookupNames[1];
      ObjRelationship relationship = field.getObjRelationship();
      ObjEntity targetObjEntity = relationship.getTargetObjEntity();
      if (targetObjEntity != null){
        for (Iterator itr = targetObjEntity.getObjEntityViews().iterator();
                    itr.hasNext();){
          ObjEntityView view = (ObjEntityView)itr.next();
          if (view.getName().equals(lookupViewName)){
            field.getLookup().setLookupObjEntityView(view);
            break;
          }
        }
        if (field.getLookup().getLookupObjEntityView() != null){
          ObjEntityView lookupView = field.getLookup().getLookupObjEntityView();
          if (lookupFieldName != null){
            for (Iterator k = lookupView.getObjEntityViewFields().iterator(); k.hasNext();){
              ObjEntityViewField lookupField = (ObjEntityViewField)k.next();
              if (lookupField.getName().equals(lookupFieldName)){
                field.getLookup().setLookupField(lookupField);
                break;
              }
            }
            if (field.getLookup().getLookupField() == null){
              loadErrors.add(fieldPath + " Lookup ObjEntity View \"" + lookupViewName
                + "\" has no field with the name \"" + lookupFieldName + "\"<br><br>");
            }
          }else{
            loadErrors.add(fieldPath + " Lookup has no attribute \"field-name\"<br><br>");
          }
        }else {
          loadErrors.add(fieldPath + " ObjEntity target of the ObjRelationship \""
            + relationship.getName() + "\" has no ObjEntity View with the name \"" + lookupViewName + "\"<br><br>");
        }

      }else {
        loadErrors.add(fieldPath + " ObjRelationship for the field (\"" + relationship.getName()
                       + "\") has no ObjEntity target<br><br>");
      }
    }

    fieldLookupsTemp = null;
  }

  private void loadDataMap(String name, String location)
      throws DVModelerException {
    File file = getFileInProjectDirectory(location);
    if (file == null)
      return;

    SAXBuilder builder = new SAXBuilder();
    Document doc;
    try {
      doc = builder.build(file);
      Element elem = doc.getRootElement();
      if (elem.getName().equals("data-map")){
        String dataMapName = (name != null ? name : location);
        DataMap dataMap = new DataMap(this, dataMapName, file);
        dataMap.setName(dataMapName);
        List elements = elem.getChildren("obj-entity");
        Iterator it = elements.iterator();
        while (it.hasNext()){
          try {
            Element element = (Element)it.next();
            ObjEntity entity = new ObjEntity(dataMap, element);
            if (objEntitiesNames.add(entity.getName()) == false){
              String entityPath = "<b>" + dataMap.getName() + "." + entity.getName() + "</b><br>";
              loadErrors.add(entityPath + "ObjEntity \"" + entity.getName()
                  + "\" already exists in the Cayenne project<br><br>");
            }
            loadErrors.addAll(entity.getLoadErrors());
            objEntityMap.put(entity.getName(), entity);

          } catch (DVModelerException ex){
            loadErrors.add(ex.getMessage());
          }
        }
        elements = elem.getChildren("obj-relationship");
        it = elements.iterator();
        while (it.hasNext()){
          Element element = (Element)it.next();
          ObjRelationship objRelationship = new ObjRelationship(dataMap, element);
          dataMap.addObjRelationship(objRelationship);
        }
        dataMaps.add(dataMap);
      }
    } catch ( JDOMException e ) {
      throw new DVModelerException(
          "Failed to parse data map file: " + file, e);
    } catch (IOException ex) {
      throw new DVModelerException(
          "Failed to parse Cayennne project file: " + projectFile,
          ex);
    }

  }

  private void loadDataView(String name, String location)
      throws DVModelerException {
    File file = getFileInProjectDirectory(location);
    if (file == null)
      return;

    SAXBuilder builder = new SAXBuilder();
    Document doc;
    try {
      doc = builder.build(file);
      Element elem = doc.getRootElement();
      if (elem.getName().equals("data-view")){
        String dataViewName = (name != null ? name : location);

        DataView dataView = new DataView(this, dataViewName, file);

        List elements = elem.getChildren("obj-entity-view");
        Iterator itr = elements.iterator();
        while (itr.hasNext()){
          Element element = (Element)itr.next();
          ObjEntityView objEntityView = new ObjEntityView(
              this, dataView, element);
          if (objEntityViewsNames.add(objEntityView.getName()) == false){
            String path = "<b>" + dataView.getName() + "." + objEntityView.getName() + "</b><br>";
            loadErrors.add(path + "ObjEntity View \"" + objEntityView.getName()
                + "\" already exists in the Cayenne project<br><br>");
          }
          loadErrors.addAll(objEntityView.getLoadErrors());
        }
        dataViews.add(dataView);
      }
    } catch ( JDOMException e ) {
      throw new DVModelerException(
          "Failed to parse data view file: " + file, e);
    } catch (IOException ex) {
      throw new DVModelerException(
          "Failed to parse Cayenne project file: " + projectFile,
          ex);
    }
  }

  private File getFileInProjectDirectory(String location) {
    if (location == null)
      return null;
    File f = new File(projectDirectory, location);
    if (f.isFile() && f.canRead())
      return f;
    else
      return null;
  }

  /*This method is called from ObjEntityViewField constructor.
  *
   */
  public void putFieldLookup(ObjEntityViewField field, String lookupViewName, String lookupFieldName){
    fieldLookupsTemp.put(field,
                     new String[]{lookupViewName, lookupFieldName});
  }

}