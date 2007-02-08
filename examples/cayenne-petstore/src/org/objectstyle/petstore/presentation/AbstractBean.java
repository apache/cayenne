package org.objectstyle.petstore.presentation;

import org.apache.struts.beanaction.ActionContext;
import org.apache.struts.beanaction.BaseBean;

public abstract class AbstractBean extends BaseBean {

  public static final String SUCCESS = "success";
  public static final String FAILURE = "failure";
  public static final String SIGNON = "signon";
  public static final String SHIPPING = "shipping";
  public static final String CONFIRM = "confirm";

  protected void setMessage(String value) {
    ActionContext.getActionContext().getRequestMap().put("message", value);
  }
  
}
