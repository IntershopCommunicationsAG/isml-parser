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
package com.intershop.beehive.isml.internal.parser;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;


/**
 * This class is an output stream that can be used to compact HTML templates.
 * It is able to remove unnecessary whitespaces and thus to reduce the size of
 * the template. Whitespaces may only be removed from content, but not from
 * ISML tag expressions. Therefore, two kinds of print-methods exist, one kind
 * to write unchangeable data and another kind to write compactable data.
 *
 * Note: This writer is applied when the template is compiled. Its performance
 * is not critical to the runtime performance of templates.
 */
public class CompactingWriter extends Writer
{
    /**
     * A marker for invalid characters during optimization.
     */

    private static final char MARKER = 0;

    /**
     * The chained output writer.
     */

    private Writer out;

    /**
     * The buffer for content that must be compacted.
     */

    private CharArrayWriter buffer;

    /**
     * The flag whether compacting is enabled or not.
     */

    private boolean enabled;

    /**
     *   The name of the character encoding being used by this stream.
     */
    private String encoding;

    /**
     * The constructor. Creates a compacting writer that is not enabled.
     * Compacting must be switched on explicitely.
     *
     * @param  out    the chained output stream
     * @param  enc    the character encoding, the jsp file is writen in
     * @throws UnsupportedEncodingException
     *
     * @see     #enable
     */

    public CompactingWriter(Writer out, String enc)
        throws UnsupportedEncodingException
    {
        this.out = out;
        buffer = new CharArrayWriter();
        enabled = false;

        byte[] testChar = {(byte)80};

        // test the decoding char set
        new String(testChar, enc);

        // no exception raised => take the encoding
        encoding = enc;
    }


    /**
     * Switches on content compacting mode. All unnecessary whitespaces are
     * removed from content that is written using the printCompact-methods.
     */

    public void enable()
    {
        enabled = true;
    }


    /**
     * Writes a string unchanged to the output stream.
     *
     * @param       s       the string
     * @throws   IOException if something went wrong
     */

    public void print(String s) throws IOException
    {
        write(s);
    }


    /**
     * Stringifies an object and writes it unchanged to the output stream.
     *
     * @param       o       the object
     * @throws   IOException if something went wrong
     */

    public void print(Object o) throws IOException
    {
        write(String.valueOf(o));
    }


    /**
     * Writes a character unchanged to the output stream.
     *
     * @param       ch      the character
     * @throws   IOException if something went wrong
     */

    public void print(char ch) throws IOException
    {
        write(ch);
    }


    /**
     * Writes a string to the output stream. Any unnecessary whitespaces are
     * removed.
     *
     * @param       s       the string
     * @throws   IOException if something went wrong
     */

    public void printCompact(String s) throws IOException
    {
        if (enabled)
        {
            // if enabled, buffer the content for later optimization
            // the buffer is flushed either when uncompacted content is
            // written or if the stream is explicitely flushed
            buffer.write(s, 0, s.length());
        }
        else
        {
            // if not enable, simply forward it to the output stream without buffering
            write(s);
        }
    }


    /**
     * Stringifies an object and writes it compacted to the output stream.
     *
     * @param       o       the object
     * @throws   IOException if something went wrong
     */

    public void printCompact(Object o) throws IOException
    {
        printCompact(String.valueOf(o));
    }

    /**
     * Return the name of the character encoding being used by this stream.
     * If the encoding has an historical name then that name is returned;
     * otherwise the encoding's canonical name is returned.
     *
     * @return The historical name of this encoding
     */

    public String getEncoding()
    {
        return encoding;
    }

    /*---------------------------------------------------------------------
                            Methods from Writer
    ---------------------------------------------------------------------*/


    /**
     * Implementation of the Writer abstract method. Writes data unchanged.
     *
     * @param       ch      the characters to be written
     * @param       off     the offset
     * @param       len     the number of characters
     * @throws   IOException if something went wrong
     */

    @Override
    public void write(char[] ch, int off, int len) throws IOException
    {
        // make sure any cached content is flushed before
        flushBuffer();
        out.write(ch, off, len);
    }


    /**
     * Flushes the stream. Any buffered content is written to the output stream.
     *
     * @throws   IOException if something went wrong
     */

    @Override
    public void flush() throws IOException
    {
        flushBuffer();
        out.flush();
    }


    /**
     * Closes the stream.
     *
     * @throws   IOException if something went wrong
     */

    @Override
    public void close() throws IOException
    {
        out.close();
    }


    /*---------------------------------------------------------------------
                            Private helpers
    ---------------------------------------------------------------------*/


    /**
     * Flushes the buffer. The buffered content is optimized before writing
     * to the output stream.
     *
     * @throws   IOException if something went wrong
     */

    private void flushBuffer() throws IOException
    {
        if (buffer.size() == 0)
        {
            // nothing to do
            return;
        }

        char[] content = buffer.toCharArray();

        // all operations operate on the whole content from 0 to end
        // do not change the order!
        int length = content.length;

        length = removeAll('\r', content, length);
        length = replaceAll('\t', ' ', content, length);
        length = replaceAll(' ', ' ', ' ', content, length);
        //length = replaceAll(' ', '\n', '\n', content, length); // leave trailing blanks, see ENFINITY-2297
        length = replaceAll('\n', ' ', '\n', content, length);
        length = replaceAll('\n', '\n', '\n', content, length);

        if (!(length == 1 && content[0] == '\n'))
        {
            // remove leading and trailing new lines if they occur immediately
            // before a tag opens or after a tag was closed
            int start = 0;
            if (length > 1 && content[length - 1] == '\n' && content[length - 2] == '>')
            {
                length--;
            }
            if (length > 1 && content[0] == '\n' && content[1] == '<')
            {
                length--;
                start++;
            }
            
            // now write the compacted content
            out.write(content, start, length);
        }
        // else don't write a single '\n'

        // empty the buffer
        buffer.reset();
    }


    /**
     * Removes all occurrences of the passed character in the buffer.
     *
     * @param       ch      the character to be removed
     * @param       buf     the buffer
     * @param       length  the length of the buffer
     * @return      the new length of the buffer
     */

    private int removeAll(char ch, char[] buf, int length)
    {
        int index = 0;

        for (int i = 0; i < length; i++)
        {
            if (buf[i] != ch)
            {
                buf[index] = buf[i];
                index++;
            }
        }

        return index;
    }


    /**
     * Replaces all occurrences of the passed character in the buffer
     * by another character.
     *
     * @param       ch1     the character to be replaced
     * @param       ch2     the new character
     * @param       buf     the buffer
     * @param       length  the length of the buffer
     * @return      the length of the buffer (doesn't change)
     */

    private int replaceAll(char ch1, char ch2, char[] buf, int length)
    {
        for (int i = 0; i < length; i++)
        {
            if (buf[i] == ch1)
            {
                buf[i] = ch2;
            }
        }

        return length;
    }


    /**
     * Replaces all occurrences of the two passed characters in the buffer
     * by another character.
     *
     * @param       ch1     the first character
     * @param       ch2     the second character
     * @param       ch3     the new character
     * @param       buf     the buffer
     * @param       length  the length of the buffer
     * @return      the new length of the buffer
     */

    private int replaceAll(char ch1, char ch2, char ch3, char[] buf, int length)
    {
        // if buf is too small, there is nothing to do
        if (length < 2)
        {
            return length;
        }

        int i1 = 0;

        for (int i = 0; i < length - 1; i = i1)
        {
            i1 = i + 1;

            if ((buf[i] == ch1) && (buf[i1] == ch2))
            {
                // mark all invalid characters
                buf[i] = MARKER;
                buf[i1] = ch3;
            }
        }

        // now remove the marked places
        return removeAll(MARKER, buf, length);
    }
}

