package yiwen.carpetgui.network.server;

import yiwen.carpetgui.network.RuleData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static yiwen.carpetgui.network.PacketIDs.SYNC_RULES;

public record RulesPacketPayload(List<RuleData> rules, String defaults, boolean isPartial) implements CustomPacketPayload {

    public static final Type<RulesPacketPayload> TYPE = new Type<>(SYNC_RULES);
    public static final StreamCodec<FriendlyByteBuf, RulesPacketPayload> CODEC =
        StreamCodec.ofMember(RulesPacketPayload::write, RulesPacketPayload::new);

    public RulesPacketPayload(FriendlyByteBuf buf) {
        this(buf.readList(RuleData::new), buf.readUtf(), buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(rules, (b, v) -> v.write(b));
        buf.writeUtf(defaults);
        buf.writeBoolean(isPartial);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
