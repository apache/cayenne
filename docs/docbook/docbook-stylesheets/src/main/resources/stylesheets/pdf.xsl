<?xml version="1.0" encoding="UTF-8"?>
<!--
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements. See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to you under the Apache License, Version
    2.0 (the "License"); you may not use this file except in compliance
    with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0 Unless required by
    applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
    CONDITIONS OF ANY KIND, either express or implied. See the License for
    the specific language governing permissions and limitations under the
    License.
-->
<!--
    This is the XSL FO (PDF) stylesheet for the Cayenne documentation.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                version="1.0">

    <xsl:import href="urn:docbkx:stylesheet"/>
    <xsl:include href="common-customizations.xsl"/>

    <xsl:template name="border">
        <xsl:param name="side" select="'start'"/>

        <xsl:attribute name="border-{$side}-width">
            <xsl:value-of select="$table.cell.border.thickness"/>
        </xsl:attribute>
        <xsl:attribute name="border-{$side}-style">
            <xsl:value-of select="$table.cell.border.style"/>
        </xsl:attribute>
        <xsl:attribute name="border-{$side}-color">
            <xsl:value-of select="$table.cell.border.color"/>
        </xsl:attribute>
        <xsl:attribute name="my-attr">
            <xsl:value-of select="'1'"/>
        </xsl:attribute>
    </xsl:template>

    <!-- don't indent the body text -->
    <xsl:param name="body.start.indent">0pt</xsl:param>

    <!-- print headers and footers mirrored so double sided printing works -->
    <!--<xsl:param name="fop1.extensions" select="1"/>-->
    <xsl:param name="double.sided" select="0"/>

    <xsl:param name="admon.graphics" select="0"/>
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
    <xsl:param name="alignment">justify</xsl:param>

    <!--  Specifies the default point size for body text. The body font size is specified in two parameters (body.font.master and body.font.size) so that math can be performed on the font size by XSLT. -->
    <xsl:param name="body.font.master">11</xsl:param>

    <xsl:param name="hyphenate">true</xsl:param>

    <!-- "normal" is 1.6. This value is multiplied by the font size -->
    <xsl:param name="line-height">1.5</xsl:param>

    <!--  Specify the spacing required between normal paragraphs. -->
    <xsl:attribute-set name="normal.para.spacing">
        <xsl:attribute name="space-before.optimum">0.8em</xsl:attribute>
        <xsl:attribute name="space-before.minimum">0.6em</xsl:attribute>
        <xsl:attribute name="space-before.maximum">1.0em</xsl:attribute>
    </xsl:attribute-set>

    <!--  Tables, examples, figures, and equations don't need to be forced onto onto one page without page breaks. -->
    <xsl:attribute-set name="formal.object.properties">
        <xsl:attribute name="keep-together.within-column">auto</xsl:attribute>
    </xsl:attribute-set>

    <!-- The body bottom margin is the distance from the last line of text in the page body to the bottom of the region-after. -->
    <xsl:param name="body.margin.bottom">20mm</xsl:param>

    <!-- The body top margin is the distance from the top of the region-before to the first line of text in the page body. -->
    <xsl:param name="body.margin.top">20mm</xsl:param>

    <!-- The top page margin is the distance from the physical top of the page to the top of the region-before. -->
    <xsl:param name="page.margin.top">10mm</xsl:param>

    <!-- The bottom page margin is the distance from the bottom of the region-after to the physical bottom of the page. -->
    <xsl:param name="page.margin.bottom">10mm</xsl:param>

    <!-- The inner page margin. The inner page margin is the distance from binding edge of the page to the first column of text. In the left-to-right, top-to-bottom writing direction, this is the left margin of recto pages. The inner and outer margins are usually the same unless the output is double-sided. -->
    <xsl:param name="page.margin.inner">18mm</xsl:param>

    <!-- The outer page margin. The outer page margin is the distance from non-binding edge of the page to the last column of text. In the left-to-right, top-to-bottom writing direction, this is the right margin of recto pages. The inner and outer margins are usually the same unless the output is double-sided. -->
    <xsl:param name="page.margin.outer">18mm</xsl:param>

    <!-- Make hyperlinks blue and don't display the underlying URL -->
    <xsl:param name="ulink.show" select="0"/>

    <xsl:attribute-set name="xref.properties">
        <xsl:attribute name="color">blue</xsl:attribute>
    </xsl:attribute-set>

    <xsl:param name="img.src.path">../</xsl:param>

    <!-- Prevent blank pages in output -->
    <xsl:template name="book.titlepage.before.verso">
    </xsl:template>
    <xsl:template name="book.titlepage.verso">
    </xsl:template>
    <xsl:template name="book.titlepage.separator">
    </xsl:template>

    <!--###################################################
                     Header
   ################################################### -->

    <!-- More space in the center header for long text -->
    <xsl:attribute-set name="header.content.properties">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$body.font.family"/>
        </xsl:attribute>
        <xsl:attribute name="margin-left">-5em</xsl:attribute>
        <xsl:attribute name="margin-right">-5em</xsl:attribute>
    </xsl:attribute-set>

    <!--###################################################
                     Custom Footer
   ################################################### -->
    <xsl:template name="footer.content">
        <xsl:param name="pageclass" select="''"/>
        <xsl:param name="sequence" select="''"/>
        <xsl:param name="position" select="''"/>
        <xsl:param name="gentext-key" select="''"/>
        <xsl:variable name="Version">
            <fo:inline min-width="150mm">v.${cayenne.version.major}</fo:inline>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$sequence='blank'">
                <xsl:if test="$position = 'left'">
                    <xsl:value-of select="$Version"/>
                </xsl:if>
            </xsl:when>
            <!-- for single sided printing, print all page numbers on the right (of the page) -->
            <xsl:when test="$double.sided = 0">
                <xsl:choose>
                    <xsl:when test="$position='left'">
                        <xsl:value-of select="$Version"/>
                    </xsl:when>
                    <xsl:when test="$position='right'">
                        <fo:page-number/>
                    </xsl:when>
                </xsl:choose>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <!--###################################################
                     Extensions
        ################################################### -->

    <!-- These extensions are required for table printing and other stuff -->
    <xsl:param name="use.extensions">1</xsl:param>
    <xsl:param name="tablecolumns.extension">1</xsl:param>
    <xsl:param name="callout.extensions">1</xsl:param>
    <!-- FOP provide only PDF Bookmarks at the moment -->
    <xsl:param name="fop1.extensions">1</xsl:param>

    <!-- ###################################################
                     Table Of Contents
         ################################################### -->

    <!-- Generate the TOCs for named components only -->
    <xsl:param name="generate.toc">
        book toc
    </xsl:param>

    <!-- Show only Sections up to level 3 in the TOCs -->
    <!--<xsl:param name="toc.section.depth">2</xsl:param>-->

    <!-- Dot and Whitespace as separator in TOC between Label and Title-->
    <xsl:param name="autotoc.label.separator" select="'.  '"/>


    <!-- ###################################################
                  Paper & Page Size
         ################################################### -->

    <!-- Paper type, no headers on blank pages, no double sided printing -->
    <xsl:param name="paper.type" select="'A4'"/>
    <!--<xsl:param name="double.sided">0</xsl:param>-->
    <xsl:param name="headers.on.blank.pages">0</xsl:param>
    <xsl:param name="footers.on.blank.pages">0</xsl:param>

    <!-- Space between paper border and content (chaotic stuff, don't touch) -->
    <!--<xsl:param name="page.margin.top">5mm</xsl:param>-->
    <xsl:param name="region.before.extent">10mm</xsl:param>
    <!--<xsl:param name="body.margin.top">10mm</xsl:param>-->

    <!--<xsl:param name="body.margin.bottom">15mm</xsl:param>-->
    <xsl:param name="region.after.extent">10mm</xsl:param>
    <!--<xsl:param name="page.margin.bottom">0mm</xsl:param>-->

    <!--<xsl:param name="page.margin.outer">18mm</xsl:param>-->
    <!--<xsl:param name="page.margin.inner">18mm</xsl:param>-->

    <!-- No intendation of Titles -->
    <xsl:param name="title.margin.left">0pc</xsl:param>

    <!-- ###################################################
                  Fonts & Styles
         ################################################### -->

    <!-- Left aligned text and no hyphenation -->
    <!--<xsl:param name="alignment">justify</xsl:param>-->
    <!--<xsl:param name="hyphenate">false</xsl:param>-->

    <!-- Default Font size -->
    <!--<xsl:param name="body.font.master">11</xsl:param>-->
    <xsl:param name="body.font.small">8</xsl:param>

    <!-- ###################################################
                  Tables
         ################################################### -->

    <!-- The table width should be adapted to the paper size -->
    <xsl:param name="default.table.width">17.4cm</xsl:param>

    <!-- Some padding inside tables -->
    <xsl:attribute-set name="table.cell.padding">
        <xsl:attribute name="padding-start">4pt</xsl:attribute>
        <xsl:attribute name="padding-end">4pt</xsl:attribute>
        <xsl:attribute name="padding-top">4pt</xsl:attribute>
        <xsl:attribute name="padding-bottom">4pt</xsl:attribute>
    </xsl:attribute-set>

    <!-- Only hairlines as frame and cell borders in tables -->
    <xsl:param name="default.table.frame">all</xsl:param>
    <xsl:param name="default.table.rules">all</xsl:param>

    <xsl:param name="table.frame.border.thickness">0.5pt</xsl:param>
    <xsl:param name="table.frame.border.style">solid</xsl:param>
    <xsl:param name="table.frame.border.color">black</xsl:param>
    <xsl:param name="table.cell.border.thickness">0.5pt</xsl:param>
    <xsl:param name="table.cell.border.color">black</xsl:param>
    <xsl:param name="table.cell.border.style">solid</xsl:param>
    <xsl:param name="border-start-style">solid</xsl:param>
    <xsl:param name="border-end-style">solid</xsl:param>
    <xsl:param name="border-top-style">solid</xsl:param>
    <xsl:param name="border-bottom-style">solid</xsl:param>

    <!--###################################################
                        Labels
   ################################################### -->

    <!-- Label Chapters and Sections (numbering) -->
    <xsl:param name="chapter.autolabel">1</xsl:param>
    <xsl:param name="section.autolabel" select="1"/>
    <xsl:param name="section.label.includes.component.label" select="1"/>

    <!--###################################################
                        Titles
   ################################################### -->

    <!-- Chapter title size -->
    <xsl:attribute-set name="chapter.titlepage.recto.style">
        <xsl:attribute name="text-align">left</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="font-size">
            <xsl:value-of select="$body.font.master * 1.8"/>
            <xsl:text>pt</xsl:text>
        </xsl:attribute>
    </xsl:attribute-set>

    <!-- Why is the font-size for chapters hardcoded in the XSL FO templates?
        Let's remove it, so this sucker can use our attribute-set only... -->
    <xsl:template match="title" mode="chapter.titlepage.recto.auto.mode">
        <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format"
                  xsl:use-attribute-sets="chapter.titlepage.recto.style">
            <xsl:call-template name="component.title">
                <xsl:with-param name="node" select="ancestor-or-self::chapter[1]"/>
            </xsl:call-template>
        </fo:block>
    </xsl:template>

    <!-- Sections 1, 2 and 3 titles have a small bump factor and padding -->
    <xsl:attribute-set name="section.title.level1.properties">
        <xsl:attribute name="space-before.optimum">0.8em</xsl:attribute>
        <xsl:attribute name="space-before.minimum">0.8em</xsl:attribute>
        <xsl:attribute name="space-before.maximum">0.8em</xsl:attribute>
        <xsl:attribute name="font-size">
            <xsl:value-of select="$body.font.master * 1.5"/>
            <xsl:text>pt</xsl:text>
        </xsl:attribute>
        <xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
        <xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
        <xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="section.title.level2.properties">
        <xsl:attribute name="space-before.optimum">0.6em</xsl:attribute>
        <xsl:attribute name="space-before.minimum">0.6em</xsl:attribute>
        <xsl:attribute name="space-before.maximum">0.6em</xsl:attribute>
        <xsl:attribute name="font-size">
            <xsl:value-of select="$body.font.master * 1.25"/>
            <xsl:text>pt</xsl:text>
        </xsl:attribute>
        <xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
        <xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
        <xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="section.title.level3.properties">
        <xsl:attribute name="space-before.optimum">0.4em</xsl:attribute>
        <xsl:attribute name="space-before.minimum">0.4em</xsl:attribute>
        <xsl:attribute name="space-before.maximum">0.4em</xsl:attribute>
        <xsl:attribute name="font-size">
            <xsl:value-of select="$body.font.master * 1.0"/>
            <xsl:text>pt</xsl:text>
        </xsl:attribute>
        <xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
        <xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
        <xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
    </xsl:attribute-set>

    <!-- Titles of formal objects (tables, examples, ...) -->
    <xsl:attribute-set name="formal.title.properties" use-attribute-sets="normal.para.spacing">
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="font-size">
            <xsl:value-of select="$body.font.master"/>
            <xsl:text>pt</xsl:text>
        </xsl:attribute>
        <xsl:attribute name="hyphenate">false</xsl:attribute>
        <xsl:attribute name="space-after.minimum">0.4em</xsl:attribute>
        <xsl:attribute name="space-after.optimum">0.6em</xsl:attribute>
        <xsl:attribute name="space-after.maximum">0.8em</xsl:attribute>
    </xsl:attribute-set>

    <!--###################################################
                     Programlistings
   ################################################### -->

    <!-- Verbatim text formatting (<code>) -->
    <xsl:attribute-set name="monospace.properties">
        <xsl:attribute name="font-size">
            <xsl:value-of select="$body.font.small"/>
            <xsl:text>pt</xsl:text>
        </xsl:attribute>
    </xsl:attribute-set>

    <!-- Verbatim text formatting (programlistings) -->
    <xsl:attribute-set name="monospace.verbatim.properties">
        <xsl:attribute name="font-size">
            <xsl:value-of select="$body.font.small"/>
            <xsl:text>pt</xsl:text>
        </xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="verbatim.properties">
        <xsl:attribute name="space-before.minimum">0.9em</xsl:attribute>
        <xsl:attribute name="space-before.optimum">0.9em</xsl:attribute>
        <xsl:attribute name="space-before.maximum">0.9em</xsl:attribute>
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

    <!--###################################################
                        Callouts
   ################################################### -->

    <!-- Use images for callouts instead of (1) (2) (3) -->
    <!--<xsl:param name="callout.graphics">0</xsl:param>-->
    <!--<xsl:param name="callout.unicode">1</xsl:param>-->

    <!-- Place callout marks at this column in annotated areas -->
    <xsl:param name="callout.defaultcolumn">90</xsl:param>

    <!--###################################################
                      Admonitions
   ################################################### -->

    <!-- Use nice graphics for admonitions -->
    <!--<xsl:param name="admon.graphics">'1'</xsl:param>-->
    <!--  <xsl:param name="admon.graphics.path">&admon_gfx_path;</xsl:param> -->

    <!--###################################################
                         Misc
   ################################################### -->

    <!-- Placement of titles -->
    <xsl:param name="formal.title.placement">
        figure after
        example before
        equation before
        table before
        procedure before
    </xsl:param>

    <!-- Format Variable Lists as Blocks (prevents horizontal overflow) -->
    <xsl:param name="variablelist.as.blocks">1</xsl:param>

    <!-- The horrible list spacing problems -->
    <xsl:attribute-set name="list.block.spacing">
        <xsl:attribute name="space-before.optimum">0.8em</xsl:attribute>
        <xsl:attribute name="space-before.minimum">0.8em</xsl:attribute>
        <xsl:attribute name="space-before.maximum">0.8em</xsl:attribute>
        <xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
        <xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
        <xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
    </xsl:attribute-set>

    <!--###################################################
             colored and hyphenated links
   ################################################### -->
    <xsl:template match="ulink">
        <fo:basic-link external-destination="{@url}"
                       xsl:use-attribute-sets="xref.properties"
                       text-decoration="underline"
                       color="blue">
            <xsl:choose>
                <xsl:when test="count(child::node())=0">
                    <xsl:value-of select="@url"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates/>
                </xsl:otherwise>
            </xsl:choose>
        </fo:basic-link>
    </xsl:template>

    <xsl:template name="table.frame">
        <xsl:variable name="frame" select="'all'" />

        <xsl:attribute name="border-start-style">
            <xsl:value-of select="$table.frame.border.style"/>
        </xsl:attribute>
        <xsl:attribute name="border-end-style">
            <xsl:value-of select="$table.frame.border.style"/>
        </xsl:attribute>
        <xsl:attribute name="border-top-style">
            <xsl:value-of select="$table.frame.border.style"/>
        </xsl:attribute>
        <xsl:attribute name="border-bottom-style">
            <xsl:value-of select="$table.frame.border.style"/>
        </xsl:attribute>
        <xsl:attribute name="border-start-width">
            <xsl:value-of select="$table.frame.border.thickness"/>
        </xsl:attribute>
        <xsl:attribute name="border-end-width">
            <xsl:value-of select="$table.frame.border.thickness"/>
        </xsl:attribute>
        <xsl:attribute name="border-top-width">
            <xsl:value-of select="$table.frame.border.thickness"/>
        </xsl:attribute>
        <xsl:attribute name="border-bottom-width">
            <xsl:value-of select="$table.frame.border.thickness"/>
        </xsl:attribute>
        <xsl:attribute name="border-start-color">
            <xsl:value-of select="$table.frame.border.color"/>
        </xsl:attribute>
        <xsl:attribute name="border-end-color">
            <xsl:value-of select="$table.frame.border.color"/>
        </xsl:attribute>
        <xsl:attribute name="border-top-color">
            <xsl:value-of select="$table.frame.border.color"/>
        </xsl:attribute>
        <xsl:attribute name="border-bottom-color">
            <xsl:value-of select="$table.frame.border.color"/>
        </xsl:attribute>
    </xsl:template>

</xsl:stylesheet>
