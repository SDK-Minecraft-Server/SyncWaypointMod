package com.oneidler.syncwaypoint.command;

import com.mojang.brigadier.CommandDispatcher;
import com.oneidler.syncwaypoint.SyncWaypointMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class SyncCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("syncwp")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    if (source.getPlayer() == null) {
                        source.sendFailure(Component.translatable(SyncWaypointMod.MOD_ID + ".command.player"));
                        return 0;
                    }
                    source.sendSuccess(() -> Component.translatable(SyncWaypointMod.MOD_ID + ".command.sync"), false);
                    SyncWaypointMod.syncWaypointsToClient(source.getPlayer());
                    return 1;
                })
        );
    }
}