package com.oneidler.syncwaypoint.pojo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


@Data
public final class LocationMarkerWaypoint implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String name;

    private String desc;

    private Integer dim;

    private Position pos;

    @Data
    public static final class Position implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Double x;
        private Double y;
        private Double z;
    }
}

