package Ariandel.carpetgui.network.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static Ariandel.carpetgui.network.PacketIDs.REQUEST_RULES;

public record RequestRulesPayload(String lang, List<String> knownRuleNames) implements CustomPacketPayload {

    public static final Type<RequestRulesPayload> TYPE = new Type<>(REQUEST_RULES);
    public static final StreamCodec<FriendlyByteBuf, RequestRulesPayload> CODEC =
        StreamCodec.ofMember(RequestRulesPayload::write, RequestRulesPayload::new);

    public RequestRulesPayload(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readList(FriendlyByteBuf::readUtf));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(lang);
        buf.writeCollection(knownRuleNames, FriendlyByteBuf::writeUtf);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
