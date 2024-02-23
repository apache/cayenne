package org.apache.cayenne.testdo.relationships_activity.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Date;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.DateProperty;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.relationships_activity.ActivityResult;

/**
 * Class _ActivityResult was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _ActivityResult extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<ActivityResult> SELF = PropertyFactory.createSelf(ActivityResult.class);

    public static final String APPOINT_DATE_PK_COLUMN = "APPOINT_DATE";
    public static final String APPOINT_NO_PK_COLUMN = "APPOINT_NO";
    public static final String RESULTNAME_PK_COLUMN = "RESULTNAME";

    public static final DateProperty<Date> APPOINT_DATE = PropertyFactory.createDate("appointDate", Date.class);
    public static final NumericProperty<Integer> APPOINT_NO = PropertyFactory.createNumeric("appointNo", Integer.class);
    public static final StringProperty<String> FIELD = PropertyFactory.createString("field", String.class);

    protected Date appointDate;
    protected int appointNo;
    protected String field;


    public void setAppointDate(Date appointDate) {
        beforePropertyWrite("appointDate", this.appointDate, appointDate);
        this.appointDate = appointDate;
    }

    public Date getAppointDate() {
        beforePropertyRead("appointDate");
        return this.appointDate;
    }

    public void setAppointNo(int appointNo) {
        beforePropertyWrite("appointNo", this.appointNo, appointNo);
        this.appointNo = appointNo;
    }

    public int getAppointNo() {
        beforePropertyRead("appointNo");
        return this.appointNo;
    }

    public void setField(String field) {
        beforePropertyWrite("field", this.field, field);
        this.field = field;
    }

    public String getField() {
        beforePropertyRead("field");
        return this.field;
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "appointDate":
                return this.appointDate;
            case "appointNo":
                return this.appointNo;
            case "field":
                return this.field;
            default:
                return super.readPropertyDirectly(propName);
        }
    }

    @Override
    public void writePropertyDirectly(String propName, Object val) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch (propName) {
            case "appointDate":
                this.appointDate = (Date)val;
                break;
            case "appointNo":
                this.appointNo = val == null ? 0 : (int)val;
                break;
            case "field":
                this.field = (String)val;
                break;
            default:
                super.writePropertyDirectly(propName, val);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeSerialized(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readSerialized(in);
    }

    @Override
    protected void writeState(ObjectOutputStream out) throws IOException {
        super.writeState(out);
        out.writeObject(this.appointDate);
        out.writeInt(this.appointNo);
        out.writeObject(this.field);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.appointDate = (Date)in.readObject();
        this.appointNo = in.readInt();
        this.field = (String)in.readObject();
    }

}
