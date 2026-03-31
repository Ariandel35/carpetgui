package ml.mypals.carpetgui.network.client;

import ml.mypals.carpetgui.network.PacketIDs;
import net.minecraft.network.FriendlyByteBuf;

//? if >= 1.20.5 {

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record RequestRulesPayload(String lang) implements CustomPacketPayload {

    public static final Type<RequestRulesPayload> ID = new Type<>(PacketIDs.REQUEST_RULES_ID);
    public static final StreamCodec<FriendlyByteBuf, RequestRulesPayload> CODEC = StreamCodec.ofMember(RequestRulesPayload::write, RequestRulesPayload::new);

    public RequestRulesPayload(FriendlyByteBuf buf) {
        this(buf.readUtf());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.lang);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
//?} else {
/*import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;

public record RequestRulesPayload(String lang) implements FabricPacket {

    public static final PacketType<RequestRulesPayload> ID = PacketType.create(
            PacketIDs.REQUEST_RULES_ID,
            buf -> new RequestRulesPayload(buf.readUtf())
    );

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.lang);
    }

    @Override
    public PacketType<?> getType() {
        return ID;
    }
}
*///?}