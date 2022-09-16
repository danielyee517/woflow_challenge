import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Crawler {

    private HashSet<String> nodes;
    private HashMap<String, Integer> counts;
    private static final String BASE_URL = "https://nodes-on-nodes-challenge.herokuapp.com/nodes/";
    private static final String START_NODE = "089ef556-dfff-4ff2-9733-654645be56fe";

    public Crawler() {
        nodes = new HashSet<>();
        counts = new HashMap<>();
    }
    public static void main(String[] args) {
        String startUrl = BASE_URL + START_NODE;
        new Crawler().getNodeLinks(startUrl);
    }

    public void getNodeLinks(String URL) {
        AtomicReference<String> mostSharedNode = new AtomicReference<>(START_NODE);
        AtomicInteger maxCount = new AtomicInteger();
        request(URL, nodes, counts);
        counts.forEach(
                (key, value)
                        -> {
                    if (value > maxCount.get()) {
                        maxCount.set(value);
                        mostSharedNode.set(key);
                    }
                });
        System.out.println("Unique nodes: " + nodes.size());
        System.out.println("Most shared node: " + mostSharedNode + " with " + maxCount + " edges");

    }
    private static void request(String url, HashSet<String> nodes, HashMap<String, Integer> edges) {
        try {
            nodes.add(url);
            Connection con = Jsoup.connect(url);
            String json = con.ignoreContentType(true).execute().body();

            if (con.response().statusCode() == 200) {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    String id = arr.getJSONObject(i).getString("id");
                    nodes.add(id);
                    Integer count = edges.getOrDefault(id, 0);
                    edges.put(id, ++count);
                    JSONArray children = arr.getJSONObject(i).getJSONArray("child_node_ids");
                    if (children.length() > 0) {
                        String newUrl = constructNewUrl(children, nodes, edges);
                        request(newUrl, nodes, edges);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("For '" + url + "': " + e.getMessage());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static String constructNewUrl(JSONArray children, HashSet<String> nodes, HashMap<String, Integer> edges) throws JSONException {
        StringBuilder url = new StringBuilder(BASE_URL);
        String id;
        for (int i = 0; i < children.length(); i++) {
            id = children.getString(i);
            Integer count = edges.getOrDefault(id, 0);
            edges.put(id, ++count);
            if (!nodes.contains(id)) {
                url.append(id).append(',');
            }
        }
        return url.toString();
    }
}
