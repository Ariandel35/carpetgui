package yiwen.carpetgui.rulestack;

import java.util.List;

public class RuleStackData {
    public String activePrefabName;
    public List<String> allPrefabNames;
    public List<LayerData> layers;
    public List<Change> pendingChanges;
    public List<LayerData> futureLayers;

    public RuleStackData(String activePrefabName, List<String> allPrefabNames,
                         List<LayerData> layers, List<Change> pendingChanges,
                         List<LayerData> futureLayers) {
        this.activePrefabName = activePrefabName;
        this.allPrefabNames = allPrefabNames;
        this.layers = layers;
        this.pendingChanges = pendingChanges;
        this.futureLayers = futureLayers;
    }

    public record LayerData(String id, String message, long timestamp, List<Change> changes) {}
    public record Change(String ruleKey, String prevValue, boolean prevIsDefault,
                          String newValue, boolean newIsDefault) {}
}
