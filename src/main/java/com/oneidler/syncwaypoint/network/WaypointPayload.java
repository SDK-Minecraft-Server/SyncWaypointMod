package com.oneidler.syncwaypoint.network;

import com.oneidler.syncwaypoint.SyncWaypointMod;
import lombok.Data;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record WaypointPayload(List<Waypoint> waypoints) implements CustomPacketPayload {

    // 新版
    public static final CustomPacketPayload.Type<WaypointPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SyncWaypointMod.MOD_ID, "waypoint_sync"));

    public static final StreamCodec<FriendlyByteBuf, WaypointPayload> STREAM_CODEC =
            StreamCodec.of(WaypointPayload::write, WaypointPayload::read);

    @Data
    public static class Waypoint {
        public final String name;
        public final int x, y, z;
        public final int dimension;

        public Waypoint(String name, int x, int y, int z, int dimension) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
        }
    }

    private static void write(FriendlyByteBuf buf, WaypointPayload payload) {
        List<Waypoint> list = payload.waypoints;
        buf.writeInt(list.size());
        for (Waypoint wp : list) {
            buf.writeUtf(wp.name);
            buf.writeInt(wp.x);
            buf.writeInt(wp.y);
            buf.writeInt(wp.z);
            buf.writeInt(wp.dimension);
        }
    }

    private static WaypointPayload read(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<Waypoint> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String name = buf.readUtf();
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            int dimension = buf.readInt();
            list.add(new Waypoint(name, x, y, z, dimension));
        }
        return new WaypointPayload(list);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}