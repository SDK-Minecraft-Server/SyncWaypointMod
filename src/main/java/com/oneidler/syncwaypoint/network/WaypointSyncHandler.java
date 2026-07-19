package com.oneidler.syncwaypoint.network;

import com.oneidler.syncwaypoint.utils.CollUtil;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class WaypointSyncHandler implements ClientModInitializer {
    // Xaero's Minimap 路径点颜色映射
    private static final String[] COLOR_MAP = {
            "0", "1", "2", "3", "4", "5", "6", "7",
            "8", "9", "10", "11", "12", "13", "14", "15"
    };

    @Override
    public void onInitializeClient() {
        log.info("客户端 WaypointSyncHandler 初始化开始");
        ClientPlayNetworking.registerGlobalReceiver(WaypointPayload.TYPE, (payload, context) -> {
            ClientLevel level = context.client().level;

            if (level == null) {
                return;
            }
            context.client().execute(() -> {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) {
                    return;
                }
                try {
                    writeWaypointsToXaero(payload.waypoints());
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        log.error("thread sleep interrupted: {}", e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                    //#if MC >= 260100
                    //$$ player.sendOverlayMessage(
                    //#endif
                    //#if MC <= 12101
                    player.sendSystemMessage(
                    //#endif
                    //#if MC > 12101 && MC < 260100
                    //$$ player.displayClientMessage(
                    //#endif
                            Component.literal("§a[同步] 已写入 " + payload.waypoints().size() + " 个路径点到 Xaero's Minimap，请重新进入服务器查看！")
                            //#if MC > 12101 && MC < 260100
                            //$$ , true
                            //#endif
                    );

                } catch (IOException e) {
                    log.error("写入路径点失败: {}", e.getMessage(), e);
                    //#if MC >= 260100
                    //$$ player.sendOverlayMessage(
                    //#endif
                    //#if MC <= 12101
                    player.sendSystemMessage(
                    //#endif
                    //#if MC > 12101 && MC < 260100
                    //$$ player.displayClientMessage(
                    //#endif
                            Component.literal("§c[同步] 写入路径点失败: " + e.getMessage())
                            //#if MC > 12101 && MC < 260100
                            //$$ , true
                            //#endif
                    );
                }
            });
        });
        log.info("客户端路径点接收器已注册");
    }

    /**
     * 将路径点写入 Xaero's Minimap 的 waypoints.txt
     */
    private void writeWaypointsToXaero(List<WaypointPayload.Waypoint> waypoints) throws IOException {
        Minecraft client = Minecraft.getInstance();
        File gameDir = client.gameDirectory;

        // 获取当前连接的服务器信息
        String serverName = getServerFolderName(client);
        if (serverName == null) {
            throw new IOException("无法获取当前服务器信息");
        }

        // XaeroWaypoints 根目录
        Path xaeroDir = Paths.get(gameDir.getAbsolutePath(), "xaero", "minimap");
        if (!Files.exists(xaeroDir)) {
            Files.createDirectories(xaeroDir);
        }

        Set<String> filenames = new HashSet<>(3);

        //获取三个维度下的所有路径点文件
        for (int i = -1; i < 2; i++) {
            String dimFolder = getDimensionFolder(i);
            Path dimPath = xaeroDir.resolve(serverName).resolve(dimFolder);
            if (!Files.exists(dimPath)) {
                Files.createDirectories(dimPath);
            } else {
                try (Stream<Path> stream = Files.walk(dimPath)) {
                    Set<String> dimFilenames = stream.filter(Files::isRegularFile)
                            .map(path -> path.getFileName().toString())
                            .collect(Collectors.toSet());
                    filenames.addAll(dimFilenames);
                } catch (IOException e) {
                    log.error("IOException:{}", e.getMessage(), e);
                }
            }
        }

        if (CollUtil.isEmpty(filenames)) {
            filenames = Set.of("waypoints.txt");
        }
        //正常新玩家只循环一次
        for (String filename : filenames) {
            // 按维度分组写入
            for (WaypointPayload.Waypoint wp : waypoints) {
                //正常也只会有一个文件，不会多次循环
                String dimFolder = getDimensionFolder(wp.dimension);
                Path dimPath = xaeroDir.resolve(serverName).resolve(dimFolder);
                Path waypointFile = dimPath.resolve(filename);
                writeWaypointToFile(waypointFile, wp);

            }
        }
    }

    /**
     * 写入单个路径点到文件（去重）
     */
    private void writeWaypointToFile(Path file, WaypointPayload.Waypoint wp) throws IOException {
        // 读取已有路径点，避免重复
        Set<String> existing = new HashSet<>();
        if (Files.exists(file)) {
            for (String line : readAllLinesWithFallback(file)) {
                if (line.startsWith("waypoint:")) {
                    String[] parts = line.split(":");
                    if (parts.length >= 6) {
                        // 用 名称+坐标 作为唯一标识
                        String key = parts[1] + ":" + parts[3] + ":" + parts[4] + ":" + parts[5];
                        existing.add(key);
                    }
                }
            }
        }

        String key = wp.name + ":" + wp.x + ":" + wp.y + ":" + wp.z;
        if (existing.contains(key)) {
            return; // 已存在，跳过
        }

        // Xaero 路径点格式:
        // waypoint:名称:缩写:X:Y:Z:颜色:禁用:类型:集合:旋转传送:传送偏航角:全局
        // 参考: https://github.com/TISUnion/Here/issues/29[reference:0]
        String color = COLOR_MAP[Math.abs(wp.name.hashCode()) % COLOR_MAP.length];
        String line = String.format("waypoint:%s:%s:%d:%d:%d:%s:false:0:gui.xaero_default:false:0:0:false\n",
                wp.name,
                wp.name.length() > 1 ? wp.name.substring(0, 1) : wp.name,
                wp.x, wp.y, wp.z,
                color
        );

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file.toFile(), true), StandardCharsets.UTF_8))) {
            writer.write(line);
        }
    }

    /**
     * 读取文件所有行，优先 UTF-8，失败则回退到系统默认编码
     */
    private List<String> readAllLinesWithFallback(Path file) throws IOException {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            log.warn("waypoint 文件非 UTF-8 编码, 尝试系统默认编码: {}", file);
            return Files.readAllLines(file, Charset.defaultCharset());
        }
    }

    /**
     * 获取服务器文件夹名称
     */
    private String getServerFolderName(Minecraft client) {
        if (client.getCurrentServer() == null) {
            return "Singleplayer";
        }
        String address = client.getCurrentServer().ip;
        if (address.isEmpty()) {
            return "Singleplayer";
        }
        if (address.contains(":")) {
            address = address.substring(0, address.lastIndexOf(":"));
        }
        // 替换特殊字符，与 Xaero 保持一致
        return "Multiplayer_" + address;
    }

    /**
     * 维度名 -> Xaero 文件夹名
     */
    private String getDimensionFolder(int dimension) {
        return switch (dimension) {
            //下界
            case -1 -> "dim%-1";
            //末地
            case 1 -> "dim%1";
            //主世界
            default -> "dim%0";
        };
    }
}