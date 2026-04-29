package yiwen.carpetgui.rulestack;

public class RuleChange {
    private final String ruleKey;
    private final RuleSnapshot previous;
    private final RuleSnapshot newSnapshot;

    public RuleChange(String ruleKey, RuleSnapshot previous, RuleSnapshot newSnapshot) {
        this.ruleKey = ruleKey;
        this.previous = previous;
        this.newSnapshot = newSnapshot;
    }

    public String ruleKey() { return ruleKey; }
    public RuleSnapshot previousSnapshot() { return previous; }
    public RuleSnapshot newSnapshot() { return newSnapshot; }

    public record RuleSnapshot(String value, boolean isDefault) {}
}
