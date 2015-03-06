package de.jexp.jequel.generator.data;

import java.io.Serializable;

/**
 * @author mh14 @ jexp.de
 * @since 26.10.2007 03:24:55 (c) 2007 jexp.de
 */
public class MetaDataElement implements Serializable {
    protected final String name;
    private String remark;

    public MetaDataElement(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setRemark(final String remark) {
        if (remark != null && remark.trim().length() == 0)
            this.remark = null;
        else
            this.remark = remark;
    }

    public String getRemark() {
        return remark;
    }

    public boolean hasRemark() {
        return remark != null;
    }

    public void addRemark(final String remark) {
        if (hasRemark()) setRemark(getRemark() + "\n" + remark);
        else setRemark(remark);
    }

}
