package Ariandel.carpetgui.network.server;

import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public class RuleStackSyncPayload {

    public final String activePrefabName;
    public final List<String> allPrefabNames;
    public final List<LayerInfo> layers;
    public final List<ChangeInfo> pendingChanges;
    public final List<LayerInfo> futureLayers;

    public record LayerInfo(String id, String message, long timestamp, List<ChangeInfo> changes) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeUtf(message);
            buf.writeLong(timestamp);
            buf.writeCollection(changes, (b, c) -> c.write(b));
        }
        public static LayerInfo read(FriendlyByteBuf buf) {
            return new LayerInfo(buf.readUtf(), buf.readUtf(), buf.readLong(),
                buf.readList(ChangeInfo::read));
        }
    }

    public record ChangeInfo(String ruleKey, String prevValue, boolean prevIsDefault,
                              String newValue, boolean newIsDefault) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(ruleKey);
            buf.writeUtf(prevValue);
            buf.writeBoolean(prevIsDefault);
            buf.writeUtf(newValue);
            buf.writeBoolean(newIsDefault);
        }
        public static ChangeInfo read(FriendlyByteBuf buf) {
            return new ChangeInfo(buf.readUtf(), buf.readUtf(), buf.readBoolean(),
                buf.readUtf(), buf.readBoolean());
        }
    }

    public RuleStackSyncPayload(String activePrefabName, List<String> allPrefabNames,
                                 List<LayerInfo> layers, List<ChangeInfo> pendingChanges,
                                 List<LayerInfo> futureLayers) {
        this.activePrefabName = activePrefabName;
        this.allPrefabNames = allPrefabNames;
        this.layers = layers;
        this.pendingChanges = pendingChanges;
        this.futureLayers = futureLayers;
    }

    public RuleStackSyncPayload(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readList(FriendlyByteBuf::readUtf),
            buf.readList(LayerInfo::read), buf.readList(ChangeInfo::read),
            buf.readList(LayerInfo::read));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(activePrefabName);
        buf.writeCollection(allPrefabNames, FriendlyByteBuf::writeUtf);
        buf.writeCollection(layers, (b, l) -> l.write(b));
        buf.writeCollection(pendingChanges, (b, c) -> c.write(b));
        buf.writeCollection(futureLayers, (b, l) -> l.write(b));
    }
}
