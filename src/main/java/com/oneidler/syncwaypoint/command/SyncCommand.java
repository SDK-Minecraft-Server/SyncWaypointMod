package com.oneidler.syncwaypoint.command;

import com.mojang.brigadier.CommandDispatcher;
import com.oneidler.syncwaypoint.SyncWaypointMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;


public class SyncCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("syncwp")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.ALL))) // 所有玩家可用
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    if (source.getPlayer() == null) {
                        source.sendFailure(Component.literal("§c此命令只能由玩家执行"));
                        return 0;
                    }
                    source.sendSuccess(() -> Component.literal("§e[同步] 正在从 MCDR 获取路径点..."), false);
                    SyncWaypointMod.syncWaypointsToClient(source.getPlayer());
                    return 1;
                })
        );
    }
}