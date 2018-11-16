package io.dockstore.provision;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.json.JSONArray;
import org.json.JSONObject;
import ro.fortsoft.pf4j.Extension;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;
import ro.fortsoft.pf4j.RuntimeMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


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

        static final Set<String> SCHEME = new HashSet<>(Lists.newArrayList("dos"));
        static final String SCHEME_PREFERENCE = "scheme-preference";
        static final int GET_SCHEME = 0;
        private Map<String, String> config;

        DOSPluginUtil dosPluginUtil = new DOSPluginUtil();
        List<String> preferredSchemes = new ArrayList<>();

        @Override
        public void setConfiguration(Map<String, String> map) {
            this.config = map;

            if (this.config.containsKey(SCHEME_PREFERENCE)) {
                this.preferredSchemes = Arrays.asList(this.config.get(SCHEME_PREFERENCE).trim().split(",\\s"));
            }
        }

        public Set<String> schemesHandled() {
            return SCHEME;
        }

        public List<String> prepareDownload(String targetPath) {
            List<String> urlList = new ArrayList<>();
            Map<String, List<String>> urlMap = new LinkedHashMap<>();
            String protocol = ":\\/\\/(.+)/";

            Optional<ImmutableTriple<String, String, String>> uri = dosPluginUtil.splitURI(targetPath);

            if (uri.isPresent() && schemesHandled().contains(uri.get().getLeft())) {

                Optional<JSONObject> jsonObj = dosPluginUtil.getResponse(uri.get());

                if(jsonObj.isPresent()) {
                    JSONArray urls = jsonObj.get().getJSONObject("data_object").getJSONArray("urls");
                    // Place all URLs into map for fast lookup time. URLs with duplicate schemes are appended to existing entry
                    for (int i = 0; i < urls.length(); i++) {
                        String url = urls.getJSONObject(i).getString("url");
                        String scheme = url.split(protocol)[GET_SCHEME];
                        if (urlMap.containsKey(scheme)) {
                            urlMap.get(scheme).add(url);
                        } else {
                            urlMap.put(scheme, new ArrayList<>(Arrays.asList(url)));
                        }
                    }
                    // Append all URLs into urlList in order of preference (if any) and remove added schemes from map to avoid duplication
                    for (String scheme : preferredSchemes) {
                        if (urlMap.containsKey(scheme)) {
                            urlList.addAll(urlMap.get(scheme));
                            urlMap.remove(scheme);
                        }
                    }
                    for (List<String> remainingSchemes : urlMap.values()) {
                        urlList.addAll(remainingSchemes);
                    }
                }
            }
            return urlList;
        }
    }
}
