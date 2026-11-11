# Central do Alfabeto - Servidor (API)

[![CI](https://github.com/Central-do-alfabeto/Central-do-Alfabeto-Servidor/actions/workflows/ci.yml/badge.svg)](https://github.com/Central-do-alfabeto/Central-do-Alfabeto-Servidor/actions/workflows/ci.yml)

Read in English: [README.md](README.md)

## Sumário

- [Visão geral](#visão-geral)
- [Funcionalidades](#funcionalidades)
- [Stack e arquitetura](#stack-e-arquitetura)
- [Endpoints](#endpoints)
- [Pré-requisitos](#pré-requisitos)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Como rodar localmente](#como-rodar-localmente)
- [Banco de dados e migrations](#banco-de-dados-e-migrations)
- [Testes](#testes)
- [Validação realizada](#validação-realizada)
- [Integração contínua](#integração-contínua)
- [Implantação](#implantação)
- [Detalhes de implementação e limitações conhecidas](#detalhes-de-implementação-e-limitações-conhecidas)
- [Créditos](#créditos)

## Visão geral

A Central do Alfabeto é um jogo de alfabetização guiado por voz: a criança ouve letras e sílabas, repete em voz alta e responde a exercícios, enquanto um educador acompanha o progresso. Foi desenvolvido como projeto de extensão do IFSP (campus Campos do Jordão, CJO).

Este repositório contém apenas a **API (backend)**. Ela cuida de contas, do vínculo educador-aluno, do progresso por fase e (opcionalmente) da transcrição de fala.

- Repositório do frontend: <https://github.com/Central-do-alfabeto/Central-do-Alfabeto-FrontEnd>
- Demo pública: <https://central-do-alfabeto.github.io/Central-do-Alfabeto-FrontEnd/>

> A demo pública funciona **sem este backend**. Não há instância hospedada desta API; para usá-la é preciso executá-la por conta própria, como descrito abaixo.

## Funcionalidades

- Cadastro e login para dois papéis: aluno (`STUDENT`) e educador (`EDUCATOR`).
- Senhas guardadas como hash BCrypt; autenticação sem estado com JWT (HS256).
- O aluno registra um resumo de progresso por fase (erros e repetições de áudio).
- O educador vincula alunos a si (por e-mail e nome exato), lista-os e consulta o progresso por fase.
- Endpoint de transcrição de fala (Vosk, modelo pequeno em português) que recebe um WAV e devolve texto. É um recurso opcional: se o modelo não carregar, o restante da API continua funcionando.
- Validação de entrada, com respostas `400` em JSON listando os campos inválidos.
- CORS restrito a uma lista de origens configurável.

## Stack e arquitetura

| Área | Tecnologia |
| --- | --- |
| Linguagem / runtime | Java 17 (compilado e testado localmente com JDK 21) |
| Framework | Spring Boot 3.5 (Web, Data JPA, Security, Validation) |
| Banco de dados | PostgreSQL (execução), H2 em modo PostgreSQL (testes) |
| Migrations | Flyway |
| Autenticação | JWT com jjwt 0.11.5, BCrypt |
| Reconhecimento de fala | Vosk 0.3.45 com `vosk-model-small-pt-0.3` |
| Build | Maven (wrapper incluído) |

Pacotes em `com.centraldoalfabeto.game`:

- `domain.controller` - controllers REST (`/auth`, `/players`, `/educators`, `/audio`).
- `service` - regras de negócio (`AuthService`, `JogadorService`, `EducadorService`, `ProgressService`, `JwtService`, `VoskTranscriptionService`).
- `repository` - repositórios Spring Data JPA.
- `domain.model` - entidades JPA: `User`, `PlayerData`, `EducatorStudentLink`.
- `dto` - objetos de requisição/resposta, com anotações de Bean Validation.
- `security` e `config` - filtro JWT, cadeia de segurança, CORS e tratamento de erros de validação.

**Autenticação.** `POST /auth/login` devolve um JWT com o id do usuário (subject), o papel e o e-mail; ele expira após `JWT_EXPIRATION_MS` (24 horas por padrão). Um filtro lê o cabeçalho `Authorization: Bearer <token>` e preenche o contexto de segurança. O papel vem da coluna JSON `metadados` do usuário (`{"role":"student"}` ou `{"role":"educator"}`), definida no cadastro. O login ignora qualquer papel enviado pelo cliente e recusa contas sem papel reconhecido.

**Autorização.** `/players/**` exige `STUDENT`, `/educators/**` exige `EDUCATOR` (exceto as rotas públicas de cadastro), e os controllers ainda conferem se o id do caminho é o do usuário autenticado. O aluno só grava o próprio progresso; o educador só lê o progresso de alunos vinculados a ele.

**Vosk.** O modelo é carregado na inicialização a partir do diretório `vosk-model/`, relativo ao diretório de execução. Se a carga falhar (modelo ausente, biblioteca nativa incompatível), o erro é registrado no log e somente `/audio/transcribe` é afetado.

## Endpoints

| Método | Caminho | Autenticação | Descrição |
| --- | --- | --- | --- |
| POST | `/auth/login` | Pública | Login com `email` e `password`; devolve token, papel e dados do papel (próxima fase para aluno, alunos vinculados para educador). |
| POST | `/players/register` | Pública | Cadastra aluno (`nome`, `email`, `senha`) e devolve token. `409` se o e-mail já existe (comparação sem diferenciar maiúsculas de minúsculas). |
| POST | `/educators/register` | Pública | Cadastra educador (mesmo corpo) e devolve token. |
| POST | `/players/{id}/progress` | `STUDENT`, próprio id | Grava o resumo de uma fase (`currentPhaseIndex` de 0 a 25, `errorsData`, `soundRepeatsData`). `400` se a fase saltar à frente da próxima esperada; `409` se a fase já foi gravada. |
| GET | `/educators/{id}/students` | `EDUCATOR`, próprio id | Lista os alunos vinculados ao educador. |
| POST | `/educators/{id}/students` | `EDUCATOR`, próprio id | Vincula um aluno; corpo com `email` e `studentName`, que devem coincidir com o cadastro do aluno. Qualquer divergência devolve o mesmo `400` genérico, para não permitir enumerar contas. |
| GET | `/educators/student-progress/{studentId}` | `EDUCATOR`, aluno vinculado | Devolve totais e resumos por fase de um aluno vinculado. |
| PUT | `/educators/{id}/updateStudentIds` | `EDUCATOR`, próprio id | O corpo é uma lista de ids de alunos; só permite manter ou remover alunos já vinculados (`400` se houver outro id). |
| POST | `/audio/transcribe` | Qualquer usuário autenticado (aluno ou educador) | Campo multipart `audioFile`: WAV PCM 16-bit, mono, 16 kHz, até 1 MB (cerca de 30 s). Devolve o texto como `text/plain`; **quando nada é reconhecido a resposta é `200` com corpo vazio** (o frontend trata a string vazia como "sem resultado"). `415` para outros formatos, `413` se for grande demais, `503` se o modelo de fala estiver indisponível. |

Os corpos de erro são JSON. Falhas de validação retornam `400` com `{"error": "Dados inválidos", "fields": {"campo": "mensagem"}}`. Token ausente, inválido ou expirado retorna `401` com `{"error": "..."}`; `403` é usado apenas por falta de permissão. Violações de unicidade (cadastro concorrente ou progresso de fase duplicado) retornam `409`. O cadastro exige nome, e-mail válido e senha de 8 a 72 caracteres.

### Códigos de erro

Todos os corpos de erro são JSON (`{"error": "..."}`), com ou sem token.

| Status | Quando |
| --- | --- |
| 400 | Falha de validação (com `fields`), JSON malformado, parâmetro ou parte multipart ausente, valor inválido na URL (por exemplo id que não é UUID), fase fora de sequência ou do intervalo, falha genérica no vínculo de aluno |
| 401 | Token ausente, inválido ou expirado (também para rotas inexistentes sem token) |
| 403 | Autenticado, mas sem permissão (papel errado ou id de outro usuário) |
| 404 | Rota inexistente (com token válido) |
| 405 | Método HTTP não suportado para a rota |
| 409 | E-mail duplicado, progresso de fase duplicado, aluno já vinculado, violação de unicidade |
| 413 | Upload maior que 1 MB |
| 415 | Tipo de conteúdo não suportado, ou áudio que não é WAV PCM 16-bit mono 16 kHz |
| 500 | Falha inesperada (mensagem genérica; detalhes apenas no log do servidor) |
| 503 | Modelo de fala não carregado |

## Pré-requisitos

- JDK 17 ou superior (`JAVA_HOME` deve apontar para ele; o Maven é baixado pelo wrapper).
- PostgreSQL 14+ para rodar a API sem Docker.
- [Git LFS](https://git-lfs.com/), para que os arquivos do modelo Vosk (`*.fst`) sejam baixados no clone.
- Docker com Compose, opcional.

## Variáveis de ambiente

| Variável | Obrigatória | Padrão | Descrição |
| --- | --- | --- | --- |
| `DATABASE_URL` | Sim | - | URL JDBC, por exemplo `jdbc:postgresql://localhost:5432/centraldoalfabeto`. |
| `DATABASE_USERNAME` | Sim | - | Usuário do banco. |
| `DATABASE_PASSWORD` | Sim | - | Senha do banco. |
| `JWT_SECRET` | Sim | - | Segredo de assinatura, com pelo menos 32 caracteres. A aplicação não inicia sem ele. |
| `JWT_EXPIRATION_MS` | Não | `86400000` | Validade do token em milissegundos. |
| `CORS_ALLOWED_ORIGINS` | Não | `http://localhost:5173` | Lista de origens permitidas, separadas por vírgula. |
| `VOSK_MODEL_PATH` | Não | `vosk-model` | Diretório do modelo Vosk, relativo ao diretório de execução. |

Veja `.env.example` para valores fictícios. A aplicação **não** lê o `.env` sozinha; exporte as variáveis no shell. O Docker Compose lê um `.env` neste diretório para a substituição de variáveis. O `.env` é ignorado pelo Git.

## Como rodar localmente

### Com Docker Compose

```bash
git lfs pull                       # garante que o modelo Vosk está presente
cp .env.example .env               # depois edite os valores
docker compose up --build
```

O Compose sobe o PostgreSQL 16 (publicado apenas em `127.0.0.1:5432`) e a API em `http://localhost:8080`. Dentro do Compose, `DATABASE_URL` aponta para o serviço `db`. A configuração Docker foi executada localmente; veja [Validação realizada](#validação-realizada).

### Sem Docker

1. Crie um banco PostgreSQL vazio e um usuário.
2. Garanta que `vosk-model/` contém o modelo. Se não contiver, rode `./download_vosk_model.sh` (Bash, requer `curl` e `unzip`), que baixa o `vosk-model-small-pt-0.3` de alphacephei.com para `vosk-model/`.
3. Exporte as variáveis e inicie a API na raiz do repositório (o caminho do modelo é relativo ao diretório de execução):

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/centraldoalfabeto
export DATABASE_USERNAME=alfabeto
export DATABASE_PASSWORD=troque-esta-senha
export JWT_SECRET=$(openssl rand -base64 48)
./mvnw spring-boot:run
```

No PowerShell use `$env:NOME = "valor"` para cada variável. O Flyway cria o schema na primeira inicialização.

Para usar com o frontend, rode-o em modo de desenvolvimento (origem padrão `http://localhost:5173`) apontando para `http://localhost:8080`.

## Banco de dados e migrations

O schema é versionado com Flyway em `src/main/resources/db/migration`. `V1__schema_inicial.sql` cria `users`, `player_data` e `educators_students`. O Hibernate roda com `ddl-auto=validate`, portanto nunca altera o schema; qualquer mudança deve ser uma nova migration.

A `V2__email_minusculo.sql` converte os e-mails existentes para minúsculas e adiciona um `CHECK` que obriga e-mails em minúsculas; junto com o `UNIQUE` da `V1`, isso torna a unicidade do e-mail insensível a maiúsculas no PostgreSQL e no H2. Ela falha de propósito se duas contas existentes diferirem apenas pela caixa. Os e-mails são normalizados (trim e minúsculas) no cadastro, no login e no vínculo de aluno.

Para um **banco preexistente** que já tem essas tabelas (por exemplo, criado antes do Flyway), não execute a `V1` sobre ele. Faça o baseline antes, para que o Flyway marque a `V1` como aplicada, por exemplo com `spring.flyway.baseline-on-migrate=true` e `spring.flyway.baseline-version=1` na primeira inicialização, conferindo se as colunas existentes correspondem à `V1`. Essa validação só foi exercitada no H2, não em um PostgreSQL real.

## Testes

```bash
./mvnw -B test
```

São 78 testes: unitários de serviço (Mockito) e de integração com MockMvc, cobrindo autorização entre usuários, validação de entrada, papel no login e CORS. Rodam em um H2 em memória em modo PostgreSQL e não precisam de banco externo. A mesma migration do Flyway é aplicada; o tipo `jsonb` é emulado no H2 por um domínio criado na URL JDBC de teste. O modelo Vosk não é carregado nos testes (`vosk.model-path` vazio) e o reconhecedor é simulado (mock).

## Validação realizada

Além dos testes automatizados, a configuração Docker foi validada manualmente em uma máquina de desenvolvimento: a imagem foi construída, iniciada com o Compose junto com o PostgreSQL 16 (credenciais descartáveis, portas do host diferentes das padrão) e depois removida. Observado: o Flyway aplicou a `V1` e a `V2`; o `ddl-auto=validate` do Hibernate passou; o modelo Vosk foi carregado dentro da imagem; a aplicação rodou como usuário não-root usando cerca de 400 a 470 MiB de memória; e os cenários de cadastro, login, progresso, vínculo de aluno e códigos de erro acima se comportaram como documentado, incluindo uma requisição real de transcrição de um WAV silencioso (`200`, corpo vazio). **Nunca foi implantado** em uma plataforma de hospedagem.

## Integração contínua

`.github/workflows/ci.yml` executa `./mvnw -B test` com JDK 17 a cada push e pull request na `main` (com Git LFS habilitado no checkout).

## Implantação

**Não existe instância pública hospedada** desta API, nem configuração de implantação além do Dockerfile. Hospedá-la exigiria:

- um runtime Java 17 (ou a imagem fornecida) e um PostgreSQL gerenciado;
- as variáveis de ambiente acima, com um `JWT_SECRET` forte e `CORS_ALLOWED_ORIGINS` apontando para a origem do frontend;
- HTTPS na frente da API (ela serve HTTP simples);
- memória suficiente para o Vosk: em uma medição local o processo usou cerca de 350 a 450 MB de RAM, principalmente o modelo de fala e memória nativa. Planos gratuitos de 512 MB ficam muito apertados; o Dockerfile define `-XX:MaxRAMPercentage=60` e `JAVA_OPTS` pode ser sobrescrita;
- o modelo Vosk dentro da imagem (`COPY vosk-model`), o que exige o conteúdo do Git LFS no momento do build;
- uma imagem baseada em glibc; a biblioteca nativa do Vosk não funciona em Alpine.

## Detalhes de implementação e limitações conhecidas

- **O vínculo educador-aluno não tem consentimento do aluno.** O educador vincula um aluno conhecendo apenas o e-mail e o nome exato; o aluno não precisa aceitar.
- **Acurácia da fala.** O modelo pequeno do Vosk em português é usado sem gramática restrita, então a acurácia é baixa em itens muito curtos, como letras isoladas. O endpoint valida o formato e o tamanho do WAV, mas não a qualidade nem a duração do áudio.
- **Progresso.** `currentPhaseIndex` é limitado a 0 a 25 (o jogo tem 26 fases) e fases não podem ser puladas: só se aceita a próxima fase esperada ou uma anterior ainda não gravada. O servidor confia nos contadores de erros e repetições enviados pelo cliente.
- **Tokens.** Os JWTs não podem ser revogados e não são conferidos no banco a cada requisição; não há refresh token, limitação de taxa nem verificação de e-mail.
- **Papéis** ficam em uma coluna JSON (`metadados`), e não em uma coluna dedicada; são lidos em um único ponto (`UserRole`), com interpretação estruturada do JSON.
- **Senhas** são limitadas a 72 caracteres (limite do BCrypt).
- **Docker**: a imagem e o Compose foram construídos e executados localmente (veja [Validação realizada](#validação-realizada)); nunca foram implantados em uma plataforma de hospedagem.
- **PostgreSQL**: os testes automatizados usam H2. As migrations e a validação do schema também foram exercitadas em um PostgreSQL 16 real na execução local com Docker, mas não fazem parte do workflow de CI.
- O histórico do repositório contém credenciais antigas de antes da limpeza; devem ser consideradas comprometidas e rotacionadas.
- A pasta de saída do Eclipse, `bin/`, não é mais versionada (está ignorada). O histórico anterior ainda carrega seus arquivos grandes.

## Créditos

Projeto desenvolvido no IFSP-CJO por:

- Brian Gustavo de Oliveira Brandão
- Tiago J. Saraiva
- Luiz Henrique de Oliveira Freitas

(Nomes extraídos do histórico Git deste repositório.)
