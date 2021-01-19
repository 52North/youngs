<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:j="http://www.w3.org/2005/xpath-functions"
    xmlns:gmd="http://www.isotc211.org/2005/gmd" xmlns:gco="http://www.isotc211.org/2005/gco"
    xmlns:gmi="http://www.isotc211.org/2005/gmi" xmlns:gml="http://www.opengis.net/gml"
    xmlns:eum="http://www.eumetsat.int/2008/gmi"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" exclude-result-prefixes="j"
    expand-text="yes" version="3.0">

    <xsl:template match="j:map" mode="metadata">
        <gmi:MI_Metadata
            xsi:schemaLocation="http://www.eumetsat.int/2008/gmi http://navigator.eumetsat.int/discovery/rest/schemas/eum/eum.xsd">
            <xsl:apply-templates select="j:string[@key = 'id']"/>
            <xsl:apply-templates select="j:string[@key = 'language']"/>
            <xsl:apply-templates select="j:string[@key = 'hierarchyLevel']"/>
            <xsl:apply-templates  select="j:string[@key = 'hierarchyLevelName'] | j:array[@key = 'hierarchyLevelName']/j:string"/>
            <gmd:contact>
                <xsl:call-template name="eumetsatResponsibleParty">
                    <xsl:with-param name="role" select="'pointOfContact'"/>
                </xsl:call-template>
            </gmd:contact>
            <xsl:apply-templates select="j:string[@key = 'dateStamp']"/>
            <gmd:metadataStandardName>
                <gco:CharacterString>ISO19115</gco:CharacterString>
            </gmd:metadataStandardName>
            <gmd:metadataStandardVersion>
                <gco:CharacterString>2003/Cor.1:2006</gco:CharacterString>
            </gmd:metadataStandardVersion>
            <xsl:call-template name="identificationInfo"/>
            <!-- todo: contentInfo with bands not contained in the JSON -->
            <xsl:call-template name="distribution"/>
            <xsl:call-template name="dataQuality"/>
            <xsl:call-template name="metadataConstraints"/>
            <xsl:call-template name="acquisitionInformation"/>
        </gmi:MI_Metadata>
    </xsl:template>

    <xsl:template match="j:string[@key = 'id']">
        <gmd:fileIdentifier>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmd:fileIdentifier>
    </xsl:template>

    <xsl:template match="j:string[@key = 'language' or @key = 'datasetLanguage']">
        <gmd:language>
            <gmd:LanguageCode codeList="http://www.w3.org/WAI/ER/IG/ert/iso639.htm" codeListValue="{.}"
            />
        </gmd:language>
    </xsl:template>

    <xsl:template match="j:string[@key = 'hierarchyLevel']">
        <gmd:hierarchyLevel>
            <gmd:MD_ScopeCode codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/Codelist/gmxCodelists.xml#MD_ScopeCode" codeListValue="{.}"/>
        </gmd:hierarchyLevel>
    </xsl:template>

    <xsl:template
        match="j:string[@key = 'hierarchyLevelName'] | j:array[@key = 'hierarchyLevelName']/j:string">
        <gmd:hierarchyLevelName>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmd:hierarchyLevelName>
    </xsl:template>

    <xsl:template name="eumetsatResponsibleParty">
        <xsl:param name="role"/>
        <gmd:CI_ResponsibleParty>
            <gmd:individualName>
                <gco:CharacterString>European Organisation for the Exploitation of Meteorological Satellites</gco:CharacterString>
            </gmd:individualName>
            <gmd:organisationName>
                <gco:CharacterString>EUMETSAT</gco:CharacterString>
            </gmd:organisationName>
            <gmd:contactInfo>
                <gmd:CI_Contact>
                    <gmd:phone>
                        <gmd:CI_Telephone>
                            <gmd:voice>
                                <gco:CharacterString>+49(0)6151-807 3660/3770</gco:CharacterString>
                            </gmd:voice>
                            <gmd:facsimile>
                                <gco:CharacterString>+49(0)6151-807 3790</gco:CharacterString>
                            </gmd:facsimile>
                        </gmd:CI_Telephone>
                    </gmd:phone>
                    <gmd:address>
                        <gmd:CI_Address>
                            <xsl:call-template name="eumetsatAddress"/>
                            <gmd:electronicMailAddress>
                                <gco:CharacterString>ops@eumetsat.int</gco:CharacterString>
                            </gmd:electronicMailAddress>
                        </gmd:CI_Address>
                    </gmd:address>
                    <gmd:onlineResource>
                        <gmd:CI_OnlineResource>
                            <gmd:linkage>
                                <gmd:URL>http://www.eumetsat.int</gmd:URL>
                            </gmd:linkage>
                        </gmd:CI_OnlineResource>
                    </gmd:onlineResource>
                </gmd:CI_Contact>
            </gmd:contactInfo>
            <gmd:role>
                <gmd:CI_RoleCode codeList="http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#CI_RoleCode" codeListValue="{$role}">{$role}</gmd:CI_RoleCode>
            </gmd:role>
        </gmd:CI_ResponsibleParty>
    </xsl:template>
    
    <xsl:template name="eumetsatAddress">
        <gmd:deliveryPoint>
            <gco:CharacterString>EUMETSAT Allee 1</gco:CharacterString>
        </gmd:deliveryPoint>
        <gmd:city>
            <gco:CharacterString>Darmstadt</gco:CharacterString>
        </gmd:city>
        <gmd:administrativeArea>
            <gco:CharacterString>Hessen</gco:CharacterString>
        </gmd:administrativeArea>
        <gmd:postalCode>
            <gco:CharacterString>64295</gco:CharacterString>
        </gmd:postalCode>
        <gmd:country>
            <gco:CharacterString>Germany</gco:CharacterString>
        </gmd:country>
    </xsl:template>

    <xsl:template match="j:string[@key = 'dateStamp']">
        <gmd:dateStamp>
            <gco:Date>{.}</gco:Date>
        </gmd:dateStamp>
    </xsl:template>

    <xsl:template name="identificationInfo">
        <gmd:identificationInfo>
            <gmd:MD_DataIdentification>
                <gmd:citation>
                    <gmd:CI_Citation>
                        <xsl:apply-templates select="j:string[@key = 'datasetTitle']"/>
                        <xsl:apply-templates select="j:string[@key = 'alternateTitle'] | j:array[@key = 'alternateTitle']/j:string"/>
                        <xsl:apply-templates select="j:string[@key = 'creation_date'] | j:string[@key = 'revision_date']"/>
                        <xsl:apply-templates select="j:string[@key = 'id']"  mode="resourceIdentifier"/>
                        <xsl:apply-templates select="j:string[@key = 'dataCiteIdentifier']"/>
                        <xsl:apply-templates select="j:map[@key = 'onlineResource'] | j:array[@key = 'onlineResource']/j:map"
                        />
                    </gmd:CI_Citation>
                </gmd:citation>
                <xsl:apply-templates select="j:string[@key = 'abstract']"/>
                <xsl:apply-templates select="j:string[@key = 'productStatus']"/>
                <xsl:apply-templates select="j:map[@key='responsibleParty']"/>
                <xsl:apply-templates select="j:string[@key = 'thumbnails']"/>
                <!-- todo: there are several keyword elements, are all of them still required after migration to native JSON -->
                <xsl:apply-templates select="j:map[@key = 'keywordsAsObjects'] | j:array[@key = 'keywordsAsObjects']/j:map"/>
                <xsl:apply-templates select="j:string[@key = 'data_policy']"/>
                <gmd:resourceConstraints>
                    <gmd:MD_LegalConstraints>
                        <xsl:apply-templates select="j:string[@key = 'conditions']"/>
                        <xsl:apply-templates select="j:string[@key = 'access_constraint']"/>
                        <xsl:apply-templates select="j:string[@key = 'use_constraint']"/>
                    </gmd:MD_LegalConstraints>
                </gmd:resourceConstraints>
                <!-- todo: is this contained in all documents? if not how do we know if it is required? -->
                <gmd:spatialRepresentationType>
                    <gmd:MD_SpatialRepresentationTypeCode  codeList="http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#MD_SpatialRepresentationTypeCode" codeListValue="grid"/>
                </gmd:spatialRepresentationType>
                <xsl:apply-templates select="j:string[@key = 'datasetLanguage']"/>
                <!-- todo: is this contained in all documents? if not how do we know if it is required? -->
                <gmd:characterSet>
                    <gmd:MD_CharacterSetCode codeList="http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#CI_CharacterSetCode" codeListValue="utf8"/>
                </gmd:characterSet>
                <xsl:apply-templates select="j:string[@key = 'topicCategory'] | j:array[@key = 'topicCategory']/j:string"/>
                <xsl:if test="j:string[@key = 'tempextent_begin'] | j:string[@key = 'tempextent_end']">
                    <gmd:extent>
                        <gmd:EX_Extent>
                            <gmd:temporalElement>
                                <gmd:EX_TemporalExtent>
                                    <gmd:extent>
                                        <gml:TimePeriod gml:id="{generate-id(.)}">
                                            <gml:beginPosition>
                                                <xsl:choose>
                                                    <xsl:when test="j:string[@key = 'tempextent_begin']">
                                                        <xsl:value-of select="j:string[@key = 'tempextent_begin']"/>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:attribute name="indeterminatePosition">unknown</xsl:attribute>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </gml:beginPosition>
                                            <gml:endPosition>
                                                <xsl:choose>
                                                    <xsl:when test="j:string[@key = 'tempextent_end']">
                                                        <xsl:value-of select="j:string[@key = 'tempextent_end']"/>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:attribute name="indeterminatePosition">unknown</xsl:attribute>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </gml:endPosition>
                                        </gml:TimePeriod>
                                    </gmd:extent>
                                </gmd:EX_TemporalExtent>
                            </gmd:temporalElement>
                        </gmd:EX_Extent>
                    </gmd:extent>
                </xsl:if>
                <xsl:apply-templates select="j:map[@key = 'location']"/>
                <xsl:apply-templates select="j:string[@key = 'coverage'] | j:array[@key = 'coverage']/j:string"/>
            </gmd:MD_DataIdentification>
        </gmd:identificationInfo>
    </xsl:template>

    <xsl:template match="j:string[@key = 'datasetTitle']">
        <gmd:title>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmd:title>
    </xsl:template>

    <xsl:template
        match="j:string[@key = 'alternateTitle'] | j:array[@key = 'alternateTitle']/j:string">
        <gmd:alternateTitle>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmd:alternateTitle>
    </xsl:template>

    <xsl:template match="j:string[@key = 'creation_date']">
        <xsl:call-template name="dateWithCode">
            <xsl:with-param name="date" select="."/>
            <xsl:with-param name="code" select="'creation'"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="j:string[@key = 'revision_date']">
        <xsl:call-template name="dateWithCode">
            <xsl:with-param name="date" select="."/>
            <xsl:with-param name="code" select="'revision'"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="dateWithCode">
        <xsl:param name="date"/>
        <xsl:param name="code"/>
        <gmd:date>
            <gmd:CI_Date>
                <gmd:date>
                    <gco:Date>{$date}</gco:Date>
                </gmd:date>
                <gmd:dateType>
                    <gmd:CI_DateTypeCode codeList="http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#CI_DateTypeCode" codeListValue="{$code}"/>
                </gmd:dateType>
            </gmd:CI_Date>
        </gmd:date>
    </xsl:template>

    <xsl:template match="j:string[@key = 'id']" mode="resourceIdentifier">
        <gmd:identifier>
            <gmd:MD_Identifier>
                <gmd:code>
                    <gco:CharacterString>{.}</gco:CharacterString>
                </gmd:code>
            </gmd:MD_Identifier>
        </gmd:identifier>
    </xsl:template>

    <xsl:template match="j:string[@key = 'dataCiteIdentifier']">
        <gmd:identifier>
            <gmd:RS_Identifier>
                <gmd:code>
                    <gco:CharacterString>{.}</gco:CharacterString>
                </gmd:code>
                <gmd:codeSpace>
                    <gco:CharacterString>http://datacite.org</gco:CharacterString>
                </gmd:codeSpace>
            </gmd:RS_Identifier>
        </gmd:identifier>
    </xsl:template>
    
    <xsl:template match="j:map[@key='responsibleParty']">
        <gmd:pointOfContact>
            <xsl:call-template name="responsibleParty"></xsl:call-template>
        </gmd:pointOfContact>
    </xsl:template>
    
    <xsl:template name="responsibleParty">
        <gmd:CI_ResponsibleParty>
            <xsl:apply-templates select="j:string[@key='individualName']"/>
            <xsl:apply-templates select="j:string[@key='organisationName']"/>
            <gmd:contactInfo>
                <gmd:CI_Contact>
                    <gmd:phone>
                        <gmd:CI_Telephone>
                            <xsl:apply-templates select="j:string[@key='phone']"/>
                            <xsl:apply-templates select="j:string[@key='facsimile']"/>
                        </gmd:CI_Telephone>
                    </gmd:phone>
                    <gmd:address>
                        <gmd:CI_Address>
                            <xsl:if test="normalize-space(j:string[@key='organisationName']) = 'EUMETSAT'">
                                <!-- todo: addresses from other organizations? -->
                                <xsl:call-template name="eumetsatAddress"/>
                            </xsl:if>
                            <xsl:apply-templates select="j:string[@key='email']"/>
                        </gmd:CI_Address>
                    </gmd:address>
                    <xsl:apply-templates select="j:string[@key='url']"/>
                </gmd:CI_Contact>
            </gmd:contactInfo>
            <gmd:role>
                <gmd:CI_RoleCode codeList="http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#CI_RoleCode" codeListValue="{j:string[@key='responsiblePartyRole']}">{j:string[@key='responsiblePartyRole']}</gmd:CI_RoleCode>
            </gmd:role>
        </gmd:CI_ResponsibleParty>
    </xsl:template>
    
    <xsl:template match="j:string[@key='individualName']">
        <gmd:individualName>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmd:individualName>
    </xsl:template>
    
    <xsl:template match="j:string[@key='organisationName']">
        <gmd:organisationName>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmd:organisationName>
    </xsl:template>
    
    <xsl:template match="j:string[@key='phone']">
        <gmd:voice>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmd:voice>
    </xsl:template>
    
    <xsl:template match="j:string[@key='facsimile']">
        <gmd:facsimile>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmd:facsimile>
    </xsl:template>
    
    <xsl:template match="j:string[@key='email']">
        <gmd:electronicMailAddress>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmd:electronicMailAddress>
    </xsl:template>
    
    <xsl:template match="j:string[@key='url']">
        <gmd:onlineResource>
            <gmd:CI_OnlineResource>
                <gmd:linkage>
                    <gmd:URL>{.}</gmd:URL>
                </gmd:linkage>
            </gmd:CI_OnlineResource>
        </gmd:onlineResource>
    </xsl:template>

    <xsl:template match="j:map[@key = 'onlineResource'] | j:array[@key = 'onlineResource']/j:map">
        <gmd:citedResponsibleParty>
            <gmd:CI_ResponsibleParty>
                <gmd:contactInfo>
                    <gmd:CI_Contact>
                        <gmd:onlineResource>
                            <gmd:CI_OnlineResource>
                                <gmd:linkage>
                                    <gmd:URL>{j:string[@key='url']}</gmd:URL>
                                </gmd:linkage>
                                <gmd:name>
                                    <gco:CharacterString>{j:string[@key='label']}</gco:CharacterString>
                                </gmd:name>
                                <gmd:function>
                                    <gmd:CI_OnLineFunctionCode codeList="http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#CI_OnLineFunctionCode" codeListValue="information"/>
                                </gmd:function>
                            </gmd:CI_OnlineResource>
                        </gmd:onlineResource>
                    </gmd:CI_Contact>
                </gmd:contactInfo>
                <gmd:role>
                    <gmd:CI_RoleCode codeList="http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#CI_RoleCode" codeListValue="author"/>
                </gmd:role>
            </gmd:CI_ResponsibleParty>
        </gmd:citedResponsibleParty>
    </xsl:template>

    <xsl:template match="j:string[@key = 'abstract']">
        <gmd:abstract>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmd:abstract>
    </xsl:template>

    <xsl:template match="j:string[@key = 'productStatus']">
        <gmd:status>
            <gmd:MD_ProgressCode
                codeList="http://navigator.eumetsat.int/metadata_schema/eum/resources/Codelist/eum_gmxCodelists.xml#MD_ProgressCode"
                codeListValue="{.}"/>
        </gmd:status>
    </xsl:template>

    <xsl:template match="j:string[@key = 'thumbnails']">
        <gmd:graphicOverview>
            <gmd:MD_BrowseGraphic>
                <gmd:fileName>
                    <gco:CharacterString>{.}</gco:CharacterString>
                </gmd:fileName>
            </gmd:MD_BrowseGraphic>
        </gmd:graphicOverview>
    </xsl:template>

    <xsl:template
        match="j:map[@key = 'keywordsAsObjects'] | j:array[@key = 'keywordsAsObjects']/j:map">
        <gmd:descriptiveKeywords>
            <gmd:MD_Keywords>
                <xsl:apply-templates select="j:array[@key = 'keyword']/j:string"/>
                <xsl:apply-templates select="j:string[@key = 'type']"/>
            </gmd:MD_Keywords>
        </gmd:descriptiveKeywords>
    </xsl:template>

    <xsl:template match="j:array[@key = 'keyword']/j:string">
        <gmd:keyword>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmd:keyword>
    </xsl:template>

    <xsl:template
        match="j:map[@key = 'keywordsAsObjects']/j:string[@key = 'type'] | j:array[@key = 'keywordsAsObjects']/j:map/j:string[@key = 'type']">
        <gmd:type>
            <gmd:MD_KeywordTypeCode codeList="{../j:string[@key='typeList']}" codeListValue="{.}"/>
        </gmd:type>
    </xsl:template>

    <xsl:template match="j:string[@key = 'data_policy']">
        <gmd:resourceConstraints>
            <gmd:MD_SecurityConstraints>
                <gmd:classification>
                    <gmd:MD_ClassificationCode codeList="http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#MD_ClassificationCode" codeListValue="{.}"/>
                </gmd:classification>
            </gmd:MD_SecurityConstraints>
        </gmd:resourceConstraints>
    </xsl:template>

    <xsl:template match="j:string[@key = 'conditions']">
        <gmd:useLimitation>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmd:useLimitation>
    </xsl:template>

    <xsl:template match="j:string[@key = 'access_constraint']">
        <gmd:accessConstraints>
            <gmd:MD_RestrictionCode codeList="http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#MD_RestrictionCode" codeListValue="{.}"/>
        </gmd:accessConstraints>
    </xsl:template>

    <xsl:template match="j:string[@key = 'use_constraint']">
        <gmd:useConstraints>
            <gmd:MD_RestrictionCode codeList="http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#MD_RestrictionCode" codeListValue="{.}"/>
        </gmd:useConstraints>
    </xsl:template>

    <xsl:template match="j:string[@key = 'topicCategory'] | j:array[@key = 'topicCategory']/j:string">
        <gmd:topicCategory>
            <gmd:MD_TopicCategoryCode>{.}</gmd:MD_TopicCategoryCode>
        </gmd:topicCategory>
    </xsl:template>

    <xsl:template match="j:map[@key = 'location']">
        <gmd:extent>
            <gmd:EX_Extent>
                <gmd:geographicElement>
                    <gmd:EX_GeographicBoundingBox>
                        <gmd:extentTypeCode>
                            <gco:Boolean>true</gco:Boolean>
                        </gmd:extentTypeCode>
                        <gmd:westBoundLongitude>
                            <gco:Decimal>{j:array[@key='coordinates']/j:array[1]/j:number[1]}</gco:Decimal>
                        </gmd:westBoundLongitude>
                        <gmd:eastBoundLongitude>
                            <gco:Decimal>{j:array[@key='coordinates']/j:array[2]/j:number[1]}</gco:Decimal>
                        </gmd:eastBoundLongitude>
                        <gmd:southBoundLatitude>
                            <gco:Decimal>{j:array[@key='coordinates']/j:array[2]/j:number[2]}</gco:Decimal>
                        </gmd:southBoundLatitude>
                        <gmd:northBoundLatitude>
                            <gco:Decimal>{j:array[@key='coordinates']/j:array[1]/j:number[2]}</gco:Decimal>
                        </gmd:northBoundLatitude>
                    </gmd:EX_GeographicBoundingBox>
                </gmd:geographicElement>
            </gmd:EX_Extent>
        </gmd:extent>
    </xsl:template>
    
    <xsl:template match="j:string[@key = 'coverage'] | j:array[@key = 'coverage']/j:string">
        <gmd:extent>
            <gmd:EX_Extent>
                <gmd:geographicElement>
                    <gmd:EX_GeographicDescription>
                        <gmd:geographicIdentifier>
                            <gmd:MD_Identifier>
                                <gmd:code>
                                    <gco:CharacterString>{.}</gco:CharacterString>
                                </gmd:code>
                            </gmd:MD_Identifier>
                        </gmd:geographicIdentifier>
                    </gmd:EX_GeographicDescription>
                </gmd:geographicElement>
            </gmd:EX_Extent>
        </gmd:extent>
    </xsl:template>
    
    <xsl:template name="distribution">
        <xsl:if test="j:map[@key='digitalTransfers'] | j:map[@key='distributorContact']"></xsl:if>
        <gmd:distributionInfo>
            <gmd:MD_Distribution>
                <gmd:distributor>
                    <eum:MD_EUMDistributor>
                        <xsl:apply-templates select="j:map[@key='distributorContact']"/>
                        <xsl:apply-templates select="j:array[@key='digitalTransfers']/j:map | j:map[@key='digitalTransfers']"/>
                    </eum:MD_EUMDistributor>
                </gmd:distributor>
            </gmd:MD_Distribution>
        </gmd:distributionInfo>
    </xsl:template>
    
    <xsl:template match="j:map[@key='distributorContact']">
        <gmd:distributorContact>
            <xsl:call-template name="responsibleParty"/>
        </gmd:distributorContact>
    </xsl:template>
    
    <xsl:template match="j:array[@key='digitalTransfers']/j:map | j:map[@key='digitalTransfers']">
        <eum:digitalTransfers>
            <eum:MD_EUMDigitalTransfer>
                <xsl:if test="j:array[@key='availability']/j:map | j:map[@key='availability']">
                    <eum:availability>
                        <eum:MD_EUMDigitalTransferOptions>
                            <xsl:apply-templates select="j:array[@key='availability']/j:map | j:map[@key='availability']"/>
                            <xsl:apply-templates select="j:array[@key='availability']/j:map[1]/j:string[@key='availability'] | j:map[@key='availability']/j:string[@key='availability']" mode="availability"/>
                            <xsl:apply-templates select="j:array[@key='availability']/j:map[1]/j:string[@key='channels'] | j:map[@key='availability']/j:string[@key='channels']" mode="availability"/>
                        </eum:MD_EUMDigitalTransferOptions>
                    </eum:availability>
                </xsl:if>
                <xsl:apply-templates select="j:array[@key='format']/j:map | j:map[@key='format']"/>
            </eum:MD_EUMDigitalTransfer>
        </eum:digitalTransfers>
    </xsl:template>
    
    <xsl:template match="j:array[@key='availability']/j:map | j:map[@key='availability']">
        <gmd:onLine>
            <gmd:CI_OnlineResource>
                <xsl:apply-templates select="j:string[@key='url']" mode="availability"/>
                <xsl:apply-templates select="j:string[@key='protocol']" mode="availability"/>
                <xsl:apply-templates select="j:string[@key='label']" mode="availability"/>
                <gmd:function>
                    <gmd:CI_OnLineFunctionCode  codeList="http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#CI_OnLineFunctionCode" codeListValue="{j:string[@key='function']}"/>
                </gmd:function>
            </gmd:CI_OnlineResource>
        </gmd:onLine>
    </xsl:template>
    
    <xsl:template match="j:string[@key='url']" mode="availability">
        <gmd:linkage>
            <gmd:URL>{.}</gmd:URL>
        </gmd:linkage>
    </xsl:template>
    
    <xsl:template match="j:string[@key='protocol']" mode="availability">
        <gmd:protocol>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmd:protocol>
    </xsl:template>

    <xsl:template match="j:string[@key='label']" mode="availability">
        <gmd:name>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmd:name>
    </xsl:template>
    
    <xsl:template match="j:string[@key='availability']" mode="availability">
        <eum:availability>
            <gco:CharacterString>{.}</gco:CharacterString>
        </eum:availability>
    </xsl:template>
    
    <xsl:template match="j:string[@key='channels']" mode="availability">
        <eum:eumetcastChannels>
            <gco:CharacterString>{.}</gco:CharacterString>
        </eum:eumetcastChannels>
    </xsl:template>
    
    <xsl:template match="j:array[@key='format']/j:map | j:map[@key='format']">
        <eum:format>
            <eum:MD_EUMFormat>
                <xsl:apply-templates select="j:string[@key='name']" mode="format"/>
                <xsl:apply-templates select="j:string[@key='version']" mode="format"/>
                <xsl:apply-templates select="j:array[@key='typicalFilename']/j:string | j:string[@key='typicalFilename']" mode="format"/>
                <xsl:apply-templates select="j:string[@key='averageFileSizeUnits']" mode="format"/>
                <xsl:apply-templates select="j:string[@key='averageFileSize']" mode="format"/>
                <xsl:apply-templates select="j:string[@key='frequency']" mode="format"/>
                <xsl:apply-templates select="j:string[@key='fileFormatDescription']" mode="format"/>
                <xsl:apply-templates select="j:string[@key='filenameConvention']" mode="format"/>
                <xsl:apply-templates select="j:map[@key='documentation']" mode="format"/>
                <xsl:apply-templates select="j:string[@key='format4DataCite']" mode="format"/>
            </eum:MD_EUMFormat>
        </eum:format>
    </xsl:template>
    
    <xsl:template match="j:string[@key='name']" mode="format">
        <gmd:name>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmd:name>
    </xsl:template>
    
    <xsl:template match="j:string[@key='version']" mode="format">
        <gmd:version>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmd:version>
    </xsl:template>
    
    <xsl:template match="j:array[@key='typicalFilename']/j:string | j:string[@key='typicalFilename']" mode="format">
        <eum:typicalFilename>
            <gco:CharacterString>{.}</gco:CharacterString>
        </eum:typicalFilename>
    </xsl:template>
    
    <xsl:template match="j:string[@key='averageFileSizeUnits']" mode="format">
        <eum:averageFileSizeUnits>
            <gco:CharacterString>{.}</gco:CharacterString>
        </eum:averageFileSizeUnits>
    </xsl:template>
    
    <xsl:template match="j:string[@key='averageFileSize']" mode="format">
        <eum:averageFileSize>
            <gco:Real>{.}</gco:Real>
        </eum:averageFileSize>
    </xsl:template>
    
    <xsl:template match="j:string[@key='frequency']" mode="format">
        <eum:frequency>
            <gco:CharacterString>{.}</gco:CharacterString>
        </eum:frequency>
    </xsl:template>
    
    <xsl:template match="j:string[@key='fileFormatDescription']" mode="format">
        <eum:fileFormatDescription>
            <gco:CharacterString>{.}</gco:CharacterString>
        </eum:fileFormatDescription>
    </xsl:template>
    
    <xsl:template match="j:string[@key='filenameConvention']" mode="format">
        <eum:filenameConvention>
            <gco:CharacterString>{.}</gco:CharacterString>
        </eum:filenameConvention>
    </xsl:template>
    
    <xsl:template match="j:map[@key='documentation']" mode="format">
        <eum:documentation>
            <gmd:CI_OnlineResource>
                <xsl:apply-templates select="j:string[@key='url']" mode="format"/>
                <xsl:apply-templates select="j:string[@key='description']" mode="format"/>
            </gmd:CI_OnlineResource>
        </eum:documentation>
    </xsl:template>
    
    <xsl:template match="j:string[@key='url']" mode="format">
        <gmd:linkage>
            <gmd:URL>{.}</gmd:URL>
        </gmd:linkage>
    </xsl:template>
    
    <xsl:template match="j:string[@key='description']" mode="format">
        <gmd:description>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmd:description>
    </xsl:template>
    
    <xsl:template match="j:string[@key='format4DataCite']" mode="format">
        <eum:format4DataCite>
            <gco:Boolean>{.}</gco:Boolean>
        </eum:format4DataCite>
    </xsl:template>
    
    <xsl:template name="dataQuality">
        <gmd:dataQualityInfo>
            <gmd:DQ_DataQuality>
                <gmd:scope>
                    <gmd:DQ_Scope>
                        <gmd:level>
                            <gmd:MD_ScopeCode codeList="http://standards.iso.org/iso/19139/resources/gmxCodelists.xml/gmxCodelists.xml#MD_ScopeCode" codeListValue="{j:string[@name='hierarchyLevel']}"/>
                        </gmd:level>
                    </gmd:DQ_Scope>
                </gmd:scope>
                <gmd:lineage>
                    <gmd:LI_Lineage>
                        <gmd:statement>
                            <gco:CharacterString>{j:string[@key='quality_statement']}</gco:CharacterString>
                        </gmd:statement>
                    </gmd:LI_Lineage>
                </gmd:lineage>
            </gmd:DQ_DataQuality>
        </gmd:dataQualityInfo>
    </xsl:template>
    
    <xsl:template name="metadataConstraints">
        <!-- todo: should this be kept in the ISO metadata? -->
        <xsl:if test="j:string[@key='eumdata'] = 'true'">
            <gmd:metadataConstraints>
                <gmd:MD_Constraints>
                    <gmd:useLimitation>
                        <gco:CharacterString>http://navigator.eumetsat.int/metadata_schema#ShownAsEUMETSATData</gco:CharacterString>
                    </gmd:useLimitation>
                </gmd:MD_Constraints>
            </gmd:metadataConstraints>
        </xsl:if>
        <xsl:if test="j:string[@key='iopaccess'] = 'true'">
            <gmd:metadataConstraints>
                <gmd:MD_Constraints>
                    <gmd:useLimitation>
                        <gco:CharacterString>http://navigator.eumetsat.int/metadata_schema#IOPAccess</gco:CharacterString>
                    </gmd:useLimitation>
                </gmd:MD_Constraints>
            </gmd:metadataConstraints>
        </xsl:if>
    </xsl:template>

    <xsl:template name="acquisitionInformation">
        <xsl:if test="j:string[@key='satellite'] | j:string[@key='orbitType']">
            <gmi:acquisitionInformation>
                <gmi:MI_AcquisitionInformation>
                    <gmi:platform>
                        <eum:MI_EUMPlatform>
                            <xsl:apply-templates select="j:string[@key='satellite']"/>
                            <xsl:apply-templates select="j:string[@key='satellite_description']"/>
                            <xsl:apply-templates select="j:string[@key='instrument']"/>
                            <xsl:apply-templates select="j:string[@key='orbitType']"/>
                        </eum:MI_EUMPlatform>
                    </gmi:platform>
                </gmi:MI_AcquisitionInformation>
            </gmi:acquisitionInformation>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="j:string[@key='satellite']">
        <gmi:identifier>
            <gmd:MD_Identifier>
                <gmd:code>
                    <gco:CharacterString>{.}</gco:CharacterString>
                </gmd:code>
            </gmd:MD_Identifier>
        </gmi:identifier>
    </xsl:template>

    <xsl:template match="j:string[@key='satellite_description']">
        <gmi:description>
            <gco:CharacterString>{.}</gco:CharacterString>
        </gmi:description>
    </xsl:template>

    <xsl:template match="j:string[@key='instrument']">
        <gmi:instrument>
            <eum:MI_EUMInstrument>
                <gmi:citation>
                    <gmd:CI_Citation>
                        <gmd:title>
                            <gco:CharacterString>{.}</gco:CharacterString>
                        </gmd:title>
                        <gmd:date>
                            <gmd:CI_Date>
                                <!-- todo: the date is stored nowhere in the JSON -->
                                <gmd:date gco:nilReason="missing"/>
                                <gmd:dateType>
                                    <gmd:CI_DateTypeCode codeList="http://standards.iso.org/iso/19139/resources/gmxCodelists.xml/gmxCodelists.xml#CI_DateTypeCode" codeListValue="creation"/>
                                </gmd:dateType>
                            </gmd:CI_Date>
                        </gmd:date>
                        <gmd:identifier>
                            <gmd:MD_Identifier>
                                <gmd:code>
                                    <gco:CharacterString>{.}</gco:CharacterString>
                                </gmd:code>
                            </gmd:MD_Identifier>
                        </gmd:identifier>
                    </gmd:CI_Citation>
                </gmi:citation>
                <gmi:type>
                    <gmi:MI_SensorTypeCode id="{generate-id(.)}" uuid="{generate-id(.)}"/>
                </gmi:type>
                <eum:type>
                    <eum:MI_EUMSensorTypeCode codeList="http://navigator.eumetsat.int/metadata_schema/eum/resources/Codelist/eum_gmxCodelists.xml#CI_SensorTypeCode" codeListValue="{../j:string[@key='instrument_type']}"/>
                </eum:type>
            </eum:MI_EUMInstrument>
        </gmi:instrument>
    </xsl:template>
    
    <xsl:template match="j:string[@key='orbitType']">
        <eum:orbitType>
            <eum:CI_OrbitTypeCode codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/Codelist/gmxCodelists.xml#CI_OrbitTypeCode" codeListValue="GEO"/>
        </eum:orbitType>
    </xsl:template>
</xsl:stylesheet>
