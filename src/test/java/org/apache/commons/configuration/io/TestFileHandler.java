/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.configuration.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.configuration.ConfigurationAssert;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.FileSystem;
import org.easymock.EasyMock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test class for {@code FileHandler}.
 *
 * @version $Id$
 */
public class TestFileHandler
{
    /** Constant for the name of a test file. */
    private static final String TEST_FILENAME = "test.properties";

    /** Constant for content of the test file. */
    private static final String CONTENT =
            "TestFileHandler: This is test content.";

    /** Helper object for managing temporary files. */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * Creates a test file test content and allows specifying a file name.
     *
     * @param f the file to be created (may be <b>null</b>)
     * @return the File object pointing to the test file
     */
    private File createTestFile(File f)
    {
        Writer out = null;
        File file = f;
        try
        {
            if (file == null)
            {
                file = folder.newFile();
            }
            out = new FileWriter(file);
            out.write(CONTENT);
        }
        catch (IOException ioex)
        {
            fail("Could not create test file: " + ioex);
            return null; // cannot happen
        }
        finally
        {
            if (out != null)
            {
                try
                {
                    out.close();
                }
                catch (IOException ioex)
                {
                    // ignore
                }
            }
        }
        return file;
    }

    /**
     * Creates a test file with the test content.
     *
     * @return the File object pointing to the test file
     */
    private File createTestFile()
    {
        return createTestFile(null);
    }

    /**
     * Reads the content of the specified reader into a string.
     *
     * @param in the reader
     * @return the read content
     * @throws IOException if an error occurs
     */
    private static String readReader(Reader in) throws IOException
    {
        StringBuilder buf = new StringBuilder();
        int c;
        while ((c = in.read()) != -1)
        {
            buf.append((char) c);
        }
        return buf.toString();
    }

    /**
     * Reads the content of the specified file into a string
     *
     * @param f the file to be read
     * @return the content of this file
     */
    private static String readFile(File f)
    {
        Reader in = null;
        try
        {
            in = new FileReader(f);
            return readReader(in);
        }
        catch (IOException ioex)
        {
            fail("Could not read file: " + ioex);
            return null; // cannot happen
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException ioex)
                {
                    // ignore
                }
            }
        }
    }

    /**
     * Tests whether a newly created instance has a default file system.
     */
    @Test
    public void testGetFileSystemDefault()
    {
        FileHandler handler = new FileHandler(new FileBasedTestImpl());
        assertEquals("Wrong default file system",
                FileSystem.getDefaultFileSystem(), handler.getFileSystem());
    }

    /**
     * Tests whether a null file system can be set to reset this property.
     */
    @Test
    public void testSetFileSystemNull()
    {
        FileSystem sys = EasyMock.createMock(FileSystem.class);
        EasyMock.replay(sys);
        FileHandler handler = new FileHandler(new FileBasedTestImpl());
        handler.setFileSystem(sys);
        assertSame("File system not set", sys, handler.getFileSystem());
        handler.setFileSystem(null);
        assertEquals("Not default file system",
                FileSystem.getDefaultFileSystem(), handler.getFileSystem());
    }

    /**
     * Tests whether the file system can be reset.
     */
    @Test
    public void testResetFileSystem()
    {
        FileSystem sys = EasyMock.createMock(FileSystem.class);
        EasyMock.replay(sys);
        FileHandler handler = new FileHandler(new FileBasedTestImpl());
        handler.setFileSystem(sys);
        handler.resetFileSystem();
        assertEquals("Not default file system",
                FileSystem.getDefaultFileSystem(), handler.getFileSystem());
    }

    /**
     * Tests whether a URL can be set.
     */
    @Test
    public void testSetURL() throws Exception
    {
        FileHandler handler = new FileHandler();
        handler.setURL(new URL(
                "http://commons.apache.org/configuration/index.html"));

        assertEquals("base path", "http://commons.apache.org/configuration/",
                handler.getBasePath());
        assertEquals("file name", "index.html", handler.getFileName());

        // file URL - This url is invalid, a valid url would be
        // file:///temp/test.properties.
        handler.setURL(new URL("file:/temp/test.properties"));
        assertEquals("base path", "file:///temp/", handler.getBasePath());
        assertEquals("file name", TEST_FILENAME, handler.getFileName());
    }

    /**
     * Tests whether a URL with parameters can be set.
     */
    @Test
    public void testSetURLWithParams() throws Exception
    {
        FileHandler handler = new FileHandler();
        URL url =
                new URL(
                        "http://issues.apache.org/bugzilla/show_bug.cgi?id=37886");
        handler.setURL(url);
        assertEquals("Base path incorrect",
                "http://issues.apache.org/bugzilla/", handler.getBasePath());
        assertEquals("File name incorrect", "show_bug.cgi",
                handler.getFileName());
        assertEquals("URL was not correctly stored", url, handler.getURL());
    }

    /**
     * Tests whether the location can be set as a file.
     */
    @Test
    public void testSetFile()
    {
        FileHandler handler = new FileHandler();
        File directory = ConfigurationAssert.TEST_DIR;
        File file = ConfigurationAssert.getTestFile(TEST_FILENAME);
        handler.setFile(file);
        assertEquals("Wrong base path", directory.getAbsolutePath(),
                handler.getBasePath());
        assertEquals("Wrong file name", TEST_FILENAME, handler.getFileName());
        assertEquals("Wrong path", file.getAbsolutePath(), handler.getPath());
    }

    /**
     * Tests whether the location can be set as a file.
     */
    @Test
    public void testSetPath() throws MalformedURLException
    {
        FileHandler config = new FileHandler();
        config.setPath(ConfigurationAssert.TEST_DIR_NAME + File.separator
                + TEST_FILENAME);
        assertEquals("Wrong file name", TEST_FILENAME, config.getFileName());
        assertEquals("Wrong base path",
                ConfigurationAssert.TEST_DIR.getAbsolutePath(),
                config.getBasePath());
        File file = ConfigurationAssert.getTestFile(TEST_FILENAME);
        assertEquals("Wrong path", file.getAbsolutePath(), config.getPath());
        assertEquals("Wrong URL", file.toURI().toURL(), config.getURL());
    }

    /**
     * Tests whether the location can be set using file name and base path.
     */
    @Test
    public void testSetFileName()
    {
        FileHandler config = new FileHandler();
        config.setBasePath(null);
        config.setFileName(TEST_FILENAME);
        assertNull("Got a base path", config.getBasePath());
        assertEquals("Wrong file name", TEST_FILENAME, config.getFileName());
    }

    /**
     * Tries to call a load() method if no content object is available.
     */
    @Test(expected = ConfigurationException.class)
    public void testLoadNoContent() throws ConfigurationException
    {
        FileHandler handler = new FileHandler();
        StringReader reader = new StringReader(CONTENT);
        handler.load(reader);
    }

    /**
     * Tests whether an IOException is handled when loading data from a reader.
     */
    @Test
    public void testLoadFromReaderIOException() throws IOException,
            ConfigurationException
    {
        FileBased content = EasyMock.createMock(FileBased.class);
        Reader in = new StringReader(CONTENT);
        IOException ioex = new IOException("Test exception");
        content.read(in);
        EasyMock.expectLastCall().andThrow(ioex);
        EasyMock.replay(content);
        FileHandler handler = new FileHandler(content);
        try
        {
            handler.load(in);
            fail("IOException not detected!");
        }
        catch (ConfigurationException cex)
        {
            assertEquals("Wrong root cause", ioex, cex.getCause());
        }
        EasyMock.verify(content);
    }

    /**
     * Tests whether data from a File can be loaded.
     */
    @Test
    public void testLoadFromFile() throws ConfigurationException
    {
        FileBasedTestImpl content = new FileBasedTestImpl();
        File file = createTestFile();
        FileHandler handler = new FileHandler(content);
        handler.load(file);
        assertEquals("Wrong content", CONTENT, content.getContent());
    }

    /**
     * Tries to load data from a File if no content object was set.
     */
    @Test
    public void testLoadFromFileNoContent() throws ConfigurationException
    {
        FileHandler handler = new FileHandler();
        File file = createTestFile();
        try
        {
            handler.load(file);
            fail("Missing content not detected!");
        }
        catch (ConfigurationException cex)
        {
            assertEquals("Wrong message", "No content available!",
                    cex.getMessage());
        }
    }

    /**
     * Checks that loading a directory instead of a file throws an exception.
     */
    @Test(expected = ConfigurationException.class)
    public void testLoadDirectoryString() throws ConfigurationException
    {
        FileHandler handler = new FileHandler(new FileBasedTestImpl());
        handler.load(ConfigurationAssert.TEST_DIR.getAbsolutePath());
    }

    /**
     * Tests that it is not possible to load a directory using the load() method
     * which expects a File.
     */
    @Test(expected = ConfigurationException.class)
    public void testLoadDirectoryFile() throws ConfigurationException
    {
        FileHandler handler = new FileHandler(new FileBasedTestImpl());
        handler.load(ConfigurationAssert.TEST_DIR);
    }

    /**
     * Tests whether whether data can be loaded from class path.
     */
    @Test
    public void testLoadFromClassPath() throws ConfigurationException
    {
        FileBasedTestImpl content = new FileBasedTestImpl();
        FileHandler config1 = new FileHandler(content);
        config1.setFileName("config/deep/deeptest.properties");
        config1.load();
        assertTrue("No data loaded", content.getContent().length() > 0);
    }

    /**
     * Tests whether data from a URL can be loaded.
     */
    @Test
    public void testLoadFromURL() throws Exception
    {
        File file = createTestFile();
        FileBasedTestImpl content = new FileBasedTestImpl();
        FileHandler handler = new FileHandler(content);
        handler.load(file.toURI().toURL());
        assertEquals("Wrong content", CONTENT, content.getContent());
    }

    /**
     * Tests whether data from an absolute path can be loaded.
     */
    @Test
    public void testLoadFromFilePath() throws ConfigurationException
    {
        File file = createTestFile();
        FileBasedTestImpl content = new FileBasedTestImpl();
        FileHandler handler = new FileHandler(content);
        handler.load(file.getAbsolutePath());
        assertEquals("Wrong content", CONTENT, content.getContent());
    }

    /**
     * Tests whether data from an input stream can be read.
     */
    @Test
    public void testLoadFromStream() throws Exception
    {
        File file = createTestFile();
        FileBasedTestImpl content = new FileBasedTestImpl();
        FileHandler handler = new FileHandler(content);
        FileInputStream in = new FileInputStream(file);
        try
        {
            handler.load(in);
        }
        finally
        {
            in.close();
        }
        assertEquals("Wrong content", CONTENT, content.getContent());
    }

    /**
     * Tests whether data from a reader can be read.
     */
    @Test
    public void testLoadFromReader() throws Exception
    {
        File file = createTestFile();
        FileBasedTestImpl content = new FileBasedTestImpl();
        FileHandler handler = new FileHandler(content);
        Reader in = new FileReader(file);
        try
        {
            handler.load(in);
        }
        finally
        {
            in.close();
        }
        assertEquals("Wrong content", CONTENT, content.getContent());
    }

    /**
     * Tests a load operation using the current location which is a URL.
     */
    @Test
    public void testLoadFromURLLocation() throws Exception
    {
        File file = createTestFile();
        FileBasedTestImpl content = new FileBasedTestImpl();
        FileHandler handler = new FileHandler(content);
        handler.setURL(file.toURI().toURL());
        handler.load();
        assertEquals("Wrong content", CONTENT, content.getContent());
    }

    /**
     * Tests a load operation using the current location which is a file name.
     */
    @Test
    public void testLoadFromFileNameLocation() throws ConfigurationException
    {
        File file = createTestFile();
        FileBasedTestImpl content = new FileBasedTestImpl();
        FileHandler handler = new FileHandler(content);
        handler.setBasePath(file.getParentFile().getAbsolutePath());
        handler.setFileName(file.getName());
        handler.load();
        assertEquals("Wrong content", CONTENT, content.getContent());
    }

    /**
     * Tries to load data if no location has been set.
     */
    @Test(expected = ConfigurationException.class)
    public void testLoadNoLocation() throws ConfigurationException
    {
        FileBasedTestImpl content = new FileBasedTestImpl();
        FileHandler handler = new FileHandler(content);
        handler.load();
    }

    /**
     * Tests whether data can be saved into a Writer.
     */
    @Test
    public void testSaveToWriter() throws ConfigurationException
    {
        FileBasedTestImpl content = new FileBasedTestImpl();
        FileHandler handler = new FileHandler(content);
        StringWriter out = new StringWriter();
        handler.save(out);
        assertEquals("Wrong content", CONTENT, out.toString());
    }

    /**
     * Tests whether an I/O exception during a save operation to a Writer is
     * handled correctly.
     */
    @Test
    public void testSaveToWriterIOException() throws ConfigurationException,
            IOException
    {
        FileBased content = EasyMock.createMock(FileBased.class);
        StringWriter out = new StringWriter();
        IOException ioex = new IOException("Test exception!");
        content.write(out);
        EasyMock.expectLastCall().andThrow(ioex);
        EasyMock.replay(content);
        FileHandler handler = new FileHandler(content);
        try
        {
            handler.save(out);
            fail("IOException not detected!");
        }
        catch (ConfigurationException cex)
        {
            assertEquals("Wrong cause", ioex, cex.getCause());
        }
        EasyMock.verify(content);
    }

    /**
     * Tries to save something to a Writer if no content is set.
     */
    @Test(expected = ConfigurationException.class)
    public void testSaveToWriterNoContent() throws ConfigurationException
    {
        FileHandler handler = new FileHandler();
        handler.save(new StringWriter());
    }

    /**
     * Tests whether data can be saved to a stream.
     */
    @Test
    public void testSaveToStream() throws ConfigurationException, IOException
    {
        File file = folder.newFile();
        FileOutputStream out = new FileOutputStream(file);
        FileHandler handler = new FileHandler(new FileBasedTestImpl());
        try
        {
            handler.save(out);
        }
        finally
        {
            out.close();
        }
        assertEquals("Wrong content", CONTENT, readFile(file));
    }

    /**
     * Tests whether data can be saved to a file.
     */
    @Test
    public void testSaveToFile() throws ConfigurationException, IOException
    {
        File file = folder.newFile();
        FileHandler handler = new FileHandler(new FileBasedTestImpl());
        handler.save(file);
        assertEquals("Wrong content", CONTENT, readFile(file));
    }

    /**
     * Tests whether data can be saved to a URL.
     */
    @Test
    public void testSaveToURL() throws Exception
    {
        File file = folder.newFile();
        URL url = file.toURI().toURL();
        FileHandler handler = new FileHandler(new FileBasedTestImpl());
        handler.save(url);
        assertEquals("Wrong content", CONTENT, readFile(file));
    }

    /**
     * Tests whether data can be saved to a file name.
     */
    @Test
    public void testSaveToFileName() throws ConfigurationException, IOException
    {
        File file = folder.newFile();
        FileHandler handler = new FileHandler(new FileBasedTestImpl());
        handler.save(file.getAbsolutePath());
        assertEquals("Wrong content", CONTENT, readFile(file));
    }

    /**
     * Tests whether a URL exception is handled when saving a file to a file
     * name.
     */
    @Test
    public void testSaveToFileNameURLException() throws ConfigurationException,
            IOException
    {
        FileSystem fs = EasyMock.createMock(FileSystem.class);
        File file = folder.newFile();
        String basePath = "some base path";
        MalformedURLException urlex =
                new MalformedURLException("Test exception");
        EasyMock.expect(fs.getURL(basePath, file.getName())).andThrow(urlex);
        EasyMock.replay(fs);
        FileHandler handler = new FileHandler(new FileBasedTestImpl());
        handler.setBasePath(basePath);
        handler.setFileSystem(fs);
        try
        {
            handler.save(file.getName());
            fail("URL exception not detected!");
        }
        catch (ConfigurationException cex)
        {
            assertEquals("Wrong cause", urlex, cex.getCause());
        }
        EasyMock.verify(fs);
    }

    /**
     * Tries to save data to a file name if the name cannot be located.
     */
    @Test
    public void testSaveToFileNameURLNotResolved()
            throws ConfigurationException, IOException
    {
        FileSystem fs = EasyMock.createMock(FileSystem.class);
        File file = folder.newFile();
        EasyMock.expect(fs.getURL(null, file.getName())).andReturn(null);
        EasyMock.replay(fs);
        FileHandler handler = new FileHandler(new FileBasedTestImpl());
        handler.setFileSystem(fs);
        try
        {
            handler.save(file.getName());
            fail("Unresolved URL not detected!");
        }
        catch (ConfigurationException cex)
        {
            EasyMock.verify(fs);
        }
    }

    /**
     * Tests whether data can be saved to the internal location if it is a file
     * name.
     */
    @Test
    public void testSaveToFileNameLocation() throws ConfigurationException,
            IOException
    {
        File file = folder.newFile();
        FileHandler handler = new FileHandler(new FileBasedTestImpl());
        handler.setFileName(file.getAbsolutePath());
        handler.save();
        assertEquals("Wrong content", CONTENT, readFile(file));
    }

    /**
     * Tests whether data can be saved to the internal location if it is a URL.
     */
    @Test
    public void testSaveToURLLocation() throws ConfigurationException,
            IOException
    {
        File file = folder.newFile();
        FileHandler handler = new FileHandler(new FileBasedTestImpl());
        handler.setURL(file.toURI().toURL());
        handler.save();
        assertEquals("Wrong content", CONTENT, readFile(file));
    }

    /**
     * Tries to save the handler if no location has been set.
     */
    @Test(expected = ConfigurationException.class)
    public void testSaveNoLocation() throws ConfigurationException
    {
        FileHandler handler = new FileHandler(new FileBasedTestImpl());
        handler.save();
    }

    /**
     * Tests loading and saving a configuration file with a complicated path
     * name including spaces. (related to issue 35210)
     */
    @Test
    public void testPathWithSpaces() throws ConfigurationException, IOException
    {
        File path = folder.newFolder("path with spaces");
        File confFile = new File(path, "config-test.properties");
        File testFile = createTestFile(confFile);
        URL url = testFile.toURI().toURL();
        FileBasedTestImpl content = new FileBasedTestImpl();
        FileHandler handler = new FileHandler(content);
        handler.setURL(url);
        handler.load();
        assertEquals("Wrong data read", CONTENT, content.getContent());
        File out = new File(path, "out.txt");
        handler.save(out);
        assertEquals("Wrong data written", CONTENT, readFile(out));
    }

    /**
     * Tests whether file names containing a "+" character are handled
     * correctly. This test is related to CONFIGURATION-415.
     */
    @Test
    public void testPathWithPlus() throws ConfigurationException, IOException
    {
        File saveFile = folder.newFile("test+config.properties");
        FileHandler handler = new FileHandler(new FileBasedTestImpl());
        handler.setFile(saveFile);
        handler.save();
        assertEquals("Wrong content", CONTENT, readFile(saveFile));
    }

    /**
     * Tests whether a FileHandler object can be used to specify a location and
     * later be assigned to a FileBased object.
     */
    @Test
    public void testAssignWithFileBased()
    {
        FileHandler h1 = new FileHandler();
        File f = new File("testfile.txt");
        h1.setFile(f);
        FileBased content = new FileBasedTestImpl();
        FileHandler h2 = new FileHandler(content, h1);
        h1.setFileName("someOtherFile.txt");
        assertSame("Content not set", content, h2.getContent());
        assertEquals("Wrong location", f, h2.getFile());
    }

    /**
     * Tests isLocationDefined() if a File has been set.
     */
    @Test
    public void testIsLocationDefinedFile()
    {
        FileHandler handler = new FileHandler();
        handler.setFile(createTestFile());
        assertTrue("Location not defined", handler.isLocationDefined());
    }

    /**
     * Tests isLocationDefined() if a URL has been set.
     */
    @Test
    public void testIsLocationDefinedURL() throws IOException
    {
        FileHandler handler = new FileHandler();
        handler.setURL(createTestFile().toURI().toURL());
        assertTrue("Location not defined", handler.isLocationDefined());
    }

    /**
     * Tests isLocationDefined() if a path has been set.
     */
    @Test
    public void testIsLocationDefinedPath()
    {
        FileHandler handler = new FileHandler();
        handler.setPath(createTestFile().getAbsolutePath());
        assertTrue("Location not defined", handler.isLocationDefined());
    }

    /**
     * Tests isLocationDefined() if a file name has been set.
     */
    @Test
    public void testIsLocationDefinedFileName()
    {
        FileHandler handler = new FileHandler();
        handler.setFileName(createTestFile().getName());
        assertTrue("Location not defined", handler.isLocationDefined());
    }

    /**
     * Tests whether an undefined location can be queried.
     */
    @Test
    public void testIsLocationDefinedFalse()
    {
        FileHandler handler = new FileHandler();
        assertFalse("Location defined", handler.isLocationDefined());
    }

    /**
     * Tests isLocationDefined() if only a base path is set.
     */
    @Test
    public void testIsLocationDefinedBasePathOnly()
    {
        FileHandler handler = new FileHandler();
        handler.setBasePath(createTestFile().getParent());
        assertFalse("Location defined", handler.isLocationDefined());
    }

    /**
     * Tests whether the location can be cleared.
     */
    @Test
    public void testClearLocation()
    {
        FileHandler handler = new FileHandler();
        handler.setFile(createTestFile());
        handler.clearLocation();
        assertFalse("Location defined", handler.isLocationDefined());
        assertNull("Got a file", handler.getFile());
        assertNull("Got a URL", handler.getURL());
        assertNull("Got a base path", handler.getBasePath());
        assertNull("Got a path", handler.getPath());
    }

    /**
     * An implementation of the FileBased interface used for test purposes.
     */
    private static class FileBasedTestImpl implements FileBased
    {
        /** The content read from a reader. */
        private String content;

        /**
         * Returns the content read from a reader.
         *
         * @return the read content
         */
        public String getContent()
        {
            return content;
        }

        public void read(Reader in) throws ConfigurationException, IOException
        {
            content = readReader(in);
        }

        public void write(Writer out) throws ConfigurationException,
                IOException
        {
            out.write(CONTENT);
            out.flush();
        }
    }
}
