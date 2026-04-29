package Ariandel.carpetgui.rulestack;

import java.util.*;

public class RuleLayer {
    private final String id;
    private final String message;
    private final long timestamp;
    private final List<RuleChange> changes;

    public RuleLayer(String id, String message, long timestamp, List<RuleChange> changes) {
        this.id = id;
        this.message = message;
        this.timestamp = timestamp;
        this.changes = changes;
    }

    public String getId() { return id; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public List<RuleChange> getChanges() { return changes; }
}
