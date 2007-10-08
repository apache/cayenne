package org.objectstyle.art;

import java.util.List;

public class ArtGroup extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String ARTIST_ARRAY_PROPERTY = "artistArray";
    public static final String CHILD_GROUPS_ARRAY_PROPERTY = "childGroupsArray";
    public static final String TO_PARENT_GROUP_PROPERTY = "toParentGroup";

    public static final String GROUP_ID_PK_COLUMN = "GROUP_ID";

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void addToArtistArray(Artist obj) {
        addToManyTarget("artistArray", obj, true);
    }
    public void removeFromArtistArray(Artist obj) {
        removeToManyTarget("artistArray", obj, true);
    }
    public List getArtistArray() {
        return (List)readProperty("artistArray");
    }
    
    
    public void addToChildGroupsArray(ArtGroup obj) {
        addToManyTarget("childGroupsArray", obj, true);
    }
    public void removeFromChildGroupsArray(ArtGroup obj) {
        removeToManyTarget("childGroupsArray", obj, true);
    }
    public List getChildGroupsArray() {
        return (List)readProperty("childGroupsArray");
    }
    
    
    public void setToParentGroup(ArtGroup toParentGroup) {
        setToOneTarget("toParentGroup", toParentGroup, true);
    }
    public ArtGroup getToParentGroup() {
        return (ArtGroup)readProperty("toParentGroup");
    } 
    
    
}



