package io.dockstore.provision;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.net.HttpURLConnection.HTTP_OK;


import org.apache.commons.lang3.math.NumberUtils;

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
    Optional<ImmutableTriple<String, String, String>> splitUri(String dosURI) {
        if (Pattern.compile(":\\/\\/(.+)/").matcher(dosURI).find()){
            List<String> split  = Lists.newArrayList(dosURI.split(":\\/\\/|/"));

            // Find out if the path is of the DOS GUID format
            // dos://dg.4503/6f9ad7df-3751-4056-9340-aa9448525b54
            // See if the Host portion starts with 'dg' and ends with a port number
            List<String> host_split = Lists.newArrayList(split.get(HOST).split("\\.", 2));
            if (host_split.size() > 1 && host_split.get(0).equals("dg") && NumberUtils.isNumber(host_split.get(1))) {
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
    Optional<JSONObject> grabJSON(ImmutableTriple<String, String, String> immutableTriple){
        String content;
        HttpURLConnection conn = null;

        try {
            conn = createConnection("http", immutableTriple);
            if (Objects.requireNonNull(conn).getResponseCode() != HTTP_OK) {
                try {
                    conn = createConnection("https", immutableTriple);
                    if (Objects.requireNonNull(conn).getResponseCode() != HTTP_OK) { return Optional.empty(); }
                } catch (IOException e) {
                    e.printStackTrace();
                    return Optional.empty();
                }
            }
            content = readResponse(conn.getInputStream());
            return Optional.of(new JSONObject(content));
        } catch (Exception e) {
            System.err.println("Plugin HttpURLConnection error: "  + e.getCause());
            e.printStackTrace();
        } finally {
            assert conn != null;
            conn.disconnect();
        }
        return Optional.empty();
    }

    HttpURLConnection createConnection(String protocol, ImmutableTriple<String, String, String> immutableTriple) {
        try {
            URL request = new URL(protocol + "://" + immutableTriple.getMiddle() + API +  immutableTriple.getRight());
            return (HttpURLConnection) request.openConnection();
        } catch ( IOException e) {
            System.err.println("ERROR opening HTTP URL Connection:" + protocol + "://" + immutableTriple.getMiddle() + API +  immutableTriple.getRight());
            e.printStackTrace();
        }
        return null;
    }

    String readResponse(InputStream stream) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream))){
            String line;
            StringBuilder content = new StringBuilder();
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
            return content.toString();
        } catch (IOException e) {
            System.err.println("ERROR reading HTTP Response object.");
            e.printStackTrace();
        }
        return null;
    }
}
