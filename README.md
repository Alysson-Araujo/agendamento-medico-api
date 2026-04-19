# Medical Scheduling API

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

API REST completa para gerenciamento de agendamentos médicos em clínicas, com autenticação JWT, documentação Swagger, testes automatizados e containerização Docker.

---

## 📋 Sobre o Projeto

A **Medical Scheduling API** é um sistema backend para clínicas médicas gerenciarem médicos, pacientes e consultas. O sistema implementa regras de negócio reais, segurança com JWT (Access + Refresh Token), documentação interativa via Swagger e testes unitários e de integração.

### Atores do Sistema

| Ator | Role | Permissões |
|---|---|---|
| Admin | `ADMIN` | Gerencia médicos, pacientes e visualiza tudo |
| Médico | `DOCTOR` | Visualiza sua própria agenda e consultas |
| Paciente | `PATIENT` | Agenda, cancela e visualiza suas próprias consultas |

---

## 🛠️ Tecnologias Utilizadas

- **Java 17** — Linguagem de programação
- **Spring Boot 3.3.5** — Framework principal
- **Spring Security 6 + JWT** — Autenticação e autorização
- **Spring Data JPA + Hibernate** — Persistência de dados
- **PostgreSQL 16** — Banco de dados relacional
- **Flyway** — Controle de migrations do banco
- **SpringDoc OpenAPI (Swagger UI)** — Documentação interativa
- **JUnit 5 + Mockito** — Testes unitários
- **Testcontainers** — Testes de integração com PostgreSQL real
- **Docker + Docker Compose** — Containerização
- **Maven** — Gerenciamento de build e dependências
- **Bean Validation (Jakarta)** — Validação de dados de entrada

---

## 🏗️ Arquitetura de Pacotes

```
src/main/java/com/medicalscheduling
├── config/                 # Configurações (Security, JWT, Swagger)
│   ├── SecurityConfig.java
│   ├── OpenApiConfig.java
│   └── JwtConfig.java
├── controller/             # REST Controllers
│   ├── AuthController.java
│   ├── DoctorController.java
│   ├── PatientController.java
│   └── AppointmentController.java
├── service/                # Lógica de negócio
│   ├── AuthService.java
│   ├── DoctorService.java
│   ├── PatientService.java
│   ├── AppointmentService.java
│   └── TokenService.java
├── repository/             # Acesso a dados (Spring Data JPA)
│   ├── UserRepository.java
│   ├── DoctorRepository.java
│   ├── PatientRepository.java
│   └── AppointmentRepository.java
├── domain/                 # Entidades JPA e Enums
│   ├── User.java
│   ├── Doctor.java
│   ├── Patient.java
│   ├── Appointment.java
│   ├── Specialty.java (enum)
│   └── AppointmentStatus.java (enum)
├── dto/                    # Data Transfer Objects
│   ├── request/
│   └── response/
├── exception/              # Tratamento global de erros
│   ├── GlobalExceptionHandler.java
│   ├── BusinessException.java
│   ├── ResourceNotFoundException.java
│   └── ErrorResponse.java
└── validator/              # Validadores de regras de negócio
    └── AppointmentValidator.java
```

---

## 🗄️ Modelagem do Banco de Dados

```
┌──────────────┐       ┌──────────────┐
│    users     │       │   doctors    │
├──────────────┤       ├──────────────┤
│ id (PK,UUID) │◄──┐   │ id (PK,UUID) │
│ name         │   └───│ user_id (FK) │
│ email (UQ)   │       │ crm (UQ)     │
│ password     │       │ specialty    │
│ role         │       │ phone        │
│ active       │       │ active       │
│ created_at   │       └──────┬───────┘
└──────┬───────┘              │
       │                      │ 1:N
       │       ┌──────────────┴───────┐
       │       │    appointments      │
       │       ├──────────────────────┤
       │       │ id (PK, UUID)        │
       │       │ doctor_id (FK)       │
       │       │ patient_id (FK)      │
       │       │ date_time            │
       │       │ status               │
       │       │ reason               │
       │       │ cancel_reason        │
       │       │ created_at           │
       │       └──────────────┬───────┘
       │                      │ 1:N
┌──────┴───────┐              │
│   patients   │──────────────┘
├──────────────┤
│ id (PK,UUID) │
│ user_id (FK) │
│ cpf (UQ)     │
│ phone        │
│ birth_date   │
│ active       │
└──────────────┘
```

### Relacionamentos
- `users` → `doctors` (1:1)
- `users` → `patients` (1:1)
- `doctors` → `appointments` (1:N)
- `patients` → `appointments` (1:N)

---

## ⚙️ Regras de Negócio

### Agendamento
- ❌ Não permitir agendamento com médico inativo
- ❌ Não permitir agendamento com paciente inativo
- ❌ Médico não pode ter dois agendamentos no mesmo horário
- ❌ Paciente não pode ter dois agendamentos no mesmo dia
- ❌ Não agendar fora do horário de funcionamento (seg–sáb, 07h–19h)
- ❌ Não agendar com menos de 30 minutos de antecedência

### Cancelamento
- ❌ Não cancelar com menos de 24 horas de antecedência
- ✅ Campo `cancel_reason` é obrigatório
- ❌ Não cancelar consulta já concluída
- ❌ Paciente só pode cancelar sua própria consulta

### Conclusão
- ❌ Só `SCHEDULED` pode ser marcada como `COMPLETED`
- ✅ Apenas `ADMIN` ou `DOCTOR` dono da consulta pode concluir

---

## 🚀 Como Executar com Docker

### Pré-requisitos
- Docker e Docker Compose instalados

### Passo a passo

```bash
# 1. Clone o repositório
git clone https://github.com/Alysson-Araujo/agendamento-medico-api.git
cd agendamento-medico-api

# 2. Execute com Docker Compose
docker compose up --build -d

# 3. Acesse a API
# API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html

# 4. Parar os containers
docker compose down
```

### Credenciais do Admin padrão
- **Email:** `admin@medical.com`
- **Senha:** `admin123`

---

## 🧪 Como Executar os Testes

```bash
# Testes unitários
mvn test -Dtest="TokenServiceTest,AppointmentServiceTest"

# Testes de integração (requer Docker rodando para Testcontainers)
mvn test -Dtest="AppointmentControllerIT"

# Todos os testes
mvn test
```

---

## 🔗 Endpoints Principais

### Auth (público)
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/auth/register` | Cadastro de novo paciente |
| POST | `/api/auth/login` | Login (retorna tokens JWT) |
| POST | `/api/auth/refresh` | Renovar access token |

### Doctors
| Método | Endpoint | Permissão | Descrição |
|--------|----------|-----------|-----------|
| POST | `/api/doctors` | ADMIN | Cadastrar médico |
| GET | `/api/doctors` | Autenticado | Listar médicos ativos |
| GET | `/api/doctors/{id}` | ADMIN/DOCTOR | Detalhar médico |
| GET | `/api/doctors/{id}/agenda` | ADMIN/DOCTOR | Agenda do médico |
| PUT | `/api/doctors/{id}` | ADMIN | Atualizar médico |
| DELETE | `/api/doctors/{id}` | ADMIN | Inativar médico |

### Patients
| Método | Endpoint | Permissão | Descrição |
|--------|----------|-----------|-----------|
| POST | `/api/patients` | ADMIN | Cadastrar paciente |
| GET | `/api/patients` | ADMIN | Listar pacientes |
| GET | `/api/patients/{id}` | ADMIN/PATIENT | Detalhar paciente |
| PUT | `/api/patients/{id}` | ADMIN/PATIENT | Atualizar paciente |
| DELETE | `/api/patients/{id}` | ADMIN | Inativar paciente |

### Appointments
| Método | Endpoint | Permissão | Descrição |
|--------|----------|-----------|-----------|
| POST | `/api/appointments` | PATIENT | Agendar consulta |
| GET | `/api/appointments` | ADMIN | Listar todas |
| GET | `/api/appointments/my` | PATIENT | Minhas consultas |
| GET | `/api/appointments/{id}` | Autenticado | Detalhar consulta |
| PATCH | `/api/appointments/{id}/cancel` | PATIENT/ADMIN | Cancelar |
| PATCH | `/api/appointments/{id}/complete` | ADMIN/DOCTOR | Concluir |

📖 **Documentação completa:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 📐 Decisões Técnicas

| Decisão | Motivo |
|---|---|
| UUID como PK | Evita exposição de IDs sequenciais, mais seguro em APIs públicas |
| Soft Delete (`active`) | Preserva histórico — consultas antigas não ficam órfãs |
| `cancel_reason` nullable | Só preenchido quando `status = CANCELLED` |
| `role` na tabela `users` | Simplifica autenticação sem tabela extra de roles |
| `crm` e `cpf` UNIQUE | Integridade de negócio no banco |
| Índices em FKs de appointments | Performance nas validações de conflito de horário |
| JWT Access + Refresh Token | Segurança sem estado no servidor, com renovação transparente |
| Testcontainers | Testes de integração com PostgreSQL real, sem mocks de banco |
| Flyway Migrations | Controle versionado e reproduzível do schema do banco |
| `@ControllerAdvice` global | Tratamento uniforme de erros em toda a API |

---

## 👤 Autor

Desenvolvido por **Alysson Araújo**

---

## 📄 Licença

Este projeto está sob a licença MIT.
