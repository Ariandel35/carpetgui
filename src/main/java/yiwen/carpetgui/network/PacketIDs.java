package yiwen.carpetgui.network;

import net.minecraft.resources.Identifier;

public class PacketIDs {
    public static final String MOD_ID = "carpetgui";
    public static final Identifier REQUEST_RULES = Identifier.fromNamespaceAndPath(MOD_ID, "request_rules");
    public static final Identifier SYNC_RULES = Identifier.fromNamespaceAndPath(MOD_ID, "sync_rules");
    public static final Identifier HELLO = Identifier.fromNamespaceAndPath(MOD_ID, "hello");
    public static final Identifier REQUEST_RULE_STACK = Identifier.fromNamespaceAndPath(MOD_ID, "request_rule_stack");
    public static final Identifier RULE_STACK_SYNC = Identifier.fromNamespaceAndPath(MOD_ID, "rule_stack_sync");
}
