package yiwen.carpetgui.rulestack;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import static net.minecraft.commands.Commands.*;

public class RuleStackCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rulestack")
            .requires(hasPermission(LEVEL_GAMEMASTERS))
            .then(Commands.literal("push")
                .executes(ctx -> {
                    // TODO: implement push
                    return 1;
                }))
            .then(Commands.literal("pop")
                .executes(ctx -> {
                    // TODO: implement pop
                    return 1;
                }))
            .then(Commands.literal("list")
                .executes(ctx -> {
                    // TODO: implement list
                    return 1;
                })));
    }
}
