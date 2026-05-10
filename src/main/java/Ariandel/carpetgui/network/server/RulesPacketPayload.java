package Ariandel.carpetgui.network.server;

import Ariandel.carpetgui.network.RuleData;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public class RulesPacketPayload {
    public final List<RuleData> rules;
    public final String defaults;
    public final boolean isPartial;

    public RulesPacketPayload(List<RuleData> rules, String defaults, boolean isPartial) {
        this.rules = rules;
        this.defaults = defaults;
        this.isPartial = isPartial;
    }

    public RulesPacketPayload(FriendlyByteBuf buf) {
        this(buf.readList(RuleData::new), buf.readUtf(), buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(rules, (b, v) -> v.write(b));
        buf.writeUtf(defaults);
        buf.writeBoolean(isPartial);
    }
}
