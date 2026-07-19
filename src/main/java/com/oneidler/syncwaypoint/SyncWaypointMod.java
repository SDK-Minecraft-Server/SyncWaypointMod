package com.oneidler.syncwaypoint;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.oneidler.syncwaypoint.command.SyncCommand;
import com.oneidler.syncwaypoint.network.WaypointPayload;
import com.oneidler.syncwaypoint.pojo.LocationMarkerWaypoint;
import com.oneidler.syncwaypoint.utils.CollUtil;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public class SyncWaypointMod implements ModInitializer, ClientModInitializer {

    public static final String MOD_ID = "syncwaypoint";

    @Override
    public void onInitialize() {
        log.info("SyncWaypointMod 初始化 (服务端)");

        // 注册服务端→客户端数据包类型
        PayloadTypeRegistry.clientboundPlay().register(WaypointPayload.TYPE, WaypointPayload.STREAM_CODEC);
        // 注册命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SyncCommand.register(dispatcher);
        });

        log.info("使用 /syncwp 手动同步路径点");
    }

    @Override
    public void onInitializeClient() {
        // 客户端数据包接收器已在 WaypointSyncHandler 中注册
        log.info("SyncWaypointMod 初始化 (客户端)");
    }

    /**
     * 从 MCDR Location Marker 获取路径点列表
     */
    public static List<WaypointPayload.Waypoint> fetchWaypointsFromMCDR() {
        List<WaypointPayload.Waypoint> waypoints = Collections.emptyList();
        try {
            // 获取 Fabric 的标准配置目录 (即服务端的 config 文件夹)
            Path configDir = FabricLoader.getInstance().getConfigDir();

            // 模组自己的配置文件路径：config/syncwaypoint/locations.json
            Path waypointFile = configDir.resolve("syncwaypoint").resolve("locations.json");

            // 检查文件是否存在
            if (!Files.exists(waypointFile)) {
                log.warn("找不到路径点文件: {}", waypointFile);
                log.info("请创建符号链接: config/syncwaypoint/locations.json -> MCDR的config/location_marker/locations.json");
                return waypoints;
            }

            String content = new String(Files.readAllBytes(waypointFile));

            // 2. 解析 JSON 文件
            // locations.json 的格式是一个 JSON 数组
            Gson gson = new Gson();
            List<LocationMarkerWaypoint> list = gson.fromJson(content, new TypeToken<>() {

            });
            if (CollUtil.isEmpty(list)) {
                return waypoints;
            }
            log.info("从 Location Marker 读取到 {} 个路径点", list.size());
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
            log.error("获取 Location Marker 路径点失败: {}", e.getMessage(), e);
        }
        return waypoints;
    }

    /**
     * 向客户端发送路径点同步数据包
     */
    public static void syncWaypointsToClient(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, WaypointPayload.TYPE)) {
            player.sendSystemMessage(
                    Component.literal("§e[同步] 你的客户端未安装 SyncWaypointMod，无法同步路径点"),
                    false
            );
            return;
        }
        List<WaypointPayload.Waypoint> waypoints = fetchWaypointsFromMCDR();
        if (waypoints == null || waypoints.isEmpty()) {
            player.sendSystemMessage(Component.literal("§e[同步] 没有从 MCDR 获取到路径点"), false);
            return;
        }
        WaypointPayload payload = new WaypointPayload(waypoints);
        ServerPlayNetworking.send(player, payload);
        player.sendSystemMessage(Component.literal("§a[同步] 已发送 " + waypoints.size() + " 个路径点到客户端"), false);
    }
}