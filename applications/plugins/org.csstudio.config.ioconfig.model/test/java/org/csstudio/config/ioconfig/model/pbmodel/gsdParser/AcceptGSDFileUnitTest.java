/**
 * 
 */
package org.csstudio.config.ioconfig.model.pbmodel.gsdParser;


import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

/**
 * @author hrickens
 *
 */
public class AcceptGSDFileUnitTest {
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
//        GSDTestFiles.B756_P33.getFileAsString();
    }
    
    @Test
    public void GSDTestFile_B756_P33() throws Exception {
        ParsedGsdFileModel model = GsdFileParser.parse(GSDTestFiles.B756_P33.getFileAsGSDFileDBO());
        Assert.assertNotNull(model);
    }

    @Test
    public void GSDTestFile_BIMF5861() throws Exception {
        ParsedGsdFileModel model = GsdFileParser.parse(GSDTestFiles.BIMF5861.getFileAsGSDFileDBO());
        Assert.assertNotNull(model);
    }
    
    @Test
    public void GSDTestFile_DESY_MSyS_V11() throws Exception {
        ParsedGsdFileModel model = GsdFileParser.parse(GSDTestFiles.DESY_MSyS_V11.getFileAsGSDFileDBO());
        Assert.assertNotNull(model);
    }
    
    @Test
    public void GSDTestFile_SOFTB203() throws Exception {
        ParsedGsdFileModel model = GsdFileParser.parse(GSDTestFiles.SOFTB203.getFileAsGSDFileDBO());
        Assert.assertNotNull(model);
    }
    
    @Test
    public void GSDTestFile_YP0004C2() throws Exception {
        ParsedGsdFileModel model = GsdFileParser.parse(GSDTestFiles.YP0004C2.getFileAsGSDFileDBO());
        Assert.assertNotNull(model);
    }
    
    @Test
    public void GSDTestFile_YP003051() throws Exception {
        ParsedGsdFileModel model = GsdFileParser.parse(GSDTestFiles.YP003051.getFileAsGSDFileDBO());
        Assert.assertNotNull(model);
    }
    
    @Test
    public void GSDTestFile_YP0206CA() throws Exception {
        ParsedGsdFileModel model = GsdFileParser.parse(GSDTestFiles.YP0206CA.getFileAsGSDFileDBO());
        Assert.assertNotNull(model);
    }
}
