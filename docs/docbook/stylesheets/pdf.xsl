<?xml version="1.0" encoding="UTF-8"?>
<!--
	Licensed to the Apache Software Foundation (ASF) under one
	or more contributor license agreements.  See the NOTICE file
	distributed with this work for additional information
	regarding copyright ownership.  The ASF licenses this file
	to you under the Apache License, Version 2.0 (the
	"License"); you may not use this file except in compliance
	with the License.  You may obtain a copy of the License at
	
	http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing,
	software distributed under the License is distributed on an
	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
	KIND, either express or implied.  See the License for the
	specific language governing permissions and limitations
	under the License.   
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<xsl:include href="common-customizations.xsl"/>

	<xsl:param name="paper.type">A4</xsl:param>

	<!-- don't indent the body text -->
	<xsl:param name="body.start.indent">0pt</xsl:param>

	<!-- //////////    Extensions    ///////////////  -->
	<!-- These extensions are required for table printing and other stuff -->

    <xsl:param name="use.extensions">1</xsl:param>
    <xsl:param name="tablecolumns.extension">0</xsl:param>
    <xsl:param name="callout.extensions">1</xsl:param>
    <!-- <xsl:param name="fop.extensions">1</xsl:param> -->
    <xsl:param name="fop1.extensions" select="1"/>
    

	<!-- /////////  Paper & Page Size   ///////////// -->

	<!-- print headers and footers mirrored so double sided printing works -->
	<xsl:param name="double.sided" select="0"/> <!-- no no!!!!!!!!! -->
	<xsl:attribute-set name="footer.content">
        <xsl:attribute name="position">right</xsl:attribute>
    </xsl:attribute-set>

 	<!-- Paper type, no headers on blank pages  -->
 	<xsl:param name="headers.on.blank.pages" select="0"/>
	<xsl:param name="footers.on.blank.pages" select="0"/>
	
	

	<xsl:param name="admon.graphics" select="1"/>
	<xsl:param name="admon.graphics.extension">.png</xsl:param>
	<xsl:param name="admon.graphics.path">stylesheets/docbook-xsl-ns/images/</xsl:param>
	<xsl:param name="admon.textlabel" select="1"/>

	<xsl:param name="callout.graphics" select="1"/>
	<xsl:param name="callout.graphics.extension">.png</xsl:param>
	<xsl:param name="callout.graphics.path">stylesheets/docbook-xsl-ns/images/callouts/</xsl:param>

	<xsl:param name="footer.rule">0</xsl:param>

	<!-- Separation between glossary terms and descriptions when glossaries are presented using lists. -->
	<xsl:param name="glossterm.separation">2em</xsl:param>

	<!-- This parameter specifies the width reserved for glossary terms when a list presentation is used.  -->
	<xsl:param name="glossterm.width">10em</xsl:param>

	<!--  Specifies the longest term in variablelists. In variablelists, the listitem is indented to leave room for the term elements. The indent may be computed if it is not specified with a termlength attribute on the variablelist element. The computation counts characters in the term elements in the list to find the longest term. However, some terms are very long and would produce extreme indents. This parameter lets you set a maximum character count. Any terms longer than the maximum would line wrap. The default value is 24. The character counts are converted to physical widths by multiplying by 0.50em. There is some variability in how many characters fit in the space since some characters are wider than others. -->
	<xsl:param name="variablelist.max.termlength">18</xsl:param>

	<!-- Custom font settings - preferred truetype font -->
	<xsl:param name="title.font.family">Lucinda Grande,sans-serif</xsl:param>
	<xsl:param name="body.font.family">Times,serif</xsl:param>
	<xsl:param name="sans.font.family">Lucinda Grande,sans-serif</xsl:param>
	<xsl:param name="dingbat.font.family">Lucinda Grande,serif</xsl:param>
	<xsl:param name="monospace.font.family">monospace</xsl:param>

	<!-- Specify the default text alignment. The default text alignment is used for most body text. -->
	<xsl:param name="alignment">left</xsl:param>


	<!--  Specifies the default point size for body text. The body font size is specified in two parameters (body.font.master and body.font.size) so that math can be performed on the font size by XSLT. -->
	<xsl:param name="body.font.master">11</xsl:param>
	<xsl:param name="body.font.small">8</xsl:param>

	<xsl:param name="hyphenate">false</xsl:param>

	<!-- "normal" is 1.6. This value is multiplied by the font size -->
	<xsl:param name="line-height">1.4</xsl:param>
	
	<!-- Chapter title size -->
	<xsl:attribute-set name="chapter.titlepage.recto.style">
                <xsl:attribute name="text-align">left</xsl:attribute>
                <xsl:attribute name="font-weight">bold</xsl:attribute>
                <xsl:attribute name="font-size">
                        <xsl:value-of select="$body.font.master * 1.8"/>
                        <xsl:text>pt</xsl:text>
                </xsl:attribute>
	</xsl:attribute-set>
	
	
	
	 <!-- //////////////// Tables //////////////////////////  -->

    <!-- The table width should be adapted to the paper size -->
    <xsl:param name="default.table.width">17.4cm</xsl:param>

    <!-- Some padding inside tables -->
    <xsl:attribute-set name="table.cell.padding">
        <xsl:attribute name="padding-left">4pt</xsl:attribute>
        <xsl:attribute name="padding-right">4pt</xsl:attribute>
        <xsl:attribute name="padding-top">4pt</xsl:attribute>
        <xsl:attribute name="padding-bottom">4pt</xsl:attribute>
    </xsl:attribute-set>

    <!-- Only hairlines as frame and cell borders in tables -->
    <xsl:param name="table.frame.border.thickness">0.1pt</xsl:param>
    <xsl:param name="table.cell.border.thickness">0.1pt</xsl:param>
	
	

	<!--  Specify the spacing required between normal paragraphs. -->
	<xsl:attribute-set name="normal.para.spacing">
		<xsl:attribute name="space-before.optimum">0.8em</xsl:attribute>
		<xsl:attribute name="space-before.minimum">0.6em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">1.0em</xsl:attribute>
	</xsl:attribute-set>
	
	
	
	<!-- ///////////////// Programlistings ////////////////// -->

    <!-- Verbatim text formatting (programlistings) -->
    <xsl:attribute-set name="monospace.verbatim.properties">
        <xsl:attribute name="font-size">
            <xsl:value-of select="$body.font.small * 1.0"/>
            <xsl:text>pt</xsl:text>
        </xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="verbatim.properties">
        <xsl:attribute name="space-before.minimum">1em</xsl:attribute>
        <xsl:attribute name="space-before.optimum">1em</xsl:attribute>
        <xsl:attribute name="space-before.maximum">1em</xsl:attribute>
        <xsl:attribute name="border-color">#444444</xsl:attribute>
        <xsl:attribute name="border-style">solid</xsl:attribute>
        <xsl:attribute name="border-width">0.1pt</xsl:attribute>
        <xsl:attribute name="padding-top">0.5em</xsl:attribute>
        <xsl:attribute name="padding-left">0.5em</xsl:attribute>
        <xsl:attribute name="padding-right">0.5em</xsl:attribute>
        <xsl:attribute name="padding-bottom">0.5em</xsl:attribute>
        <xsl:attribute name="margin-left">0.5em</xsl:attribute>
        <xsl:attribute name="margin-right">0.5em</xsl:attribute>
    </xsl:attribute-set>

    <!-- Shade (background) programlistings -->
    <xsl:param name="shade.verbatim">1</xsl:param>
    <xsl:attribute-set name="shade.verbatim.style">
        <xsl:attribute name="background-color">#F0F0F0</xsl:attribute>
    </xsl:attribute-set>


	<!--  Tables, examples, figures, and equations don't need to be forced onto onto one page without page breaks. -->
	<xsl:attribute-set name="formal.object.properties">
		<xsl:attribute name="keep-together.within-column">auto</xsl:attribute>
	</xsl:attribute-set>
	

	<!--  TOP  -->
	<!-- The body top margin is the distance from the top of the region-before to the first line of text in the page body. -->
	<xsl:param name="body.margin.top">10mm</xsl:param>

	<!-- The top page margin is the distance from the physical top of the page to the top of the region-before. -->
	<xsl:param name="page.margin.top">5mm</xsl:param>
	
	<!--  BOTTOM  -->
	<!-- The body bottom margin is the distance from the last line of text in the page body to the bottom of the region-after. -->
	<xsl:param name="body.margin.bottom">15mm</xsl:param>

	<!-- The bottom page margin is the distance from the bottom of the region-after to the physical bottom of the page. -->
	<xsl:param name="page.margin.bottom">5mm</xsl:param>
	
	<xsl:param name="region.before.extent">10mm</xsl:param>
	<xsl:param name="region.after.extent">10mm</xsl:param>
	
	<!-- No intendation of Titles -->
   <!--  <xsl:param name="title.margin.left">0pc</xsl:param>  -->

	<!-- The inner page margin. The inner page margin is the distance from binding edge of the page to the first column of text. In the left-to-right, top-to-bottom writing direction, this is the left margin of recto pages. The inner and outer margins are usually the same unless the output is double-sided. -->
	<xsl:param name="page.margin.inner">18mm</xsl:param>

	<!-- The outer page margin. The outer page margin is the distance from non-binding edge of the page to the last column of text. In the left-to-right, top-to-bottom writing direction, this is the right margin of recto pages. The inner and outer margins are usually the same unless the output is double-sided. -->
	<xsl:param name="page.margin.outer">18mm</xsl:param>

	<!-- Make hyperlinks blue and don't display the underlying URL -->
	<xsl:param name="ulink.show" select="0" />

	<xsl:attribute-set name="xref.properties">
		<xsl:attribute name="color">blue</xsl:attribute>
	</xsl:attribute-set>

	<xsl:attribute-set name="monospace.verbatim.properties">
		<xsl:attribute name="font-size">7pt</xsl:attribute>
	</xsl:attribute-set>
	
	<xsl:param name="img.src.path">../</xsl:param>

</xsl:stylesheet>
