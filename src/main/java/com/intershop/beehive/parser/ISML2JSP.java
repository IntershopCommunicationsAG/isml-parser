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
 */package com.intershop.beehive.parser;

import com.intershop.beehive.isml.capi.ISMLCompilerConfiguration;
import com.intershop.beehive.isml.capi.ISMLException;
import com.intershop.beehive.isml.capi.ISMLTemplateConstants;
import com.intershop.beehive.isml.internal.TemplatePrecompileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is an offline ISML to JSP compiler. A running server is NOT 
 * required. It can be used as an ANT task or from the command line. This 
 * compiler is used by the build tools.
 *
 */
public class ISML2JSP
{
    
    /**
     * The source directory to scan for *.isml files
     */
    protected File srcDirectory = null;
    
    /**
     * The destination directory.
     */
    protected File destDirectory = null;
    
    /**
     * The default content encoding (e.g. UTF-8).
     */
    protected String contentEncoding = null;

    /**
     * Indicates whether the build will continue even if there are compilation 
     * errors; defaults to true. 
     */
    protected boolean failOnError = true;

    /**
     * Indicates whether an error has occured during the command line compilation. 
     * In this case a negative exit value is returned.  
     */
    protected static AtomicBoolean errorExit = new AtomicBoolean(false);
    
    /**
     * List with jsp encodings.
     */
    private List<Encoding> jspEncodings = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Default constructor.
     */
    public ISML2JSP()
    {
          // nothing to do
    }

    /**
     * Constructor with the main properties.
     *
     * @param srcDir
     * @param destDir
     * @param contentEncoding
     */
    public ISML2JSP(File srcDir, File destDir, String contentEncoding)
    {
        this.srcDirectory = srcDir;
        this.destDirectory = destDir;
        this.contentEncoding = contentEncoding;
    }

    /**
     * Returns the source directory.
     * 
     * @return the source directory.
     */
    public File getSrcdir()
    {
        return srcDirectory;
    }


    /**
     * Sets the source directory.
     * 
     * @param file - the source directory.
     */
    
    public void setSrcdir(File file)
    {
        srcDirectory = file;
    }


    /**
     * Returns the destination directory.
     * 
     * @return the destination directory
     */
    
    public File getDestdir()
    {
        return destDirectory;
    }


    /**
     * Sets the destination directory.
     * 
     * @param file - the destination directory
     */
    
    public void setDestdir(File file)
    {
        destDirectory = file;
    }


    /**
     * Returns the default text encoding
     * 
     * @return the default text encoding or null, if none is set.
     */
    
    public String getContentEncoding()
    {
        return contentEncoding;
    }


    /**
     * Sets the default text encoding (e.g. UTF-8)
     * 
     * @param encoding the default text encoding string
     */
    
    public void setContentEncoding(String encoding)
    {
        contentEncoding= encoding;
    }

    /**
     * Add a jsp encoding configuration to the ISML compiler configuration.
     *
     * @param mimetype
     * @param encoding
     */
    public void addJspEncoding(String mimetype, String encoding) {
        jspEncodings.add(new Encoding(mimetype, encoding));
    }

    /**
     * This method collects all template language directories from the given root directory
     * (this is a site template directory or a cartridge template directory). The language
     * directories are 1st level subdirectories of the given root. Note that there are some
     * "special" language identifiers, e.g. "impex", that are also returned.
     *
     * @param rootDir The template root directory.
     *
     * @return An array of files representing all template directories, never null
     */

    protected File[] getAllLanguageDirs(String rootDir)
    {
        if (rootDir != null)
        {
            // get the root dir
            File root = new File(rootDir);

            // get all isml files
            return root.listFiles(ISMLTemplateConstants.directoryFilter);
        }
        else
        {
            // null dir returns no matches
            return new File[0];
        }
    }    
    
    /**
     * This internal helper collects all template file names out of the given directory
     * recursively. The names are built to match the requirements of building template IDs, i.e.
     * containing a sub directory (optionally) and a template file name (w/ extension) separated
     * by slashes.
     *
     * @param dir The directory. Initially (1st recursion) it is the language directory.
     * @param subDirPath The sub directory path
     * @param result The resulting template names.
     */

    protected void getAllTemplateFileNames(File dir, String subDirPath, Collection<String> result)
    {
        // get all isml files and subdirs
        File[] files = dir.listFiles(ISMLTemplateConstants.ismlFilter);

        // iterate results, recurse dirs, add files to result
        for (int i = 0; i < files.length; i++)
        {
            if (files[i].isDirectory())
            {
                String sd = null;

                // create current subdir path
                if (subDirPath == null)
                {
                    // we're the 1st one
                    sd = files[i].getName() + '/';
                }
                else
                {
                    // append
                    sd = subDirPath + files[i].getName() + '/';
                }

                // recurse
                getAllTemplateFileNames(files[i], sd, result);
            }
            else
            {
                // add file name to result vector
                if (subDirPath == null)
                {
                    // add "pure" file name
                    result.add(files[i].getName());
                }
                else
                {
                    // prepend base dir
                    result.add(subDirPath + files[i].getName());
                }
            }
        }
    }

    public void execute() throws ISMLException
    {
        ArrayList<File[]> compilePathList = new ArrayList<>();
                  
        // check, if the source directory attribute is set.
        if (getSrcdir() == null) {
            throw new ISMLException("srcdir attribute must be set!");
        }        

        // check, if the destination directory attribute is set.
        if (getDestdir() == null) {
            throw new ISMLException("destdir attribute must be set!");
        }        
        
        File srcDir = getSrcdir();
        String srcDirName = srcDir.getAbsolutePath().replace('\\','/');
                
        // check, if the source directory exists
        if (!srcDir.exists())
        {
            throw new ISMLException("srcdir doesn't exist!!");
        }
        
        // Create compile configuration
        ISMLCompilerConfiguration configuration = getCompilerConfiguration();
        TemplatePrecompileUtils precompUtils = new TemplatePrecompileUtils(configuration);

        File destDir = getDestdir();
        String destDirName = destDir.getAbsolutePath().replace('\\','/');

        // get all language subdirectories
        File[] langDirs = getAllLanguageDirs(srcDirName);
        if (langDirs != null)
        {
            for (int i = 0; i < langDirs.length; i++)
            {
                // get all template names
                Collection<String> ismlFiles = new ArrayList<>();
                getAllTemplateFileNames(langDirs[i], null, ismlFiles);
                
                // iterate all templates and check if we should compile
                for(String ismlFileName : ismlFiles)
                {
                    // scan for files to compile
                    File sourceFile = null;
                    
                    String ismlSubPathName = langDirs[i].getName() + File.separatorChar + ismlFileName;
                    String jspSubPathName = ismlSubPathName.substring(0, ismlSubPathName.length() - 
                                            ISMLTemplateConstants.TEMPLATE_EXTENSION.length()) +
                                            ISMLTemplateConstants.TEMPLATE_PAGECOMPILE_EXTENSION;

                    sourceFile = new File(srcDirName, ismlSubPathName);
                    File jspFile = new File(destDirName, jspSubPathName);
                    
                    if (!(sourceFile.isFile() && sourceFile.canRead()))
                    {
                        continue; 
                    }
                    
                    // check, if compilation is required
                    if (!jspFile.exists() || (jspFile.lastModified() < sourceFile.lastModified()))
                    {
                        compilePathList.add(new File[] {sourceFile, jspFile});
                    }
                    else
                    {
                        logger.info("Skipping file: {}. Target is up to date.", sourceFile.getAbsolutePath() );
                    }
                }
            }
        }

        // check whether there is something to compile at all
        if(compilePathList.isEmpty())
        {
            return;
        }

        logger.info("Compiling {} source files to {}.", compilePathList.size(), getDestdir().getAbsolutePath());

        for (File[] entry : compilePathList)
        {
            File sourceFile = entry[0];
            File jspFile = entry[1];

            try
            {
                // compile isml -> jsp
                logger.debug("Compiling isml file: {}", sourceFile.getAbsolutePath());

                // remove .jsp file
                if (jspFile.exists() && jspFile.isFile())
                {
                    Files.delete(jspFile.toPath());
                }

                File outDir = jspFile.getParentFile();

                if (!outDir.exists())
                {
                    outDir.mkdirs();
                }

                precompUtils.compileISML(sourceFile, jspFile);
            }
            catch (Exception ex)
            {
                logger.error("Error compiling '" + sourceFile.getAbsolutePath() + "'.\nReason:", ex);
            }
        }
    }
    
    /**
     * Creates the compiler configuration.
     * @return Compiler condifuration
     */
    private ISMLCompilerConfiguration getCompilerConfiguration()
    {
        final Map<String, String> encodingMap = new HashMap<>(jspEncodings.size());
        for (Encoding encoding : jspEncodings)           
        {
          encodingMap.put(encoding.getMimeType(), encoding.getEncoding());  
        }
        
        return new ISMLCompilerConfiguration()
        {
            @Override
            public String getJspEncoding(String mimeType)
            {
                String encoding = encodingMap.get(mimeType);
                return (encoding == null) ? getDefaultContentEncoding() : encoding;
            }
            
            @Override
            public String getDefaultContentEncoding()
            {
                return getContentEncoding();
            }
        };
    }

    /**
     * Creates a new encoding sub-element
     * @return Encoding sub element
     */
    public Encoding createJspEncoding() {     
        
        final Encoding encoding = new Encoding();
        jspEncodings.add(encoding);
        return encoding;
    }

    
    /**
     * Encoding for the task.
     * 
     * @author wfrank
     */
    public class Encoding
    {
        private String mimeType;
        private String encoding;

        public Encoding() 
        {
            // nothing to do
        }

        public Encoding(String mimeType, String encoding)
        {
            this.mimeType = mimeType;
            this.encoding = encoding;
        }

        /**
         * Gets the mime type.
         * @return mime type
         */
        public String getMimeType()
        {
            return mimeType;
        }
        
        /**
         * Sets the mime type.
         * @param mimeType Mime type
         */
        public void setMimeType(String mimeType)
        {
            this.mimeType = mimeType;
        }
        
        /**
         * Gets the encoding for the given mime type.
         * @return Encoding
         */
        public String getEncoding()
        {
            return encoding;
        }
        
        /**
         * Sets the encoding for the given mime type.
         * @param encoding Encoding
         */
        public void setEncoding(String encoding)
        {
            this.encoding = encoding;
        }
        
    }


    /**
     * Java main method for standalone execution of the ISML2JSP compiler.
     * 
     * @param args
     */
    public static void main(String[] args)
    {
        String srcDirName = null;
        String destDirName = null;
        File srcDir = null;
        File destDir = null;

        if (args.length == 0)
        {
            System.out.println("Usage: java "+ISML2JSP.class.getName()+" [-verbose] [-contentencoding <encoding>] <src dir> <dest dir>");
            System.exit(0);
        }
        
        ISML2JSP compiler = new ISML2JSP();
                
        // parse command line arguments
        for(int i=0;i<args.length;i++)
        {
            if ("-contentencoding".equalsIgnoreCase(args[i]))
            {
                i++;
                compiler.setContentEncoding(args[i]);
            }
            else
            {
                if (srcDirName == null)
                {
                    srcDirName = args[i];
                } 
                else if (destDirName == null)
                {
                    destDirName = args[i];
                }
                else
                {
                    System.err.println("Too many arguments given!");
                }
            }
        }

        srcDir = new File(srcDirName);
        destDir = new File(destDirName);

        if (!srcDir.exists() || !srcDir.isDirectory() || !srcDir.canRead())
        {
            System.err.println("ISML source directory not accessible: '" + srcDirName + "'");
            System.exit(0);
        }

        compiler.setSrcdir(srcDir);
        compiler.setDestdir(destDir);
        compiler.execute();

        // exit with a negative return value
        // if a compile error has occurred        
        if (errorExit.get())
        {
            System.exit(-1);
        }        
    }

}

