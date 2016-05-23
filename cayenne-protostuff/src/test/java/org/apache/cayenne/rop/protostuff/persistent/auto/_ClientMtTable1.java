package org.apache.cayenne.rop.protostuff.persistent.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.rop.protostuff.persistent.ClientMtTable2;
import org.apache.cayenne.util.PersistentObjectList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

/**
 * A generated persistent class mapped as "MtTable1" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTable1 extends PersistentObject {

    public static final Property<LocalDate> DATE_ATTRIBUTE = new Property<LocalDate>("dateAttribute");
    public static final Property<String> GLOBAL_ATTRIBUTE = new Property<String>("globalAttribute");
    public static final Property<Date> OLD_DATE_ATTRIBUTE = new Property<Date>("oldDateAttribute");
    public static final Property<String> SERVER_ATTRIBUTE = new Property<String>("serverAttribute");
    public static final Property<LocalTime> TIME_ATTRIBUTE = new Property<LocalTime>("timeAttribute");
    public static final Property<LocalDateTime> TIMESTAMP_ATTRIBUTE = new Property<LocalDateTime>("timestampAttribute");
    public static final Property<List<ClientMtTable2>> TABLE2ARRAY = new Property<List<ClientMtTable2>>("table2Array");

    protected LocalDate dateAttribute;
    protected String globalAttribute;
    protected Date oldDateAttribute;
    protected String serverAttribute;
    protected LocalTime timeAttribute;
    protected LocalDateTime timestampAttribute;
    protected List<ClientMtTable2> table2Array;

    public LocalDate getDateAttribute() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "dateAttribute", false);
        }

        return dateAttribute;
    }
    public void setDateAttribute(LocalDate dateAttribute) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "dateAttribute", false);
        }

        Object oldValue = this.dateAttribute;
        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "dateAttribute", oldValue, dateAttribute);
        }
        
        this.dateAttribute = dateAttribute;
    }

    public String getGlobalAttribute() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "globalAttribute", false);
        }

        return globalAttribute;
    }
    public void setGlobalAttribute(String globalAttribute) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "globalAttribute", false);
        }

        Object oldValue = this.globalAttribute;
        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "globalAttribute", oldValue, globalAttribute);
        }
        
        this.globalAttribute = globalAttribute;
    }

    public Date getOldDateAttribute() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "oldDateAttribute", false);
        }

        return oldDateAttribute;
    }
    public void setOldDateAttribute(Date oldDateAttribute) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "oldDateAttribute", false);
        }

        Object oldValue = this.oldDateAttribute;
        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "oldDateAttribute", oldValue, oldDateAttribute);
        }
        
        this.oldDateAttribute = oldDateAttribute;
    }

    public String getServerAttribute() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "serverAttribute", false);
        }

        return serverAttribute;
    }
    public void setServerAttribute(String serverAttribute) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "serverAttribute", false);
        }

        Object oldValue = this.serverAttribute;
        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "serverAttribute", oldValue, serverAttribute);
        }
        
        this.serverAttribute = serverAttribute;
    }

    public LocalTime getTimeAttribute() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "timeAttribute", false);
        }

        return timeAttribute;
    }
    public void setTimeAttribute(LocalTime timeAttribute) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "timeAttribute", false);
        }

        Object oldValue = this.timeAttribute;
        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "timeAttribute", oldValue, timeAttribute);
        }
        
        this.timeAttribute = timeAttribute;
    }

    public LocalDateTime getTimestampAttribute() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "timestampAttribute", false);
        }

        return timestampAttribute;
    }
    public void setTimestampAttribute(LocalDateTime timestampAttribute) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "timestampAttribute", false);
        }

        Object oldValue = this.timestampAttribute;
        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "timestampAttribute", oldValue, timestampAttribute);
        }
        
        this.timestampAttribute = timestampAttribute;
    }

    public List<ClientMtTable2> getTable2Array() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table2Array", true);
        } else if (this.table2Array == null) {
        	this.table2Array = new PersistentObjectList(this, "table2Array");
		}

        return table2Array;
    }
    public void addToTable2Array(ClientMtTable2 object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table2Array", true);
        } else if (this.table2Array == null) {
        	this.table2Array = new PersistentObjectList(this, "table2Array");
		}

        this.table2Array.add(object);
    }
    public void removeFromTable2Array(ClientMtTable2 object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table2Array", true);
        } else if (this.table2Array == null) {
        	this.table2Array = new PersistentObjectList(this, "table2Array");
		}

        this.table2Array.remove(object);
    }

}
