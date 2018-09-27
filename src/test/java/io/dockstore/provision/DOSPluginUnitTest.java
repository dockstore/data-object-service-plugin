package io.dockstore.provision;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class DOSPluginUnitTest {

    @Test
    public void testDOSProvision() {
        DOSPlugin.DOSPreProvision dos = new DOSPlugin.DOSPreProvision();
    }


    @Test
    public void testSchemesHandled() {
        DOSPlugin.DOSPreProvision dos = new DOSPlugin.DOSPreProvision();
        Set<String> scheme = new HashSet<>(Collections.singletonList("dos"));
        Assert.assertEquals(scheme, dos.schemesHandled());
    }

    @Test
    public void testSchemesHandledFailed() {
        DOSPlugin.DOSPreProvision dos = new DOSPlugin.DOSPreProvision();
        Set<String> scheme = new HashSet<>(Collections.singletonList("fake"));
        Assert.assertNotEquals(scheme, dos.schemesHandled());
    }


    @Test
    public void testPrepareDownload() {
        DOSPlugin.DOSPreProvision dos = new DOSPlugin.DOSPreProvision();
        List<String> expected = new ArrayList<>();
        String targetPath = "dos://dg.4503/630d31c3-381e-488d-b639-ce5d047a0142";
        expected.add("gs://cgp-commons-multi-region-public/topmed_open_access/44a8837b-4456-5709-b56b-54e23000f13a/NWD100953.recab.cram.crai");
        expected.add("s3://cgp-commons-public/topmed_open_access/44a8837b-4456-5709-b56b-54e23000f13a/NWD100953.recab.cram.crai");
        Assert.assertEquals(expected, dos.prepareDownload(targetPath));
    }

    @Test
    public void testPrepareDownloadReturnEmpty1() {
        DOSPlugin.DOSPreProvision dos = new DOSPlugin.DOSPreProvision();
        String targetPath = "dos://dos-dss.ucsc-cgp-dev.org/fff5a29f-d184-4e3b-9c5b-6f44aea7f527?version=2018-02-28T033124.129027Zf";
        Assert.assertTrue(dos.prepareDownload(targetPath).isEmpty());
    }


    @Test
    public void testPrepareDownloadReturnEmpty2() {
        DOSPlugin.DOSPreProvision dos = new DOSPlugin.DOSPreProvision();
        String targetPath = "dos://dg.4503/630d31c3-381e-488d-b639-ffffffffffff";
        Assert.assertTrue(dos.prepareDownload(targetPath).isEmpty());
    }


    @Test
    public void testPrepareDownloadReturnEmpty3() {
        DOSPlugin.DOSPreProvision dos = new DOSPlugin.DOSPreProvision();
        String targetPath = "fake";
        Assert.assertTrue(dos.prepareDownload(targetPath).isEmpty());
    }


    @Test
    public void testPrepareDownloadReturnEmpty4() {
        DOSPlugin.DOSPreProvision dos = new DOSPlugin.DOSPreProvision();
        String targetPath = "dos:/fake";
        Assert.assertTrue(dos.prepareDownload(targetPath).isEmpty());
    }
}
