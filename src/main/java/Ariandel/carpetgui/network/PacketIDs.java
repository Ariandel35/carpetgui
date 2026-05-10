package Ariandel.carpetgui.network;

import net.minecraft.resources.ResourceLocation;

public class PacketIDs {
    public static final String MOD_ID = "carpetgui";
    public static final ResourceLocation REQUEST_RULES = new ResourceLocation(MOD_ID, "request_rules");
    public static final ResourceLocation SYNC_RULES = new ResourceLocation(MOD_ID, "sync_rules");
    public static final ResourceLocation HELLO = new ResourceLocation(MOD_ID, "hello");
    public static final ResourceLocation REQUEST_RULE_STACK = new ResourceLocation(MOD_ID, "request_rule_stack");
    public static final ResourceLocation RULE_STACK_SYNC = new ResourceLocation(MOD_ID, "rule_stack_sync");
}
