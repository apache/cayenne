package de.jexp.jequel.generator.data;

import java.io.Serializable;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class MetaDataElement implements Serializable {
    private static final long serialVersionUID = -6933918319611835143L;

    private final String name;
    private String remark;

    public MetaDataElement(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setRemark(String remark) {
        this.remark = isBlank(remark) ? null : remark;
    }

    public String getRemark() {
        return remark;
    }

    public boolean hasRemark() {
        return getRemark() != null;
    }

    public void addRemark(String remark) {
        if (hasRemark()) {
            setRemark(getRemark() + "\n" + remark);
        } else {
            setRemark(remark);
        }
    }
}
