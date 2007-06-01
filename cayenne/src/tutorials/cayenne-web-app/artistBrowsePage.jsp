<%@ page language="java" %>

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
<span class="titleTextStrong">Artist Browse Page</span><br><br>

<table width="100%" class="standardTable" border="0" cellspacing="0">
<logic:iterate id="anArtist" name="artists">
<tr bgcolor="silver">
	<td colspan="2"><b>Artist Details:</b></td>
	<td align="right"><a href="addPainting.do?name=<bean:write name="anArtist" property="artistName"/>">add painting</a></td>
</tr>
<tr>
	<td width="125"><bean:write name="anArtist" property="artistName"/> </td>
	<td><b>Paintings:</b></td>
	<td align="right"><b>DOB:</b> <bean:write name="anArtist" property="dateOfBirth"/></td>
</tr>
<tr>
	<td></td>
	<td valign="top">
	  	<nested:root name="anArtist">
        <nested:iterate property="paintingArray">
           <LI><i><nested:write property="paintingTitle" /></i>
		    <nested:present property="toGallery">
			, currently displayed at <nested:write property="toGallery.galleryName" /> 
			</nested:present>
			<nested:notPresent property="toGallery">
			- <a href="addPaintingToGallery.do?title=<nested:write property="paintingTitle" />">add painting to gallery display</a>
			</nested:notPresent>
        </nested:iterate>
    	</nested:root>
		
	</td>
	<td></td>
</tr>
<tr>
	<td colspan="3"><br></td>
</tr>
</logic:iterate>
<tr>
	<td colspan="3" align="center"><br><a href="addArtist.do">Add Artist</a></td>
</tr>
</table>

</body>

</html:html>

