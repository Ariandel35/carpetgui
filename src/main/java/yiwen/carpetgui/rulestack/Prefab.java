package yiwen.carpetgui.rulestack;

import java.util.*;

public class Prefab {
    private final String name;
    private final List<RuleLayer> layers = new ArrayList<>();
    private final List<RuleLayer> futureLayers = new ArrayList<>();
    private final List<RuleChange> futureChanges = new ArrayList<>();

    public Prefab(String name) { this.name = name; }

    public String getName() { return name; }
    public List<RuleLayer> getLayers() { return layers; }
    public List<RuleLayer> getFutureLayers() { return futureLayers; }
    public List<RuleChange> getFutureChanges() { return futureChanges; }
}
