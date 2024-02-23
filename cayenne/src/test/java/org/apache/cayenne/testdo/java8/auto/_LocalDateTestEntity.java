package org.apache.cayenne.testdo.java8.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.DateProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.testdo.java8.LocalDateTestEntity;

/**
 * Class _LocalDateTestEntity was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _LocalDateTestEntity extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<LocalDateTestEntity> SELF = PropertyFactory.createSelf(LocalDateTestEntity.class);

    public static final NumericIdProperty<Integer> ID_PK_PROPERTY = PropertyFactory.createNumericId("ID", "LocalDateTest", Integer.class);
    public static final String ID_PK_COLUMN = "ID";

    public static final DateProperty<LocalDate> DATE = PropertyFactory.createDate("date", LocalDate.class);

    protected LocalDate date;


    public void setDate(LocalDate date) {
        beforePropertyWrite("date", this.date, date);
        this.date = date;
    }

    public LocalDate getDate() {
        beforePropertyRead("date");
        return this.date;
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "date":
                return this.date;
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
            case "date":
                this.date = (LocalDate)val;
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
        out.writeObject(this.date);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.date = (LocalDate)in.readObject();
    }

}
