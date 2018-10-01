package io.dockstore.provision;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;


public class DOSPluginUtilUnitTest {

    private static DOSPluginUtil pluginUtil = new DOSPluginUtil();

    @Test
    public void testSplitUriOldFormat() {
        String uri = "dos://dos-dss.ucsc-cgp-dev.org/fff5a29f-d184-4e3b-9c5b-6f44aea7f527?version=2018-02-28T033124.129027Zf";
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("dos", "dos-dss.ucsc-cgp-dev.org", "fff5a29f-d184-4e3b-9c5b-6f44aea7f527?version=2018-02-28T033124.129027Zf");
        Assert.assertEquals(split, pluginUtil.splitURI(uri).get());
    }

    @Test
    public void testSplitUriNewFormat() {
        String uri = "dos://dg.4503/1aad0eb6-0d89-4fdd-976c-f9aa248fc88c";
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("dos", "dataguids.org", "dg.4503/1aad0eb6-0d89-4fdd-976c-f9aa248fc88c");
        Assert.assertEquals(split, pluginUtil.splitURI(uri).get());

    }

    @Test
    public void testSplitUriMalformedPath() {
        String uri = "fake:/host//uid";
        Assert.assertFalse(pluginUtil.splitURI(uri).isPresent());
    }

    @Test
    public void testSplitUriBadPath1() {
        String uri = "fake";
        Assert.assertFalse(pluginUtil.splitURI(uri).isPresent());
    }

    @Test
    public void testSplitUriBadPath2() {
        String uri = "fake://host";
        Assert.assertFalse(pluginUtil.splitURI(uri).isPresent());
    }

    @Test
    public void testGetResponse() {
        DOSPluginUtil spyPluginUtil = Mockito.spy(DOSPluginUtil.class);
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("dos", "dataguids.org", "dg.4503/b1266976-fd47-4a32-a7f4-d367d545abb2");

        InputStream expectedResponse = IOUtils.toInputStream(
        "{" +
                "\"data_object\":{" +
                        "\"checksums\":[" +
                        "{" +
                                "\"checksum\":\"2f160ab47e55d0e2f51b19264f917a30\"," +
                                        "\"type\":\"md5\"" +
                        "}" +
                        "]," +
                        "\"urls\":[" +
                        "{" +
                                "\"url\":\"gs://cgp-commons-multi-region-public/topmed_open_access/1111ec7b-675d-5c00-8aa4-7eea28f2b846/NWD480514.recab.cram.crai\"" +
                        "}," +
                        "{" +
                                "\"url\":\"s3://cgp-commons-public/topmed_open_access/1111ec7b-675d-5c00-8aa4-7eea28f2b846/NWD480514.recab.cram.crai\"" +
                        "}" +
                        "]," +
                        "\"size\":1416635," +
                        "\"mime_type\":\"\"," +
                        "\"created\":\"2018-05-26T13:43:17.443990\"," +
                        "\"name\":null," +
                        "\"description\":\"\"," +
                        "\"id\":\"dg.4503/b1266976-fd47-4a32-a7f4-d367d545abb2\"," +
                        "\"updated\":\"2018-05-26T13:43:17.443999\"," +
                        "\"version\":\"e4a460cb\"" +
                "}" +
        "}");

        // Mock downloadJSON() to return (non-empty) InputStream
        Mockito.doReturn(Optional.of(expectedResponse)).when(spyPluginUtil).downloadJSON(split);
        Mockito.doNothing().when(spyPluginUtil).disconnect();

        Assert.assertTrue(spyPluginUtil.getResponse(split).isPresent());
    }

    @Test
    public void testGetResponseReturnEmpty1() throws IOException {
        DOSPluginUtil spyPluginUtil = Mockito.spy(DOSPluginUtil.class);
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("fake-scheme", "fake-host", "fake-path");

        // Create mock HttpURLConnection
        HttpURLConnection mockConn = Mockito.mock(HttpURLConnection.class);
        Mockito.when(mockConn.getResponseCode()).thenReturn(500);

        // Return mockConn (with mocked response code) when createConnection() is called
        Mockito.doReturn(mockConn).when(spyPluginUtil).createConnection("http", split);
        Assert.assertFalse(spyPluginUtil.getResponse(split).isPresent());
    }

    @Test
    public void testGetResponseReturnEmpty2() throws IOException {
        DOSPluginUtil spyPluginUtil = Mockito.spy(DOSPluginUtil.class);
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("fake-scheme", "fake-host", "fake-path");

        // Create mock HttpURLConnection
        HttpURLConnection mockConn = Mockito.mock(HttpURLConnection.class);
        Mockito.when(mockConn.getResponseCode()).thenReturn(500).thenReturn(403);

        // Return mockConnection (with mocked response code) when createConnection() is called
        Mockito.doReturn(mockConn).when(spyPluginUtil).createConnection("http", split);
        Mockito.doReturn(mockConn).when(spyPluginUtil).createConnection("https", split);

        Assert.assertFalse(spyPluginUtil.getResponse(split).isPresent());
    }

    @Test
    public void testGetResponseBadURI() {
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("fake-scheme", "fake-host", "fake-path");
        Assert.assertFalse(pluginUtil.getResponse(split).isPresent());
    }

    @Test
    public void testDownloadJSON() throws IOException {
        DOSPluginUtil spyPluginUtil = Mockito.spy(DOSPluginUtil.class);
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("dos", "dataguids.org", "dg.4503/630d31c3-381e-488d-b639-ce5d047a0142");

        // Mock createConnection() response with valid mocked InputStream obj
        HttpURLConnection mockConn = Mockito.mock(HttpURLConnection.class);
        InputStream mockInputStream = Mockito.mock(InputStream.class);

        // Return valid connection when createConn() is called inside downloadJSON()
        Mockito.when(mockConn.getResponseCode()).thenReturn(200);
        Mockito.when(mockConn.getInputStream()).thenReturn(mockInputStream);

        Mockito.doReturn(mockConn).when(spyPluginUtil).createConnection("http", split);

        Assert.assertTrue(spyPluginUtil.downloadJSON(split).isPresent());
    }

    @Test
    public void testDownloadJSONReturnEmpty() throws IOException {
        DOSPluginUtil spyPluginUtil = Mockito.spy(DOSPluginUtil.class);
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("dos", "dataguids.org", "dg.4503/630d31c3-381e-488d-b639-ce5d047a0142");
        // Construct URL https://dataguids.org/ga4gh/dos/v1/dataobjects/dg.4503/630d31c3-381e-488d-b639-ce5d047a0142

        // Create mock HttpURLConnection
        HttpURLConnection mockConn = Mockito.mock(HttpURLConnection.class);
        Mockito.when(mockConn.getResponseCode()).thenReturn(500).thenReturn(403);

        // Return mockConnection (with mocked response code) when createConnection() is called
        Mockito.doReturn(mockConn).when(spyPluginUtil).createConnection("http", split);
        Mockito.doReturn(mockConn).when(spyPluginUtil).createConnection("https", split);

        Assert.assertFalse(spyPluginUtil.downloadJSON(split).isPresent());
    }

    @Test
    public void testCreateConnection() throws IOException {
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("dos", "dos-dss.ucsc-cgp-dev.org", "630d31c3-381e-488d-b639-ce5d047a0142?version=2018-05-26T134315.070662Z");

        URL mockURL = new URL("http://" + split.getMiddle() + "/ga4gh/dos/v1/dataobjects/" + split.getRight());
        HttpURLConnection mockConn = (HttpURLConnection) mockURL.openConnection();
        Assert.assertThat(mockConn.toString(), CoreMatchers.containsString(pluginUtil.createConnection("http", split).toString()));
    }

    @Test
    public void testCreateConnectionReturnNull() {
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("fake-scheme", "fake-host", "fake-path");

        Assert.assertNull(pluginUtil.createConnection("fake-protocol", split));
    }

    @Test
    public void testReadContent() throws IOException {
        String expectedResponse = "{\"data_object\": {\"checksums\": [{\"checksum\": \"3b0f63a815384a3d44c61b4abd40caf9\", " +
                "\"type\": \"md5\"}], \"created\": \"2018-05-26T13:43:15.070662\", \"description\": \"\", " +
                "\"id\": \"dg.4503/630d31c3-381e-488d-b639-ce5d047a0142\", \"mime_type\": \"\", \"name\": null, " +
                "\"size\": 2201638, \"updated\": \"2018-05-26T13:43:15.070672\", " +
                "\"urls\": [{\"url\": \"gs://cgp-commons-multi-region-public/topmed_open_access/44a8837b-4456-5709-b56b-54e23000f13a/NWD100953.recab.cram.crai\"}, " +
                "{\"url\": \"s3://cgp-commons-public/topmed_open_access/44a8837b-4456-5709-b56b-54e23000f13a/NWD100953.recab.cram.crai\"}], " +
                "\"version\": \"2ba0ac25\"}}";

        InputStream testInputStream = IOUtils.toInputStream(
        "{" +
                "\"data_object\": {" +
                        "\"checksums\": [" +
                        "{" +
                                "\"checksum\": \"3b0f63a815384a3d44c61b4abd40caf9\", " +
                                        "\"type\": \"md5\"" +
                        "}" +
                        "], " +
                        "\"created\": \"2018-05-26T13:43:15.070662\", " +
                        "\"description\": \"\", " +
                        "\"id\": \"dg.4503/630d31c3-381e-488d-b639-ce5d047a0142\", " +
                        "\"mime_type\": \"\", " +
                        "\"name\": null, " +
                        "\"size\": 2201638, " +
                        "\"updated\": \"2018-05-26T13:43:15.070672\", " +
                        "\"urls\": [" +
                        "{" +
                                "\"url\": \"gs://cgp-commons-multi-region-public/topmed_open_access/44a8837b-4456-5709-b56b-54e23000f13a/NWD100953.recab.cram.crai\"" +
                        "}, " +
                        "{" +
                                "\"url\": \"s3://cgp-commons-public/topmed_open_access/44a8837b-4456-5709-b56b-54e23000f13a/NWD100953.recab.cram.crai\"" +
                        "}" +
                        "], " +
                        "\"version\": \"2ba0ac25\"" +
                "}" +
        "}");

        Assert.assertEquals(expectedResponse, pluginUtil.readContent(testInputStream));
    }
}
