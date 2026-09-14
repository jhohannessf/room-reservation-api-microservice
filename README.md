# Room Reservation API — Microservices

Evolução do projeto [room-reservation-api](https://github.com/jhohannessf/room-reservation-api) (originalmente um monolito) para uma **arquitetura de microsserviços**, com **Service Discovery (Eureka)**, **API Gateway**, **configuração centralizada (Spring Cloud Config)**, **resiliência com Circuit Breaker**, comunicação síncrona via **OpenFeign**, autenticação **stateless com JWT**, **login social (Google e GitHub)** e **autenticação de dois fatores (2FA)** por e-mail ou aplicativo autenticador (TOTP).

> 📌 Este repositório é a continuação do projeto monolítico citado acima. Aqui a mesma regra de negócio (gestão de usuários, salas e reservas) foi redesenhada como um sistema distribuído.

## Sumário

- [Arquitetura](#arquitetura)
- [Stack técnica](#stack-técnica)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Segurança](#segurança)
- [Resiliência](#resiliência)
- [Serviços e endpoints](#serviços-e-endpoints)
- [Modelagem de dados](#modelagem-de-dados)
- [Como rodar o projeto](#como-rodar-o-projeto)
- [Roadmap](#roadmap)

## Arquitetura

O sistema é composto por 6 aplicações Spring Boot: três microsserviços de negócio (cada um com seu próprio banco de dados, padrão *Database per Service*), um servidor de descoberta de serviços, um servidor de configuração centralizada e um API Gateway como porta de entrada única.

| Serviço | Responsabilidade | Porta |
|---|---|---|
| `server-ms` | Eureka Server — registro e descoberta de serviços | `8761` |
| `config-server-ms` | Spring Cloud Config Server — configuração centralizada dos serviços de negócio | `8888` |
| `gateway-ms` | API Gateway — ponto único de entrada, roteamento dinâmico via Eureka | `8080` |
| `user-ms` | Cadastro de usuários, autenticação, login social, 2FA | `8081` |
| `room-ms` | Cadastro e controle de status das salas | `8082` |
| `booking-ms` | Regras de reserva, orquestração via Feign, resiliência com Circuit Breaker | `8083` |

```mermaid
flowchart TB
    Client[Cliente / Frontend]
    Server[server-ms<br/>Eureka Server :8761]
    Config[config-server-ms<br/>Config Server :8888]
    Gateway[gateway-ms<br/>API Gateway :8080]

    subgraph user-ms[user-ms :8081]
        UC[AuthController / UsuarioController]
    end

    subgraph room-ms[room-ms :8082]
        RC[SalaController]
    end

    subgraph booking-ms[booking-ms :8083]
        BC[ReservaController]
        BS[ReservaService]
    end

    Client -->|JWT Bearer| Gateway
    Gateway -->|/user-ms/**| UC
    Gateway -->|/room-ms/**| RC
    Gateway -->|/booking-ms/**| BC

    user-ms -.registra/descobre.- Server
    room-ms -.registra/descobre.- Server
    booking-ms -.registra/descobre.- Server
    Gateway -.consulta.- Server

    user-ms -.busca config.- Config
    room-ms -.busca config.- Config
    booking-ms -.busca config.- Config

    BS -->|Feign via Eureka: valida usuário| UC
    BS -->|"Feign via Eureka: valida/atualiza sala (com Circuit Breaker)"| RC
```

Como funciona na prática:

- **`server-ms`** é o Eureka Server: cada um dos outros serviços se registra nele ao subir, e é através dele que se descobrem uns aos outros (em vez de URLs fixas).
- **`config-server-ms`** centraliza o `application.properties` de `user-ms`, `room-ms` e `booking-ms` — em vez de cada um carregar sua própria configuração local, eles importam de `http://localhost:8888` na inicialização (`spring.config.import=optional:configserver:...`). Os arquivos de configuração reais ficam empacotados no classpath do próprio Config Server, em `config-server-ms/src/main/resources/config-repo/` — uma escolha deliberada para não depender de um caminho absoluto no disco (ver [Roadmap](#roadmap) para o histórico dessa decisão).
- **`gateway-ms`** é o único serviço com porta exposta que faz sentido o cliente conhecer. Ele recebe a requisição, olha o prefixo do path (`/user-ms/**`, `/room-ms/**` ou `/booking-ms/**`), remove esse prefixo (`StripPrefix=1`) e encaminha para uma instância saudável do serviço correspondente, resolvida dinamicamente via Eureka (`lb://user-ms`, por exemplo).
- **`booking-ms`** continua sendo o orquestrador da regra de negócio: antes de confirmar uma reserva, ele consulta `user-ms` e `room-ms` via **Feign Clients**, resolvidos via Eureka. Essas chamadas são protegidas por **Circuit Breaker** (ver seção [Resiliência](#resiliência)).
- Assim como antes, cada serviço valida o JWT **localmente**, sem precisar chamar `user-ms` a cada requisição — os serviços compartilham a mesma chave secreta (`jwt.key`).

## Stack técnica

- **Java 21** + **Spring Boot** (multi-módulo Maven, com um `pom.xml` raiz agregando os 6 serviços)
- **Spring Cloud Netflix Eureka** — service discovery (server e client)
- **Spring Cloud Config** — configuração centralizada (modo *native*, lendo do classpath)
- **Spring Cloud Gateway (MVC)** — API Gateway com roteamento declarativo
- **Spring Cloud OpenFeign** + **Spring Cloud LoadBalancer** — comunicação HTTP síncrona entre `booking-ms` e os demais serviços, com resolução de endereço via Eureka
- **Resilience4j (Spring Cloud Circuit Breaker)** — tolerância a falhas nas chamadas Feign de `booking-ms`
- **Spring `@Scheduled`** — job de reconciliação para reservas pendentes de integração
- **Spring Data JPA / Hibernate** — persistência
- **Flyway** — versionamento de schema (MySQL)
- **Spring Security** — autenticação e autorização
- **JJWT + Auth0 java-jwt** — geração e validação de JWT
- **Spring Security OAuth2 Client** — login social (Google e GitHub)
- **GoogleAuth (warrenstrange)** — geração/validação de códigos TOTP (Google Authenticator)
- **Spring Mail** — envio de código de 2FA por e-mail
- **MySQL** — um banco por serviço de negócio
- **Docker Compose** — sobe os 6 serviços + MySQL com um único comando
- **RabbitMQ** — infraestrutura já provisionada no `docker-compose.yml` para uma futura camada de mensageria assíncrona (ainda não integrada ao código)
- **Bean Validation (Jakarta Validation)**

## Estrutura do repositório

Monorepo multi-módulo: um `pom.xml` raiz agrega os 6 serviços, cada um com seu próprio `pom.xml`, `Dockerfile` e ciclo de vida independente.

```
room-reservation-microservices/
├── pom.xml                        # POM agregador (packaging pom)
├── docker-compose.yml
├── .env.example
├── server-ms/                     # Eureka Server
│   └── Dockerfile
├── config-server-ms/              # Config Server
│   ├── Dockerfile
│   └── src/main/resources/config-repo/   # .properties centralizados (classpath)
├── gateway-ms/                    # API Gateway
│   └── Dockerfile
├── user-ms/                       # autenticação, usuários, perfis, 2FA
│   └── Dockerfile
├── room-ms/                       # salas
│   └── Dockerfile
├── booking-ms/                    # reservas (orquestra user-ms + room-ms via Feign, com Circuit Breaker)
│   └── Dockerfile
└── README.md
```

## Segurança

O ponto forte do projeto está na camada de segurança do `user-ms`, replicada de forma simplificada (apenas validação) em `room-ms` e `booking-ms`. O `gateway-ms`, por ora, só roteia — a validação do JWT continua acontecendo em cada serviço de destino, não no gateway.

### Autenticação local (e-mail/senha)

- Senha armazenada com hash **BCrypt**.
- `POST /api/v1/auth/registrar` cria o usuário com o perfil padrão `ESTUDANTE`.
- `POST /api/v1/auth/login` autentica via `AuthenticationManager` e:
  - se o usuário **não tem 2FA ativa**, já retorna o JWT;
  - se tem, retorna uma resposta sinalizando que é necessário confirmar o segundo fator (sem token ainda).

### Login social (OAuth2 — Google e GitHub)

- Fluxo padrão do Spring Security OAuth2 Client (`/oauth2/authorization/google` e `/oauth2/authorization/github`).
- Um `OAuth2LoginSuccessHandler` customizado intercepta o sucesso do login:
  1. identifica o provedor (`google` ou `github`);
  2. extrai nome e e-mail (no GitHub, o e-mail primário/verificado é buscado via chamada à API do GitHub, já que nem sempre vem nos atributos do OAuth2User);
  3. busca o usuário pelo e-mail ou cria um novo, com senha aleatória e perfil `ESTUDANTE`;
  4. gera o **mesmo JWT** usado no login local e devolve ao cliente.

Isso significa que, depois do login social, o restante do sistema (roles, 2FA, autorização) funciona exatamente igual a um login local — o provedor de login (`LOCAL`, `GOOGLE`, `GITHUB`) fica registrado no usuário só para fins de auditoria.

### Autenticação de dois fatores (2FA)

Suporta dois tipos, com o mesmo contrato de resposta (`TipoA2f`: `DESATIVADA`, `EMAIL`, `AUTHENTICATOR`):

- **Por e-mail**: no login, gera um código numérico de 6 dígitos com expiração de 5 minutos e envia por e-mail (SMTP Gmail). O código é de uso único (`utilizado = true` após validado).
- **Por aplicativo autenticador (TOTP / RFC 6238)**: fluxo de ativação em duas etapas —
  1. `POST /api/v1/auth/2fa/totp/setup` gera um secret e devolve uma URL de QR Code para escanear no Google Authenticator (ou similar);
  2. `POST /api/v1/auth/2fa/totp/confirm` confirma o primeiro código gerado pelo app antes de ativar a 2FA de fato.

Depois de ativada, todo login exige uma segunda chamada a `POST /api/v1/auth/2fa/verify` (com e-mail + código) para só então receber o JWT.

### Autorização (roles e hierarquia)

- Perfis: `ESTUDANTE`, `INSTRUTOR`, `MODERADOR`, `ADMINISTRADOR`.
- Hierarquia de roles configurada via `RoleHierarchy`: `ADMINISTRADOR` implica `MODERADOR`, que implica `ESTUDANTE` e `INSTRUTOR`.
- Autorização feita tanto na cadeia de filtros (`SecurityFilterChain`) quanto em nível de método (`@PreAuthorize`), incluindo regras de dono do recurso (ex: `#id == authentication.principal.id or hasRole('ADMINISTRADOR')` para um usuário poder editar os próprios dados ou um admin poder editar qualquer um).

### JWT stateless e distribuído

- Token assinado com HMAC (chave `jwt.key`, compartilhada por todos os serviços de negócio via variável de ambiente).
- Claims incluem `usuarioId` e `authorities` (perfis), então `room-ms` e `booking-ms` conseguem autenticar e autorizar a requisição **sem consultar o `user-ms`**, só decodificando o token.
- Sessão stateless (`SessionCreationPolicy.STATELESS`), sem cookies de sessão.
- `booking-ms` propaga o JWT recebido do cliente para as chamadas Feign a `room-ms`/`user-ms` através de um `RequestInterceptor`, preservando o contexto de segurança entre serviços — a resolução de qual instância chamar é feita pelo Eureka, não por uma URL fixa.

## Resiliência

`booking-ms` depende de `room-ms` para validar e atualizar o status de uma sala a cada reserva — se `room-ms` cair, isso não pode travar o sistema inteiro. Por isso, essa chamada é protegida com **Resilience4j Circuit Breaker**, com um fluxo de degradação controlada em vez de simplesmente falhar:

1. Ao confirmar uma reserva (`PATCH /api/v1/reservas/{id}`), o `booking-ms` tenta atualizar o status da sala no `room-ms` via Feign.
2. Se o `room-ms` estiver indisponível, o Circuit Breaker aciona o **fallback** (`salaAtualizadaComIntegracaoPendente`): a reserva não é perdida nem rejeitada — ela é marcada com o status **`ATIVA_SEM_INTEGRACAO`**, indicando que a reserva existe mas a sala ainda não foi sincronizada.
3. Um job agendado (`@Scheduled(fixedDelay = 60000)`, rodando a cada 1 minuto) varre periodicamente as reservas nesse estado e tenta reconciliar: se `room-ms` já voltou, a reserva é promovida para `ATIVA` e a sala é marcada como `OCUPADA`; se ainda estiver fora do ar, tenta de novo no próximo ciclo.
4. Todas as chamadas Feign de `booking-ms` também têm o circuit breaker do próprio OpenFeign habilitado globalmente (`spring.cloud.openfeign.circuitbreaker.enabled=true`), como uma camada adicional de proteção.

Esse padrão — aceitar uma escrita em estado degradado e reconciliar depois — é uma forma simples de **consistência eventual**, e é um bom talking point de entrevista: mostra entendimento de que, em sistemas distribuídos, "a chamada falhou" nem sempre deveria significar "a operação inteira falhou".

## Serviços e endpoints

Todas as rotas abaixo passam pelo `gateway-ms` (porta `8080`), prefixadas pelo nome do serviço — o gateway remove esse prefixo antes de encaminhar (`StripPrefix=1`). Ex: `POST http://localhost:8080/user-ms/api/v1/auth/login` chega em `user-ms` como `POST /api/v1/auth/login`.

### `user-ms` (prefixo `/user-ms`)

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/api/v1/auth/registrar` | Cadastro de usuário | Público |
| POST | `/api/v1/auth/login` | Login local | Público |
| POST | `/api/v1/auth/2fa/verify` | Confirma código 2FA (e-mail ou TOTP) e emite o JWT | Público |
| POST | `/api/v1/auth/2fa/totp/setup` | Gera secret + QR Code para ativar TOTP | Autenticado |
| POST | `/api/v1/auth/2fa/totp/confirm` | Confirma o primeiro código TOTP e ativa a 2FA | Autenticado |
| GET | `/oauth2/authorization/google` \| `/github` | Início do login social | Público |
| GET/POST/PUT/DELETE | `/api/v1/usuarios/**` | CRUD de usuários, perfis e ativação de 2FA por e-mail | Autenticado / dono do recurso / `ADMINISTRADOR` |

### `room-ms` (prefixo `/room-ms`)

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| GET | `/api/v1/salas` \| `/listar-paginado` \| `/{id}` | Consulta de salas | Público |
| POST | `/api/v1/salas` | Cadastro de sala | `ADMINISTRADOR` |
| PUT | `/api/v1/salas/{id}` | Atualização completa da sala | `ADMINISTRADOR` |
| DELETE | `/api/v1/salas/{id}` | Remoção de sala | `ADMINISTRADOR` |
| PATCH | `/api/v1/salas/alterar-status/{id}` | Altera apenas o status (LIVRE/OCUPADA) | Autenticado |

### `booking-ms` (prefixo `/booking-ms`)

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| GET | `/api/v1/reservas` \| `/listar-paginado` \| `/sala/{id}` \| `/{id}` | Consulta de reservas | Autenticado |
| POST | `/api/v1/reservas` | Cria reserva (valida usuário e sala via Feign, conflito de horário, capacidade, horário de funcionamento) | Autenticado |
| PUT | `/api/v1/reservas/{id}` | Atualiza reserva (apenas o dono) | Autenticado |
| DELETE | `/api/v1/reservas/{id}` | Cancela reserva (apenas o dono) | Autenticado |
| PATCH | `/api/v1/reservas/{id}` | Confirma reserva; se `room-ms` estiver fora do ar, aciona o Circuit Breaker e marca como `ATIVA_SEM_INTEGRACAO` | Autenticado |

### Painéis

- Eureka: `http://localhost:8761`
- Config Server (para inspecionar a config resolvida de um serviço): `http://localhost:8888/{nome-do-serviço}/default`, ex: `http://localhost:8888/user-ms/default`

## Modelagem de dados

Cada serviço de negócio versiona seu próprio schema via Flyway:

- **user-ms**: `usuarios`, `perfis`, `usuarios_perfis` (N:N), `codigos_a2f` — com colunas incrementais para `provedor_login`, `tipo_a2f`, `a2f_ativa` e `a2f_secret`, adicionadas em migrations sucessivas conforme o recurso evoluiu.
- **room-ms**: `salas` (com constraint de capacidade positiva), com status `LIVRE`, `OCUPADA` ou `OCUPADA_SEM_INTEGRACAO`.
- **booking-ms**: `reservas` (referencia `usuario_id` e `sala_id` por chave lógica, não por FK física), com status `ATIVA`, `CANCELADA` ou `ATIVA_SEM_INTEGRACAO`.

## Como rodar o projeto

### Pré-requisitos

- Docker e Docker Compose (recomendado) **ou** Java 21 + MySQL 8+ para rodar manualmente
- Contas OAuth2 configuradas no Google Cloud Console e no GitHub Developer Settings (para o login social)
- Uma conta de e-mail com senha de app (para o envio do código de 2FA por e-mail)

### Opção A — Docker Compose (recomendado)

1. Copie `.env.example` para `.env` e preencha os valores (senha do MySQL, `JWT_KEY`, credenciais de e-mail e OAuth2 — lembre-se de usar aspas se algum valor tiver `#` ou `$`).
2. Suba tudo com um único comando:

```bash
docker compose up --build
```

Isso builda as 6 imagens e sobe um MySQL compartilhado, com cada serviço de negócio criando seu próprio schema (`user_ms`, `room_ms`, `booking_ms`) automaticamente na primeira conexão. O Compose já cuida da ordem de subida e da resolução de endereços entre containers (Eureka e Config Server apontando para os nomes dos serviços, não para `localhost`).

Depois de tudo no ar, todas as chamadas do cliente são feitas em `http://localhost:8080/<nome-do-serviço>/...`.

### Opção B — Rodando cada serviço manualmente

Como os serviços dependem do Eureka para se descobrirem e do Config Server para sua configuração, a ordem de subida importa:

```bash
# Terminal 1 — Eureka Server
cd server-ms && ./mvnw spring-boot:run

# Terminal 2 — Config Server
cd config-server-ms && ./mvnw spring-boot:run

# Terminal 3, 4, 5 — serviços de negócio (em qualquer ordem)
cd user-ms && ./mvnw spring-boot:run
cd room-ms && ./mvnw spring-boot:run
cd booking-ms && ./mvnw spring-boot:run

# Terminal 6 — Gateway (por último)
cd gateway-ms && ./mvnw spring-boot:run
```

### Variáveis de ambiente

**user-ms:**
```
DATASOURCE_USERNAME=
DATASOURCE_PASSWORD=
JWT_KEY=
JWT_EXPIRATION=900000
EMAIL_USERNAME=
EMAIL_PASSWORD=
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GITHUB_CLIENT_ID=
GITHUB_CLIENT_SECRET=
```

**room-ms** e **booking-ms:**
```
DATASOURCE_USERNAME=
DATASOURCE_PASSWORD=
JWT_KEY=
JWT_EXPIRATION=900000
```

**gateway-ms**, **server-ms** e **config-server-ms** não precisam de variáveis de ambiente para rodar localmente.

> ⚠️ `JWT_KEY` **precisa ser o mesmo valor em `user-ms`, `room-ms` e `booking-ms`** — é o que permite que cada um valide tokens emitidos pelo `user-ms` sem se comunicar entre si para isso.
>
> ⚠️ Nunca coloque valores reais de `.env` em um arquivo versionado no Git. Use sempre o `.env.example` como referência e mantenha o `.env` real fora do controle de versão (`.gitignore`).

## Roadmap

- [x] `docker-compose.yml` com os 6 serviços + MySQL, portas corretas e resolução de Eureka/Config Server entre containers
- [x] Service Discovery (Eureka) em vez de URLs fixas nos `@FeignClient`
- [x] API Gateway (Spring Cloud Gateway) como ponto único de entrada
- [x] Centralizar configuração (Spring Cloud Config), com `search-locations` portável via classpath (em vez de caminho absoluto)
- [x] Circuit breaker (Resilience4j) nas chamadas Feign de `booking-ms`, com job de reconciliação
- [ ] Revisar o `SalaClient.alterarStatusSala` — hoje chama o PUT genérico de atualização de sala (`ADMINISTRADOR`-only) em vez do PATCH de status; confirmar se é esse o comportamento esperado
- [ ] Integrar RabbitMQ ao código (hoje só provisionado no `docker-compose.yml`, sem uso real ainda) — candidato natural: notificações assíncronas de reserva confirmada/cancelada, ou fila para o próprio fluxo de reconciliação
- [ ] Testes de unidade e integração (JUnit 5 + Mockito) para os seis serviços
- [ ] Healthchecks via Spring Boot Actuator, para o `docker-compose.yml` poder usar `service_healthy` em vez de `service_started` nas dependências entre os microsserviços
- [ ] JWT validado no próprio `gateway-ms`, para rotas públicas x autenticadas serem decididas antes de chegar aos serviços de negócio
- [ ] Migrar o Config Server para modo Git-backed, se for necessário editar configuração sem rebuildar o serviço

---

Projeto com fins de estudo e portfólio, evoluindo o [room-reservation-api](https://github.com/jhohannessf/room-reservation-api) original de monolito para uma arquitetura de microsserviços com service discovery, configuração centralizada, API Gateway e resiliência.
