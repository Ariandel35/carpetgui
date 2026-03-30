package ml.mypals.carpetgui.ruleStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.*;

public class Prefab {

    private final String name;
    private final long createdAt;
    private final Map<String, RuleValueSnapshot> baseline;
    private final List<RuleLayer> layers;
    /**
     * Redo-stack: layers that were popped but not yet discarded.
     * {@code getLast()} is the next layer to redo (most-recently popped).
     * {@code getFirst()} is the oldest popped layer (would be redone last).
     * The list is cleared as soon as a "dirty" push happens.
     */
    private final List<RuleLayer> futureLayers;
    private int layerCounter;

    public Prefab(String name, Map<String, RuleValueSnapshot> baseline) {
        this.name = name;
        this.createdAt = System.currentTimeMillis();
        this.baseline = new HashMap<>(baseline);
        this.layers = new ArrayList<>();
        this.futureLayers = new ArrayList<>();
        this.layerCounter = 0;
    }

    Prefab(String name, long createdAt,
           Map<String, RuleValueSnapshot> baseline,
           List<RuleLayer> layers,
           List<RuleLayer> futureLayers,
           int counter) {
        this.name = name;
        this.createdAt = createdAt;
        this.baseline = baseline;
        this.layers = layers;
        this.futureLayers = futureLayers;
        this.layerCounter = counter;
    }

    public static Prefab fromJson(JsonObject o) {
        Map<String, RuleValueSnapshot> baseline = new HashMap<>();
        o.getAsJsonObject("baseline").entrySet()
                .forEach(e -> baseline.put(e.getKey(),
                        RuleValueSnapshot.fromJson(e.getValue().getAsJsonObject())));

        List<RuleLayer> layers = new ArrayList<>();
        o.getAsJsonArray("layers")
                .forEach(e -> layers.add(RuleLayer.fromJson(e.getAsJsonObject())));

        List<RuleLayer> futureLayers = new ArrayList<>();
        if (o.has("futureLayers")) {
            o.getAsJsonArray("futureLayers")
                    .forEach(e -> futureLayers.add(RuleLayer.fromJson(e.getAsJsonObject())));
        }

        return new Prefab(
                o.get("name").getAsString(),
                o.get("createdAt").getAsLong(),
                baseline,
                layers,
                futureLayers,
                o.get("counter").getAsInt()
        );
    }

    // -------------------------------------------------------------------------
    // Basic getters
    // -------------------------------------------------------------------------

    public String getName()      { return name; }
    public long   getCreatedAt() { return createdAt; }
    public int    getSize()      { return layers.size(); }
    public boolean isEmpty()     { return layers.isEmpty(); }

    public List<RuleLayer> getLayers() {
        return Collections.unmodifiableList(layers);
    }

    public Map<String, RuleValueSnapshot> getBaseline() {
        return Collections.unmodifiableMap(baseline);
    }

    public RuleLayer peek() {
        return layers.isEmpty() ? null : layers.getLast();
    }

    // -------------------------------------------------------------------------
    // Active-stack mutations
    // -------------------------------------------------------------------------

    public int nextId() {
        return ++layerCounter;
    }

    public void pushLayer(RuleLayer layer) {
        layers.add(layer);
    }

    public RuleLayer popLayer() {
        return layers.isEmpty() ? null : layers.removeLast();
    }

    // -------------------------------------------------------------------------
    // Future (redo) stack
    // -------------------------------------------------------------------------

    /** Returns true when there is at least one layer that can be re-applied. */
    public boolean hasFuture() {
        return !futureLayers.isEmpty();
    }

    /**
     * Ordered oldest-first; {@code getLast()} is the next layer to redo.
     */
    public List<RuleLayer> getFutureLayers() {
        return Collections.unmodifiableList(futureLayers);
    }

    /** Called by pop: remembers the layer so it can be redone later. */
    public void pushFuture(RuleLayer layer) {
        futureLayers.add(layer);
    }

    /**
     * Called by push when there are no pending changes: removes and returns
     * the next-to-redo layer (most recently popped).
     */
    public RuleLayer popFuture() {
        return futureLayers.isEmpty() ? null : futureLayers.removeLast();
    }

    /**
     * Called by push when the user made new changes after a pop: the redo
     * history is no longer valid.
     */
    public void clearFuture() {
        futureLayers.clear();
    }

    // -------------------------------------------------------------------------
    // State resolution
    // -------------------------------------------------------------------------

    public Map<String, RuleValueSnapshot> resolvedState() {
        Map<String, RuleValueSnapshot> state = new HashMap<>(baseline);
        for (RuleLayer layer : layers) {
            for (RuleChange c : layer.getChanges()) {
                state.put(c.ruleKey(), c.newSnapshot());
            }
        }
        return state;
    }

    // -------------------------------------------------------------------------
    // Serialization
    // -------------------------------------------------------------------------

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("createdAt", createdAt);
        o.addProperty("counter", layerCounter);

        JsonObject bl = new JsonObject();
        baseline.forEach((k, v) -> bl.add(k, v.toJson()));
        o.add("baseline", bl);

        JsonArray arr = new JsonArray();
        layers.forEach(l -> arr.add(l.toJson()));
        o.add("layers", arr);

        JsonArray future = new JsonArray();
        futureLayers.forEach(l -> future.add(l.toJson()));
        o.add("futureLayers", future);

        return o;
    }
}