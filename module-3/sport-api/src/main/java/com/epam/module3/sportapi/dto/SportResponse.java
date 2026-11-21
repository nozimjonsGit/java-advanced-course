package com.epam.module3.sportapi.dto;

import com.epam.module3.sportapi.entity.Sport;

public record SportResponse(int id, String name) {

    public static SportResponse from(Sport sport) {
        return new SportResponse(sport.id(), sport.name());
    }
}
