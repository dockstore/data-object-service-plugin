package io.dockstore.provision;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class DOSPluginUnitTest {

    @Mock (name = "pluginUtil")
    DOSPluginUtil pluginUtil;
    @InjectMocks DOSPlugin.DOSPreProvision dosPreProvision;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSchemesHandled() {
        Set<String> scheme = new HashSet<>(Collections.singletonList("dos"));
        Assert.assertEquals(scheme, dosPreProvision.schemesHandled());
    }

    @Test
    public void testSchemesHandledFailed() {
        Set<String> scheme = new HashSet<>(Collections.singletonList("fake"));
        Assert.assertNotEquals(scheme, dosPreProvision.schemesHandled());
    }

    @Test
    public void testPrepareDownload() throws IOException {
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("dos", "dataguids.org", "dg.4503/1aad0eb6-0d89-4fdd-976c-f9aa248fc88c");

        List<String> expected = new ArrayList<>();
        expected.add("gs://cgp-commons-multi-region-public/topmed_open_access/53ae11a8-ef4d-5aa6-9227-f372b422f5a1/NWD446684.recab.cram.crai");
        expected.add("s3://cgp-commons-public/topmed_open_access/53ae11a8-ef4d-5aa6-9227-f372b422f5a1/NWD446684.recab.cram.crai");

        InputStream mockStream = IOUtils.toInputStream(
        "{" +
                "\"data_object\": {" +
                        "\"checksums\": [" +
                                "{" +
                                    "\"checksum\": \"bf140ba9778dd091d533c59d888c4463\"," +
                                    "\"type\": \"md5\"" +
                                "}" +
                        "]," +
                        "\"created\": \"2018-05-26T13:43:17.056861\"," +
                        "\"description\": \"\"," +
                        "\"id\": \"dg.4503/1aad0eb6-0d89-4fdd-976c-f9aa248fc88c\"," +
                        "\"mime_type\": \"\"," +
                        "\"name\": null," +
                        "\"size\": 1503901," +
                        "\"updated\": \"2018-05-26T13:43:17.056871\"," +
                        "\"urls\": [" +
                                "{" +
                                        "\"url\": \"gs://cgp-commons-multi-region-public/topmed_open_access/53ae11a8-ef4d-5aa6-9227-f372b422f5a1/NWD446684.recab.cram.crai\"" +
                                "}," +
                                "{" +
                                        "\"url\": \"s3://cgp-commons-public/topmed_open_access/53ae11a8-ef4d-5aa6-9227-f372b422f5a1/NWD446684.recab.cram.crai\"" +
                                "}" +
                        "]," +
                        "\"version\": \"89dfdc16\"" +
                "}" +
        "}");

        BufferedReader in = new BufferedReader(new InputStreamReader(mockStream));
        String line;
        StringBuilder content = new StringBuilder();
        while ((line = in.readLine()) != null) {
            content.append(line);
        }
        JSONObject expectedJSON = new JSONObject(content.toString());

        // Mock expected DOSPluginUtil object functionality when splitURI() and getResponse() are called
        Mockito.when(pluginUtil.splitURI("dos://dg.4503/1aad0eb6-0d89-4fdd-976c-f9aa248fc88c")).thenReturn(java.util.Optional.ofNullable(split));
        Mockito.when(pluginUtil.getResponse(split)).thenReturn(java.util.Optional.ofNullable(expectedJSON));

        List<String> actual = dosPreProvision.prepareDownload("dos://dg.4503/1aad0eb6-0d89-4fdd-976c-f9aa248fc88c");
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPrepareDownloadReturnEmpty1() {
        String targetPath = "dos://dos-dss.ucsc-cgp-dev.org/ffffffff";
        Assert.assertTrue(dosPreProvision.prepareDownload(targetPath).isEmpty());
    }

    @Test
    public void testPrepareDownloadReturnEmpty2() {
        String targetPath = "dos://dg.4503/630d31c3-381e-488d-b639-ffffffffffff";
        Assert.assertTrue(dosPreProvision.prepareDownload(targetPath).isEmpty());
    }

    @Test
    public void testPrepareDownloadReturnEmpty3() {
        String targetPath = "fake";
        Assert.assertTrue(dosPreProvision.prepareDownload(targetPath).isEmpty());
    }

    @Test
    public void testPrepareDownloadReturnEmpty4() {
        String targetPath = "dos:/fake";
        Assert.assertTrue(dosPreProvision.prepareDownload(targetPath).isEmpty());
    }
}
