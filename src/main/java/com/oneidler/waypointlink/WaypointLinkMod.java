package com.oneidler.waypointlink;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.oneidler.waypointlink.command.SyncCommand;
import com.oneidler.waypointlink.config.ConfigManager;
import com.oneidler.waypointlink.network.WaypointPayload;
import com.oneidler.waypointlink.pojo.LocationMarkerWaypoint;
import com.oneidler.waypointlink.utils.CollUtil;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public class WaypointLinkMod implements ModInitializer, ClientModInitializer {

    public static final String MOD_ID = "waypointlink";

    @Override
    public void onInitialize() {
        ConfigManager.load();
        // 注册服务端→客户端数据包类型
        //#if MC < 260100
        PayloadTypeRegistry.playS2C().register(WaypointPayload.TYPE, WaypointPayload.STREAM_CODEC);
        //#else
        //$$ PayloadTypeRegistry.clientboundPlay().register(WaypointPayload.TYPE, WaypointPayload.STREAM_CODEC);
        //#endif

        // 注册命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SyncCommand.register(dispatcher);
        });
    }

    /**
     * 从 MCDR Location Marker 获取路径点列表
     */
    public static List<WaypointPayload.Waypoint> fetchWaypointsFromMCDR() {
        List<WaypointPayload.Waypoint> waypoints = Collections.emptyList();
        try {
            String filePath = ConfigManager.CONFIG.getFile();
            if (filePath == null || filePath.isEmpty()) {
                log.warn("Waypoint file path is not configured, skipping sync");
                return waypoints;
            }
            Path path = Path.of(filePath);
            if (!Files.exists(path)) {
                log.warn("Waypoint file not found: {}", filePath);
                return waypoints;
            }
            String content = Files.readString(path);
            Gson gson = new Gson();
            List<LocationMarkerWaypoint> list = gson.fromJson(content, new TypeToken<>() {

            });
            if (CollUtil.isEmpty(list)) {
                return waypoints;
            }
            return list.stream()
                    .map(e ->
                            new WaypointPayload.Waypoint(e.getName(),
                                    (int) Math.round(Optional.ofNullable(e.getPos().getX()).orElse(0d)),
                                    (int) Math.round(Optional.ofNullable(e.getPos().getY()).orElse(0d)),
                                    (int) Math.round(Optional.ofNullable(e.getPos().getZ()).orElse(0d)),
                                    Optional.ofNullable(e.getDim()).orElse(0))
                    )
                    .toList();
        } catch (Exception e) {
            log.error("Failed to get waypoint: {}", e.getMessage(), e);
        }
        return waypoints;
    }

    /**
     * 向客户端发送路径点同步数据包
     */
    public static void syncWaypointsToClient(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, WaypointPayload.TYPE)) {
            player.sendSystemMessage(
                    Component.translatable(WaypointLinkMod.MOD_ID + ".message.client_not_installed"),
                    false
            );
            return;
        }
        List<WaypointPayload.Waypoint> waypoints = fetchWaypointsFromMCDR();
        if (waypoints == null || waypoints.isEmpty()) {
            player.sendSystemMessage(Component.translatable(WaypointLinkMod.MOD_ID + ".message.no_waypoints"), false);
            return;
        }
        WaypointPayload payload = new WaypointPayload(waypoints);
        ServerPlayNetworking.send(player, payload);
        player.sendSystemMessage(Component.translatable(WaypointLinkMod.MOD_ID + ".message.synced", waypoints.size()), false);
    }

    @Override
    public void onInitializeClient() {

    }
}