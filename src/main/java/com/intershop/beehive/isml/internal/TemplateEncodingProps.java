/*
 * Copyright 2021 Intershop Communications AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intershop.beehive.isml.internal;

/**
 * This class is a container for the isml charset,
 * the possible JSP charset, a possible xml charset
 * and the mime type of a ISML template.
 */

public class TemplateEncodingProps
{
    /**
     * The ISML charset.
     */

    private String ismlCharset = null;

    /**
     * The possible JSP charset.
     */

    private String jspCharset = null;

    /**
     * The encoding attribute value of a xml header.
     */

    private String xmlCharset = null;

    /**
     * The template mime type.
     */

    private String mineType = null;

    /**
     * Is a ISCONTENT tag present?
     */

    private boolean isIsContentPresent = false;


    /**
     * The constructor.
     *
     * @param ismlCharset      - the ISML charset
     * @param jspCharset       - the possible JSP charset
     * @param xmlCharset       - the encoding attribute of an xml header
     * @param mimeType         - the template mime type
     */

    public TemplateEncodingProps(String ismlCharset,
                                 String jspCharset,
                                 String xmlCharset,
                                 String mimeType,
                                 boolean isIsContentPresent)
    {
        this.ismlCharset = ismlCharset;
        this.jspCharset = jspCharset;
        this.xmlCharset = xmlCharset;
        this.mineType = mimeType;
        this.isIsContentPresent = isIsContentPresent;
    }

    /**
     * This method returns a charset, which should be used to read
     * an ISML file with.
     *
     * @return the ISML charset
     */

    public String getIsmlCharset()
    {
        return ismlCharset;
    }


    /**
     * This method returns a charset, which should be used to write
     * a JSP file and which is in the contentType attribute of the JSP
     * page tag.
     *
     * @return the JSP charset, or null, if none is set
     */

    public String getJspCharset()
    {
        return jspCharset;
    }

    /**
     * This method returns a charset, which was found in the encoding
     * attribute of an xml header, or null.
     *
     * @return the xml charset, or null, if none is set
     */

    public String getXmlCharset()
    {
        return xmlCharset;
    }


    /**
     * This method returns the mime type, which was given in the ISML
     * ISCONTENT tag with the attribute "type".
     *
     * @return the mime type, or null if none was set
     */

    public String getMimeType()
    {
        return mineType;
    }

    /**
     * Is the IsContent tag present?
     *
     * @return true, if the ISCONTENT tag is present, false otherwise
     */

    public boolean isIsContentPresent()
    {
        return isIsContentPresent;
    }
}

