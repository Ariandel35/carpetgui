package Ariandel.carpetgui.network.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import static Ariandel.carpetgui.network.PacketIDs.HELLO;

public record HelloPacketPayload() implements CustomPacketPayload {

    public static final Type<HelloPacketPayload> TYPE = new Type<>(HELLO);
    public static final StreamCodec<FriendlyByteBuf, HelloPacketPayload> CODEC =
        StreamCodec.ofMember(HelloPacketPayload::write, HelloPacketPayload::new);

    public HelloPacketPayload(FriendlyByteBuf buf) { this(); }

    public void write(FriendlyByteBuf buf) {}

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
