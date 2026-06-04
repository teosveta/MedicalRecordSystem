# Медицинска Информационна Система

REST API за управление на медицински записи — лекари, пациенти, прегледи, диагнози и болнични листове. Изградена с Java 21, Spring Boot 3 и JWT автентикация.

---

## Технологии

- Java 21
- Spring Boot 3.3.5
- Spring Security (JWT чрез jjwt 0.12.3)
- Spring Data JPA (Hibernate)
- MySQL 8.x
- Maven
- Lombok

---

## Изисквания

- Java 21+
- MySQL 8.x
- Maven 3.8+

---

## Настройка на база данни

```sql
CREATE DATABASE medical_record_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'med_app_user'@'localhost' IDENTIFIED BY 'MedProject123!';
GRANT ALL PRIVILEGES ON medical_record_db.* TO 'med_app_user'@'localhost';
FLUSH PRIVILEGES;
```

---

## Стартиране

```bash
mvn spring-boot:run
```

> **Внимание:** При всяко стартиране схемата на базата данни се пресъздава (`ddl-auto=create`) и началните данни се вмъкват наново чрез `DataInitializer`.

---

## Потребители по подразбиране

Всички seed акаунти използват парола: **Az1234!**

| Имейл | Роля | Бележка |
|---|---|---|
| admin@medical.com | ADMIN | |
| d.petrov@medical.com | DOCTOR | Д-р Димитър Петров, ОПЛ, canBeGP=true |
| d.ivanova@medical.com | DOCTOR | Д-р Мария Иванова, Кардиолог, canBeGP=true |
| d.georgiev@medical.com | DOCTOR | Д-р Георги Георгиев, Невролог, canBeGP=true |
| p.kolev@medical.com | PATIENT | Иван Колев, осигурен |
| p.todorova@medical.com | PATIENT | Елена Тодорова, неосигурена |
| p.stoyanov@medical.com | PATIENT | Петър Стоянов, осигурен |
| p.dimitrova@medical.com | PATIENT | Надя Димитрова, осигурена |
| p.marinov@medical.com | PATIENT | Александър Маринов, неосигурен |

---

## Автентикация

### Вход

```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin@medical.com",
  "password": "Admin123!"
}
```

**Отговор:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin@medical.com",
  "role": "ADMIN"
}
```

### Използване на токена

```bash
curl http://localhost:8080/api/admin/doctors \
  -H "Authorization: Bearer <token>"
```

---

## API Ендпойнти

### Автентикация
| Метод | URL | Достъп |
|---|---|---|
| POST | `/api/auth/login` | Публичен |
| POST | `/api/auth/register` | Публичен — регистрация на нов пациент |

### Лекари (публични и административни)
| Метод | URL | Описание |
|---|---|---|
| GET | `/api/doctors/gp` | Публичен — лекари с canBeGP=true (за регистрация) |
| GET | `/api/doctors/me` | DOCTOR — профил на текущия лекар |
| PUT | `/api/doctors/change-password` | DOCTOR — смяна на парола |
| GET | `/api/admin/doctors` | ADMIN — всички лекари |
| GET | `/api/admin/doctors/specialties` | ADMIN — всички специалности |
| POST | `/api/admin/doctors` | ADMIN — добавяне на лекар |
| DELETE | `/api/admin/doctors/{id}` | ADMIN — изтриване на лекар |

### Пациенти (Администратор)
| Метод | URL | Описание |
|---|---|---|
| GET | `/api/admin/patients` | Всички пациенти |
| POST | `/api/admin/patients` | Добавяне на пациент |
| PUT | `/api/admin/patients/{id}` | Редактиране на пациент |
| DELETE | `/api/admin/patients/{id}` | Изтриване на пациент |
| PUT | `/api/admin/patients/{id}/assign-doctor/{doctorId}` | Назначаване на личен лекар |

### Диагнози
| Метод | URL | Достъп |
|---|---|---|
| GET | `/api/diagnoses` | Всички роли |
| POST | `/api/admin/diagnoses` | ADMIN |
| PUT | `/api/admin/diagnoses/{id}` | ADMIN |
| DELETE | `/api/admin/diagnoses/{id}` | ADMIN |

### Прегледи
| Метод | URL | Описание |
|---|---|---|
| GET | `/api/examinations` | ADMIN/DOCTOR: всички; PATIENT: само свои |
| POST | `/api/examinations` | DOCTOR — лекарят се взима от JWT |
| PUT | `/api/examinations/{id}` | DOCTOR: само свои; ADMIN: всички |
| DELETE | `/api/examinations/{id}` | DOCTOR: само свои; ADMIN: всички |

### Болнични листове
| Метод | URL | Описание |
|---|---|---|
| GET | `/api/sick-leaves` | ADMIN/DOCTOR: всички; PATIENT: само свои |
| POST | `/api/sick-leaves` | DOCTOR — подава examinationId |
| DELETE | `/api/sick-leaves/{id}` | ADMIN |

### История на пациент
| Метод | URL | Описание |
|---|---|---|
| GET | `/api/patient/history` | PATIENT — собствена история |

### Статистики (ADMIN и DOCTOR)
| Метод | URL | Описание |
|---|---|---|
| GET | `/api/statistics/patients-by-diagnosis` | Пациенти по диагноза |
| GET | `/api/statistics/most-common-diagnosis` | Най-честа диагноза |
| GET | `/api/statistics/patients-by-doctor` | Пациенти по лекар |
| GET | `/api/statistics/total-patient-payments` | Общи плащания от пациенти |
| GET | `/api/statistics/patient-payments-by-doctor` | Плащания по лекар |
| GET | `/api/statistics/patients-count-per-gp` | Брой пациенти на ОПЛ |
| GET | `/api/statistics/visits-per-doctor` | Посещения по лекар |
| GET | `/api/statistics/examinations-by-doctor-and-period` | Прегледи по лекар и период |
| GET | `/api/statistics/month-most-sick-leaves` | Месец с най-много болнични |
| GET | `/api/statistics/doctor-most-sick-leaves` | Лекар с най-много болнични |

---

## Структура на грешките

```json
{
  "status": 404,
  "грешка": "Не е намерено",
  "съобщение": "Пациент с id 5 не е намерен",
  "timestamp": "2025-01-15T10:30:00"
}
```

---

## Лекар като пациент

Системата **не позволява** един потребителски акаунт да има едновременно роля DOCTOR и PATIENT. Ако лекар иска да бъде регистриран и като пациент (напр. за преглед при друг лекар), трябва да бъде създаден **отделен акаунт** с различен имейл и роля PATIENT. Двата акаунта (DOCTOR и PATIENT) са напълно независими.

---

## Роли и права

| Действие | ADMIN | DOCTOR | PATIENT |
|---|---|---|---|
| Управление на лекари | ✓ | ✗ | ✗ |
| Управление на пациенти | ✓ | ✗ | ✗ |
| Управление на диагнози | ✓ | ✗ | ✗ |
| Всички прегледи | ✓ | ✓ | ✗ |
| Собствени прегледи | ✓ | ✓ (само свои) | ✓ (само свои) |
| Болнични листове | ✓ (пълен достъп) | ✓ (създава) | ✓ (само свои) |
| Статистики | ✓ | ✓ | ✗ |
| Смяна на парола | ✓ | ✓ | ✗ |
