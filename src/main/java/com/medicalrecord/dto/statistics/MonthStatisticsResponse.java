package com.medicalrecord.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthStatisticsResponse {

    private int month;
    private int year;
    private long count;
}
