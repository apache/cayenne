package org.apache.cayenne.testdo.unsupported_distinct_types.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.unsupported_distinct_types.Customer;
import org.apache.cayenne.testdo.unsupported_distinct_types.Product;

/**
 * Class _Product was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Product extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<Product> SELF = PropertyFactory.createSelf(Product.class);

    public static final NumericIdProperty<Integer> ID_PK_PROPERTY = PropertyFactory.createNumericId("ID", "Product", Integer.class);
    public static final String ID_PK_COLUMN = "ID";

    public static final StringProperty<String> LONGVARCHAR_COL = PropertyFactory.createString("longvarcharCol", String.class);
    public static final ListProperty<Product> BASE = PropertyFactory.createList("base", Product.class);
    public static final ListProperty<Product> CONTAINED = PropertyFactory.createList("contained", Product.class);
    public static final ListProperty<Customer> ORDER_BY = PropertyFactory.createList("orderBy", Customer.class);

    protected String longvarcharCol;

    protected Object base;
    protected Object contained;
    protected Object orderBy;

    public void setLongvarcharCol(String longvarcharCol) {
        beforePropertyWrite("longvarcharCol", this.longvarcharCol, longvarcharCol);
        this.longvarcharCol = longvarcharCol;
    }

    public String getLongvarcharCol() {
        beforePropertyRead("longvarcharCol");
        return this.longvarcharCol;
    }

    public void addToBase(Product obj) {
        addToManyTarget("base", obj, true);
    }

    public void removeFromBase(Product obj) {
        removeToManyTarget("base", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<Product> getBase() {
        return (List<Product>)readProperty("base");
    }

    public void addToContained(Product obj) {
        addToManyTarget("contained", obj, true);
    }

    public void removeFromContained(Product obj) {
        removeToManyTarget("contained", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<Product> getContained() {
        return (List<Product>)readProperty("contained");
    }

    public void addToOrderBy(Customer obj) {
        addToManyTarget("orderBy", obj, true);
    }

    public void removeFromOrderBy(Customer obj) {
        removeToManyTarget("orderBy", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<Customer> getOrderBy() {
        return (List<Customer>)readProperty("orderBy");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "longvarcharCol":
                return this.longvarcharCol;
            case "base":
                return this.base;
            case "contained":
                return this.contained;
            case "orderBy":
                return this.orderBy;
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
            case "longvarcharCol":
                this.longvarcharCol = (String)val;
                break;
            case "base":
                this.base = val;
                break;
            case "contained":
                this.contained = val;
                break;
            case "orderBy":
                this.orderBy = val;
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
        out.writeObject(this.longvarcharCol);
        out.writeObject(this.base);
        out.writeObject(this.contained);
        out.writeObject(this.orderBy);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.longvarcharCol = (String)in.readObject();
        this.base = in.readObject();
        this.contained = in.readObject();
        this.orderBy = in.readObject();
    }

}
