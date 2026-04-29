package Ariandel.carpetgui.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.List;
import java.util.Map;

/**
 * Rule data transferred between server and client.
 * Protocol-compatible with original CarpetGUI.
 */
public class RuleData {
    public String manager;
    public String name;
    public String localName;
    public String defaultValue;
    public String value;
    public String description;
    public String localDescription;
    public Class<?> type;
    public List<String> suggestions;
    public List<Map.Entry<String, String>> categories;
    public boolean isGamerule;

    public RuleData() {
        this.manager = "";
        this.name = "";
        this.localName = "";
        this.defaultValue = "";
        this.value = "";
        this.description = "";
        this.localDescription = "";
        this.type = String.class;
        this.suggestions = List.of();
        this.categories = List.of();
    }

    public RuleData(String manager, String name, String localName, Class<?> type,
                    String defaultValue, String value, String description,
                    String localDescription, List<String> suggestions,
                    List<Map.Entry<String, String>> categories) {
        this.manager = manager;
        this.name = name;
        this.localName = localName;
        this.type = type;
        this.defaultValue = defaultValue;
        this.value = value;
        this.description = description;
        this.localDescription = localDescription;
        this.suggestions = suggestions;
        this.categories = categories;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(manager);
        buf.writeUtf(name);
        buf.writeUtf(localName);
        buf.writeUtf(type.getName());
        buf.writeUtf(defaultValue);
        buf.writeUtf(value);
        buf.writeUtf(description);
        buf.writeUtf(localDescription);
        buf.writeCollection(suggestions, FriendlyByteBuf::writeUtf);
        buf.writeCollection(categories, (bf, entry) -> {
            bf.writeUtf(entry.getKey());
            bf.writeUtf(entry.getValue());
        });
    }

    public RuleData(FriendlyByteBuf buf) {
        this(
            buf.readUtf(),
            buf.readUtf(),
            buf.readUtf(),
            getRuleType(buf.readUtf()),
            buf.readUtf(),
            buf.readUtf(),
            buf.readUtf(),
            buf.readUtf(),
            buf.readList(FriendlyByteBuf::readUtf),
            buf.readList(bf -> Map.entry(bf.readUtf(), bf.readUtf()))
        );
    }

    public static Class<?> getRuleType(String name) {
        return switch (name) {
            case "Boolean", "java.lang.Boolean", "boolean" -> Boolean.class;
            case "Integer", "java.lang.Integer", "int" -> Integer.class;
            case "Float", "java.lang.Float", "float" -> Float.class;
            case "Enum", "java.lang.Enum" -> Enum.class;
            default -> String.class;
        };
    }
}
