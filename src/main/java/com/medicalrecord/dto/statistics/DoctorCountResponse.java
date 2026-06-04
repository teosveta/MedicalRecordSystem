package com.medicalrecord.dto.statistics;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoctorCountResponse {

    private Long doctorId;
    private String doctorName;
    private long count;
}
