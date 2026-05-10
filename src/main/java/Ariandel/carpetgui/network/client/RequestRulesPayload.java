package Ariandel.carpetgui.network.client;

import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public class RequestRulesPayload {
    public final String lang;
    public final List<String> knownRuleNames;

    public RequestRulesPayload(String lang, List<String> knownRuleNames) {
        this.lang = lang;
        this.knownRuleNames = knownRuleNames;
    }

    public RequestRulesPayload(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readList(FriendlyByteBuf::readUtf));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(lang);
        buf.writeCollection(knownRuleNames, FriendlyByteBuf::writeUtf);
    }
}
