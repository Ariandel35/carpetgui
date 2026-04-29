package Ariandel.carpetgui.network.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import static Ariandel.carpetgui.network.PacketIDs.REQUEST_RULE_STACK;

public record RequestRuleStackPayload() implements CustomPacketPayload {

    public static final Type<RequestRuleStackPayload> TYPE = new Type<>(REQUEST_RULE_STACK);
    public static final StreamCodec<FriendlyByteBuf, RequestRuleStackPayload> CODEC =
        StreamCodec.ofMember(RequestRuleStackPayload::write, RequestRuleStackPayload::new);

    public RequestRuleStackPayload(FriendlyByteBuf buf) {
        this();
    }

    public void write(FriendlyByteBuf buf) {}

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
