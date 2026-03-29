package ml.mypals.carpetgui.network.server;

import ml.mypals.carpetgui.network.PacketIDs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record RuleStackSyncPayload(
        String activePrefabName,
        List<String> allPrefabNames,
        List<RuleStackSyncPayload.LayerInfo> layers,
        List<RuleStackSyncPayload.ChangeInfo> pendingChanges
) implements CustomPacketPayload {

    public static final Type<RuleStackSyncPayload> ID =
            new Type<>(PacketIDs.RULE_STACK_SYNC_ID);

    public static final StreamCodec<FriendlyByteBuf, RuleStackSyncPayload> CODEC =
            StreamCodec.ofMember(RuleStackSyncPayload::write, RuleStackSyncPayload::new);

    public RuleStackSyncPayload(FriendlyByteBuf buf) {
        this(
                buf.readUtf(),
                buf.readList(FriendlyByteBuf::readUtf),
                buf.readList(LayerInfo::read),
                buf.readList(ChangeInfo::read)
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(activePrefabName);
        buf.writeCollection(allPrefabNames, FriendlyByteBuf::writeUtf);
        buf.writeCollection(layers, (b, l) -> l.write(b));
        buf.writeCollection(pendingChanges, (b, c) -> c.write(b));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public record ChangeInfo(
            String ruleKey,
            String prevValue,  boolean prevIsDefault,
            String newValue,   boolean newIsDefault
    ) {
        public String ruleName() {
            int i = ruleKey.indexOf(':');
            return i >= 0 ? ruleKey.substring(i + 1) : ruleKey;
        }

        public String managerId() {
            int i = ruleKey.indexOf(':');
            return i >= 0 ? ruleKey.substring(0, i) : ruleKey;
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(ruleKey);
            buf.writeUtf(prevValue);
            buf.writeBoolean(prevIsDefault);
            buf.writeUtf(newValue);
            buf.writeBoolean(newIsDefault);
        }

        public static ChangeInfo read(FriendlyByteBuf buf) {
            return new ChangeInfo(
                    buf.readUtf(),
                    buf.readUtf(), buf.readBoolean(),
                    buf.readUtf(), buf.readBoolean()
            );
        }
    }

    public record LayerInfo(int id, String message, long timestamp, List<ChangeInfo> changes) {

        public void write(FriendlyByteBuf buf) {
            buf.writeInt(id);
            buf.writeUtf(message);
            buf.writeLong(timestamp);
            buf.writeCollection(changes, (b, c) -> c.write(b));
        }

        public static LayerInfo read(FriendlyByteBuf buf) {
            return new LayerInfo(
                    buf.readInt(),
                    buf.readUtf(),
                    buf.readLong(),
                    buf.readList(ChangeInfo::read)
            );
        }
    }
}