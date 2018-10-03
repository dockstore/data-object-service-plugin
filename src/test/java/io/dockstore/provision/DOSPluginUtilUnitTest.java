package io.dockstore.provision;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
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
        // Construct URL https://dataguids.org/ga4gh/dos/v1/dataobjects/dg.4503/b1266976-fd47-4a32-a7f4-d367d545abb2
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("dos", "dataguids.org", "dg.4503/b1266976-fd47-4a32-a7f4-d367d545abb2");

        // Create mock valid HttpURLConnection object
        HttpURLConnection mockConn = Mockito.mock(HttpURLConnection.class);
        Mockito.doReturn(mockConn).when(spyPluginUtil).createConnection(split);

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
        Mockito.doReturn(Optional.of(expectedResponse)).when(spyPluginUtil).downloadJSON(mockConn);
        Mockito.doNothing().when(spyPluginUtil).disconnect(mockConn);

        Assert.assertTrue(spyPluginUtil.getResponse(split).isPresent());
    }

    @Test
    public void testGetResponseReturnEmpty1() {
        // Test for broken link such that createConnection() returns null

        DOSPluginUtil spyPluginUtil = Mockito.spy(DOSPluginUtil.class);
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("dos", "dos-dss.ucsc-cgp-dev.org", "fff5a29f-ffffffffff");

        Mockito.doReturn(null).when(spyPluginUtil).createConnection(split);
        Assert.assertFalse(spyPluginUtil.getResponse(split).isPresent());
    }

    @Test
    public void testGetResponseReturnEmpty2() {
        // Test for null string such that readStream() returns null

        DOSPluginUtil spyPluginUtil = Mockito.spy(DOSPluginUtil.class);
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("dos", "dos-dss.ucsc-cgp-dev.org", "fff5a29f-ffffffffff");

        // Create mock HttpURLConnection & InputStream objects
        HttpURLConnection mockConn = Mockito.mock(HttpURLConnection.class);
        InputStream mockInputStream = Mockito.mock(InputStream.class);

        // Return mockConn when createConnection() is called
        Mockito.doReturn(mockConn).when(spyPluginUtil).createConnection(split);
        // Return null when readStream() is called
        Mockito.doReturn(null).when(spyPluginUtil).readStream(mockInputStream);

        Assert.assertFalse(spyPluginUtil.getResponse(split).isPresent());
    }

    @Test
    public void testGetResponseReturnEmpty3() {
        // Test for invalid URI such that createConnection() returns null

        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("fake-scheme", "fake-host", "fake-path");
        Assert.assertFalse(pluginUtil.getResponse(split).isPresent());
    }

    @Test
    public void testDownloadJSON() throws IOException {
        DOSPluginUtil spyPluginUtil = Mockito.spy(DOSPluginUtil.class);

        // Create mock HttpURLConnection & InputStream objects
        HttpURLConnection mockConn = Mockito.mock(HttpURLConnection.class);
        InputStream mockInputStream = Mockito.mock(InputStream.class);

        Mockito.doReturn(mockInputStream).when(mockConn).getInputStream();
        Assert.assertTrue(spyPluginUtil.downloadJSON(mockConn).isPresent());
    }

    @Test
    public void testDownloadJSONReturnEmpty() {
        // Test for handling null InputStreams

        DOSPluginUtil spyPluginUtil = Mockito.spy(DOSPluginUtil.class);

        // Create mock HttpURLConnection object with null InputStream
        HttpURLConnection mockConn = Mockito.mock(HttpURLConnection.class);
        Assert.assertFalse(spyPluginUtil.downloadJSON(mockConn).isPresent());
    }

    @Test
    public void testDownloadJSONReturnEmpty2() throws IOException {
        // Test for handling IOExceptions

        DOSPluginUtil spyPluginUtil = Mockito.spy(DOSPluginUtil.class);

        // Create mock HttpURLConnection object
        HttpURLConnection mockConn = Mockito.mock(HttpURLConnection.class);

        Mockito.doThrow(new IOException()).when(mockConn).getInputStream();
        Assert.assertFalse(spyPluginUtil.downloadJSON(mockConn).isPresent());
    }

    @Test
    public void testCreateConnection1() throws IOException {
        DOSPluginUtil spyPluginUtil = Mockito.spy(DOSPluginUtil.class);
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("dos", "dataguids.org", "dg.4503/630d31c3-381e-488d-b639-ce5d047a0142");
        // Construct URL https://dataguids.org/ga4gh/dos/v1/dataobjects/dg.4503/630d31c3-381e-488d-b639-ce5d047a0142

        // Create mock URL & HttpURLConnection objects
        HttpURLConnection mockConn = Mockito.mock(HttpURLConnection.class);
        Mockito.when(mockConn.getResponseCode()).thenReturn(200);

        Mockito.doReturn(mockConn).when(spyPluginUtil).openURL("http", split);
        Assert.assertNotNull(spyPluginUtil.createConnection(split));
   }

    @Test
    public void testCreateConnection2() throws IOException {
        DOSPluginUtil spyPluginUtil = Mockito.spy(DOSPluginUtil.class);
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("dos", "dataguids.org", "dg.4503/630d31c3-381e-488d-b639-ce5d047a0142");

        // Create mock HttpURLConnection objects that fails on http but works for https
        HttpURLConnection mockConn = Mockito.mock(HttpURLConnection.class);
        Mockito.when(mockConn.getResponseCode()).thenReturn(500).thenReturn(200);

        Mockito.doReturn(mockConn).when(spyPluginUtil).openURL("http", split);
        Mockito.doReturn(mockConn).when(spyPluginUtil).openURL("https", split);

        Assert.assertNotNull(spyPluginUtil.createConnection(split));
    }

    @Test
    public void testCreateConnectionReturnNull1() {
        // Test for broken link

        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("dos", "dos-dss.ucsc-cgp-dev.org", "630d31c3ffffffffffffff");
        Assert.assertNull(pluginUtil.createConnection(split));
    }

    @Test
    public void testCreateConnectionReturnNull2() throws IOException {
        // Test for handling IOExceptions

        DOSPluginUtil spyPluginUtil = Mockito.spy(DOSPluginUtil.class);
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("fake-scheme", "fake-host", "fake-path");

        // Create mock HttpURLConnection object
        HttpURLConnection mockConn = Mockito.mock(HttpURLConnection.class);

        Mockito.doThrow(new IOException()).when(mockConn).getResponseCode();
        Assert.assertNull(spyPluginUtil.createConnection(split));
    }

    @Test
    public void testCreateConnectionReturnNull3() {
        // Test for null HttpURLConnection object such that openURL() returns null

        DOSPluginUtil spyPluginUtil = Mockito.spy(DOSPluginUtil.class);
        ImmutableTriple<String, String, String> split =
                new ImmutableTriple<>("fake-scheme", "fake-host", "fake-path");

        Mockito.doReturn(null).when(spyPluginUtil).openURL("http", split);
        Mockito.doReturn(null).when(spyPluginUtil).openURL("https", split);

        Assert.assertNull(spyPluginUtil.createConnection(split));
    }

    @Test
    public void testReadStream() {
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

        Assert.assertEquals(expectedResponse, pluginUtil.readStream(testInputStream));
    }

    @Test
    public void testReadStreamReturnNull() throws IOException {
        // Test for handling IOExceptions

        InputStream mockInputStream = Mockito.mock(InputStream.class);

        Mockito.doThrow(new IOException()).when(mockInputStream).read();
        Assert.assertNull(pluginUtil.readStream(mockInputStream));
    }
}
