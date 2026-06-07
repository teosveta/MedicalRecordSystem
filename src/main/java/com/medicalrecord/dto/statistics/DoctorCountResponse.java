package com.medicalrecord.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DoctorCountResponse {

    private Long doctorId;
    private String doctorName;
    private long count;
}
