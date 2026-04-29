package Ariandel.carpetgui.rulestack;

import net.minecraft.server.MinecraftServer;
import java.util.*;

public class PrefabManager {
    private final MinecraftServer server;
    private final List<Prefab> prefabs = new ArrayList<>();
    private Prefab activePrefab;
    private String activeName = "default";

    public PrefabManager(MinecraftServer server) {
        this.server = server;
    }

    public void init() {
        activePrefab = new Prefab("default");
        prefabs.add(activePrefab);
    }

    public Prefab getActivePrefab() { return activePrefab; }
    public String getActiveName() { return activeName; }
    public List<Prefab> getAllPrefabs() { return prefabs; }
    public List<RuleChange> getPendingChanges() { return activePrefab.getFutureChanges(); }
}
