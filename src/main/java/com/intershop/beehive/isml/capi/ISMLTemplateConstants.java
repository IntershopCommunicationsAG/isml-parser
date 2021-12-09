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
package com.intershop.beehive.isml.capi;

import java.io.File;
import java.io.FileFilter;

/**
 * This class defines some constants used in template processing.
 */
public class ISMLTemplateConstants
{
    /**
     * standard TEXT content type.
     */

    public static final String TYPE_TEXT = "text/plain";

    /**
     * standard HTML content type.
     */

    public static final String TYPE_HTML = "text/html";

    /**
     * standard XML content type.
     */

    public static final String TYPE_XML = "text/xml";

    /**
     * de facto standard WML content type.
     */

    public static final String TYPE_WML = "text/vnd.wap.wml";

    /**
     * XHTML content type
     */

    public static final String TYPE_XHTML = "application/xhtml+xml";

    /**
     * The ISML file extension
     */
    public static final String TEMPLATE_EXTENSION = ".isml";

    /**
     * The JSP file extension
     */
    public static final String TEMPLATE_PAGECOMPILE_EXTENSION = ".jsp";

    /**
     * The Java file extension
     */
    public static final String TEMPLATE_JAVA_EXTENSION = ".java";

    /**
     * The class file extension
     */
    public static final String TEMPLATE_CLASS_EXTENSION = ".class";

    /**
     * The class name of the TemplateExecutionConfig class.
     */
    public static final String TEMPLATE_EXEC_CONFIG_NAME = "com.intershop.beehive.core.internal.template.TemplateExecutionConfig";

    /**
     * The name of the pipeline dictionary object, which can be used in a
     * JSP EL construct as a named variable.
     */
    public static final String JSP_EL_PIPELINE_DICT_NAME = "pipelineDict";

    /**
     * The template default character set.
     */
    public static final String DEFAULT_CHARSET = "UTF-8";

    /**
     * Default initial buffer size for buffered input and output streams
     * in the template processor section.
     */
    public static final int DEFAULT_TEMPLATE_BUFFERSIZE = 16384;

    /**
     * The prefix directory name in the root context
     */

    public static final String JSP_DIR_PREFIX = "org/apache/jsp";
    
    /**
     * A file filter that accepts directories and .isml files (exclude the template ".isml" without name).
     */
    public static FileFilter ismlFilter =
        new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                return file.isDirectory() ||
                    file.getName().toLowerCase().endsWith(ISMLTemplateConstants.TEMPLATE_EXTENSION)
                    && file.getName().length() > ISMLTemplateConstants.TEMPLATE_EXTENSION.length();
            }
        };

    /**
     * A file filter that accepts directories only.
     */
    public static FileFilter directoryFilter =
        new FileFilter()
            {
                @Override
                public boolean accept(File file)
                {
                    return file.isDirectory();
                }
            };
}
