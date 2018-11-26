package io.dockstore.provision;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.json.JSONException;
import org.json.JSONObject;

import static java.net.HttpURLConnection.HTTP_OK;

class DOSPluginUtil {

    private static final String API = "/ga4gh/dos/v1/dataobjects/";
    private static final int SCHEME = 0;
    private static final int HOST = 1;
    private static final int PATH = 2;

    private static final String DG_HOST = "dataguids.org";
    private static final int DG_PREFIX = 1;
    private static final int DG_UUID = 2;

    // Package-private constructor
    DOSPluginUtil() {
    }

    /**
     *
     *
     * @param dosURI The string targetPath
     * @return the scheme, host, and path of the targetPath, or <code>Optional.empty()</code>
     */
    Optional<ImmutableTriple<String, String, String>> splitURI(String dosURI) {
        if (Pattern.compile(":\\/\\/(.+)/").matcher(dosURI).find()){
            List<String> split  = Lists.newArrayList(dosURI.split(":\\/\\/|/"));
            // Find out if the path is of the DOS GUID old format
            // dos://dos-dss.ucsc-cgp-dev.org/630d31c3-381e-488d-b639-ce5d047a0142?version=2018-05-26T134315.070662Z
            // or the new format
            // dos://dg.4503/630d31c3-381e-488d-b639-ce5d047a0142
            // See if the Host portion starts with 'dg' and ends with a port number
            List<String> hostSplit = Lists.newArrayList(split.get(HOST).split("\\.", 2));
            if (hostSplit.size() > 1 && hostSplit.get(0).equals("dg") && NumberUtils.isNumber(hostSplit.get(1))) {
                return Optional.of(new ImmutableTriple<>(split.get(SCHEME), DG_HOST, split.get(DG_PREFIX) + "/" + split.get(DG_UUID)));
            }
            return Optional.of(new ImmutableTriple<>(split.get(SCHEME), split.get(HOST), split.get(PATH)));
        }
        return Optional.empty();
    }

    /**
     * Gets the JSON response from targetPath using HTTP GET request
     *
     * @param immutableTriple The targetPath as an ImmutableTriple of <scheme, host, path>
     * @return The JSONObject containing the content of the JSON response, or <code>Optional.empty()</code>
     */
    Optional<JSONObject> getResponse(ImmutableTriple<String, String, String> immutableTriple) {
        HttpURLConnection conn = createConnection(immutableTriple);
        try {
            if (conn == null) {
                return Optional.empty();
            }

            final Optional<InputStream> jsonResponse = downloadJSON(conn);
            final String content = jsonResponse.map(this::readStream).orElse(null);

            if (content != null) {
                return Optional.of(new JSONObject(content));
            }

        } catch (JSONException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            disconnect(conn);
        }
        return Optional.empty();
    }

    HttpURLConnection createConnection(ImmutableTriple<String, String, String> immutableTriple) {
        try {
            HttpURLConnection validConn;
            validConn = openURL("http", immutableTriple);       // Separate method for accurate unit testing

            if (validConn == null || validConn.getResponseCode() != HTTP_OK) {
                validConn = openURL("https", immutableTriple);
                if (validConn == null || validConn.getResponseCode() != HTTP_OK) {
                    return null;
                }
            }
            return validConn;

        } catch ( IOException e) {
            System.err.println("Error: " + e.getMessage() + ". Returning null");
            return null;
        }
    }

    HttpURLConnection openURL(String protocol, ImmutableTriple<String, String, String> immutableTriple) {
        try {
            URL request = new URL(protocol + "://" + immutableTriple.getMiddle() + API +  immutableTriple.getRight());
            return (HttpURLConnection) request.openConnection();
        } catch (IOException e) {
            System.err.println("Error:" + e.getMessage() + ". Returning null");
            return null;
        }
    }

    Optional<InputStream> downloadJSON(HttpURLConnection conn) {
        try {
            return Optional.ofNullable(conn.getInputStream());
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage() + ". Returning empty");
            return Optional.empty();
        }
    }

    String readStream(InputStream stream) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream))){
            String line;
            StringBuilder content = new StringBuilder();
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
            return content.toString();

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage() + ". Returning null");
            return null;
        }
    }

    void disconnect(HttpURLConnection conn) {
        if (conn != null) {
            conn.disconnect();
        }
    }
}
