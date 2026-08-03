# Waypoint Link

智能同步 [Location Marker](https://mcdreforged.com/zh-CN/plugin/location_marker) 与 [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) 的路径点。

## 简介

本模组用于将服务端 MCDR Location Marker 插件记录的路径点，一键同步到客户端的 Xaero's Minimap 小地图中，免去手动添加路径点的繁琐操作。

## 使用方法

### 1. 服务端

服务端需安装 [MCDR](https://mcdreforged.com/zh-CN) 并添加 [Location Marker](https://mcdreforged.com/zh-CN/plugin/location_marker) 插件。

### 2. 配置文件

在服务端 `config/waypointlink.json` 中配置 Location Marker 的路径点存储路径，例如：

```json
{
  "file": "/mcdr/config/location_marker/locations.json"
}
```

### 3. 客户端

客户端需同时安装以下两个模组：

- **Waypoint Link**（本模组）
- [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap)

进入服务器后，在聊天栏输入 `/syncwp` 即可同步路径点。

### 4. 重进生效

同步完成后，**重新进入服务器**即可在小地图中看到同步的路径点。

## 注意事项

- 对于 VC 等群组服务器，客户端需在 Xaero's Minimap 设置中开启 **"多世界检测"** 功能。
