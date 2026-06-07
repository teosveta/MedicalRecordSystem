package com.medicalrecord;

import com.medicalrecord.entity.*;
import com.medicalrecord.enums.Role;
import com.medicalrecord.enums.Specialty;
import com.medicalrecord.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Инициализира базата с начални данни при стартиране на приложението
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final ExaminationRepository examinationRepository;
    private final SickLeaveRepository sickLeaveRepository;
    private final ExaminationFeeRepository examinationFeeRepository;
    private final AdditionalServiceRepository additionalServiceRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           DoctorRepository doctorRepository,
                           PatientRepository patientRepository,
                           DiagnosisRepository diagnosisRepository,
                           ExaminationRepository examinationRepository,
                           SickLeaveRepository sickLeaveRepository,
                           ExaminationFeeRepository examinationFeeRepository,
                           AdditionalServiceRepository additionalServiceRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.examinationRepository = examinationRepository;
        this.sickLeaveRepository = sickLeaveRepository;
        this.examinationFeeRepository = examinationFeeRepository;
        this.additionalServiceRepository = additionalServiceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Вмъкваме началните данни само ако базата е празна (ddl-auto=update)
        if (userRepository.count() == 0) {
            insertSeedData();
        }
        // Специалностите за диагнозите се сийдват при всяко стартиране ако липсват
        seedDiagnosisSpecialties();
        // Специалностите за сийд-лекарите се поправят при всяко стартиране ако са празни
        seedDoctorSpecialties();
        // Допълнителните услуги се сийдват при всяко стартиране ако липсват
        seedAdditionalServices();
    }

    private void seedAdditionalServices() {
        if (additionalServiceRepository.count() > 0) return;
        additionalServiceRepository.save(AdditionalService.builder()
                .name("Вземане на кръвна проба").fee(new BigDecimal("15.00")).build());
        additionalServiceRepository.save(AdditionalService.builder()
                .name("Изпращане на материали за анализ").fee(new BigDecimal("20.00")).build());
        additionalServiceRepository.save(AdditionalService.builder()
                .name("ЕКГ").fee(new BigDecimal("25.00")).build());
        additionalServiceRepository.save(AdditionalService.builder()
                .name("Превръзка / манипулация").fee(new BigDecimal("10.00")).build());
    }

    private void seedDiagnosisSpecialties() {
        assignSpecialties("Z00", Specialty.values());
        assignSpecialties("J06", Specialty.GP, Specialty.PEDIATRICIAN);
        assignSpecialties("I10", Specialty.GP, Specialty.CARDIOLOGIST);
        assignSpecialties("E11", Specialty.GP, Specialty.CARDIOLOGIST);
        assignSpecialties("M54", Specialty.GP, Specialty.ORTHOPEDIST, Specialty.NEUROLOGIST);
        assignSpecialties("J45", Specialty.GP, Specialty.CARDIOLOGIST, Specialty.PEDIATRICIAN);
        assignSpecialties("K29", Specialty.GP, Specialty.SURGEON);
        assignSpecialties("F32", Specialty.PSYCHIATRIST, Specialty.GP);
        assignSpecialties("N30", Specialty.GP, Specialty.UROLOGIST);
        assignSpecialties("A09", Specialty.GP, Specialty.PEDIATRICIAN);
    }

    private void assignSpecialties(String code, Specialty... specialties) {
        diagnosisRepository.findByCode(code).ifPresent(d -> {
            if (d.getSpecialties().addAll(Arrays.asList(specialties))) {
                diagnosisRepository.save(d);
            }
        });
    }

    // Поправяме сийд-лекарите, чиято specialty е изгубена след миграцията към @ElementCollection
    private void seedDoctorSpecialties() {
        fixDoctorSpecialties("d.petrov@medical.com",   Specialty.GP);
        fixDoctorSpecialties("d.ivanova@medical.com",  Specialty.CARDIOLOGIST);
        fixDoctorSpecialties("d.georgiev@medical.com", Specialty.NEUROLOGIST);
    }

    private void fixDoctorSpecialties(String username, Specialty... specialties) {
        doctorRepository.findByUser_Username(username).ifPresent(d -> {
            if (d.getSpecialties().addAll(Arrays.asList(specialties))) {
                doctorRepository.save(d);
            }
        });
    }

    private void insertSeedData() {

        // =============================================
        // 1. Администратор
        // =============================================
        User adminUser = User.builder()
                .username("admin@medical.com")
                .password(passwordEncoder.encode("Az1234!"))
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(adminUser);

        // =============================================
        // 2. Потребители на лекарите
        // =============================================
        User userPetrov = User.builder()
                .username("d.petrov@medical.com")
                .password(passwordEncoder.encode("Az1234!"))
                .role(Role.DOCTOR)
                .enabled(true)
                .build();
        User userIvanova = User.builder()
                .username("d.ivanova@medical.com")
                .password(passwordEncoder.encode("Az1234!"))
                .role(Role.DOCTOR)
                .enabled(true)
                .build();
        User userGeorgiev = User.builder()
                .username("d.georgiev@medical.com")
                .password(passwordEncoder.encode("Az1234!"))
                .role(Role.DOCTOR)
                .enabled(true)
                .build();
        userRepository.save(userPetrov);
        userRepository.save(userIvanova);
        userRepository.save(userGeorgiev);

        // =============================================
        // 3. Лекари
        // =============================================
        Doctor doctorPetrov = Doctor.builder()
                .uniqueIdentificationNumber("1000000001")
                .firstName("Димитър")
                .lastName("Петров")
                .specialties(new HashSet<>(Set.of(Specialty.GP)))
                .canBeGP(true)
                .user(userPetrov)
                .build();
        Doctor doctorIvanova = Doctor.builder()
                .uniqueIdentificationNumber("1000000002")
                .firstName("Мария")
                .lastName("Иванова")
                .specialties(new HashSet<>(Set.of(Specialty.CARDIOLOGIST)))
                .canBeGP(true)
                .user(userIvanova)
                .build();
        Doctor doctorGeorgiev = Doctor.builder()
                .uniqueIdentificationNumber("1000000003")
                .firstName("Георги")
                .lastName("Георгиев")
                .specialties(new HashSet<>(Set.of(Specialty.NEUROLOGIST)))
                .canBeGP(false)
                .user(userGeorgiev)
                .build();
        doctorRepository.save(doctorPetrov);
        doctorRepository.save(doctorIvanova);
        doctorRepository.save(doctorGeorgiev);

        // =============================================
        // 4. Потребители на пациентите
        // =============================================
        User userKolev = User.builder()
                .username("p.kolev@medical.com")
                .password(passwordEncoder.encode("Az1234!"))
                .role(Role.PATIENT)
                .enabled(true)
                .build();
        User userTodorova = User.builder()
                .username("p.todorova@medical.com")
                .password(passwordEncoder.encode("Az1234!"))
                .role(Role.PATIENT)
                .enabled(true)
                .build();
        User userStoyanov = User.builder()
                .username("p.stoyanov@medical.com")
                .password(passwordEncoder.encode("Az1234!"))
                .role(Role.PATIENT)
                .enabled(true)
                .build();
        User userDimitrova = User.builder()
                .username("p.dimitrova@medical.com")
                .password(passwordEncoder.encode("Az1234!"))
                .role(Role.PATIENT)
                .enabled(true)
                .build();
        User userMarinov = User.builder()
                .username("p.marinov@medical.com")
                .password(passwordEncoder.encode("Az1234!"))
                .role(Role.PATIENT)
                .enabled(true)
                .build();
        userRepository.save(userKolev);
        userRepository.save(userTodorova);
        userRepository.save(userStoyanov);
        userRepository.save(userDimitrova);
        userRepository.save(userMarinov);

        // =============================================
        // 5. Пациенти
        // =============================================
        Patient patientKolev = Patient.builder()
                .firstName("Иван")
                .lastName("Колев")
                .egn("8501011234")
                .personalDoctor(doctorPetrov)
                .healthInsured(true)
                .user(userKolev)
                .build();
        Patient patientTodorova = Patient.builder()
                .firstName("Елена")
                .lastName("Тодорова")
                .egn("9203044567")
                .personalDoctor(doctorPetrov)
                .healthInsured(false)
                .user(userTodorova)
                .build();
        Patient patientStoyanov = Patient.builder()
                .firstName("Петър")
                .lastName("Стоянов")
                .egn("7809128901")
                .personalDoctor(doctorPetrov)
                .healthInsured(true)
                .user(userStoyanov)
                .build();
        Patient patientDimitrova = Patient.builder()
                .firstName("Надя")
                .lastName("Димитрова")
                .egn("8506152345")
                .personalDoctor(doctorIvanova)
                .healthInsured(true)
                .user(userDimitrova)
                .build();
        Patient patientMarinov = Patient.builder()
                .firstName("Александър")
                .lastName("Маринов")
                .egn("9312041234")
                .personalDoctor(doctorIvanova)
                .healthInsured(false)
                .user(userMarinov)
                .build();
        patientRepository.save(patientKolev);
        patientRepository.save(patientTodorova);
        patientRepository.save(patientStoyanov);
        patientRepository.save(patientDimitrova);
        patientRepository.save(patientMarinov);

        // =============================================
        // 6. Диагнози (МКБ-10)
        // =============================================
        Diagnosis diagZ00 = Diagnosis.builder()
                .code("Z00").name("Здрав / Без диагноза")
                .description("Преглед на здрав индивид без установена диагноза").build();
        Diagnosis diagJ06 = Diagnosis.builder()
                .code("J06").name("Остра инфекция на горните дихателни пътища")
                .description("Инфекция на горните дихателни пътища с неуточнена локализация").build();
        Diagnosis diagI10 = Diagnosis.builder()
                .code("I10").name("Есенциална хипертония")
                .description("Повишено артериално налягане без установена причина").build();
        Diagnosis diagE11 = Diagnosis.builder()
                .code("E11").name("Захарен диабет тип 2")
                .description("Диабет с инсулинова резистентност и нарушен глюкозен толеранс").build();
        Diagnosis diagM54 = Diagnosis.builder()
                .code("M54").name("Болки в гърба")
                .description("Болкови синдроми в областта на гърба с различна локализация").build();
        Diagnosis diagJ45 = Diagnosis.builder()
                .code("J45").name("Астма")
                .description("Хронично възпалително заболяване на дихателните пътища").build();
        Diagnosis diagK29 = Diagnosis.builder()
                .code("K29").name("Гастрит")
                .description("Възпаление на стомашната лигавица").build();
        Diagnosis diagF32 = Diagnosis.builder()
                .code("F32").name("Депресивен епизод")
                .description("Епизод на депресия с различна степен на тежест").build();
        Diagnosis diagN30 = Diagnosis.builder()
                .code("N30").name("Цистит")
                .description("Възпаление на пикочния мехур").build();
        Diagnosis diagA09 = Diagnosis.builder()
                .code("A09").name("Диария и гастроентерит")
                .description("Функционален дефект на стомашно-чревния тракт с диария").build();
        diagnosisRepository.save(diagZ00);
        diagnosisRepository.save(diagJ06);
        diagnosisRepository.save(diagI10);
        diagnosisRepository.save(diagE11);
        diagnosisRepository.save(diagM54);
        diagnosisRepository.save(diagJ45);
        diagnosisRepository.save(diagK29);
        diagnosisRepository.save(diagF32);
        diagnosisRepository.save(diagN30);
        diagnosisRepository.save(diagA09);

        // =============================================
        // 7. Прегледи
        // =============================================
        // Иван Колев е осигурен — не плаща
        Examination exam1 = Examination.builder()
                .examinationDate(LocalDate.of(2025, 1, 15))
                .doctor(doctorPetrov)
                .patient(patientKolev)
                .diagnosis(diagJ06)
                .treatment("Предписан Amoxicillin 500мг за 5 дни, обилно пиене на течности и почивка")
                .price(new BigDecimal("20.00"))
                .paidByPatient(!patientKolev.isHealthInsured())
                .build();

        // Елена Тодорова е неосигурена — плаща
        Examination exam2 = Examination.builder()
                .examinationDate(LocalDate.of(2025, 2, 20))
                .doctor(doctorPetrov)
                .patient(patientTodorova)
                .diagnosis(diagI10)
                .treatment("Предписан Amlodipine 5мг дневно, диета с намалено съдържание на сол")
                .price(new BigDecimal("30.00"))
                .paidByPatient(!patientTodorova.isHealthInsured())
                .build();

        // Петър Стоянов е осигурен — не плаща
        Examination exam3 = Examination.builder()
                .examinationDate(LocalDate.of(2025, 3, 10))
                .doctor(doctorIvanova)
                .patient(patientStoyanov)
                .diagnosis(diagE11)
                .treatment("Предписан Metformin 500мг два пъти дневно, контрол на кръвната захар")
                .price(new BigDecimal("25.00"))
                .paidByPatient(!patientStoyanov.isHealthInsured())
                .build();

        // Надя Димитрова е осигурена — не плаща
        Examination exam4 = Examination.builder()
                .examinationDate(LocalDate.of(2025, 4, 5))
                .doctor(doctorGeorgiev)
                .patient(patientDimitrova)
                .diagnosis(diagF32)
                .treatment("Препоръчана психотерапия, предписан Sertraline 50мг, контрол след 4 седмици")
                .price(new BigDecimal("40.00"))
                .paidByPatient(!patientDimitrova.isHealthInsured())
                .build();

        examinationRepository.save(exam1);
        examinationRepository.save(exam2);
        examinationRepository.save(exam3);
        examinationRepository.save(exam4);

        // =============================================
        // 8. Болнични листове
        // =============================================
        SickLeave sickLeave1 = SickLeave.builder()
                .examination(exam1)
                .startDate(LocalDate.of(2025, 1, 16))
                .numberOfDays(5)
                .doctor(exam1.getDoctor())
                .patient(exam1.getPatient())
                .build();
        SickLeave sickLeave2 = SickLeave.builder()
                .examination(exam2)
                .startDate(LocalDate.of(2025, 2, 21))
                .numberOfDays(3)
                .doctor(exam2.getDoctor())
                .patient(exam2.getPatient())
                .build();
        sickLeaveRepository.save(sickLeave1);
        sickLeaveRepository.save(sickLeave2);

        // =============================================
        // 9. Такси за прегледи по специалност
        // =============================================
        Map<Specialty, BigDecimal> fees = Map.of(
                Specialty.GP,             new BigDecimal("20.00"),
                Specialty.CARDIOLOGIST,   new BigDecimal("60.00"),
                Specialty.NEUROLOGIST,    new BigDecimal("70.00"),
                Specialty.DERMATOLOGIST,  new BigDecimal("50.00"),
                Specialty.ORTHOPEDIST,    new BigDecimal("60.00"),
                Specialty.PEDIATRICIAN,   new BigDecimal("30.00"),
                Specialty.PSYCHIATRIST,   new BigDecimal("80.00"),
                Specialty.SURGEON,        new BigDecimal("100.00"),
                Specialty.UROLOGIST,      new BigDecimal("65.00"),
                Specialty.ONCOLOGIST,     new BigDecimal("90.00")
        );
        fees.forEach((specialty, baseFee) -> {
            ExaminationFee fee = ExaminationFee.builder()
                    .specialty(specialty)
                    .baseFee(baseFee)
                    .build();
            examinationFeeRepository.save(fee);
        });
    }
}
