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

import com.intershop.beehive.isml.capi.ISMLCompilerConfiguration;
import com.intershop.beehive.isml.capi.ISMLException;
import com.intershop.beehive.isml.capi.ISMLTemplateConstants;
import com.intershop.beehive.isml.internal.parser.ISMLtoJSPcompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This helper class offers a method to convert an .isml file into an jsp
 * file.
 */

public class TemplatePrecompileUtils
{
    /**
     * pattern for the iscontent charset
     */

    private Pattern patternCharSet = null;

    /**
     *pattern for the iscontent type
     */

    private Pattern patternType = null;

    /**
     * pattern for the xml encoding
     */

    private Pattern patternXML = null;

    /**
     * Default encoding for generated jsp and java source files.
     */

    private static final String JAVA_ENCODING = "UTF8";

    /**
     * Encoding for UTF-16 with big-endian .
     */

    private static final String UNICODE_BIG = "UTF-16BE";

    /**
     * Encoding for UTF-16 with little-endian .
     */

    private static final String UNICODE_LITTLE = "UTF-16LE";

    /**
     * The default character set.
     */
    // private final String defaultCharSet;

    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** 
     * Configuration of the isml compiler. 
     */
    private ISMLCompilerConfiguration compilerConfiguration;

    /**
     * The constructor. Creates a new TemplatePrecompileUtils class. <BR> <BR>
     */

    public TemplatePrecompileUtils(ISMLCompilerConfiguration compilerConfiguration)
    {
        this.compilerConfiguration = compilerConfiguration;

        // we're looking for 'charset' attribute from ISML ISCONTENT tag
        patternCharSet = Pattern.compile("(iscontent)[^>]+(charset)[:blank:]*=[:blank:]*(\"|')([^\"']+?)(\"|')", Pattern.CASE_INSENSITIVE);

        // we're looking also for 'type' attribute from ISML ISCONTENT tag
        patternType =    Pattern.compile("(iscontent)[^>]+(type)[:blank:]*=[:blank:]*(\"|')([^\"']+?)(\"|')", Pattern.CASE_INSENSITIVE);

        // look for the encoding attribute in the xml header
        patternXML =     Pattern.compile("(<[?]xml)[^>]+(encoding)[:blank:]*=[:blank:]*(\"|')([^\"']+?)(\"|')", Pattern.CASE_INSENSITIVE);
        
    }

    /**
     * This method executes the 1st compilation step. It compiles the isml file to a jsp file
     * and creates all required directories and removes outdated files on the fly.
     *
     * @param sourceFile       - the source ISML file
     * @param destinationFile  - the resulting JSP file
     * @throws IOException if an IO error occurs during the process.
     * @throws ISMLException if the compilation failed.
     */

    public void compileISML(File sourceFile, File destinationFile) throws IOException, ISMLException
    {
        // template source input stream
        InputStream sourceIn = null;

        // JSP output stream
        OutputStreamWriter jspOut = null;

        File jspOutputFile = destinationFile;

        // log, which ISML file should be compiled
        logger.debug("Compiling ISML file: {} to {}", sourceFile.getAbsolutePath(), jspOutputFile.getAbsolutePath());

        // get template encoding
        TemplateEncodingProps templateEncProps = findIsmlEncoding(sourceFile);

        // determine the encoding of the resulting jspFile
        String outCharset = findJspEncoding(templateEncProps);

        logger.debug("Using charset {} to write JSP file.", outCharset);

        try
        {
            InputStream inStream =
                new ByteArrayInputStream(unicodeEscape(sourceFile,
                                                       templateEncProps.getIsmlCharset()));

            if (!templateEncProps.isIsContentPresent())
            {
                // dummy iscontent entry
                String defaultIsContent = "<iscontent charset=\"" + outCharset + "\">";
                // insert dummy iscontent at the beginning of the file
                inStream = new SequenceInputStream(new ByteArrayInputStream(defaultIsContent.getBytes()),
                                                   inStream);
            }

            // JavaCC will work with the Unicode-escaped data to preserve integrity
            sourceIn = new BufferedInputStream(inStream, ISMLTemplateConstants.DEFAULT_TEMPLATE_BUFFERSIZE);
            sourceIn.mark(Integer.MAX_VALUE);

            // this is the writer that the JavaCC compiler classes will use
            jspOut = new OutputStreamWriter(new FileOutputStream(jspOutputFile), outCharset);

            // compile
            ISMLtoJSPcompiler pagePreProcessor = new ISMLtoJSPcompiler(sourceIn);

            if (!pagePreProcessor.compileTemplate(ISMLtoJSPcompiler.ALLOW_ALL,
                                                  jspOut, sourceFile, sourceIn))
            {
                throw new ISMLException("Failed to compile ISML to JSP.");
            }

            jspOut.flush();
        }
        catch (ISMLException sevx)
        {
            // first close the output stream
            if (jspOut != null)
            {
                try
                {
                    jspOut.close();
                }
                catch (IOException e)
                {
                    // do nothing
                    logger.debug(e.getMessage(), e);
                }
            }

            // remove .jsp file, because it contains incomplete content
            if (jspOutputFile.exists() && jspOutputFile.isFile())
            {
                jspOutputFile.delete();
            }
            throw sevx;
        }
        finally
        {
            // do cleanup
            if (sourceIn != null)
            {
                try
                {
                    sourceIn.close();
                }
                catch (IOException e)
                {
                    // do nothing
                    logger.debug(e.getMessage(), e);
                }
                sourceIn = null;
            }

            if (jspOut != null)
            {
                try
                {
                    jspOut.close();
                }
                catch (IOException e)
                {
                    // do nothing
                    logger.debug(e.getMessage(), e);
                }
                jspOut = null;
            }
        }
    }

    protected ISMLCompilerConfiguration getCompilerConfiguration()
    {
        if (null != compilerConfiguration)
        {
            return compilerConfiguration; 
        }
        
        return new ISMLCompilerConfiguration()
        {
            @Override
            public String getDefaultContentEncoding()
            {
                return ISMLTemplateConstants.DEFAULT_CHARSET;
            }

            @Override
            public String getJspEncoding(String mimeType)
            {
                return getDefaultContentEncoding();
            }
        };
    }
    

    /**
     * Helper method, try to find charset encoding in first 1024 bytes
     * in the provided template file (assuming ASCII compatible charset).
     *
     * @param   aFile the file object for the template
     * @return  the encoding for the template, or the system encoding, if an
     *          error occurs or none is found
     */

    protected TemplateEncodingProps findIsmlEncoding(File aFile)
    {
        String ismlCharset = null;
        String jspCharset = null;
        String xmlCharset = null;
        String type = null;
        byte[] scan = new byte[1024];
        String scanString = null;
        FileInputStream scanFile = null;
        TemplateEncodingProps encProps = null;
        boolean isIsContentPresent = false;

        try
        {
            scanFile = new FileInputStream(aFile);

            scanFile.read(scan, 0, scanFile.available()<1024?scanFile.available():1024);

            // check for real unicode
            if (scan[0]==(byte)0xFF && scan[1]==(byte)0xFE)
            {
                // little-endian unicode file
                ismlCharset = UNICODE_LITTLE;
            }
            else if (scan[0]==(byte)0xFE && scan[1]==(byte)0xFF)
            {
                // big-endian unicode file
                ismlCharset = UNICODE_BIG;
            }
            else if (scan[0]==(byte)0xEF && scan[1]==(byte)0xBB && scan[2]==(byte)0xBF)
            {
                // utf-8 encoded file
                ismlCharset = JAVA_ENCODING;
            }
            
            scanString = new String(scan, JAVA_ENCODING);

            // check for ISML ISCONTENT tag with attribute charset using regexp engine
            if (patternCharSet != null) //may only be null if screwed during the static init.
            {

                Matcher matcher = patternCharSet.matcher(scanString);
                if (matcher.find())
                {
                    String isContentHTMLCharset = matcher.group(4);
    
                    // convert the ISCONTENT charset attribute to a Java charset
                    jspCharset = CharacterSetMappings.mapHttpToCharset(isContentHTMLCharset);
    
                    // test, if the ISCONTENT charset is valid
                    try
                    {
                        byte[] testChar = {(byte)80};
                        new String(testChar, jspCharset);
                    }
                    catch (UnsupportedEncodingException usex)
                    {
                        logger.debug("The ISCONTENT charset attribute {} doesn't describe a valid charset.", isContentHTMLCharset);
                        jspCharset = null;
                    }
    
                    // if no file prefix was found, use the the ISCONTENT charset
                    // to read the isml file
                    if (ismlCharset == null)
                    {
                        ismlCharset = jspCharset;
                    }
                } // else do nothing
            } // else proceed; charset can not be determined, will use system default

            // check if at least one ISCONTENT tag is present
            if (scanString.toUpperCase().indexOf("ISCONTENT") != -1)
            {
                isIsContentPresent = true;
            }
        }
        catch (Exception e)
        {
            logger.error("A problem occurred while trying to find the charset for the template: {}", e.getMessage());
            ismlCharset = null;
        }
        finally
        {
            if (scanFile != null)
            {
                try
                {
                    scanFile.close();
                }
                catch (Exception e)
                {
                    // do nothing
                    logger.debug(e.getMessage(), e);
                }
            }
        }

        if (ismlCharset == null)
        {
            ismlCharset = System.getProperty("file.encoding");
            logger.debug("Could not determine ISML charset, assuming systems: {}", ismlCharset);
        }
        else
        {
            logger.debug("Using charset {} to read ISML file.", ismlCharset);
        }

        // check for ISML ISCONTENT tag with attribute type using regexp engine
        if (patternType != null) //may only be null if screwed during the static init.
        {

            try
            {
                // convert the file string to the given isml charset
                String typeString = new String(scan, ismlCharset);
                
                Matcher matcher = patternType.matcher(typeString);
                if (matcher.find())
                {
                    type = matcher.group(4);
                } // else do nothing

            }
            catch (UnsupportedEncodingException ex)
            {
                // charset is not valid, so set type to null
                type = ISMLTemplateConstants.TYPE_HTML;
            }
        }

        // check for an xml head tag
        if (patternXML != null) //may only be null if screwed during the static init.
        {

            try
            {
                // convert the file string to the given isml charset
                String xmlString = new String(scan, ismlCharset);

                Matcher matcher = patternXML.matcher(xmlString);
                if (matcher.find())
                {
                    String xmlEncodingCharset = matcher.group(4);

                    // convert the xml charset attribute to a Java charset
                    xmlCharset = CharacterSetMappings.mapHttpToCharset(xmlEncodingCharset);

                    // test, if the xml charset is valid
                    try
                    {
                        byte[] testChar = {(byte)80};
                        new String(testChar, xmlCharset);
                        type = ISMLTemplateConstants.TYPE_XML;
                    }
                    catch (UnsupportedEncodingException usex)
                    {
                        logger.debug("The XML encoding attribute {} doesn't describe a valid charset.", xmlEncodingCharset);
                        xmlCharset = null;
                    }
                } // else do nothing

            }
            catch (UnsupportedEncodingException ex)
            {
                // charset is not valid, so set type to null
                xmlCharset = null;
            }
        }

        encProps = new TemplateEncodingProps(ismlCharset,
                                             jspCharset,
                                             xmlCharset,
                                             type,
                                             isIsContentPresent);

        return encProps;
    }

    /**
     * A helper method to determine the file encoding for the resulting JSP
     * output file.
     *
     * @param templateEncRes - an container for the template charset, mime type
     *                         and the contentCharset flag
     *
     * @return the file encoding to use for the resulting JSP output file
     */

    protected String findJspEncoding(TemplateEncodingProps templateEncRes)
    {
        String jspCharset = templateEncRes.getJspCharset();
        String type = templateEncRes.getMimeType();
        String defaultContentCharset =
            CharacterSetMappings.mapHttpToCharset(determineDefaultHTMLCharset());
        String cutomEncoding = null;

        // if no type is specified, assume "text/html"
        if (type == null)
        {
            type = ISMLTemplateConstants.TYPE_HTML;
        }

        if (type.equalsIgnoreCase(ISMLTemplateConstants.TYPE_HTML))
        {
            // 1. determine, if a custom charset mapping exists for text/html
            cutomEncoding = getCustomCharsetMapping(ISMLTemplateConstants.TYPE_HTML);
            if (cutomEncoding != null)
            {
                return cutomEncoding;
            }

            // 2. no explicit charset given, take a look at the default content encoding
            // settings
            return defaultContentCharset;
        }
        else if (type.startsWith(ISMLTemplateConstants.TYPE_XML))
        {
            // 1. detemerine the xml header charset
            if (templateEncRes.getXmlCharset() != null)
            {
                return templateEncRes.getXmlCharset();
            }

            // 2. use the ISCONTENT charset, if one was given
            if (jspCharset != null)
            {
                return jspCharset;
            }

            // 3. determine, if a custom charset mapping exists for text/xml
            cutomEncoding = getCustomCharsetMapping(ISMLTemplateConstants.TYPE_XML);
            if (cutomEncoding != null)
            {
                return cutomEncoding;
            }

            // 4. use the default charset
            return defaultContentCharset;
        }
        else if (type.startsWith("text/"))
        {
            // 1. use the ISCONTENT charset, if one was given
            if (jspCharset != null)
            {
                return jspCharset;
            }

            // 2. determine, if a custom charset mapping exists for text/*
            cutomEncoding = getCustomCharsetMapping("text/*");
            if (cutomEncoding != null)
            {
                return cutomEncoding;
            }

            // 3. use the default charset
            return defaultContentCharset;
        }

        // 1. determine, if a custom charset mapping exists for the given mime type
        cutomEncoding = getCustomCharsetMapping(type);
        if (cutomEncoding != null)
        {
            return cutomEncoding;
        }

        // 2. in all other cases, use the default
        return defaultContentCharset;
    }

    /**
     * A helper method, which checks, if a custom encoding for a special MIME
     * type is defined. The cutom MIME type in the properties has the syntax.
     *
     * intershop.template.encoding.&lt;mimeType&gt; = &lt;custom encoding name&gt;
     *
     * @param key
     *            the MIME type, for which a mapping should be searched
     * @return the Java charset name or null, if no mapping is defined or the
     *         charset name is not supported
     */

    protected String getCustomCharsetMapping(String key)
    {
        String charsetName = getCompilerConfiguration().getJspEncoding(key);

        if (charsetName != null)
        {
            charsetName = CharacterSetMappings.mapHttpToCharset(charsetName);

            try
            {
                Charset.isSupported(charsetName);
            }
            catch (IllegalCharsetNameException ilex)
            {
                logger.error("The custom MIME type contentEncoding for the MIME type {} charset {} is not valid!", key, charsetName);
                return null;
            }

            return charsetName;
        }

        return null;
    }

    /**
     * Gets the default character set.
     * 
     * @return the default character set
     */
    protected String getDefaultCharSet()
    {
        String encoding = getCompilerConfiguration().getDefaultContentEncoding();
        return null != encoding? encoding: ISMLTemplateConstants.DEFAULT_CHARSET;
    }
    
    /**
     * A helper method, which determins the default html charset, either given
     * in the appserver.properties file as "intershop.template.DefaultContentEncoding"
     * or "UTF-8" as default
     *
     * @return the default content charset, which should be used
     */

    protected String determineDefaultHTMLCharset()
    {
        // initialize default value
        String encodingCharSet = getDefaultCharSet();

        // test the decoding char set
        try
        {
            byte[] testChar = {(byte)80};

            String javaCharset = CharacterSetMappings.mapHttpToCharset(encodingCharSet);
            new String(testChar, javaCharset);
            // no exception raised
        }
        catch (UnsupportedEncodingException e)
        {
            /* invalid char set, disable decoding */
            logger.error("The ContentEncoding charset {} is not valid! Using Charset : {}", encodingCharSet, ISMLTemplateConstants.DEFAULT_CHARSET);
            encodingCharSet = ISMLTemplateConstants.DEFAULT_CHARSET;
        }

        // set the charset for encoding
        return encodingCharSet;
    }

    // process input file converting to unicode-escaped data
    // this step is necessary for JavaCC not screwing with original data
    /**
     * Process input file converting to unicode-escaped data.
     * This step is necessary for JavaCC not screwing with original data.
     *
     * @param   aFile the file object for the template
     * @param   charset the presumed charset for the template
     *
     * @return  byte array containing the unicode-escaped data for
     * the template.
     *
     * @throws IOException if an IO error occurs during the process.
     */
    protected byte[] unicodeEscape(File aFile, String charset) throws IOException
    {
        byte[] bytes = null;
        byte[] prefix = null;
        BufferedReader rd = null;
        PrintWriter wr = null;

        try
        {
            BufferedInputStream bufInStream = new BufferedInputStream(new FileInputStream(aFile));
            rd = new BufferedReader(new InputStreamReader(bufInStream, charset));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wr = new PrintWriter(new OutputStreamWriter(baos));

            if (UNICODE_BIG.equalsIgnoreCase(charset) ||
                UNICODE_LITTLE.equalsIgnoreCase(charset))
            {
                // cut off the utf-16 file prefix
                prefix = new byte[2];
                bufInStream.read(prefix, 0, prefix.length);
            }
            else if (JAVA_ENCODING.equalsIgnoreCase(charset))
            {
                // cut off the utf-8 file prefix, if it exists
                bufInStream.mark(3);
                prefix = new byte[3];
                bufInStream.read(prefix, 0, prefix.length);

                if (!(prefix[0]==(byte)0xEF && prefix[1]==(byte)0xBB && prefix[2]==(byte)0xBF))
                {
                    // the first three characters are not significant for utf-8,
                    // so reset the reader
                    bufInStream.reset();
                }
            }

            int c;

            while((c = rd.read()) != -1)
            {
                if (isASCII((char) c))
                {
                    wr.write(c);
                }
                else
                {
                    wr.write(unicodeToString((char) c));
                }
            }

            wr.flush();

            bytes = baos.toByteArray();
        }
        catch (FileNotFoundException e)
        {
            logger.error("An error occurred while trying to apply the Unicode-escaping conversion to the template: {}", e.getMessage());
            throw new ISMLException(e);
        }
        catch (UnsupportedEncodingException e)
        {
            logger.error("An error occurred while trying to apply the Unicode-escaping conversion to the template: {}", e.getMessage());
            throw new ISMLException(e);
        }
        finally
        {
            if (rd!=null)
            {
                try
                {
                    rd.close();
                }
                catch (Exception e)
                {
                    // do nothing
                    logger.debug(e.getMessage(), e);
                }
            }
            if (wr!=null)
            {
                try
                {
                    wr.close();
                }
                catch (Exception e)
                {
                    // do nothing
                    logger.debug(e.getMessage(), e);
                }
            }
        }

        return bytes;
    }

    /**
     * Helper method, converts non-ASCII characters
     * in a given String to unicode-escaped equivalents.
     *
     * @param   s   the String
     * @return  the converted string
     */

    protected String convertString(String s)
    {
        int len = s.length();
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<len; i++)
        {
            char c = s.charAt(i);
            if (isASCII(c))
                sb.append(c);
            else
                sb.append(unicodeToString(c));
        }
        return sb.toString();
    }


    /*--------------------------------------------------------------------------
      Helper methods for string processing
      --------------------------------------------------------------------------*/

    /**
     * Helper method, checks whether a particular character belongs to
     * the ASCII chart.
     *
     * @param   c   the Character
     * @return  true if ASCII, false otherwise
     */

    private boolean isASCII(char c)
    {
        return (c>=32 && c<=126) || (c=='\r') || (c=='\n') ||  (c=='\t');
    }


    /**
     * Helper method, converts a character to its unicode-escaped counterpart.
     *
     * @param   c   the Character
     * @return  char array containing the unicode-escaped string for the given
     * character.
     */

    private char[] unicodeToString(char c)
    {
        char[] ca = { '\\', 'u', '\0', '\0', '\0', '\0' };
        ca[2] = hexToChar((c >> 12) & 0x0f);
        ca[3] = hexToChar((c >>  8) & 0x0f);
        ca[4] = hexToChar((c >>  4) & 0x0f);
        ca[5] = hexToChar((c     ) & 0x0f);
        return ca;
    }

    /**
     * Helper method, returns the representative character for a single
     * hex value (0-F).
     *
     * @param   hex   the Hex value
     * @return  the equivalent char
     */

    private char hexToChar (int hex)
    {
        if (hex < 10)
            return (char)(hex + '0');
        else
            return (char)(hex  - 10 + 'a');
    }
}
