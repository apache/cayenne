package org.apache.cayenne.testdo.persistent.auto;

import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.testdo.persistent.Country;

/**
 * A generated persistent class mapped as "Continent" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _Continent extends PersistentObject {

    public static final String NAME_PROPERTY = "name";
    public static final String COUNTRIES_PROPERTY = "countries";

    protected String name;
    protected List<Country> countries;

    public String getName() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "name", false);
        }

        return name;
    }
    public void setName(String name) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "name", false);
        }

        Object oldValue = this.name;
        this.name = name;

        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "name", oldValue, name);
        }
    }

    public List<Country> getCountries() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "countries", true);
        }

        return countries;
    }
    public void addToCountries(Country object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "countries", true);
        }

        this.countries.add(object);
    }
    public void removeFromCountries(Country object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "countries", true);
        }

        this.countries.remove(object);
    }

}
