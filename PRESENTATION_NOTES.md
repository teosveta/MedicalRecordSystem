# Обяснение на проекта — подготовка за презентация

---

### СИГУРНОСТ — JWT И АВТЕНТИКАЦИЯ

## 2. Какъв е типът на security-то?

> **Въпрос на преподавателя:** „Какъв е типа на security-то?"

**JWT (JSON Web Token) — stateless автентикация.** Файл: `SecurityConfig.java`.

Не се пазят сесии на сървъра. При всяка заявка клиентът изпраща токена в хедъра:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Конфигурацията изрично задава `STATELESS` политика:

```java
// SecurityConfig.java → securityFilterChain()
.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

**Защо stateless?** При stateless архитектура сървърът не трябва да пази информация за потребителите между заявките — всичко е в токена. Това позволява лесно мащабиране (scale-out) и е подходящо за REST API.

**Как работи JWT:** Токенът е Base64-кодиран JSON с три части — Header (алгоритъм), Payload (данни: имейл + роля + валидност), Signature (HMAC-SHA256 подпис с тайния ключ). Сървърът проверява подписа при всяка заявка — ако е валиден, доверява на данните в токена.

---

## 16. Къде си конфигурирал, че с access token ще осигуриш security-то?

> **Въпрос на преподавателя:** „Къде си конфигурирал че с аксес тоукен ще осигуриш секюрити?"

**Три файла работят заедно:** `application.properties` (ключ и валидност), `JwtUtil.java` (генерация/валидация), `SecurityConfig.java` (регистрация на JWT филтъра), `JwtAuthenticationFilter.java` (реалната проверка).

**Файл:** `application.properties`, `JwtUtil.java`

```properties
jwt.secret=твоят-таен-ключ-поне-32-символа
jwt.expiration=86400000
```

- `jwt.expiration=86400000` = **24 часа** (в милисекунди)
- Алгоритъм: **HMAC-SHA256** (симетричен — един таен ключ за подписване и проверка)
- Токенът съдържа: `subject` (имейл), `role` (ролята), `issuedAt` (кога е издаден), `expiration` (кога изтича)

Токенът **не се пази в базата данни** — stateless. Ако потребителят се изгони (logout), токенът технически е валиден до изтичане, но JavaScript на клиента го изтрива от `localStorage`.

Проверката при всяка заявка:
```java
// JwtAuthenticationFilter.java
String token = authHeader.substring(7);  // премахваме "Bearer "
if (jwtUtil.validateToken(token)) {
    String username = jwtUtil.extractUsername(token);  // = имейл
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    // → зареждаме потребителя от БД и слагаме в SecurityContext
}
```

---

## 26. Обясни стъпка по стъпка как работи `JwtAuthenticationFilter` при всяка заявка.

**Кратък отговор:** Филтърът се изпълнява веднъж за всяка HTTP заявка, проверява дали има валиден JWT в `Authorization` хедъра и ако да — зарежда потребителя в `SecurityContext`.

**Детайлно обяснение — пълният поток в `doFilterInternal()`:**

```java
// JwtAuthenticationFilter.java → doFilterInternal()

// Стъпка 1: Проверяваме дали хедърът съществува
String authHeader = request.getHeader("Authorization");
if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    filterChain.doFilter(request, response);  // Продължаваме без автентикация
    return;
}

// Стъпка 2: Извличаме токена (премахваме "Bearer " — 7 символа)
String token = authHeader.substring(7);

// Стъпка 3: Проверяваме подписа и датата на изтичане
// Стъпка 4: Извличаме имейла от subject-а на токена
if (jwtUtil.validateToken(token)
        && SecurityContextHolder.getContext().getAuthentication() == null) {
    String username = jwtUtil.extractUsername(token);  // = имейл

    // Стъпка 5: Зареждаме UserDetails от базата данни
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

    // Стъпка 6: Създаваме Authentication обект с правата на потребителя
    UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

    // Стъпка 7: Поставяме в SecurityContext — от тук нататък @PreAuthorize работи
    SecurityContextHolder.getContext().setAuthentication(authentication);
}

// Стъпка 8: Продължаваме по filter chain-а към контролера
filterChain.doFilter(request, response);
```

**Какво се случва при липсващ или невалиден токен:**
- Не се хвърля изключение
- Просто не се задава Authentication в SecurityContext
- SecurityContext остава с `null` Authentication
- Когато заявката стигне до контролер с `@PreAuthorize`, Spring Security вижда "неавтентикиран потребител" и извиква `authenticationEntryPoint` от `SecurityConfig` → връща 401 JSON

**Защо проверяваме `SecurityContextHolder.getContext().getAuthentication() == null`:**
Да не презапишем вече зададена автентикация (напр. ако друг филтър вече е логнал потребителя).

**Покажи в кода:**
- Файл: `JwtAuthenticationFilter.java` → метод `doFilterInternal()` — стъпките са коментирани
- Файл: `SecurityConfig.java` → `.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)` — филтърът се вмъква преди стандартния

---

## 12. Къде е конфигурацията на security-то? Къде е глобалният конфигурационен файл и в него — къде по-точно?

> **Въпрос на преподавателя:** „Къде е конфигурацията на това какво секюрити ползваме? Къде е глобалния конфигурационен файл и в него къде по точно?"

**Файл:** `SecurityConfig.java` → клас анотиран с `@Configuration @EnableWebSecurity @EnableMethodSecurity`

Основните точки:
1. **CSRF е изключен** — REST API-та не се нуждаят от CSRF защита (нямат форми с cookie-базирани сесии)
2. **Сесиите са STATELESS** — всяка заявка е независима
3. **Публични endpoints** (не изискват токен):
   - `/api/auth/**` — вход и регистрация
   - `/api/doctors/gp` — списък с ОПЛ (за регистрационната форма)
   - HTML страниците (`/`, `/login`, `/register`, `/admin/**`, `/doctor/**`, `/patient/**`)
   - Статични ресурси (`/css/**`, `/js/**`)
4. **JWT филтърът** се добавя преди стандартния `UsernamePasswordAuthenticationFilter`
5. **Персонализирани JSON отговори** при 401 и 403 (вместо HTML redirect)

```java
// SecurityConfig.java → securityFilterChain()
.exceptionHandling(ex -> ex
    .authenticationEntryPoint((req, res, e) -> {
        res.setStatus(401);
        res.getWriter().write("{\"грешка\":\"Неупълномощен достъп\"}");
    })
    .accessDeniedHandler((req, res, e) -> {
        res.setStatus(403);
        res.getWriter().write("{\"грешка\":\"Забранен достъп\"}");
    })
)
```

---

## 3. В security-то какво показваш за username-а?

> **Въпрос на преподавателя:** „В security-то какво показваш за юзърнейма?"

**Файл:** `JwtUtil.java` → **Метод:** `generateToken` и `extractUsername`

Като subject (потребителско „иден") в JWT се пази **имейл адресът** на потребителя, не реалното му потребителско име. В проекта `username` полето на `User` entity **съдържа имейл** — така работи Spring Security по конвенция (UserDetails.getUsername() = уникален идентификатор).

```java
public String generateToken(String username, String role) {
    return Jwts.builder()
            .subject(username)        // username = имейл (напр. "d.petrov@medical.com")
            .claim("role", role)      // ролята (ADMIN / DOCTOR / PATIENT)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(key)
            .compact();
}
```

В `User` ентитието полето `username` е **имейл адрес**:

```java
// User.java
@Column(name = "username", unique = true, nullable = false)
private String username;  // съдържа имейл — напр. "d.petrov@medical.com"
```

Затова `auth.getName()` в контролерите връща имейл, и всички репозитории имат методи като `findByUser_Username(String email)`.

---

## 4. Сетил ли си се, че е Serializable?

> **Въпрос на преподавателя:** „Сетнал ли си че е сиреаляйзъбул?"

**Файл:** `CustomUserDetailsService.java` → **Метод:** `loadUserByUsername`

В проекта се използва **вградения** Spring Security клас `org.springframework.security.core.userdetails.User` (не custom имплементация). Той **вече имплементира `Serializable`** вътрешно — в изходния код на Spring Security: `public class User implements UserDetails, CredentialsContainer, Serializable`.

**Защо изобщо е нужно `Serializable` за `UserDetails`:**
При **session-based** автентикация (HttpSession) Spring сериализира `UserDetails` обекта и го пази в HTTP сесията — за да може при клъстерирани сървъри данните да се споделят между нодовете. За да работи тази сериализация, класът трябва да имплементира `Serializable`.

**Защо тук е без значение:**
Проектът използва **stateless JWT** — сесии не се пазят (`SessionCreationPolicy.STATELESS`). Обектът `UserDetails` се създава наново при всяка заявка от `CustomUserDetailsService.loadUserByUsername()` и никога не се сериализира. Затова дори и да нямаше `Serializable`, нямаше да е проблем.

**Отговорът за преподавателя:** Да, Spring Security's `User` вече е `Serializable`. При нашия stateless JWT подход това няма практическо значение, но е добра практика за ако системата се смени към session-based auth.

---

## 10. На ниво методи — не в глобалния конфигурационен файл — имаш ли ограничение кой до какво да има достъп?

> **Въпрос на преподавателя:** „На ниво на методите не в глобалният конфигурационен файл за секюритито имаш ли ограничение кой до какво да има достъп? В контролера и в сървиза например"

**Да — чрез `@PreAuthorize` анотацията.** Файл: `SecurityConfig.java` + всички контролери.

`@EnableMethodSecurity(prePostEnabled = true)` в `SecurityConfig` **включва** тази функционалност. Без тази анотация `@PreAuthorize` би се игнорирал тихо.

Примери от проекта:
```java
// DiagnosisController.java → createDiagnosis()
@PreAuthorize("hasRole('ADMIN')")

// ExaminationController.java → createExamination()
@PreAuthorize("hasRole('DOCTOR')")

// PatientMedicalRecordController.java (ниво клас)
@PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")

// DiagnosisController.java → getAllDiagnoses()
@PreAuthorize("isAuthenticated()")
```

Spring Security проверява ролята **преди** изпълнението на метода. Ако потребителят няма нужната роля, се хвърля `AccessDeniedException` → `GlobalExceptionHandler` го хваща → връща `403 Forbidden`.

**Ролите в SecurityContext имат префикс `ROLE_`** (добавя се от `CustomUserDetailsService`):
```java
// CustomUserDetailsService.java → loadUserByUsername()
List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
// → "ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT"
```
Затова `hasRole('ADMIN')` съответства на `ROLE_ADMIN`.

---


### ПОТРЕБИТЕЛИ И РОЛИ

## 5. Къде са потребителите? Покажи ми ги!

> **Въпрос на преподавателя:** „Къде са потребителите? Покажи ми ги!"

**Файл:** `User.java` (entity, таблица `users`), `UserRepository.java` (достъп до БД)

Потребителите се пазят в таблица `users` в MySQL. Всеки потребител има:
- `username` — имейл адрес (уникален)
- `password` — BCrypt хеш (никога plain text)
- `role` — ADMIN, DOCTOR или PATIENT (enum, пази се като STRING в БД)
- `enabled` — дали акаунтът е активен

```java
// User.java
@Entity
@Table(name = "users")
public class User {
    @Column(name = "username", unique = true, nullable = false)
    private String username;         // имейл

    @Column(name = "password")
    private String password;         // BCrypt хеш

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;               // "ADMIN" / "DOCTOR" / "PATIENT"

    @Column(name = "enabled")
    private boolean enabled = true;
}
```

Паролата се хешира с `BCryptPasswordEncoder` (извиква се при регистрация и при създаване на лекар/пациент от admin):

```java
// AuthServiceImpl.java → register() / DoctorServiceImpl.java → createDoctor()
passwordEncoder.encode(request.getPassword())
```

BCrypt генерира различен хеш всеки път (salt е вграден), затова `passwordEncoder.matches(raw, hashed)` се използва при сравнение.

---

## 15. Къде пазиш потребителите? (от гледна точка на базата данни)

> **Въпрос на преподавателя:** „Къде пазиш потребителите? / Къде са потребителите?"

**Файл:** `User.java` (таблица `users`), `Doctor.java` (таблица `doctors`), `Patient.java` (таблица `patients`)

Структурата е **разделена**: общите данни за автентикация са в `users`, а профилните данни са в отделни таблици. Всеки лекар и пациент имат свой ред в `users` + ред в `doctors`/`patients` свързани с FK `user_id`.

```
users (id, username=email, password=BCrypt, role, enabled)
    ↕ @OneToOne
doctors (id, uin, first_name, last_name, can_be_gp, user_id)
    ↕ (отделна таблица)
doctor_specialties (doctor_id, specialty)   ← @ElementCollection

    ↕ @OneToOne
patients (id, first_name, last_name, egn, health_insured, personal_doctor_id, user_id)
```

Администраторът **няма профил в `doctors` или `patients`** — само ред в `users` с `role = ADMIN`.

Ролята се пази като STRING (`@Enumerated(EnumType.STRING)`), за да е четима в базата данни (`"ADMIN"`, `"DOCTOR"`, `"PATIENT"`).

---

## 19. Къде се разпределят ролите — в DTO-то ли?

> **Въпрос на преподавателя:** „Къде се разпределят ролите, в DTO-то ли?"

**Не — ролите се задават в сервизния слой, не в DTO-то.** Файл: `Role.java`, `AuthServiceImpl.java`, `PatientServiceImpl.java`

DTO-то (`RegisterRequest`) **не съдържа поле `role`** — клиентът не може да избере собствена роля. Ако съдържаше, всеки би могъл да се регистрира като ADMIN. Ролята е **фиксирана в кода** на сервизния слой.

Ролите са дефинирани като Java enum:
```java
public enum Role {
    ADMIN, DOCTOR, PATIENT
}
```

**Кой може да създава каква роля:**

| Кой | Роля | Как |
|---|---|---|
| Потребителска регистрация (`/api/auth/register`) | PATIENT | Фиксирано в кода — `Role.PATIENT` |
| ADMIN чрез `/api/admin/patients` | PATIENT | Фиксирано в кода — `Role.PATIENT` |
| ADMIN чрез `/api/admin/doctors` | DOCTOR | Фиксирано в кода — `Role.DOCTOR` |
| Няма endpoint | ADMIN | Само чрез seed данни (`DataInitializer`) |

```java
// AuthServiceImpl.java — регистрацията винаги създава PATIENT
User user = User.builder()
        .username(request.getUsername())
        .password(passwordEncoder.encode(request.getPassword()))
        .role(Role.PATIENT)  // фиксирано
        .enabled(true)
        .build();
```

Администраторски акаунт се създава само от `DataInitializer` при старт на приложението ако базата е празна. Това предотвратява случайното създаване на администратори чрез API.

---

## 24. Как работи BCrypt и защо се използва за пароли?

**Кратък отговор:** BCrypt е еднопосочна хеш функция с вграден случаен salt. Не може да бъде обратена, а сравнението на пароли се прави чрез `passwordEncoder.matches()`.

**Детайлно обяснение:**

**Как работи:**
- При `encode("Az1234!")` → BCrypt генерира случаен 16-байтов salt, хешира паролата заедно с него → получава се нещо като `$2a$10$xK7/...` (60 символа)
- Всяко извикване на `encode()` с **същата парола** дава **различен резултат** (защото salt е различен всеки път)
- При `matches("Az1234!", hashedPassword)` → BCrypt извлича salt от хеша, хешира подадената парола по същия начин и сравнява резултатите

```java
// Сравнение — работи правилно въпреки различния salt:
passwordEncoder.matches("Az1234!", "$2a$10$xK7/...") → true
passwordEncoder.matches("Грешна", "$2a$10$xK7/...") → false
```

**Където се дефинира bean-ът:**
```java
// SecurityConfig.java:
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**Където се извиква `encode()`:**
- `DataInitializer.java` — при seed данните: `passwordEncoder.encode("Az1234!")`
- `AuthServiceImpl.java` → `register()`: `passwordEncoder.encode(request.getPassword())`
- `DoctorServiceImpl.java` → `createDoctor()`: `passwordEncoder.encode(request.getPassword())`
- `PatientServiceImpl.java` → `createPatient()`: `passwordEncoder.encode(request.getPassword())`
- `DoctorServiceImpl.java` → `changePassword()`: `passwordEncoder.encode(request.getNewPassword())`

**Защо plain text е критичен проблем:** Ако паролите се пазят като текст и базата бъде компрометирана (SQL инжекция, backup изтичане), нападателят получава директен достъп до всички акаунти и може да ги опита в Gmail, банки и т.н. (password reuse attack). BCrypt прави brute-force изключително скъп изчислително.

**Покажи в кода:**
- Файл: `DoctorServiceImpl.java` → метод `changePassword()` — вижте `passwordEncoder.matches()` за верификация на текущата парола, след това `encode()` за новата

---

## 36. Какво се случва при опит за регистрация с вече съществуващ имейл или ЕГН?

**Кратък отговор:** `AuthServiceImpl.register()` проверява за дублиране преди запис. При намерен дублиращ се запис хвърля `IllegalArgumentException` → `GlobalExceptionHandler` го хваща → връща `400 Bad Request` с конкретно съобщение на български.

**Пълен поток на грешката:**

**Стъпка 1 — Проверка в сервиза:**
```java
// AuthServiceImpl.java → register():
if (userRepository.existsByUsername(request.getUsername())) {
    throw new IllegalArgumentException(
            "Потребител с имейл '" + request.getUsername() + "' вече съществува");
}
if (patientRepository.existsByEgn(request.getEgn())) {
    throw new IllegalArgumentException(
            "Пациент с ЕГН '" + request.getEgn() + "' вече е регистриран");
}
```

**Стъпка 2 — `GlobalExceptionHandler` хваща изключението:**
```java
// GlobalExceptionHandler.java → handleIllegalArgument()
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(400).body(ErrorResponse.builder()
            .status(400)
            .error("Невалидни данни")
            .message(ex.getMessage())   // ← конкретното съобщение от сервиза
            .build());
}
```

**Стъпка 3 — `register.html` показва грешката:**
```javascript
// register.html → submitRegistration()
const msg = await getErrorMessage(r);   // auth.js: извлича 'съобщение' от JSON
showError('reg-error', msg);            // показва в div#reg-error
```

**Защо се прави проверка в сервиза, а не само разчитаме на БД constraint:**
Базата данни ще хвърли `DataIntegrityViolationException` при дублиране — но `GlobalExceptionHandler` го обработва с **генерично** съобщение „Записът нарушава ограничение за уникалност" без детайли. При изричната проверка в сервиза клиентът получава **конкретно** съобщение на Bulgarian с точния имейл или ЕГН.

| | Без сервиз проверка | Със сервиз проверка |
|---|---|---|
| HTTP статус | 409 Conflict | 400 Bad Request |
| Съобщение | Генерично | "Потребител с имейл '...' вече съществува" |
| Поведение | Всичко се записва, след това гърми | Не се прави нищо при дублиране |

**Покажи в кода:**
- Файл: `AuthServiceImpl.java` → метод `register()` — двете `existsBy...` проверки
- Файл: `GlobalExceptionHandler.java` → `handleIllegalArgument()` vs `handleDataIntegrity()` — вижте разликата в съобщенията

---


### ПАЦИЕНТ-ДОКТОР РЕЛАЦИИ

## 6. Има ли връзка между модела на пациента и модела на доктора? Как закачаш пациент към един доктор?

> **Въпрос на преподавателя:** „Къде закачаме пациентите към доктора в кода? Има ли връзка между моделите на пациента и модела на доктора?" / „Как закачаш пациент към един доктор?"

**Файл:** `Patient.java`, `Doctor.java`, `PatientServiceImpl.java`

**Да — има директна JPA връзка.** Пациентът има **личен лекар** (ОПЛ — Общопрактикуващ Лекар). Връзката е `@ManyToOne` — много пациенти могат да имат един и същ личен лекар. В базата данни `patients` таблицата има FK колона `personal_doctor_id` → `doctors.id`.

```java
// Patient.java — страната, която пази FK колоната
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "personal_doctor_id")   // → FK колона в таблица patients
private Doctor personalDoctor;             // може да е null (пациент без личен лекар)
```

```java
// Doctor.java — обратната страна на връзката (navigational, без FK)
@OneToMany(mappedBy = "personalDoctor", fetch = FetchType.LAZY)
private Set<Patient> patients;   // всички пациенти на лекаря
```

**`mappedBy = "personalDoctor"`** означава: „не ти (Doctor) управляваш FK колоната — тя се управлява от полето `personalDoctor` в `Patient`". Ако пропуснем `mappedBy`, Hibernate ще създаде ненужна junction таблица.

**Как се закача пациент към лекар — в кода:**

```java
// PatientServiceImpl.java → assignPersonalDoctor()
public PatientResponse assignPersonalDoctor(Long patientId, Long doctorId) {
    Patient patient = patientRepository.findById(patientId).orElseThrow(...);
    Doctor doctor   = doctorRepository.findById(doctorId).orElseThrow(...);
    if (!doctor.isCanBeGP()) throw new IllegalArgumentException(...);
    patient.setPersonalDoctor(doctor);              // закачане
    return patientMapper.toResponse(patientRepository.save(patient));  // записване
}
```

**При регистрация** лекарят се избира от dropdown-а в `register.html` (зареден от `/api/doctors/gp`) и се подава като `personalDoctorId` в `PatientRequest`. Ако полето е `null` — пациентът остава без личен лекар.

Правило: **само лекари с `canBeGP = true` могат да бъдат личен лекар**. При опит да се зададе лекар без тази флага системата хвърля `IllegalArgumentException`:

```java
// PatientServiceImpl.java → createPatient()
if (!personalDoctor.isCanBeGP()) {
    throw new IllegalArgumentException("Д-р " + personalDoctor.getLastName() + " не може да бъде личен лекар");
}
```

---

## 9. Ако булевата `canBeGP` е false и отидеш да закачиш пациент, какво ще стане?

> **Въпрос на преподавателя:** „Entity където показва дали лекарят може да бъде личен лекар, ако булевата е false и отидеш да закачиш пациент какво ще стане?"

**Файл:** `Doctor.java`, `PatientServiceImpl.java`

`canBeGP` е булево поле на `Doctor` ентитието. Указва дали лекарят може да бъде **личен лекар (ОПЛ)**.

**Ако `canBeGP = false` и се опиташ да закачиш пациент към него:** Сервизът хвърля `IllegalArgumentException` → `GlobalExceptionHandler` го хваща → клиентът получава `400 Bad Request` с конкретно съобщение.

В реалния медицински свят само лекари с определена специализация (например обща медицина) могат да бъдат ОПЛ. В проекта администраторът задава флагата ръчно при създаване/редакция на лекар.

Ефектите на `canBeGP = true`:
1. **Пациент може да бъде назначен към този лекар** като личен лекар
2. **Лекарят вижда такса за ОПЛ** в dropdown-а при създаване на преглед (`getAvailableFees`)
3. **`/api/doctors/gp`** — публичен endpoint, достъпен без автентикация, показва списък с всички лекари с `canBeGP = true`

```java
// PatientServiceImpl.java → assignPersonalDoctor()
if (!personalDoctor.isCanBeGP()) {
    throw new IllegalArgumentException("Д-р " + personalDoctor.getLastName() + " не може да бъде личен лекар (ОПЛ)");
}
```

---

## 28. Какво се случва ако пациентът няма личен лекар (`personalDoctor = null`)?

**Кратък отговор:** `personalDoctor` е nullable — пациентът може да съществува без личен лекар. Системата обработва `null` на всички нива.

**Детайлно обяснение:**

**В entity-то — `personalDoctor` е nullable:**
```java
// Patient.java:
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "personal_doctor_id")   // БЕЗ nullable = false → nullable по подразбиране
private Doctor personalDoctor;
```
Колоната `personal_doctor_id` в таблицата `patients` може да е NULL.

**При регистрация — личният лекар е незадължителен:**
В `register.html` dropdown-ът започва с опция `"— без личен лекар —"`:
```html
<!-- register.html -->
<select class="form-select" id="personalDoctor">
    <option value="">— без личен лекар —</option>
    ...
</select>
```
В JavaScript-а ако стойността е празна, `personalDoctorId` не се включва в тялото:
```javascript
// register.html → submitRegistration()
if (personalDoctorId) {
    body.personalDoctorId = parseInt(personalDoctorId);
}
```

**В `PatientMapper.toResponse()` — null проверка:**
```java
// PatientMapper.java → toResponse()
if (patient.getPersonalDoctor() != null) {
    response.setPersonalDoctorId(patient.getPersonalDoctor().getId());
    response.setPersonalDoctorName(
            patient.getPersonalDoctor().getFirstName() + " " +
            patient.getPersonalDoctor().getLastName());
}
// Ако personalDoctor е null → personalDoctorId и personalDoctorName остават null в DTO
```

**В статистиката — пациентите без ОПЛ се изключват:**
```java
// PatientRepository.java:
@Query("... FROM Patient p WHERE p.personalDoctor IS NOT NULL ...")
List<DoctorCountResponse> countPatientsByPersonalDoctor();
```
`WHERE p.personalDoctor IS NOT NULL` — пациентите без личен лекар не се броят в статистиката.

**Покажи в кода:**
- Файл: `Patient.java` → поле `personalDoctor` — вижте `@JoinColumn` без `nullable = false`
- Файл: `PatientMapper.java` → метод `toResponse()` → `if (patient.getPersonalDoctor() != null)`
- Файл: `PatientRepository.java` → `countPatientsByPersonalDoctor()` → `WHERE p.personalDoctor IS NOT NULL`

---

## 11. Как fetch-ваме данните между пациент и доктор?

> **Въпрос на преподавателя:** „как фечваме данните между пациент и доктор?"

**Файлове:** `Patient.java` (FetchType), `PatientMedicalRecordController.java`, `PatientServiceImpl.java`

**На ниво JPA релация — LAZY fetch:**

```java
// Patient.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "personal_doctor_id")
private Doctor personalDoctor;
```

`FetchType.LAZY` означава: когато зареждаме `Patient`, **не се прави JOIN към `doctors`** автоматично. SQL е: `SELECT * FROM patients WHERE id = ?`. Данните за лекаря се зареждат **само когато се достъпят** (`patient.getPersonalDoctor().getFirstName()`) — тогава Hibernate прави отделен `SELECT * FROM doctors WHERE id = ?`.

**На ниво API — лекарят вижда данните на пациента чрез endpoint:**

```java
// PatientMedicalRecordController.java
GET /api/patients/{id}/examinations   → всички прегледи на пациента
GET /api/patients/{id}/sick-leaves    → всички болнични листове на пациента
```

Достъпът е само за ADMIN и DOCTOR (`@PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")`).

Логиката:
```java
// PatientServiceImpl.java → getPatientExaminationsById()
Patient patient = patientRepository.findById(patientId).orElseThrow(...);
return examinationRepository.findByPatient(patient).stream()
        .map(examinationMapper::toResponse)
        .collect(Collectors.toList());
```

**Пациентът вижда само своята история** чрез `/api/patients/my-history` — системата взима имейла от JWT токена и го използва да намери точно неговите данни:
```java
// PatientServiceImpl.java → getPatientHistory()
patientRepository.findByUser_Username(username)  // username = имейл от JWT
```

---


### JPA И ДАННИ

## 38. Каква е разликата между `@Entity`, `@Table` и `@Column` анотациите?

**Кратък отговор:** `@Entity` казва на JPA „това е персистиран клас". `@Table` задава конкретното ime на таблицата. `@Column` задава детайлите на колоната — ime, ограничения, размер.

**Детайлно обяснение:**

```java
// Patient.java — конкретен пример:

@Entity                              // → JPA знае, че класът е таблица
@Table(name = "patients")           // → таблицата се казва "patients" (не "patient" по подразбиране)
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // → AUTO_INCREMENT
    private Long id;

    @Column(name = "first_name",     // → колоната се казва "first_name" (не "firstName")
            nullable = false,        // → NOT NULL constraint
            length = 100)            // → VARCHAR(100)
    private String firstName;

    @Column(name = "egn",
            unique = true,           // → UNIQUE INDEX
            nullable = false,
            length = 10)
    private String egn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_doctor_id")  // → FK колона "personal_doctor_id"
    private Doctor personalDoctor;            // без nullable=false → nullable FK
}
```

**Как тези анотации се превеждат в DDL (с `ddl-auto=update`):**
```sql
CREATE TABLE patients (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    first_name         VARCHAR(100) NOT NULL,
    last_name          VARCHAR(100) NOT NULL,
    egn                VARCHAR(10)  NOT NULL,
    health_insured     BIT(1)       NOT NULL,
    personal_doctor_id BIGINT,
    user_id            BIGINT,
    PRIMARY KEY (id),
    UNIQUE KEY (egn),
    FOREIGN KEY (personal_doctor_id) REFERENCES doctors(id),
    FOREIGN KEY (user_id)            REFERENCES users(id)
);
```

**Без `@Table(name = "patients")`:** Hibernate би създал таблица `patient` (само Class name). В проекта последователно се използва `@Table` за да са имената в множествено число: `users`, `doctors`, `patients`, `examinations`, `sick_leaves`, `diagnoses`.

**Покажи в кода:**
- Файл: `User.java` → `@Table(name = "users")` и `@Column(name = "username", unique = true)`
- Файл: `Examination.java` → `@Column(name = "paid_by_patient", nullable = false)` — camelCase → snake_case

---

## 39. Какво е `@ElementCollection` и къде се използва?

**Кратък отговор:** `@ElementCollection` се използва за колекция от прости стойности (не entity обекти) — в проекта за `Set<Specialty>` в `Doctor` и `Diagnosis`. Hibernate създава отделна junction таблица.

**Детайлно обяснение:**

```java
// Doctor.java:
@ElementCollection(targetClass = Specialty.class, fetch = FetchType.EAGER)
@Enumerated(EnumType.STRING)
@CollectionTable(name = "doctor_specialties", joinColumns = @JoinColumn(name = "doctor_id"))
@Column(name = "specialty", nullable = false)
private Set<Specialty> specialties = new HashSet<>();
```

Hibernate създава:
```sql
CREATE TABLE doctor_specialties (
    doctor_id BIGINT       NOT NULL,
    specialty VARCHAR(255) NOT NULL,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id)
);
-- Примерни данни:
-- (1, 'GP')
-- (2, 'CARDIOLOGIST')
-- (3, 'NEUROLOGIST')
```

**Защо не отделен `@Entity Specialty`:**
`Specialty` е прост enum — тя не притежава собствено поведение и не се отнася към конкретни данни с id. Нямаме нужда да CRUD-ваме специалности независимо — те са константи в кода. `@ElementCollection` е подходящо за „колекция от прости стойности, принадлежащи на entity-то".

**`Diagnosis.specialties` — същото, но за `diagnosis_specialties`:**
```java
// Diagnosis.java:
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "diagnosis_specialties", joinColumns = @JoinColumn(name = "diagnosis_id"))
@Column(name = "specialty")
@Enumerated(EnumType.STRING)
private Set<Specialty> specialties = new HashSet<>();
```

**Как `findBySpecialtiesContaining(Specialty)` работи:**
```java
// DiagnosisRepository.java:
List<Diagnosis> findBySpecialtiesContaining(Specialty specialty);
// → SELECT d.* FROM diagnoses d
//   JOIN diagnosis_specialties ds ON d.id = ds.diagnosis_id
//   WHERE ds.specialty = ?
```

**Защо `Z00` (Здрав) е зададена на ВСИЧКИ специалности:**
```java
assignSpecialties("Z00", Specialty.values());  // DataInitializer.java
```
Диагноза `Z00 — Здрав / Без диагноза` е релевантна за всеки лекар — независимо от специалността, лекарят може да прегледа здрав пациент. Ако беше само за GP, кардиолог не би могъл да направи профилактичен преглед.

**Защо `Set<>` вместо `List<>`:**
`Set` предотвратява дублиращи се специалности и позволява `addAll()` идемпотентно. Освен това `Set<>` с `@ElementCollection` не причинява `MultipleBagFetchException`, за разлика от `List<>` при множество EAGER колекции.

**Покажи в кода:**
- Файл: `Doctor.java` → `@ElementCollection` полето `specialties` и `@CollectionTable(name = "doctor_specialties")`
- Файл: `DataInitializer.java` → метод `seedDiagnosisSpecialties()` → `assignSpecialties("Z00", Specialty.values())`

---

## 33. Каква е разликата между `FetchType.LAZY` и `EAGER`? Кое използваш?

**Кратък отговор:** `LAZY` зарежда свързаните данни само когато се достъпят изрично. `EAGER` ги зарежда веднага с JOIN. Проектът използва `LAZY` навсякъде с изключение на `Doctor.specialties` и `Diagnosis.specialties`.

**Детайлно обяснение:**

```java
// Patient.java — LAZY:
@ManyToOne(fetch = FetchType.LAZY)
private Doctor personalDoctor;
// SQL при loadване на Patient: SELECT * FROM patients WHERE id = ?
// SQL при достъп до patient.getPersonalDoctor().getFirstName():
//   → SELECT * FROM doctors WHERE id = ?  (отделна заявка)

// Doctor.java — EAGER за specialties:
@ElementCollection(targetClass = Specialty.class, fetch = FetchType.EAGER)
private Set<Specialty> specialties;
// SQL при loadване на Doctor: SELECT d.*, ds.specialty
//   FROM doctors d LEFT JOIN doctor_specialties ds ON ...
// Специалностите са нужни почти винаги → EAGER е оправдано
```

**Защо `LAZY` е предпочитан за релации:**
При зареждане на списък от 100 пациента с `EAGER` personalDoctor → 1 заявка за patients + 100 заявки за doctors = 101 заявки. При `LAZY` → само 1 заявка за patients, докато не достъпим personalDoctor.

**N+1 проблемът в проекта:**
В `PatientMapper.toResponse()`:
```java
// PatientMapper.java → toResponse()
if (patient.getPersonalDoctor() != null) {
    response.setPersonalDoctorName(
        patient.getPersonalDoctor().getFirstName() + ...);
    // ↑ LAZY достъп → SELECT * FROM doctors WHERE id = ? (отделна заявка за всеки пациент)
}
```
При 100 пациента → 1 + 100 заявки = N+1 проблем. В проекта обемът е малък (seed данни), затова не е проблем на практика. Решението в production би било `@EntityGraph` или JOIN FETCH в репозиторий заявка.

**Покажи в кода:**
- Файл: `Patient.java` → всички `@ManyToOne` и `@OneToOne` → `FetchType.LAZY`
- Файл: `Diagnosis.java` → `@ElementCollection(fetch = FetchType.EAGER)` — изключение, обяснено защо

---

## 23. Какво е Spring Data JPA и как работят репозиторийте?

**Кратък отговор:** Spring Data JPA генерира автоматично SQL заявки от имената на методите или от JPQL анотации. Не е нужно да пишем BoilerPlate JDBC код — декларираме интерфейс, Spring създава имплементацията при стартиране.

**Детайлно обяснение:**

`JpaRepository<T, ID>` предоставя готови методи без да пишем нито ред:
```java
findById(Long id)        → SELECT * FROM ... WHERE id = ?
findAll()                → SELECT * FROM ...
save(entity)             → INSERT или UPDATE (ако id е зададен)
delete(entity)           → DELETE FROM ... WHERE id = ?
count()                  → SELECT COUNT(*) FROM ...
existsById(Long id)      → SELECT COUNT(*) > 0 WHERE id = ?
```

**Автоматично генериране от метод-имена:**
Spring парсва името на метода и генерира SQL:
```java
// DoctorRepository:
findByUser_Username(String username)
// → SELECT d FROM Doctor d WHERE d.user.username = ?

findByCanBeGPTrue()
// → SELECT d FROM Doctor d WHERE d.canBeGP = true

// PatientRepository:
existsByEgn(String egn)
// → SELECT COUNT(*) > 0 FROM Patient p WHERE p.egn = ?

findByPersonalDoctor(Doctor doctor)
// → SELECT p FROM Patient p WHERE p.personalDoctor = ?

// UserRepository:
existsByUsername(String username)
// → SELECT COUNT(*) > 0 FROM User u WHERE u.username = ?
```

**Кога е нужен `@Query`:** Когато заявката е твърде сложна за метод-имена — GROUP BY, COUNT, SUM, CONCAT:
```java
// ExaminationRepository.java:
@Query("SELECT new com.medicalrecord.dto.statistics.DoctorCountResponse(" +
       "e.doctor.id, CONCAT(e.doctor.firstName, ' ', e.doctor.lastName), COUNT(e)) " +
       "FROM Examination e GROUP BY e.doctor.id, e.doctor.firstName, e.doctor.lastName " +
       "ORDER BY COUNT(e) DESC")
List<DoctorCountResponse> countByDoctor();
```
Методът-имена не може да изрази CONCAT или GROUP BY, затова се използва JPQL.

**Покажи в кода:**
- Файл: `DoctorRepository.java` → `findByCanBeGPTrue()` — Spring генерира WHERE can_be_gp = true
- Файл: `PatientRepository.java` → `countPatientsByPersonalDoctor()` — сложна JPQL с `@Query`, WHERE `personalDoctor IS NOT NULL`

---

## 8. Защо заявките в репозиторията връщат конкретната стойност, която връщат?

> **Въпрос на преподавателя:** „Защо заявките в repository връщата конкретната стойност която връщат? Например за заявката `sumAllPaymentsByDoctor()` — защо връща `List<PaymentByDoctorResponse>`? Така и за другите заявки защо връщат каквото връщат?"

**Файл:** `ExaminationRepository.java`

Типът на връщане се избира според **естеството на въпроса** — броя на очакваните резултати и вида на данните.

**`Optional<T>`** — когато търсим **конкретен запис по уникален ключ**. Резултатът е точно 1 или 0. `Optional` принуждава да обработим „не е намерено" изрично — ако просто върнем `T`, нямаме начин да различим `null` от грешка:
```java
findByCode("Z00")  → Optional<Diagnosis>     // 0 или 1 диагноза с точно този код
findById(5L)       → Optional<Examination>   // 0 или 1 преглед с точно това ID
```

**`List<T>`** — когато очакваме **множество записи**. Може да върне 0, 1 или N:
```java
findByPatient(patient) → List<Examination>   // всички прегледи на пациента (0..N)
findByDoctor(doctor)   → List<Examination>
```

**`BigDecimal`** — когато заявката изчислява **единична агрегатна стойност** (SUM). Резултатът е едно число:
```java
// ExaminationRepository.java
@Query("SELECT SUM(e.price) FROM Examination e WHERE e.paidByPatient = true")
BigDecimal sumPaidByPatient();
// SQL: SELECT SUM(price) → 1 ред, 1 стойност → BigDecimal
```

**`List<IdCountRow>`** — когато имаме GROUP BY + COUNT и всеки ред съдържа ID + брой:
```java
// ExaminationRepository.java
@Query("SELECT new ...IdCountRow(e.diagnosis.id, COUNT(e)) " +
       "FROM Examination e GROUP BY e.diagnosis.id ORDER BY COUNT(e) DESC")
List<IdCountRow> countByDiagnosis();
// SQL: GROUP BY diagnosis_id → много редове → List; всеки ред = {id, count} → IdCountRow
```

**`List<PaymentByDoctorResponse>`** — специалният случай, за който пита преподавателят:

```java
// ExaminationRepository.java
@Query("SELECT new ...PaymentByDoctorResponse(" +
       "e.doctor.id, CONCAT(e.doctor.firstName, ' ', e.doctor.lastName), " +
       "SUM(e.price), " +
       "SUM(CASE WHEN e.paidByPatient = false THEN e.price END), " +  // ← от НЗОК
       "SUM(CASE WHEN e.paidByPatient = true  THEN e.price END)) " +  // ← от пациента
       "FROM Examination e " +
       "GROUP BY e.doctor.id, e.doctor.firstName, e.doctor.lastName " +
       "ORDER BY SUM(e.price) DESC")
List<PaymentByDoctorResponse> sumAllPaymentsByDoctor();
```

- **Защо `List<>`:** Заявката има `GROUP BY doctor` → **един ред за всеки лекар** → много редове → `List`
- **Защо `PaymentByDoctorResponse`:** Един ред съдържа **4 стойности** (ID, name, totalSum, nhifSum, patientSum). Тези 4 стойности не могат да се върнат като прост тип — нужен е обект-контейнер. Spring Data JPA го изгражда чрез **JPQL constructor expression**: `new ClassName(arg1, arg2, arg3, arg4)` — вика конструктора директно в заявката.
- **`CASE WHEN`:** Изчислява два отделни сбора в рамките на един `GROUP BY` — едната колона сумира само прегледи на осигурени (НЗОК плаща), другата — само на неосигурени (пациентът плаща).

---

## 32. Как се предотвратява SQL инжекция?

**Кратък отговор:** Spring Data JPA използва Prepared Statements с параметри (`?`) навсякъде — нито метод-имената, нито `@Query` анотациите конкатенират потребителски входове директно в SQL низ.

**Детайлно обяснение:**

**Опасен код (НЕ е в проекта):**
```java
// SQL injection — НЕ правете това:
String sql = "SELECT * FROM users WHERE username = '" + username + "'";
// username = "'; DROP TABLE users; --" → катастрофа
```

**Как Spring Data JPA предотвратява това:**

**Метод-имена** → Spring генерира Prepared Statement:
```java
// UserRepository.java
findByUsername(String username)
// → PreparedStatement: "SELECT ... WHERE username = ?"
//   параметърът се bind-ва отделно, не конкатениран
```

**`@Query` с `:param` синтаксис** → параметризиран JPQL:
```java
// ExaminationRepository.java:
@Query("SELECT e FROM Examination e WHERE e.diagnosis.id = :diagnosisId")
List<Examination> findByDiagnosisId(@Param("diagnosisId") Long diagnosisId);
// Hibernate → PreparedStatement с ?, никога конкатениран низ
```

**`@Query` с JPQL constructor expressions** → същото:
```java
// ExaminationRepository.java
@Query("SELECT new ...DoctorCountResponse(e.doctor.id, ..., COUNT(e)) " +
       "FROM Examination e GROUP BY e.doctor.id ...")
List<DoctorCountResponse> countByDoctor();
// Всички стойности са bind-нати, не е конкатенация
```

**Допълнителен слой:** Bean Validation (`@Pattern(regexp = "\\d{10}")` за ЕГН, `@Email` за имейл) валидира входа преди да стигне до репозиторий — злонамерени символи не стигат до базата данни.

**Покажи в кода:**
- Файл: `ExaminationRepository.java` → метода `findByDiagnosisId(@Param("diagnosisId") Long diagnosisId)` — `:diagnosisId` синтаксис
- Файл: `UserRepository.java` → `findByUsername(String username)` — метод-имена се компилират до PreparedStatement

---


### ВАЛИДАЦИИ

## 7. В entity-то на examination има ли валидации? Къде съм описала, че това трябва да бъде валидирано?

> **Въпрос на преподавателя:** „В entity в examination имам ли валидации, къде съм описала че това трябва да бъде валидирано всъщност?"

**Важна особеност:** Валидациите **не са в entity-то** (`Examination.java`), а в **DTO класа** (`ExaminationRequest.java`). Това е умишлено — entity-то е картина на базата данни, а DTO-то е картина на входния request.

**Защо валидациите са в Request DTO, не в Entity:**
Entity-то може да бъде създадено програмно (напр. от `DataInitializer`) с данни, заобикалящи нормалния валидационен поток. Ако слагаме `@NotNull` в entity-то, seed данните може да гърмят. DTO-то защитава само API входа — публичната граница на системата.

Датата на прегледа се валидира на **две нива**:

**Ниво 1 — Bean Validation (в `ExaminationRequest.java`):**
```java
@NotNull(message = "Датата на прегледа е задължителна")
@PastOrPresent(message = "Датата не може да бъде в бъдещето")
private LocalDate examinationDate;
```
Анотацията `@PastOrPresent` не позволява бъдещи дати. Ако клиентът изпрати утрешна дата, Spring хвърля `MethodArgumentNotValidException` **преди дори да се стигне до сервиза** — отговорът е `400 Bad Request`.

**Ниво 2 — Бизнес правило (в `ExaminationServiceImpl.java`):**
```java
if (request.getExaminationDate().isBefore(LocalDate.now().minusDays(2))) {
    throw new IllegalArgumentException("Датата на прегледа не може да бъде повече от 2 дни назад");
}
```
Лекарят може да въведе преглед само ако датата е **не по-стара от 2 дни**. Тази логика е в сервизния слой, защото е бизнес правило, не просто формат.

---

## 18. За какво имаш валидации?

> **Въпрос на преподавателя:** „За какво имаш валидации?"

**Кратък отговор:** Валидациите са на три нива — Bean Validation анотации върху DTO класовете (`@NotNull`, `@Pattern`, custom `@NotTooOld`), бизнес правила в сервизния слой (дати, ownership), и ограничения на ниво база данни (UNIQUE, NOT NULL). Никоя от тях **не е в entity класовете**.

### Bean Validation (анотации върху DTO класовете)

**Прегледи (`ExaminationRequest`):**
- `@NotNull` + `@PastOrPresent` на `examinationDate` — задължителна, не може да е бъдеща
- `@NotNull` на `patientId`, `diagnosisId` — задължителни
- `@NotBlank` + `@Size(max=2000)` на `treatment` — задължително лечение
- `@NotNull` + `@DecimalMin("0.01")` на `price` — задължителна цена > 0

**Болнични листове (`SickLeaveRequest`):**
- `@NotNull` на `examinationId` — задължителна
- `@NotNull` + `@NotTooOld` на `startDate` — задължителна, не по-стара от 2 дни (custom анотация)
- `@Min(1)` + `@Max(30)` на `numberOfDays` — между 1 и 30 дни

**Лекари (`DoctorRequest`):**
- `@NotBlank` + `@Pattern(regexp = "\\d{10}")` на `uniqueIdentificationNumber` — УИН 10 цифри
- `@NotBlank` + `@Size(min=2, max=100)` на `firstName`, `lastName`

**Пациенти (`PatientRequest`):**
- `@NotBlank` + `@Size` на имената
- `@NotBlank` + `@Pattern(regexp = "\\d{10}")` на `egn` — ЕГН 10 цифри

**Потребители (`User`):**
- `@Email` + `@NotBlank` на `username` (имейл)
- `@NotBlank` на `password`

### Custom validator: `@NotTooOld`

```java
// NotTooOldValidator.java
public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
    if (value == null) return true;  // null → обработва се от @NotNull
    return !value.isBefore(LocalDate.now().minusDays(2));
}
```

### Бизнес правила в сервизите

- Преглед не може да е по-стар от 2 дни (`ExaminationServiceImpl`)
- Болничен лист само за преглед от последните 7 дни (`SickLeaveServiceImpl`)
- Не може да се издаде втори болничен за същия преглед
- Лекар без `canBeGP=true` не може да е личен лекар
- ЕГН и имейл трябва да са уникални
- Такса трябва да е положително число (`ExaminationFeeController`)

---

## 45. Какво е папката `validation/` и какво прави `@NotTooOld`?

**Кратък отговор:** Папката съдържа **custom Bean Validation анотация** `@NotTooOld`, която проверява дали `LocalDate` поле е не по-старо от 2 дни. Стандартните анотации (`@Past`, `@PastOrPresent`) не поддържат ограничение на броя дни — затова е написана собствена.

**Детайлно обяснение:**

Bean Validation (Jakarta Validation) позволява да се напишат собствени анотации. Нужни са два файла:
1. **Анотацията** — декларира ограничението и посочва кой клас прави проверката
2. **Валидаторът** — имплементира логиката на проверката

**`NotTooOld.java` — анотацията:**

```java
// NotTooOld.java
@Target({ElementType.FIELD})          // Може да се слага само на полета
@Retention(RetentionPolicy.RUNTIME)   // Анотацията съществува по време на изпълнение
@Documented
@Constraint(validatedBy = NotTooOldValidator.class)  // Кой клас прави проверката
public @interface NotTooOld {
    String message() default "Началната дата не може да бъде повече от 2 дни назад";
    Class<?>[] groups() default {};           // Задължително за Bean Validation
    Class<? extends Payload>[] payload() default {};  // Задължително за Bean Validation
}
```

`groups()` и `payload()` са **задължителни** методи за всяка `@Constraint` анотация — Bean Validation framework-ът ги изисква. В проекта не се използват активно, но трябва да присъстват.

`@Retention(RetentionPolicy.RUNTIME)` е критично — ако е `CLASS` или `SOURCE`, анотацията не е видима по runtime и валидацията изобщо не се изпълнява.

**`NotTooOldValidator.java` — логиката:**

```java
// NotTooOldValidator.java
public class NotTooOldValidator implements ConstraintValidator<NotTooOld, LocalDate> {

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;  // null се обработва от @NotNull — не дублираме
        }
        return !value.isBefore(LocalDate.now().minusDays(2));
        //      ↑ датата трябва да е ≥ (днес - 2 дни)
    }
}
```

`ConstraintValidator<NotTooOld, LocalDate>` — два generic параметъра: типа на анотацията и типа на полето, което валидира. Validator-ът работи само с `LocalDate` полета.

`if (value == null) return true` — **важна конвенция** в Bean Validation: ако стойността е `null`, валидаторът я приема за валидна. Null се обработва от `@NotNull`. Така анотациите са независими и могат да се комбинират:
```java
@NotNull(message = "Датата е задължителна")
@NotTooOld
private LocalDate startDate;
```

**Къде се използва `@NotTooOld`:**

```java
// SickLeaveRequest.java
@NotNull(message = "Началната дата е задължителна")
@NotTooOld
private LocalDate startDate;
```

Болничният лист не може да започва повече от 2 дни назад — лекарят не може да издаде болничен за миналата седмица. Тази проверка се изпълнява от `@Valid` в контролера **преди** да се извика сервизният слой.

**Защо е нужна и в сервиза:**

В `SickLeaveServiceImpl` има допълнителна проверка за датата — Bean Validation проверява само `startDate`, но сервизът проверява и допълнителни бизнес правила (преглед не по-стар от 7 дни). `@NotTooOld` е валидация на входния DTO; сервизната проверка е за бизнес правила.

**Покажи в кода:**
- Файл: `NotTooOld.java` → `@Constraint(validatedBy = NotTooOldValidator.class)` — връзката между анотация и логика
- Файл: `NotTooOldValidator.java` → `implements ConstraintValidator<NotTooOld, LocalDate>` → метод `isValid()`
- Файл: `SickLeaveRequest.java` → полето `startDate` с `@NotNull` и `@NotTooOld` едновременно

---


### DTO И MAPPER

## 21. Какво е DTO патернът и защо не връщаш entity обекти директно от API-то?

**Кратък отговор:** DTO (Data Transfer Object) е отделен клас, предназначен само за пренос на данни към клиента. Ентититата не се изпращат директно, защото съдържат чувствителни полета и могат да причинят безкрайна рекурсия при сериализация.

**Детайлно обяснение:**

Проектът разделя данните на два слоя:
- **Entity** — JPA обект, свързан с таблица в базата, съдържа всички данни включително релации
- **DTO (Response)** — прост обект само с полетата, нужни на клиента

**DTO пакети в проекта:**
```
dto/auth/     → LoginRequest, RegisterRequest, LoginResponse
dto/doctor/   → DoctorRequest, DoctorResponse, DoctorUpdateRequest, ChangePasswordRequest
dto/patient/  → PatientRequest, PatientResponse, PatientHistoryResponse
dto/examination/ → ExaminationRequest, ExaminationResponse, ExaminationFeeResponse, FeeOptionResponse, PatientNoteRequest
dto/sickleave/ → SickLeaveRequest, SickLeaveResponse, UpdateSickLeaveRequest
dto/diagnosis/ → DiagnosisRequest, DiagnosisResponse
dto/statistics/ → IdCountRow, DoctorCountResponse, MonthStatisticsResponse, PaymentByDoctorResponse
```

**Конкретен пример — `Examination` entity vs `ExaminationResponse` DTO:**

```
Examination entity:                    ExaminationResponse DTO:
  Long id                     →          Long id
  LocalDate examinationDate   →          LocalDate examinationDate
  Doctor doctor               →          Long doctorId
  (пълен обект с User)        →          String doctorName  (само "Петров Димитър")
  Patient patient             →          Long patientId
  (пълен обект с examinations →          String patientName
   set → circular!)
  Diagnosis diagnosis         →          Long diagnosisId
  String treatment            →          String diagnosisCode
  BigDecimal price            →          String diagnosisName
  boolean paidByPatient       →          String treatment
  String patientNote          →          BigDecimal price
                                         boolean paidByPatient
                                         String patientNote
```

**Защо е опасно да върнеш entity директно:**

1. **Изтичане на парола**: `Examination` → `Patient` → `User` → `password` (BCrypt хеш) ще бъде сериализиран в JSON отговора
2. **Безкрайна рекурсия**: `Doctor` има `Set<Examination>`, `Examination` има `Doctor` → Jackson се опитва да сериализира → StackOverflowError
3. **Излагане на вътрешна структура**: клиентът вижда имена на таблици, JPA метаданни, ненужни полета

**Покажи в кода:**
- Файл: `ExaminationResponse.java`
- Клас: `ExaminationResponse` — вижте какво липсва: няма `User`, няма `Set<SickLeave>`, само плоски полета
- Файл: `ExaminationMapper.java` → метод `toResponse()` — тук се прави конверсията

---

## 22. Как работят Mapper класовете? Покажи пример.

**Кратък отговор:** Mapper класовете са Spring компоненти (`@Component`), всеки с един метод `toResponse()`, който взима entity и го конвертира в DTO — копира нужните полета и прави `null` проверки за lazy-заредените релации.

**Детайлно обяснение:**

**Mapper файлове в проекта:**
```
mapper/ExaminationMapper.java  → Examination → ExaminationResponse
mapper/PatientMapper.java      → Patient → PatientResponse
mapper/SickLeaveMapper.java    → SickLeave → SickLeaveResponse
mapper/DoctorMapper.java       → Doctor → DoctorResponse
mapper/DiagnosisMapper.java    → Diagnosis → DiagnosisResponse
```

**Конкретен пример — `ExaminationMapper.toResponse()`:**
```java
// ExaminationMapper.java → toResponse()
public ExaminationResponse toResponse(Examination examination) {
    ExaminationResponse response = new ExaminationResponse();
    response.setId(examination.getId());
    response.setExaminationDate(examination.getExaminationDate());
    response.setTreatment(examination.getTreatment());
    response.setPrice(examination.getPrice());
    response.setPaidByPatient(examination.isPaidByPatient());
    response.setPatientNote(examination.getPatientNote());

    if (examination.getDoctor() != null) {
        response.setDoctorId(examination.getDoctor().getId());
        response.setDoctorName(examination.getDoctor().getFirstName() + " " +
                               examination.getDoctor().getLastName());
    }
    // ... и т.н. за Patient и Diagnosis
}
```

Важното: `if (examination.getDoctor() != null)` — тази проверка е необходима, защото при `FetchType.LAZY` достъпването на свързания обект предизвиква допълнителна SQL заявка. Ако Hibernate сесията е затворена, ще хвърли `LazyInitializationException`. Null проверката предпазва и от NullPointerException при тестови данни без relации.

**Защо НЕ се използва MapStruct:** MapStruct е annotation processor, който генерира mapper код автоматично по интерфейс. Той е по-ефективен, но изисква допълнителна конфигурация. Ръчните mapper-и са по-прозрачни — всяка трансформация е видима и лесна за обяснение на презентация. При нестандартни преобразувания (конкатениране на firstName + lastName) MapStruct изисква допълнителни анотации.

**Покажи в кода:**
- Файл: `PatientMapper.java` → метод `toResponse()` — вижте null проверката за `personalDoctor`
- Файл: `SickLeaveMapper.java` → вижте как `doctorName` и `patientName` се взимат от пряко полета на `SickLeave`, не чрез `examination`

---

## 44. Какво е папката `mapper/` и защо съществува?

**Кратък отговор:** Mapper-ите конвертират JPA entity обекти в DTO обекти преди да ги изпратим към клиента. Без тях API-то би връщало цялата entity структура — с lazy-loaded релации, вътрешни полета и риск от безкрайна рекурсия.

**Защо не се връща entity директно от контролера:**

```
Без mapper → ExaminationController връща Examination entity
Jackson сериализира: examination.doctor → doctor.examinations → examination.doctor → ...
                                                                     ↑ безкрайна рекурсия!
```

Освен рекурсията:
- Entity-то съдържа lazy-loaded релации → `LazyInitializationException` при сериализация извън транзакция
- Entity-то излага вътрешни идентификатори и полета, ненужни за клиента
- Промяна в entity структурата веднага се отразява в API contract-а

**Пет mapper-а — по един за всеки основен entity:**

| Mapper | Entity | DTO |
|---|---|---|
| `ExaminationMapper` | `Examination` | `ExaminationResponse` |
| `PatientMapper` | `Patient` | `PatientResponse` |
| `DoctorMapper` | `Doctor` | `DoctorResponse` |
| `DiagnosisMapper` | `Diagnosis` | `DiagnosisResponse` |
| `SickLeaveMapper` | `SickLeave` | `SickLeaveResponse` |

**Общата структура на всеки mapper:**
```java
@Component          // Spring инжектира в сервизите
public class XxxMapper {
    public XxxResponse toResponse(Xxx entity) {
        XxxResponse response = new XxxResponse();
        // 1. Плоски полета — директно копиране
        // 2. Lazy релации — само след null проверка
        return response;
    }
}
```

**Защо `@Component` а не `@Service`:** Mapper-ите нямат бизнес логика и не са transactional — те само копират данни. `@Component` е семантично по-точно.

**Защо ръчни mapper-и вместо MapStruct:**
MapStruct генерира mapper код по анотации — полезно в enterprise проекти с десетки entity-та. Тук ръчните mapper-и са умишлен избор: кодът е изцяло прозрачен, null проверките са видими, и не е нужна допълнителна библиотека за учебен проект.

---

**`ExaminationMapper.java` — най-сложният:**

```java
// ExaminationMapper.java → toResponse()
public ExaminationResponse toResponse(Examination examination) {
    ExaminationResponse response = new ExaminationResponse();

    // Плоски полета — без релации, директно достъпни
    response.setId(examination.getId());
    response.setExaminationDate(examination.getExaminationDate());
    response.setTreatment(examination.getTreatment());
    response.setPrice(examination.getPrice());
    response.setPaidByPatient(examination.isPaidByPatient());
    response.setPatientNote(examination.getPatientNote());

    // Lazy релации — null проверката е задължителна
    if (examination.getDoctor() != null) {
        response.setDoctorId(examination.getDoctor().getId());
        response.setDoctorName(examination.getDoctor().getFirstName() + " " +
                               examination.getDoctor().getLastName());
    }
    if (examination.getPatient() != null) {
        response.setPatientId(examination.getPatient().getId());
        response.setPatientName(examination.getPatient().getFirstName() + " " +
                                examination.getPatient().getLastName());
    }
    if (examination.getDiagnosis() != null) {
        response.setDiagnosisId(examination.getDiagnosis().getId());
        response.setDiagnosisCode(examination.getDiagnosis().getCode());
        response.setDiagnosisName(examination.getDiagnosis().getName());
    }

    return response;
}
```

Три null проверки — не защото `doctor`/`patient`/`diagnosis` могат да са null в базата, а защото:
1. Полетата са `FetchType.LAZY` — могат да са proxy обекти, незаредени в момента
2. В unit тестове с `mock` обекти полетата могат да са `null` при тест данни

Mapper-ът **изравнява (flattens)** структурата: вместо `{ "doctor": { "id": 1, "firstName": "..." } }` връща `{ "doctorId": 1, "doctorName": "Димитър Петров" }` — JavaScript получава само нужното.

---

**`PatientMapper.java`:**

```java
// PatientMapper.java → toResponse()
if (patient.getUser() != null) {
    response.setEmail(patient.getUser().getUsername());  // имейлът е в User, не в Patient
}
if (patient.getPersonalDoctor() != null) {
    response.setPersonalDoctorId(patient.getPersonalDoctor().getId());
    response.setPersonalDoctorName(firstName + " " + lastName);
}
```

`personalDoctor` може **легитимно** да е `null` (пациент без ОПЛ) — null проверката тук е бизнес логика, не само предпазна мярка. Имейлът се взима от `patient.getUser().getUsername()` защото `Patient` entity не пази имейл директно — пази се в свързания `User`.

---

**`DoctorMapper.java`:**

```java
// DoctorMapper.java → toResponse()
response.setSpecialties(doctor.getSpecialties());  // Set<Specialty> — директно, без конверсия
response.setCanBeGP(doctor.isCanBeGP());
if (doctor.getUser() != null) {
    response.setEmail(doctor.getUser().getUsername());
}
```

`specialties` е `@ElementCollection(fetch = EAGER)` — **всичко е заредено автоматично** с основния entity, няма нужда от null проверка. `Set<Specialty>` се сериализира от Jackson директно като JSON масив от низове: `["GP", "CARDIOLOGIST"]`.

---

**`DiagnosisMapper.java` — най-простият:**

```java
// DiagnosisMapper.java → toResponse()
response.setId(diagnosis.getId());
response.setCode(diagnosis.getCode());
response.setName(diagnosis.getName());
response.setDescription(diagnosis.getDescription());
```

Само плоски полета, никакви релации. `Diagnosis` има `@ElementCollection specialties`, но те **не се включват** в `DiagnosisResponse` — клиентът не се нуждае от тях при показване на диагнозата в преглед.

---

**`SickLeaveMapper.java`:**

```java
// SickLeaveMapper.java → toResponse()
if (sickLeave.getExamination() != null) {
    response.setExaminationId(sickLeave.getExamination().getId());
}
if (sickLeave.getDoctor() != null) {
    response.setDoctorId(...);
    response.setDoctorName(firstName + " " + lastName);
}
if (sickLeave.getPatient() != null) {
    response.setPatientId(...);
    response.setPatientName(firstName + " " + lastName);
}
```

`SickLeave` entity пази директни `@ManyToOne` полета `doctor` и `patient` (копирани от прегледа при създаване). Mapper-ът ги изравнява до имена и ID-та. Три null проверки за LAZY релациите.

**Покажи в кода:**
- Файл: `ExaminationMapper.java` → трите `if (... != null)` блока за doctor, patient, diagnosis
- Файл: `PatientMapper.java` → `if (patient.getPersonalDoctor() != null)` — бизнес null vs. lazy null
- Файл: `DoctorMapper.java` → `response.setSpecialties(doctor.getSpecialties())` — EAGER, без null проверка

---


### СЕРВИЗЕН СЛОЙ

## 34. Защо сервизният слой е нужен? Защо не можеш да викаш репозиторийте директно от контролерите?

**Кратък отговор:** Сервизният слой съдържа бизнес логиката. Контролерът не трябва да знае *как* се прави операцията — само *какво* иска клиентът. Разделението прави кода тестваем, преизползваем и поддържаем.

**Детайлно обяснение:**

Трислойната архитектура:
```
Controller  →  Service  →  Repository
(HTTP)         (Бизнес)    (Данни)
```

**Какво би се случило без сервизен слой:**
- Бизнес правила в контролера → невъзможно да се тестват без HTTP заявка
- Дублиран код: ако две места трябва да създадат преглед, правилата се повтарят
- Контролерът е свързан директно с JPA → не може да се тества изолирано
- Нарушен Single Responsibility Principle

**Конкретен пример — `ExaminationServiceImpl.createExamination()`:**
```java
// ExaminationServiceImpl.java → createExamination()
public ExaminationResponse createExamination(ExaminationRequest request, String doctorUsername) {
    // 1. Зарежда лекаря от JWT токена (не от request body!)
    Doctor doctor = doctorRepository.findByUser_Username(doctorUsername)...
    // 2. Зарежда пациента и диагнозата по ID
    Patient patient = patientRepository.findById(request.getPatientId())...
    Diagnosis diagnosis = diagnosisRepository.findById(request.getDiagnosisId())...
    // 3. Проверява бизнес правило — дата не повече от 2 дни назад
    if (request.getExaminationDate().isBefore(LocalDate.now().minusDays(2))) {
        throw new IllegalArgumentException(...);
    }
    // 4. Изчислява paidByPatient спрямо здравна осигуровка
    boolean paidByPatient = !patient.isHealthInsured();
    // 5. Записва прегледа
    return examinationMapper.toResponse(examinationRepository.save(examination));
}
```
Всичко това не може да бъде в контролера без да го направим нетестваем.

**`PatientServiceImpl.assignPersonalDoctor()` — 4 проверки преди save:**
```java
// PatientServiceImpl.java → assignPersonalDoctor()
public PatientResponse assignPersonalDoctor(Long patientId, Long doctorId) {
    Patient patient = patientRepository.findById(patientId).orElseThrow(...); // 1
    Doctor doctor = doctorRepository.findById(doctorId).orElseThrow(...);     // 2
    if (!doctor.isCanBeGP()) throw new IllegalArgumentException(...);          // 3
    patient.setPersonalDoctor(doctor);
    return patientMapper.toResponse(patientRepository.save(patient));          // 4
}
```

**Как сервизният слой прави unit тестването възможно:**
```java
// PatientServiceTest.java
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {
    @Mock private DoctorRepository doctorRepository;   // Mock — не реална БД
    @InjectMocks private PatientServiceImpl patientService;

    @Test
    void assignPersonalDoctor_whenDoctorCannotBeGP_throwsException() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(nonGpDoctor));
        assertThrows(IllegalArgumentException.class,
                () -> patientService.assignPersonalDoctor(1L, 1L));
    }
}
```

**Покажи в кода:**
- Файл: `ExaminationServiceImpl.java` → метод `createExamination()` — 5+ стъпки бизнес логика
- Файл: `PatientServiceImpl.java` → метод `deletePatient()` — каскадно изтриване в правилен ред

---

## 27. Какво е `@Transactional` и къде го използваш?

**Кратък отговор:** `@Transactional` обгръща метода в база данни транзакция — ако нещо се провали по средата, всичко до тук се отменя (rollback). Без него всяко `save()` е отделна транзакция.

**Детайлно обяснение:**

```java
// DoctorServiceImpl.java → createDoctor()
// Без @Transactional — ОПАСНО при multiple saves:
userRepository.save(user);         // Транзакция 1 — ако следващото гръмне, user е в БД
doctorRepository.save(doctor);     // Транзакция 2 — може да не изпълни, user е orphan

// С @Transactional — безопасно:
@Transactional
public DoctorResponse createDoctor(DoctorRequest request) {
    userRepository.save(user);     // \
    doctorRepository.save(doctor); //  > всичко в ЕДНА транзакция
}                                  // При изключение → ROLLBACK на всичко
```

**Конкретни места в проекта:**

Сервизните класове са анотирани на ниво клас с `@Transactional`:
```java
// DoctorServiceImpl.java
@Service
@Transactional            // Всички методи са transactional по подразбиране
public class DoctorServiceImpl { ... }
```

`DataInitializer.run()`:
```java
// DataInitializer.java → run()
@Override
@Transactional
public void run(String... args) {
    if (userRepository.count() == 0) {
        insertSeedData();  // 50+ записа в 1 транзакция
    }
}
```

**`@Transactional(readOnly = true)` — оптимизация:**
```java
// ExaminationServiceImpl.java → getExaminations()
@Override
@Transactional(readOnly = true)
public List<ExaminationResponse> getExaminations(String username) { ... }
```
При `readOnly = true`:
- Hibernate не следи промени в entity обектите (dirty checking е изключен)
- Базата данни може да насочи заявката към read replica
- По-бърза изпълнение при SELECT-heavy операции

**Покажи в кода:**
- Файл: `PatientServiceImpl.java` → метод `deletePatient()` — изтрива болнични, прегледи, пациент и user в 1 транзакция
- Файл: `DataInitializer.java` → метод `run()` — 50+ записа в 1 транзакция при seed

---


### ПРЕГЛЕДИ И БОЛНИЧНИ ЛИСТОВЕ

## 14. Как свързваш болничния лист към прегледа?

> **Въпрос на преподавателя:** „Как свързваш болничният лист към прегледа?"

**Файл:** `SickLeave.java`, `SickLeaveServiceImpl.java`

`SickLeave` задължително е свързан с `Examination` чрез `@ManyToOne(optional = false)` — болничен лист **не може да съществува без преглед**:

```java
// SickLeave.java
@ManyToOne(optional = false, fetch = FetchType.LAZY)
@JoinColumn(name = "examination_id", nullable = false)
private Examination examination;
```

Правила при издаване на болничен лист:
- Лекарят може да издаде болничен **само за собствен преглед**
- **Само един болничен лист** може да се издаде за даден преглед (`existsByExaminationId`)
- Прегледът **не може да е по-стар от 7 дни**
- Началната дата **не може да е повече от 2 дни назад** (двойна проверка — анотация `@NotTooOld` + проверка в сервиза)
- Лекарят и пациентът се **взимат автоматично от прегледа** (не се подават ръчно):

```java
// SickLeaveServiceImpl.java → createSickLeave()
SickLeave sickLeave = SickLeave.builder()
        .examination(examination)
        .startDate(request.getStartDate())
        .numberOfDays(request.getNumberOfDays())
        .doctor(examination.getDoctor())    // автоматично от прегледа
        .patient(examination.getPatient())  // автоматично от прегледа
        .build();
```

Обратното правило: **не може да се изтрие преглед, към който е издаден болничен лист**:
```java
// ExaminationServiceImpl.java → deleteExamination()
if (sickLeaveRepository.existsByExamination(examination)) {
    throw new IllegalArgumentException("Не можете да изтриете преглед към който е издаден болничен лист.");
}
```

---

## 37. Как гарантираш, че болничен лист може да се създаде само от лекаря, направил прегледа?

**Кратък отговор:** В `SickLeaveServiceImpl.createSickLeave()` се зарежда лекарят от JWT токена и се сравнява с лекаря на прегледа. Ако не съвпадат — `AccessDeniedException`. Това е ownership check, различен от role check-а.

**Пълна верига на проверките:**

```java
// SickLeaveServiceImpl.java → createSickLeave():

// 1. Зареждаме прегледа по ID
Examination examination = examinationRepository.findById(request.getExaminationId())
        .orElseThrow(() -> new ResourceNotFoundException(...));

// 2. Зареждаме текущия лекар от JWT (не от request body!)
Doctor currentDoctor = doctorRepository.findByUser_Username(doctorUsername)
        .orElseThrow(() -> new ResourceNotFoundException(...));

// 3. Сравняваме ID-тата
if (!examination.getDoctor().getId().equals(currentDoctor.getId())) {
    throw new AccessDeniedException(
            "Можете да издавате болнични листове само за свои прегледи");
}
```

**Два различни слоя сигурност:**

| Слой | Какво прави | Файл |
|---|---|---|
| `@PreAuthorize("hasRole('DOCTOR')")` | Проверява *дали* потребителят е DOCTOR | `SickLeaveController.java` |
| Ownership check в сервиза | Проверява *чий* е прегледът | `SickLeaveServiceImpl.java` |

`@PreAuthorize` не може да провери ownership — не знае ID-тата. Затова имаме два слоя: първо ролята, после собствеността.

**Тестът, който проверява това:**
В `SickLeaveServiceTest.java` има тест за точно тази ситуация — лекар се опитва да издаде болничен за преглед на друг лекар → проверява се, че `AccessDeniedException` е хвърлено.

**Покажи в кода:**
- Файл: `SickLeaveController.java` → `@PreAuthorize("hasRole('DOCTOR')")` на метода `createSickLeave()`
- Файл: `SickLeaveServiceImpl.java` → метод `createSickLeave()` → редовете с `examination.getDoctor().getId().equals(currentDoctor.getId())`

---


### КОНТРОЛЕРИ И ФРОНТЕНД

## 25. Каква е разликата между `@RestController` и `@Controller`? Кое използваш и защо?

**Кратък отговор:** `@Controller` връща имена на изгледи (HTML шаблони). `@RestController` = `@Controller` + `@ResponseBody` и сериализира върнатия обект директно в JSON. Проектът използва и двете.

**Детайлно обяснение:**

```java
// PageController.java → doctorDashboard()
// @Controller — метод връща низ = Thymeleaf шаблон
@GetMapping("/doctor/dashboard")
public String doctorDashboard() {
    return "doctor/dashboard";   // → зарежда templates/doctor/dashboard.html
}

// ExaminationController.java → getExaminations()
// @RestController — метод връща обект → Jackson го сериализира в JSON
@GetMapping("/api/examinations")
public ResponseEntity<List<ExaminationResponse>> getExaminations(...) {
    return ResponseEntity.ok(examinationService.getExaminations(...));
}
```

**Кои контролери са `@RestController` (REST API):**
```
ExaminationController         → /api/examinations
SickLeaveController           → /api/sick-leaves
AuthController                → /api/auth
AdminDoctorController         → /api/admin/doctors
AdminPatientController        → /api/admin/patients
PatientMedicalRecordController → /api/patients/{id}/...
DiagnosisController           → /api/diagnoses
ExaminationFeeController      → /api/examination-fees
DoctorProfileController       → /api/doctors/...
PatientHistoryController      → /api/patients/history
StatisticsController          → /api/statistics
DoctorStatisticsController    → /api/doctors/statistics
PatientForDoctorController    → /api/doctors/patients
```

**Кой контролер е `@Controller` (сервира HTML):**
```java
// PageController.java:
@Controller
public class PageController {
    @GetMapping("/login")     → templates/login.html
    @GetMapping("/register")  → templates/register.html
    @GetMapping("/admin/dashboard") → templates/admin/dashboard.html
    @GetMapping("/doctor/dashboard") → templates/doctor/dashboard.html
    @GetMapping("/patient/dashboard") → templates/patient/dashboard.html
}
```

**Защо проектът се нуждае от двата типа:** HTML страниците се сервират от Spring чрез Thymeleaf (`@Controller`), а данните се зареждат динамично от JavaScript чрез REST API (`@RestController`). Разделението позволява SPA-подобно поведение — страницата се зарежда веднъж, след това JavaScript прави асинхронни заявки.

**Покажи в кода:**
- Файл: `PageController.java` — единственият `@Controller`, само методи за HTML страниците
- Файл: `ExaminationController.java` — `@RestController`, всички методи връщат `ResponseEntity<...>`

---

## 30. Как фронтендът комуникира с бекенда? Обясни целия цикъл на една заявка.

**Кратък отговор:** Фронтендът е Thymeleaf HTML + vanilla JavaScript. JavaScript прави `fetch()` заявки към REST API, добавяйки JWT токена в `Authorization` хедъра. Отговорът е JSON, с който JavaScript обновява таблиците в UI-а.

**Пълен цикъл — "лекарят създава нов преглед":**

**Стъпка 1:** Лекарят попълва формата в `doctor/dashboard.html` и натиска „Запази":
```html
<!-- doctor/dashboard.html -->
<select id="exam-patient">...</select>
<input id="exam-price" readonly>  <!-- Цената се изчислява автоматично -->
<button onclick="submitExam()">Запази</button>
```

**Стъпка 2:** JavaScript `submitExam()` прави `fetch()` с JSON тяло:
```javascript
// doctor/dashboard.html → submitExam()
const r = await fetch('/api/examinations', {
    method: 'POST',
    headers: authHeaders(),   // { 'Authorization': 'Bearer ' + token }
    body: JSON.stringify({
        examinationDate: document.getElementById('exam-date').value,
        patientId: ...,
        diagnosisId: ...,
        treatment: ...,
        price: ...
    })
});
```

**Стъпка 3:** `auth.js` → `authHeaders()` добавя `Authorization: Bearer eyJ...` от `localStorage`.

**Стъпка 4:** `JwtAuthenticationFilter.doFilterInternal()` проверява токена, зарежда лекаря в `SecurityContext`.

**Стъпка 5:** `ExaminationController.createExamination()` получава `@RequestBody ExaminationRequest`:
```java
// ExaminationController.java → createExamination()
@PostMapping
@PreAuthorize("hasRole('DOCTOR')")
public ResponseEntity<ExaminationResponse> createExamination(
        @Valid @RequestBody ExaminationRequest request,
        Authentication authentication) {
```

**Стъпка 6:** `@Valid` активира Bean Validation на `ExaminationRequest` — проверява `@NotNull`, `@PastOrPresent`, `@NotBlank`. При грешка → 400.

**Стъпка 7:** `ExaminationServiceImpl.createExamination()` изпълнява бизнес логика:
- Зарежда лекаря по `authentication.getName()` (имейл от JWT)
- Проверява дали датата е в рамките на 2 дни
- Изчислява `paidByPatient` спрямо `patient.isHealthInsured()`

**Стъпка 8:** `examinationRepository.save(examination)` — Hibernate прави INSERT в MySQL.

**Стъпка 9:** `ExaminationMapper.toResponse()` конвертира entity → DTO.

**Стъпка 10:** JavaScript получава JSON отговора и добавя нов ред в таблицата `#exams-tbody`.

**Покажи в кода:**
- Файл: `doctor/dashboard.html` → функция `submitExam()` — fetch заявката
- Файл: `auth.js` → функция `authHeaders()` — добавяне на токена
- Файл: `ExaminationController.java` → метод `createExamination()` — приемане на заявката
- Файл: `ExaminationServiceImpl.java` → метод `createExamination()` — бизнес логиката

---

## 31. Как е решен проблемът с CORS?

**Кратък отговор:** В проекта няма CORS конфигурация, защото фронтендът и бекендът се сервират от **един и същ сървър** на един и същ порт — браузърът не прилага CORS ограничения при same-origin заявки.

**Детайлно обяснение:**

CORS (Cross-Origin Resource Sharing) е браузърна политика за сигурност: ако JavaScript на `http://frontend.com` прави заявка към `http://api.com`, браузърът блокира отговора, освен ако сървърът не е разрешил изрично този origin с `Access-Control-Allow-Origin` хедър.

**В проекта:**
- Spring Boot сервира HTML файловете от `src/main/resources/templates/` на `http://localhost:8080`
- REST API-то е достъпно на `http://localhost:8080/api/...`
- JavaScript от страницата прави заявки към **същия** `localhost:8080`
- Origin е еднакъв → same-origin policy → CORS не се прилага → не е нужна конфигурация

**Ако фронтендът беше отделно приложение (напр. React на порт 3000):**
```java
// Щеше да е нужно в SecurityConfig или отделен @Bean:
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:3000"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    // ...
}
```

**Покажи в кода:**
- Файл: `SecurityConfig.java` → `securityFilterChain()` — забележете, че няма `.cors(...)` конфигурация
- Файл: `doctor/dashboard.html` → `fetch('/api/examinations', ...)` — URL е относителен (без host) → same-origin

---


### УПРАВЛЕНИЕ НА ГРЕШКИ

## 1. Управление на изключенията глобализира ли се някъде? Глобално управляваш ли какво се случва с грешките?

> **Въпрос на преподавателя:** „Управление на изключенията глибализира ли се някъде? Глобално управляваш ли какво се случва с грешките?"

**Да — файл `GlobalExceptionHandler.java`, анотация `@RestControllerAdvice`.**

Проектът използва централизирана обработка на грешки чрез `@RestControllerAdvice`. Това означава, че **нито един контролер не хваща изключения директно** — всички грешки се насочват към един клас. Ако се хвърли изключение навсякъде в приложението, Spring го прихваща и го насочва тук.

`@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody` — отговорите се сериализират автоматично в JSON.

| Изключение | HTTP статус | Смисъл |
|---|---|---|
| `ResourceNotFoundException` | 404 | Търсен запис не е намерен |
| `AccessDeniedException` | 403 | Потребителят няма право |
| `BadCredentialsException` | 401 | Грешно потребителско име или парола |
| `MethodArgumentNotValidException` | 400 | Невалидни входни данни (Bean Validation) |
| `DataIntegrityViolationException` | 409 | Нарушена уникалност в БД |
| `IllegalArgumentException` | 400 | Нарушено бизнес правило |
| `Exception` (общо) | 500 | Непредвидена грешка |

```java
// GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .status(404)
                .error("Не е намерено")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(404).body(response);
    }
}
```

За валидационни грешки се връща и **карта с грешки по поле** (`"грешки": {"firstName": "Името е задължително"}`), за да знае клиентът кое точно поле е проблемно.

**Важно:** Без `@RestControllerAdvice` всяко изключение щеше да е в отделен `try-catch` в контролера — дублиран код и непоследователни JSON отговори. С него имаме едно място за всички грешки.

---

## 13. Къде в кода те препраща към страницата с грешката? Глобално управляваш ли какво се случва с грешките?

> **Въпрос на преподавателя:** „Къде в кода ме препраща към страницата с грешката? Глобално управляваш ли какво се случва с грешките?"

**Файл:** `SecurityConfig.java` (за 401/403), `GlobalExceptionHandler.java` (за останалите грешки), `auth.js` (за пренасочването към `/login`)

**Пренасочването към `/login` НЕ се прави от Java — прави се от JavaScript:**

```javascript
// admin/dashboard.html, doctor/dashboard.html, patient/dashboard.html — първи ред след зареждане
checkAuth('ADMIN');   // пренасочва към /login ако токенът липсва или ролята е грешна
```

`checkAuth()` в `auth.js` проверява `localStorage` за токен. Ако липсва или ролята не съответства — `window.location.href = '/login'`. Пренасочването е client-side, не server-side redirect.

**Глобалното управление на грешките в API-то е в `GlobalExceptionHandler.java`** — вж. В1.

HTML страниците са **предварително рендирани статични файлове** в `src/main/resources/static/`. Spring Boot ги сервира директно. Чрез конфигурацията се позволява достъп до тях без автентикация:

```java
// SecurityConfig.java → securityFilterChain()
.requestMatchers("/", "/login", "/register", "/css/**", "/js/**",
        "/admin/**", "/doctor/**", "/patient/**").permitAll()
```

Фронтендът е изграден с ванилен JavaScript. Всяка HTML страница проверява дали потребителят е логнат чрез `auth.js`:
```javascript
// admin/dashboard.html, doctor/dashboard.html, patient/dashboard.html — първи ред след зареждане
checkAuth('ADMIN');   // пренасочва към /login ако токенът липсва или ролята е грешна
```

---


### ТАКСИ

## 35. Как работи системата за таксуване (`ExaminationFee`)?

**Кратък отговор:** Всяка специалност има точно една базова такса в таблица `examination_fees`. При създаване на преглед лекарят избира тип консултация от dropdown, цената се изчислява автоматично и полето е read-only.

**Детайлно обяснение:**

**`ExaminationFee` entity:**
```java
// ExaminationFee.java
@Entity
@Table(name = "examination_fees")
public class ExaminationFee {
    @Enumerated(EnumType.STRING)
    @Column(name = "specialty", unique = true, nullable = false)  // UNIQUE — 1 такса на специалност
    private Specialty specialty;

    @DecimalMin(value = "0.01")
    @Column(name = "base_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFee;
}
```

**Начални такси (от `DataInitializer`):**
| Специалност | Такса |
|---|---|
| GP (ОПЛ) | 20.00 лв. |
| Кардиолог | 60.00 лв. |
| Невролог | 70.00 лв. |
| Дерматолог | 50.00 лв. |
| Ортопед | 60.00 лв. |
| Педиатър | 30.00 лв. |
| Психиатър | 80.00 лв. |
| Хирург | 100.00 лв. |
| Уролог | 65.00 лв. |
| Онколог | 90.00 лв. |

**Допълнителни услуги (`AdditionalService`):**
- Вземане на кръвна проба — 15.00 лв.
- Изпращане на материали за анализ — 20.00 лв.
- ЕКГ — 25.00 лв.
- Превръзка / манипулация — 10.00 лв.

**Как цената се задава при преглед:**
1. Лекарят отваря модал „Нов преглед" → JavaScript зарежда `/api/examination-fees/my-options`
2. `ExaminationFeeServiceImpl.getAvailableFees()` връща таксата за специалностите на лекаря + всички допълнителни услуги
3. Лекарят избира тип консултация от dropdown + евентуални допълнителни услуги (checkboxes)
4. JavaScript `recalculatePrice()` сумира избраните такси и попълва полето `#exam-price` (read-only)
5. Крайната цена се изпраща в `ExaminationRequest.price`

**Само ADMIN може да промени таксата:**
```java
// ExaminationFeeController.java:
@PutMapping("/api/admin/examination-fees/{specialty}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ExaminationFeeResponse> updateFee(...) { ... }
```

**Покажи в кода:**
- Файл: `ExaminationFee.java` → `@Column(name = "specialty", unique = true)` — 1 ред на специалност
- Файл: `ExaminationFeeServiceImpl.java` → метод `getAvailableFees()` — логиката кои такси вижда лекарят
- Файл: `doctor/dashboard.html` → поле `#exam-price` с атрибут `readonly` — лекарят не може да промени ръчно

---

## 41. Какво е `ExaminationFee` и как е моделирана таксата за преглед?

**Кратък отговор:** `ExaminationFee` е entity с таблица `examination_fees`, в която всяка специалност има **точно един ред** с базова такса. Уникалността е наложена на ниво база данни с `unique = true` на колоната `specialty`.

**Детайлно обяснение:**

```java
// ExaminationFee.java
@Entity
@Table(name = "examination_fees")
public class ExaminationFee {

    @Enumerated(EnumType.STRING)
    @Column(name = "specialty", unique = true, nullable = false)
    private Specialty specialty;

    @NotNull(message = "Базовата такса е задължителна")
    @DecimalMin(value = "0.01", message = "Таксата трябва да бъде положително число")
    @Column(name = "base_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFee;
}
```

**Три важни решения в дизайна:**

**1. `unique = true` на `specialty`** — не може да има два реда за `CARDIOLOGIST`. Ако администраторът се опита да вмъкне втора такса за кардиолог, MySQL хвърля `DataIntegrityViolationException`, което `GlobalExceptionHandler` връща като 409 Conflict.

**2. `@Enumerated(EnumType.STRING)`** — в базата се записва `"GP"`, `"CARDIOLOGIST"`, не числото на enum стойността. Ако утре добавим нова специалност или разместим enum-а, съществуващите редове в БД остават валидни. При `EnumType.ORDINAL` (числото) разместването би счупило данните.

**3. `BigDecimal` с `precision = 10, scale = 2`** — правилният тип за парични суми. `double` и `float` имат проблеми с точността при финансови изчисления (`0.1 + 0.2 != 0.3`). `BigDecimal` гарантира точни аритметични операции.

**Начални такси (заредени от `DataInitializer`):**

| Специалност | Такса |
|---|---|
| GP (ОПЛ) | 20.00 лв. |
| Кардиолог | 60.00 лв. |
| Невролог | 70.00 лв. |
| Дерматолог | 50.00 лв. |
| Ортопед | 60.00 лв. |
| Педиатър | 30.00 лв. |
| Психиатър | 80.00 лв. |
| Хирург | 100.00 лв. |
| Уролог | 65.00 лв. |
| Онколог | 90.00 лв. |

**Покажи в кода:**
- Файл: `ExaminationFee.java` → `@Column(name = "specialty", unique = true)` — ключовото ограничение
- Файл: `DataInitializer.java` → `insertSeedData()` → секция „Такси за прегледи" — `Map<Specialty, BigDecimal>` и `fees.forEach(...)`

---

## 42. Как работи пълната система за таксуване — Repository, Service, Controller?

**Кратък отговор:** `ExaminationFeeRepository` дава методи за намиране по специалност. `ExaminationFeeServiceImpl` изгражда персонализиран списък с опции за всеки лекар. `ExaminationFeeController` излага три ендпойнта — за admin, за всички и само за текущия лекар.

**`ExaminationFeeRepository.java`:**

```java
// ExaminationFeeRepository.java
Optional<ExaminationFee> findBySpecialty(Specialty specialty);
boolean existsBySpecialty(Specialty specialty);
```

Само два метода над стандартните от `JpaRepository<ExaminationFee, Long>`:
- `findBySpecialty` → `Optional` защото е допустимо таксата за дадена специалност все още да не е добавена
- `existsBySpecialty` → използва се в `DataInitializer` за idempotent seed: ако таксата вече съществува, не я дублира

**`ExaminationFeeServiceImpl.java` — методът `getAvailableFees()`:**

Това е най-интересната бизнес логика в таксуването. Лекарят не вижда всички 10 такси — само тези, с които може да работи:

```java
// ExaminationFeeServiceImpl.java → getAvailableFees()
public List<FeeOptionResponse> getAvailableFees(String doctorUsername) {
    Doctor doctor = doctorRepository.findByUser_Username(doctorUsername)...

    List<FeeOptionResponse> options = new ArrayList<>();

    // Стъпка 1: Специалностите на лекаря
    doctor.getSpecialties().forEach(specialty ->
        examinationFeeRepository.findBySpecialty(specialty).ifPresent(f -> {
            opt.setLabel(f.getSpecialty().getLabel() + " (консултация)");
            opt.setGroup("SPECIALTY");
            options.add(opt);
        })
    );

    // Стъпка 2: GP такса — само ако canBeGP=true И specialty != GP
    if (doctor.isCanBeGP() && !doctor.getSpecialties().contains(Specialty.GP)) {
        examinationFeeRepository.findBySpecialty(Specialty.GP).ifPresent(f -> {
            opt.setLabel("ОПЛ (консултация)");  // label = "ОПЛ"
            opt.setGroup("SPECIALTY");
            options.add(opt);
        });
    }

    // Стъпка 3: Всички AdditionalService (ЕКГ, кръвна проба и др.)
    additionalServiceRepository.findAll().forEach(s -> {
        opt.setGroup("ADDITIONAL");
        options.add(opt);
    });

    return options;
}
```

**Конкретни примери:**
- **Д-р Петров** (GP, `canBeGP=true`): Стъпка 1 → GP такса (20 лв.). Стъпка 2 → пропуска се, защото `specialties.contains(GP)` е `true`. Стъпка 3 → 4 допълнителни услуги.
- **Д-р Иванова** (CARDIOLOGIST, `canBeGP=true`): Стъпка 1 → Кардиолог (60 лв.). Стъпка 2 → добавя GP (20 лв.) защото `canBeGP=true` и не е GP. Стъпка 3 → 4 допълнителни.
- **Д-р Георгиев** (NEUROLOGIST, `canBeGP=false`): Стъпка 1 → Невролог (70 лв.). Стъпка 2 → пропуска се (`canBeGP=false`). Стъпка 3 → 4 допълнителни.

**`ExaminationFeeController.java` — три ендпойнта:**

```java
// ExaminationFeeController.java

// 1. Всички такси — за admin панела
@GetMapping("/api/examination-fees")
@PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
public ResponseEntity<List<ExaminationFeeResponse>> getAllFees() { ... }

// 2. Промяна на такса — само ADMIN
@PutMapping("/api/admin/examination-fees/{specialty}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ExaminationFeeResponse> updateFee(
        @PathVariable String specialty,
        @RequestBody Map<String, BigDecimal> body) {
    Specialty spec = Specialty.valueOf(specialty.toUpperCase());
    BigDecimal baseFee = body.get("baseFee");
    ...
}

// 3. Опциите само за текущия лекар
@GetMapping("/api/examination-fees/my-options")
@PreAuthorize("hasRole('DOCTOR')")
public ResponseEntity<List<FeeOptionResponse>> getAvailableFees(Authentication authentication) {
    return ResponseEntity.ok(examinationFeeService.getAvailableFees(authentication.getName()));
}
```

**Особеност при `updateFee`:** Path variable-ът `{specialty}` е низ (напр. `"CARDIOLOGIST"`). В контролера се прави `Specialty.valueOf(specialty.toUpperCase())` — ако низът не съответства на никоя enum стойност, хвърля `IllegalArgumentException` (Java стандартно), което `GlobalExceptionHandler` превръща в 400. Тялото е `Map<String, BigDecimal>` с ключ `"baseFee"` — лесна JSON структура: `{ "baseFee": 75.00 }`.

**`FeeOptionResponse` и `ExaminationFeeResponse` — два различни DTO-та:**

```java
// ExaminationFeeResponse.java — за admin панела
{ specialty: "CARDIOLOGIST", specialtyLabel: "Кардиолог", baseFee: 60.00 }

// FeeOptionResponse.java — за dropdown-а при лекаря
{ label: "Кардиолог (консултация)", fee: 60.00, group: "SPECIALTY" }
{ label: "ЕКГ", fee: 25.00, group: "ADDITIONAL" }
```

`group` полето позволява на JavaScript да различи основна такса от допълнителна при изчисляване на крайната цена.

**Покажи в кода:**
- Файл: `ExaminationFeeRepository.java` → `findBySpecialty()` и `existsBySpecialty()`
- Файл: `ExaminationFeeServiceImpl.java` → метод `getAvailableFees()` — трите стъпки
- Файл: `ExaminationFeeController.java` → метод `updateFee()` — `Specialty.valueOf()` конверсия

---

## 43. Какво е `AdditionalService` и защо е отделен entity?

**Кратък отговор:** `AdditionalService` представлява допълнителна медицинска услуга (вземане на кръв, ЕКГ, превръзка), която не е обвързана с конкретна специалност и може да се добавя към всеки преглед. Отделен entity е защото няма enum — услугите са гъвкав списък с произволни имена.

**`AdditionalService.java`:**

```java
// AdditionalService.java
@Entity
@Table(name = "additional_services")
public class AdditionalService {

    @Column(name = "name", unique = true, nullable = false)
    private String name;    // напр. "ЕКГ", "Вземане на кръвна проба"

    @DecimalMin(value = "0.01")
    @Column(name = "fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal fee;
}
```

**Разлика от `ExaminationFee`:**

| | `ExaminationFee` | `AdditionalService` |
|---|---|---|
| Ключ | `Specialty` enum | `String` name |
| Брой редове | Точно 10 (по 1 на специалност) | Произволен брой |
| Може да се добави нов тип | Само ако добавим нова enum стойност | Просто нов ред в таблицата |
| `unique` ограничение | На `specialty` колоната | На `name` колоната |

**Seed данните:**
```java
// DataInitializer.java → seedAdditionalServices()
if (additionalServiceRepository.count() > 0) return;  // Idempotent!
"Вземане на кръвна проба"      → 15.00 лв.
"Изпращане на материали за анализ" → 20.00 лв.
"ЕКГ"                          → 25.00 лв.
"Превръзка / манипулация"      → 10.00 лв.
```

**`AdditionalServiceRepository.java`:**

```java
// AdditionalServiceRepository.java
public interface AdditionalServiceRepository extends JpaRepository<AdditionalService, Long> {
    boolean existsByName(String name);
}
```

Само `existsByName` — използва се в `DataInitializer` преди запис, за да не дублира услуги. `findAll()` (от `JpaRepository`) е достатъчен за четене — `ExaminationFeeServiceImpl.getAvailableFees()` го използва директно.

**Важна особеност — няма `AdditionalServiceController`:** Допълнителните услуги не могат да се управляват от UI. Те се вмъкват само от `DataInitializer` и се четат от `getAvailableFees()`. Ако се налага управление, ще трябва нов контролер — това е умишлено ограничение на обхвата на проекта.

**Покажи в кода:**
- Файл: `AdditionalService.java` → сравнете `name` (String) спрямо `ExaminationFee` `specialty` (Enum)
- Файл: `AdditionalServiceRepository.java` → `existsByName()` — само един метод
- Файл: `DataInitializer.java` → `seedAdditionalServices()` → `count() > 0` проверката

---


### DATAINITIALIZER

## 29. Как работи `DataInitializer` и защо проверява дали данните вече съществуват?

**Кратък отговор:** `DataInitializer` имплементира `CommandLineRunner` — Spring го извиква автоматично след зареждане на контекста. Проверката `userRepository.count() == 0` предотвратява дублиране при всяко рестартиране на приложението.

**Детайлно обяснение:**

```java
// DataInitializer.java → run()
@Component
public class DataInitializer implements CommandLineRunner {
    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() == 0) {    // Само ако БД е празна
            insertSeedData();
        }
        seedDiagnosisSpecialties();   // Изпълнява се всеки път (има собствена проверка)
        seedDoctorSpecialties();      // Поправя specialty след migration
        seedAdditionalServices();     // Само ако count() == 0
    }
}
```

**Защо е нужна проверката:**
`application.properties` има `ddl-auto=update` — при второ стартиране таблиците **не** се изтриват, те вече съдържат данни. Без `count() == 0` проверката, `insertSeedData()` ще се опита да запише `admin@medical.com` отново → `DataIntegrityViolationException` при UNIQUE нарушение на `username` колоната.

**Редът на вмъкване (важен заради Foreign Key constraints):**
```
1. Users (admin, 3 лекари, 5 пациенти)   → users таблицата
2. Doctors (с user_id → FK към users)     → нужда users да съществуват
3. Patients (с user_id + personal_doctor_id) → нужда doctors да съществуват
4. Diagnoses                              → независими
5. Examinations (doctor_id + patient_id + diagnosis_id)
6. SickLeaves (examination_id)            → нужда examinations да съществуват
7. ExaminationFees                        → независими
```

**`seedDiagnosisSpecialties()` — изпълнява се безопасно всеки път:**
```java
// DataInitializer.java → assignSpecialties()
private void assignSpecialties(String code, Specialty... specialties) {
    diagnosisRepository.findByCode(code).ifPresent(d -> {
        if (d.getSpecialties().addAll(Arrays.asList(specialties))) {
            // addAll() връща true само ако е добавено нещо ново
            diagnosisRepository.save(d);
        }
    });
}
```
`Set.addAll()` игнорира дублиращи се стойности — ако специалността вече е добавена, тя не се добавя отново и `save()` не се извиква.

**Покажи в кода:**
- Файл: `DataInitializer.java` → метод `run()` — вижте `if (userRepository.count() == 0)`
- Файл: `DataInitializer.java` → метод `insertSeedData()` → редът User → Doctor → Patient → Diagnosis → Examination → SickLeave

---

## 46. Как работи `DataInitializer` — пълно обяснение?

**Кратък отговор:** `DataInitializer` имплементира `CommandLineRunner` — Spring го извиква автоматично след стартиране на контекста. Зарежда начални данни ако базата е празна: 1 admin, 3 лекари, 5 пациенти, 10 диагнози, 4 прегледа, 2 болнични, 10 такси, 4 допълнителни услуги.

**Как Spring извиква `DataInitializer`:**

```java
// DataInitializer.java
@Component
public class DataInitializer implements CommandLineRunner {
    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() == 0) {
            insertSeedData();
        }
        seedDiagnosisSpecialties();
        seedDoctorSpecialties();
        seedAdditionalServices();
    }
}
```

`CommandLineRunner` е Spring Boot интерфейс с един метод `run()`. Spring открива всички `@Component` класове, имплементиращи `CommandLineRunner`, и ги извиква по ред след зареждане на application context-а. Не е нужна никаква допълнителна конфигурация.

**Защо `@Transactional` на `run()`:**
`insertSeedData()` вмъква 30+ обекта. Ако нещо се провали по средата (напр. нарушение на FK constraint), `@Transactional` гарантира rollback на **всичко** — няма да останат частично заредени данни.

**Защо `userRepository.count() == 0` проверката:**

`application.properties` има `spring.jpa.hibernate.ddl-auto=update` — при второ стартиране таблиците **не** се изтриват, само се ъпдейтват схемата. Без проверката `insertSeedData()` ще опита да вмъкне `admin@medical.com` отново → MySQL хвърля UNIQUE нарушение на `username` → `DataIntegrityViolationException`.

**Редът на вмъкване (важен заради FK constraints):**

```
1. adminUser → users таблицата
2. userPetrov, userIvanova, userGeorgiev → users (лекарски потребители)
3. doctorPetrov, doctorIvanova, doctorGeorgiev → doctors (FK → users)
4. userKolev, userTodorova, ... → users (пациентски потребители)
5. patientKolev, ... → patients (FK → users + FK → doctors)
6. diagZ00, diagJ06, ... → diagnoses (независими)
7. exam1, exam2, ... → examinations (FK → doctors + patients + diagnoses)
8. sickLeave1, sickLeave2 → sick_leaves (FK → examinations)
9. ExaminationFee × 10 → examination_fees (независими)
```

Ако вмъкнем `patientKolev` преди `doctorPetrov`, FK constraint `personal_doctor_id` ще гърми — лекарят не съществува все още.

**Трите seed лекари — умишлено разнообразие:**

```java
// DataInitializer.java → insertSeedData() → лекари
Doctor doctorPetrov = Doctor.builder()
    .firstName("Димитър").lastName("Петров")
    .specialties(Set.of(Specialty.GP))
    .canBeGP(true)   // ОПЛ
    .build();

Doctor doctorIvanova = Doctor.builder()
    .firstName("Мария").lastName("Иванова")
    .specialties(Set.of(Specialty.CARDIOLOGIST))
    .canBeGP(true)   // Кардиолог, но може и като ОПЛ
    .build();

Doctor doctorGeorgiev = Doctor.builder()
    .firstName("Георги").lastName("Георгиев")
    .specialties(Set.of(Specialty.NEUROLOGIST))
    .canBeGP(false)  // Само невролог — не може като ОПЛ
    .build();
```

Д-р Иванова е кардиолог с `canBeGP=true` — умишлено за да може да се тества `getAvailableFees()` логиката (получава и GP, и Кардиолог такса). Д-р Георгиев е с `canBeGP=false` — тества правилото за личен лекар.

**`paidByPatient` — изчислено от здравно осигуряване:**

```java
// DataInitializer.java → insertSeedData() → прегледи
Examination exam1 = Examination.builder()
    .patient(patientKolev)          // Осигурен
    .paidByPatient(!patientKolev.isHealthInsured())  // = !true = false
    .build();

Examination exam2 = Examination.builder()
    .patient(patientTodorova)       // Неосигурена
    .paidByPatient(!patientTodorova.isHealthInsured())  // = !false = true
    .build();
```

Seed данните следват същата логика като реалното приложение — осигурените пациенти не плащат, неосигурените плащат.

**Четири метода, изпълнявани при всяко стартиране (idempotent):**

```java
// DataInitializer.java → run()
seedDiagnosisSpecialties();  // Set.addAll() → игнорира дублиращи се специалности
seedDoctorSpecialties();     // Set.addAll() → поправя специалности на seed лекарите
seedAdditionalServices();    // count() > 0 → пропуска ако вече са заредени
```

`seedDiagnosisSpecialties()` и `seedDoctorSpecialties()` използват `Set.addAll()` — ако специалността вече е в Set-а, `addAll()` връща `false` и `save()` не се извиква. Безопасно при многократно стартиране.

**`assignSpecialties()` — помощният метод:**

```java
// DataInitializer.java → assignSpecialties()
private void assignSpecialties(String code, Specialty... specialties) {
    diagnosisRepository.findByCode(code).ifPresent(d -> {
        if (d.getSpecialties().addAll(Arrays.asList(specialties))) {
            // addAll() връща true само ако е добавено нещо ново
            diagnosisRepository.save(d);
        }
    });
}
```

`ifPresent()` — ако диагнозата с даден код не съществува, методът тихо пропуска. `addAll()` на `Set` игнорира дублиращи се стойности и връща `true` само ако нещо е добавено → `save()` се извиква само когато има реална промяна.

**Покажи в кода:**
- Файл: `DataInitializer.java` → `run()` — четирите метода и `if (userRepository.count() == 0)`
- Файл: `DataInitializer.java` → `insertSeedData()` → редът на вмъкване User → Doctor → Patient → Diagnosis → Examination → SickLeave → ExaminationFee
- Файл: `DataInitializer.java` → `assignSpecialties()` → `Set.addAll()` идемпотентна логика
6. Абстрактен `BaseIntegrationTest` без `@SpringBootTest` → не може да се стартира като самостоятелен тест

---

### ТЕСТОВЕ

## 17. Покажи ми един интеграционен тест от контролера до базата. Към каква база е този тест?

> **Въпрос на преподавателя:** „Покажи ми един интегрейшън тест от контролера до базата, към каква база е този тест? Към каква база е този тест, покажи ми базата към която е теста?"

**Файл:** `BaseIntegrationTest.java`, `ExaminationControllerIntegrationTest.java`

Тестовете са **реални интеграционни тестове** — стартират целия Spring Boot контекст и използват **реална MySQL база данни** (`medical_record_db_test`).

**Към каква база:** `application-test.properties` конфигурира отделна тестова база:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/medical_record_db_test
spring.jpa.hibernate.ddl-auto=create-drop   # ← изтрива и пресъздава при всяко стартиране
```
Тестовата база е **изолирана** от production базата `medical_record_db`. `create-drop` означава, че при стартиране на тестовете таблиците се изтриват и пресъздават — чист старт.

**Архитектурата:**
```
BaseIntegrationTest (abstract)
    ├── ExaminationControllerIntegrationTest (@SpringBootTest + @AutoConfigureMockMvc)
    ├── SickLeaveControllerIntegrationTest (@SpringBootTest + @AutoConfigureMockMvc)
    └── AuthControllerIntegrationTest (@SpringBootTest + @AutoConfigureMockMvc)
```

`@SpringBootTest(webEnvironment = MOCK)` + `@AutoConfigureMockMvc` са само на **конкретните** класове — не на `BaseIntegrationTest`. Причината: ако абстрактният клас имаше анотациите, IntelliJ щеше да се опита да го стартира директно и щеше да се провали.

**Изолация на тестовете:**
- `@BeforeEach` — зарежда seed обекти от базата (лекари, пациенти, диагнози)
- `@AfterEach` — изтрива създадени от теста записи

**Seed данните** се добавят при стартиране на контекста от `DataInitializer` (само ако БД е празна). Тестовете разчитат на тези seed данни:
```java
// ExaminationControllerIntegrationTest.java → loadSeedData() (@BeforeEach)
doctorPetrov = doctorRepository.findByUser_Username("d.petrov@medical.com").orElseThrow();
```

**Пример за тест:**
```java
// ExaminationControllerIntegrationTest.java
@Test
void createExamination_asDoctorTodayDateInsuredPatient_returns201AndPaidByPatientFalse() throws Exception {
    String token = getToken("d.petrov@medical.com", "Az1234!");
    // getToken() е от BaseIntegrationTest.java — извиква реалния /api/auth/login endpoint

    mockMvc.perform(post("/api/examinations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", bearer(token))
                    .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paidByPatient").value(false));
}
```

---

## 20. Покажи ми репозитори тест.

> **Въпрос на преподавателя:** „Покажи ми репозитори тест."

**Важна бележка:** Проектът **няма отделни repository тестове** — вместо това има unit тестове за сервизния слой с mock-нати репозиторий-та, и интеграционни тестове, които достигат реалната БД (и тестват репозиторийте имплицитно).

**Файл:** `src/test/java/com/medicalrecord/service/` — unit тестове за услугите

Проектът има **unit тестове за сервизния слой** с Mockito (репозиторийте са mock-нати):

```
ExaminationServiceTest   (4 теста)
SickLeaveServiceTest     (5 теста)
PatientServiceTest       (4 теста)
StatisticsServiceTest    (3 теста)
GlobalExceptionHandlerTest (4 теста)
```

**Как работят unit тестовете:**
- Репозиторийте са `@Mock` — не се свързват с реална база данни
- Само сервизът е реален (`@InjectMocks`)
- Тестовете проверяват **бизнес логиката** — дали изключенията се хвърлят при правилните условия

```java
// ExaminationServiceTest.java
@ExtendWith(MockitoExtension.class)
class ExaminationServiceTest {
    @Mock private ExaminationRepository examinationRepository;
    @Mock private DoctorRepository doctorRepository;
    @InjectMocks private ExaminationServiceImpl examinationService;

    @Test
    void deleteExamination_asDoctorForPastExam_throwsException() {
        // Arrange
        Examination pastExam = Examination.builder()
                .examinationDate(LocalDate.now().minusDays(3))
                .doctor(doctor).build();
        when(examinationRepository.findById(1L)).thenReturn(Optional.of(pastExam));
        when(userRepository.findByUsername("d.petrov@medical.com")).thenReturn(Optional.of(doctorUser));

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> examinationService.deleteExamination(1L, "d.petrov@medical.com"));
    }
}
```

**Разликата между unit тестовете и интеграционните:**
- **Unit тестове** — бързи, изолирани, тестват само логиката на сервиза без реална БД
- **Интеграционни тестове** — бавни, тестват целия стек (контролер → сервиз → БД), проверяват HTTP статус кодове

---


### АРХИТЕКТУРА

## 40. Ако трябва да обясниш архитектурата на проекта с 3 изречения, как би го направил?

**Кратък отговор:**

MedicalRecordSystem е уеб приложение за управление на електронни медицински досиета, изградено с **Java 21 + Spring Boot 3.3.5** като бекенд REST API и **Thymeleaf + vanilla JavaScript** като фронтенд, с **MySQL** база данни.

Сигурността е базирана на **JWT stateless автентикация** — при вход сървърът издава токен, валиден 24 часа, и всяка следваща заявка носи токена в `Authorization: Bearer` хедъра; сесии на сървъра не се пазят.

Системата поддържа три роли: **ADMIN** управлява всички потребители, лекари, пациенти, диагнози и такси; **DOCTOR** вижда всички прегледи и може да създава/редактира само свои, а пациентите на своята практика; **PATIENT** вижда само своята медицинска история.

---

**Разширен вариант (ако имате повече от 3 изречения):**

**Технологичен стек:**
- Java 21 + Spring Boot 3.3.5 (Web, Security, Data JPA, Validation, Thymeleaf)
- MySQL — production (`medical_record_db`), тестове (`medical_record_db_test`)
- JJWT 0.12.3 за JWT генерация и валидация
- Lombok за намаляване на boilerplate
- JUnit 5 + Mockito за unit тестове, MockMvc за интеграционни

**Слоева архитектура:**
```
Controller (@RestController / @Controller)
    ↓  @Valid → Bean Validation
Service (@Service, @Transactional)
    ↓  бизнес логика, ownership checks
Repository (JpaRepository)
    ↓  Hibernate → MySQL
```

**Ключови дизайн решения:**
1. JWT stateless (не session-based) → мащабируемост
2. DTO pattern (не entity директно) → сигурност, без circular references
3. Ръчни mapper-и (не MapStruct) → прозрачност
4. `@ElementCollection` за специалности (не отделен entity) → простота
5. `GlobalExceptionHandler` централизиран → консистентни JSON грешки

---


*Документът е генериран за подготовка на университетска презентация на проект MedicalRecordSystem.*
