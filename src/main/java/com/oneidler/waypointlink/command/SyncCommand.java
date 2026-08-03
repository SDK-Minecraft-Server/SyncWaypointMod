package com.oneidler.waypointlink.command;

import com.mojang.brigadier.CommandDispatcher;
import com.oneidler.waypointlink.WaypointLinkMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class SyncCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("syncwp")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    if (source.getPlayer() == null) {
                        source.sendFailure(Component.translatable(WaypointLinkMod.MOD_ID + ".command.player"));
                        return 0;
                    }
                    source.sendSuccess(() -> Component.translatable(WaypointLinkMod.MOD_ID + ".command.sync"), false);
                    WaypointLinkMod.syncWaypointsToClient(source.getPlayer());
                    return 1;
                })
        );
    }
}