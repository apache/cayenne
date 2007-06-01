<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-nested.tld" prefix="nested" %>

<html:html locale="true">

<head>

<title><bean:message key="index.title"/></title>

<html:base/>

<LINK REL="stylesheet" TYPE="text/css" href="styles.css" TITLE="default">

</head>

<body bgcolor="white">
<jsp:include page="navi.html" flush="true"/>

<br><br>
<span class="titleTextStrong">Artist Detail Page</span><br><br>

<table width="640" class="standardTable">
<tr>
	<td bgcolor="#FFFFCE" colspan="3"><b><bean:write name="artist" property="artistName" scope="request" /></b></td>
</tr>
<tr>
	<td width="100">&nbsp;</td>
	<td><b>Paintings:</b></td>
	<td align="right"><b>DOB:</b> <bean:write name="artist" property="dateOfBirth" scope="request"/></td>
</tr>
<tr>
	<td></td>
	<td>
	  	<nested:root name="artist">
        <nested:iterate property="paintingArray">
            <br>&nbsp;&nbsp;&nbsp;<i><nested:write property="paintingTitle" /></i>
			<nested:present property="toGallery">
			, currently displayed at <nested:write property="toGallery.galleryName" /> 
			</nested:present>
        </nested:iterate>
    	</nested:root>
		
		<br><br>
		
		<html:form action="/savePainting">
		<html:hidden name="artist" property="artistName" />
		<html:text property="paintingTitle" size="50"/><br>
		<html:text property="estimatedPrice" size="10"/><br>
		

		
	  	<html:submit>
        <bean:message key="button.addpainting"/>
      	</html:submit>
		</html:form>
	</td>
	<td></td>
</tr>
</table>


</body>

</html:html>
