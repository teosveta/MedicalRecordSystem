package com.medicalrecord.dto.examination;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientNoteRequest {

    @Size(max = 500, message = "Бележката не може да надвишава 500 символа")
    private String note;
}
