package com.medicalrecord.dto.statistics;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonthStatisticsResponse {

    private int month;
    private int year;
    private long count;
}
