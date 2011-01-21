package org.csstudio.opibuilder.test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.csstudio.opibuilder.persistence.URLPath;
import org.csstudio.opibuilder.util.ResourceUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.junit.Ignore;
import org.junit.Test;

/** [Headless] JUnit Plug-In test
 *
 * @author Xihui Chen
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ResourceUtilTest {
    public final static IPath URL_PATH = new Path("http://ics-srv-web2.sns.ornl.gov/opi/main.opi");
	public final static IPath LOCAL_PATH = new Path("C:\\Users\\5hz\\Desktop\\2_5_1_XY_Graph.opi");

	/** This test requires a workspace
	 *  @throws Exception on error
	 */
	@Test
	public void testPathToInputStream() throws Exception
	{
	    // Prepare known workspace layout
        final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        System.out.println("Workspace: " + root.getLocation());

        // Create test project
        final IProject project = root.getProject("Project");
        if (! project.exists())
            project.create(new NullProgressMonitor());
        project.open(new NullProgressMonitor());

        // Create test folder
        final IFolder folder = project.getFolder("Folder");
        if (! folder.exists())
            folder.create(true, true, new NullProgressMonitor());

        // Create test File
        final IFile file = folder.getFile("File.ext");
        if (! file.exists())
        {
            final InputStream content = new ByteArrayInputStream("This is a test\n".getBytes());
            file.create(content, true, new NullProgressMonitor());
        }

        // Actual tests

        // Read file from workspace
        Path path = new Path("Project/Folder/File.ext");
        System.out.println("Workspace path: " + path);
        InputStream stream = ResourceUtil.pathToInputStream(path);
        assertFileContent(stream);

        // Read file from local file system
        path = new Path(file.getLocation().makeAbsolute().toOSString());
        System.out.println("Local file system path: " + path);
        stream = ResourceUtil.pathToInputStream(path);
        assertFileContent(stream);

        // Read file as URL
        path = new Path("file://" + file.getLocation().makeAbsolute().toOSString());
        System.out.println("URL path: " + path);
        stream = ResourceUtil.pathToInputStream(path);
        assertFileContent(stream);

        // Read some network URL
        path = new Path("http://www.google.com");
        System.out.println("Web URL: " + path);
        stream = ResourceUtil.pathToInputStream(path);
        assertNotNull(stream);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        final String line = reader.readLine();
        System.out.println(line);
        assertNotNull(line);
        reader.close();

        // Missing workspace file
        path = new Path("Project/Folder/NoSuchFile.xyz");
        System.out.println("Workspace path: " + path);
        try
        {
            ResourceUtil.pathToInputStream(path);
            fail("Found missing file?");
        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage());
        }

        // Cleanup
        // project.delete(true, new NullProgressMonitor());
	}

	/** Check content of test file */
    private void assertFileContent(final InputStream stream) throws Exception
    {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        final String line = reader.readLine();
        System.out.println(line);
        // Note: reads line without trailing "\n"
        assertEquals(line, "This is a test");
        reader.close();
    }

    /** This could run as a plain JUnit test */
	@Test
	public void testBuildRelativePath(){
		IPath path = new URLPath("http://a/b/c/d.txt");
		IPath path2 = new URLPath("http://a/b/e.txt");
		System.out.println(Arrays.toString(path2.segments()));
		IPath path3 = path.makeRelativeTo(path2);
		assertEquals("../c/d.txt", path3.toString());
		System.out.println(Arrays.toString(new Path("../c/d/e.txt").segments()));
		IPath path4 = ResourceUtil.buildRelativePath(path2, path);
		assertEquals("../c/d.txt", path4.toString());
	}

	@Test
	@Ignore
	public void testURLPathToInputStream() throws Exception{
		BufferedReader in = new BufferedReader(
				new InputStreamReader(ResourceUtil.pathToInputStream(URL_PATH)));
		String inputLine;
		while ((inputLine = in.readLine()) != null)
		    System.out.println(inputLine);
		in.close();
    }

	@Test
    @Ignore
	public void testLOCALPathToInputStream() throws Exception{
		BufferedReader in = new BufferedReader(
				new InputStreamReader(ResourceUtil.pathToInputStream(LOCAL_PATH)));
		String inputLine;
		while ((inputLine = in.readLine()) != null)
		    System.out.println(inputLine);
		in.close();
    }
}
