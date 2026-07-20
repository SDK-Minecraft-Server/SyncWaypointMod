# SyncWaypointMod

Smart waypoint synchronization between [Location Marker](https://mcdreforged.com/zh-CN/plugin/location_marker) and [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap).

## Overview

This mod syncs waypoints recorded by the MCDR Location Marker plugin on the server side to the client's Xaero's Minimap with a single command, eliminating the need to manually add waypoints.

## Usage

### 1. Server

The server must have [MCDR](https://mcdreforged.com/zh-CN) installed with the [Location Marker](https://mcdreforged.com/zh-CN/plugin/location_marker) plugin.

### 2. Config

Configure the path to Location Marker's waypoint storage in the server's `config/syncwaypoint.json`:

```json
{
  "file": "/mcdr/config/location_marker/locations.json"
}
```

### 3. Client

The client must have both of the following mods installed:

- **SyncWaypointMod** (this mod)
- [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap)

After joining the server, run `/syncwp` in chat to sync waypoints.

### 4. Rejoin

After syncing, **rejoin the server** to see the synced waypoints on the minimap.

## Notes

- For VC such as groupServer, the client must enable **"Multi-World Detection"** in Xaero's Minimap settings.
