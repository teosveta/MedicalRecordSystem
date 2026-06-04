package com.medicalrecord.enums;

public enum Specialty {
    GP("Общопрактикуващ лекар (ОПЛ)"),
    CARDIOLOGIST("Кардиолог"),
    NEUROLOGIST("Невролог"),
    DERMATOLOGIST("Дерматолог"),
    ORTHOPEDIST("Ортопед"),
    PEDIATRICIAN("Педиатър"),
    PSYCHIATRIST("Психиатър"),
    SURGEON("Хирург"),
    UROLOGIST("Уролог"),
    ONCOLOGIST("Онколог");

    private final String label;

    Specialty(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
