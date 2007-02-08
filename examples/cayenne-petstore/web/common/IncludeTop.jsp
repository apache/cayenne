<%@ page contentType="text/html" %>
<%@ taglib uri="struts-logic" prefix="logic" %>
<%@ taglib uri="struts-bean" prefix="bean" %>
<%@ taglib uri="struts-html" prefix="html" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">

<link rel="StyleSheet" href="../css/jpetstore.css" type="text/css" media="screen"/>

<head>
  <meta name="generator"
        content="HTML Tidy for Linux/x86 (vers 1st November 2002), see www.w3.org"/>
  <title>JPetStore Demo</title>
  <meta content="text/html; charset=windows-1252" http-equiv="Content-Type"/>
  <meta http-equiv="Cache-Control" content="max-age=0"/>
  <meta http-equiv="Cache-Control" content="no-cache"/>
  <meta http-equiv="expires" content="0"/>
  <meta http-equiv="Expires" content="Tue, 01 Jan 1980 1:00:00 GMT"/>
  <meta http-equiv="Pragma" content="no-cache"/>
</head>

<body>

<div id="Header">
  
  <div id="Logo">
    <div id="LogoContent">
      <html:link page="/shop/index.shtml"><img src="../images/logo-topbar.gif"/></html:link>
    </div>
  </div>

  <div id="Menu">
    <div id="MenuContent">
      <html:link page="/shop/viewCart.shtml"><img align="middle" name="img_cart" src="../images/cart.gif"/></html:link>
      <img align="middle" src="../images/separator.gif"/>
      <logic:notPresent name="accountBean" scope="session">
        <html:link page="/shop/signonForm.shtml">Sign In</html:link>
      </logic:notPresent>
      <logic:present name="accountBean" scope="session">
        <logic:notEqual name="accountBean" property="authenticated" value="true" scope="session">
          <html:link page="/shop/signonForm.shtml">Sign In</html:link>
        </logic:notEqual>
      </logic:present>
      <logic:present name="accountBean" scope="session">
        <logic:equal name="accountBean" property="authenticated" value="true" scope="session">
          <html:link page="/shop/signoff.shtml">Sign Out</html:link>
          <img align="middle" src="../images/separator.gif"/>
          <html:link page="/shop/editAccountForm.shtml">My Account</html:link>
        </logic:equal>
      </logic:present>

      <img align="middle" src="../images/separator.gif"/>
      <html:link href="../help.html">?</html:link>
    </div>
  </div>

  <div id="Search">
    <div id="SearchContent">
      <html:form method="post" action="/shop/searchProducts.shtml">
        <input name="keyword" size="14"/>&nbsp;<input type="submit" name="SearchButton"
        value="Search"/>
      </html:form>
    </div>
  </div>

  <div id="QuickLinks">
    <html:link page="/shop/viewCategory.shtml?categoryId=FISH">
      <img src="../images/sm_fish.gif"/></html:link>
    <img src="../images/separator.gif"/>
    <html:link page="/shop/viewCategory.shtml?categoryId=DOGS">
      <img src="../images/sm_dogs.gif"/></html:link>
    <img src="../images/separator.gif"/>
    <html:link page="/shop/viewCategory.shtml?categoryId=REPTILES">
      <img src="../images/sm_reptiles.gif"/></html:link>
    <img src="../images/separator.gif"/>
    <html:link page="/shop/viewCategory.shtml?categoryId=CATS">
      <img src="../images/sm_cats.gif"/></html:link>
    <img src="../images/separator.gif"/>
    <html:link page="/shop/viewCategory.shtml?categoryId=BIRDS">
      <img src="../images/sm_birds.gif"/></html:link>
  </div>

</div>

<div id="Content">

<html:errors/>

<!-- Support for non-traditional but simple message -->
<logic:present name="message">
  <bean:write name="message"/>
</logic:present>

<!-- Support for non-traditional but simpler use of errors... -->
<logic:present name="errors">
  <logic:iterate id="error" name="errors">
    <bean:write name="error"/>
  </logic:iterate>
</logic:present>