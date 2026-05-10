package Ariandel.carpetgui.mixin;

import carpet.network.ClientNetworkHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import Ariandel.carpetgui.CarpetGUIRewriteClient;
import Ariandel.carpetgui.network.RuleData;

@Mixin(value = ClientNetworkHandler.class, remap = false)
public class CarpetClientNetworkMixin {

    @WrapOperation(method = "lambda$static$1",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/nbt/CompoundTag;get(Ljava/lang/String;)Lnet/minecraft/nbt/Tag;"))
    private static Tag onRuleData(CompoundTag instance, String key, Operation<Tag> original) {
        CompoundTag ruleNbt = (CompoundTag) original.call(instance, key);
        if (ruleNbt.contains("Manager")) {
            RuleData rd = new RuleData();
            String manager = ruleNbt.getString("Manager");
            rd.manager = manager != null ? manager : "";
            String value = ruleNbt.getString("Value");
            rd.value = value != null ? value : "";
            String name = ruleNbt.getString("Rule");
            rd.name = name != null ? name : "";
            CarpetGUIRewriteClient.incompleteServerRules.add(rd);
        }
        return ruleNbt;
    }
}
