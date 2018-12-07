package io.dockstore.provision;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.json.JSONArray;
import org.json.JSONObject;
import ro.fortsoft.pf4j.Extension;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;
import ro.fortsoft.pf4j.RuntimeMode;

public class DOSPlugin extends Plugin {

    private static PluginWrapper pluginWrapper;

    /**
     * Constructor to be used by plugin manager for plugin instantiation.
     * Your plugins have to provide constructor with this exact signature to
     * be successfully loaded by manager.
     *
     * @param wrapper
     */
    public DOSPlugin(PluginWrapper wrapper) {
        super(wrapper);
        pluginWrapper = wrapper;
    }

    @Override
    public void start() {
        // for testing the development mode
        if (RuntimeMode.DEVELOPMENT.equals(wrapper.getRuntimeMode())) {
            System.out.println(StringUtils.upperCase("DOSPlugin development mode"));
        }
    }

    @Override
    public void stop() {
        System.out.println("DOSPlugin.stop()");
    }

    @Extension
    public static class DOSPreProvision implements PreProvisionInterface {

        static final Set<String> SCHEME = new HashSet<>(Arrays.asList("dos"));
        static final String SCHEME_PREFERENCE = "scheme-preference";

        DOSPluginUtil dosPluginUtil = new DOSPluginUtil();
        List<String> preferredSchemes = new ArrayList<>();

        @Override
        public void setConfiguration(Map<String, String> map) {
            MapConfiguration config = new MapConfiguration(map);
            config.setListDelimiterHandler(new DefaultListDelimiterHandler(','));

            // Retrieve the scheme preferences from the config file
            this.preferredSchemes = config.getList(String.class, SCHEME_PREFERENCE, Collections.emptyList());
            // Remove any empty strings from the list
            this.preferredSchemes.removeIf(e -> e.equals(""));
        }

        public Set<String> schemesHandled() {
            return SCHEME;
        }

        public List<String> prepareDownload(String targetPath) {
            List<String> urlList = new ArrayList<>();

            // Linked Hash Maps ensure insertion-ordered key-value pairs. URLs resolved by the plugin are
            // inserted by scheme (key) and a list of that scheme's URL(s) (value)
            Map<String, List<String>> urlMap = new LinkedHashMap<>();

            Optional<ImmutableTriple<String, String, String>> dosUri = dosPluginUtil.splitURI(targetPath);

            if (dosUri.isPresent() && schemesHandled().contains(dosUri.get().getLeft())) {
                Optional<JSONObject> response = dosPluginUtil.getResponse(dosUri.get());
                if(response.isPresent()) {
                    JSONArray retrievedUrls = response.get().getJSONObject("data_object").getJSONArray("urls");

                    // Place all URLs into map for fast lookup time. URLs with duplicate schemes are merged into the existing list
                    for (int i = 0; i < retrievedUrls.length(); i++) {
                        try {
                            URI uri = new URI(retrievedUrls.getJSONObject(i).getString("url"));
                            String scheme = uri.getScheme();

                            // Merge the new URI to the list associated with the current scheme
                            urlMap.merge(scheme, Collections.singletonList(uri.toString()), (list1, list2) ->
                                    Stream.of(list1, list2)
                                            .flatMap(Collection::stream)
                                            .collect(Collectors.toList()));
                        } catch (URISyntaxException e) {
                            continue;
                        }
                    }
                    // Append all URLs into urlList in order of preference (if any) and remove added schemes from map to avoid duplication
                    for (String scheme : this.preferredSchemes) {
                        if (urlMap.containsKey(scheme)) {
                            urlList.addAll(urlMap.get(scheme));
                            urlMap.remove(scheme);
                        }
                    }
                    for (List<String> remainder : urlMap.values()) {
                        urlList.addAll(remainder);
                    }
                }
            }
            return urlList;
        }
    }
}
