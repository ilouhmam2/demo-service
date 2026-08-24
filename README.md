# demo-service

Service de demonstration genere a partir de `foundation-archetype`, base sur Spring Boot 4.1.x et les starters `foundation-*`.

L'objectif de ce service est de montrer une implementation complete de style hexagonal:
- API REST entrante
- persistance PostgreSQL (JPA/Flyway)
- publication/consommation NATS
- appel REST externe (client genere OpenAPI)
- appel SOAP externe (client genere CXF)
- observabilite (Actuator, Micrometer, Prometheus)

## Quick Start 5 minutes

Depuis le dossier `demo-service`:

```bash
# 1) Installer les artefacts foundation en local (une fois)
cd ../foundation-platform && mvn clean install

# 2) Revenir dans le service
cd ../demo-service

# 3) Construire les 2 modules clients generes
mvn -f demo-service-rest-client/pom.xml clean install
mvn -f demo-service-soap-client/pom.xml clean install

# 4) Demarrer les dependances locales
docker compose up -d

# 5) Lancer le service
mvn spring-boot:run
```

Verification rapide:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/items
```

## Ce qu'il faut avoir

## Prerequis logiciels

- Java 21
- Maven 3.9+
- Docker Desktop (obligatoire pour PostgreSQL/NATS/WireMock en local et pour Testcontainers)

## Prerequis de dependances Maven

Ce projet depend des artefacts `foundation-*` en version `0.0.1-SNAPSHOT`.
Avant de lancer `demo-service`, il faut installer `foundation-platform` en local:

```bash
cd ../foundation-platform
mvn clean install
```

Ensuite, revenir dans `demo-service`:

```bash
cd ../demo-service
```

## Ce qu'il faut savoir sur la structure

Le service principal (`demo-service`) consomme 2 modules clients generes:
- `demo-service-rest-client`: client Java genere depuis OpenAPI (`external-items-api.yaml`)
- `demo-service-soap-client`: client Java genere depuis WSDL (`item-service.wsdl`)

Structure metier (hexagonale):

```text
fr.francetv.demo/
├── domain/
│   ├── model/
│   └── port/
│       ├── in/
│       └── out/
├── application/
│   └── usecase/
└── infrastructure/
     ├── adapter/
     │   ├── in/   (web, messaging)
     │   └── out/  (persistence, rest, soap, messaging)
     └── config/
```

## Comment le lancer

## 1) Construire les clients generes

Depuis `demo-service`:

```bash
mvn -f demo-service-rest-client/pom.xml clean install
mvn -f demo-service-soap-client/pom.xml clean install
```

## 2) Demarrer les dependances locales

Le fichier `docker-compose.yml` demarre:
- PostgreSQL (`localhost:5432`)
- NATS (`localhost:4222`)
- WireMock (`localhost:8089`)

Demarrage manuel:

```bash
docker compose up -d
```

Arret:

```bash
docker compose down
```

Note: `spring-boot-docker-compose` est present et `spring.docker.compose.lifecycle-management=start-and-stop` est configure. Spring Boot peut donc gerer le cycle Docker Compose automatiquement lors du `spring-boot:run` si Docker est disponible.

## 3) Lancer l'application

```bash
mvn spring-boot:run
```

Application disponible sur `http://localhost:8080`.

## Configuration principale

Fichier: `src/main/resources/application.yml`

Valeurs importantes:
- `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`
- `foundation.nats.server-url`
- `foundation.http-client.base-url` et `foundation.http-client.clients.external-items.base-url`
- `demo.soap.item-service-address`
- `management.endpoints.web.exposure.include`

## Endpoints exposes

API metier:
- `POST /api/items`: creer un item
- `GET /api/items/{id}`: recuperer un item
- `GET /api/items`: lister les items
- `POST /api/items/sync`: synchroniser les items depuis l'API REST externe
- `GET /api/items/{id}/soap-check`: recuperer un item via SOAP

Observabilite:
- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
- `GET /actuator/metrics`
- `GET /actuator/prometheus`

## Comment le tester

## Build complet

```bash
mvn clean verify
```

Cette commande execute:
- tests unitaires (`surefire`)
- tests d'integration (`failsafe`)
- controle de couverture JaCoCo en phase `verify`

## Tests unitaires uniquement

```bash
mvn test
```

## Exemple de test cible

```bash
mvn -Dtest=ItemMapperTest test
```

## Conditions de succes pour les tests

- Docker doit etre lance (Testcontainers utilise des conteneurs, notamment NATS et PostgreSQL)
- Les ports locaux ne doivent pas etre deja pris (5432, 4222, 8089)
- Les modules clients (`demo-service-rest-client`, `demo-service-soap-client`) doivent etre installes

## Depannage (Maven, PowerShell, Testcontainers)

## 1) `mvn` n'est pas reconnu sous Windows

Symptome:
- `mvn : The term 'mvn' is not recognized ...`

Correctif:

```powershell
mvn -v
```

Si la commande echoue, ajouter Maven au `PATH` (session courante):

```powershell
$env:PATH = "$env:PATH;C:\Users\lenovo\.m2\wrapper\dists\apache-maven-3.9.9-bin\f299a5f2280b048b392fec1060c8e9de\apache-maven-3.9.9\bin"
mvn -v
```

## 2) `mvn verify` semble en echec avec filtre PowerShell

Symptome:
- commande de type `mvn ... | Select-String ...` qui se termine avec un code 1
- impression d'echec meme quand les tests passent

Explication:
- avec un pipeline PowerShell, le code de retour final peut venir de `Select-String`, pas de Maven.

Commande fiable pour verifier le vrai statut Maven:

```powershell
mvn clean verify --no-transfer-progress
```

Dans ce projet, une execution complete recente donne:
- `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`
- `All coverage checks have been met.`
- `BUILD SUCCESS`

## 3) Testcontainers ne demarre pas

Symptomes frequents:
- `Could not find a valid Docker environment`
- timeouts au demarrage des tests d'integration

Correctifs:
- verifier que Docker Desktop est demarre
- verifier l'acces Docker:

```powershell
docker version
docker ps
```

- relancer ensuite:

```powershell
mvn clean verify --no-transfer-progress
```

## 4) Ports deja utilises en local

Symptome:
- conflit sur `5432`, `4222` ou `8089` lors du `docker compose up`

Correctifs:
- arreter les stacks existantes:

```powershell
docker compose down
```

- identifier un process local occupant un port:

```powershell
netstat -ano | findstr :5432
netstat -ano | findstr :4222
netstat -ano | findstr :8089
```

## 5) Avertissement Boot Manifest-JAR dans les rapports de test

Symptome:
- message `Boot Manifest-JAR contains absolute paths in classpath ...`

Interpretation:
- avertissement non bloquant observe dans les dumpstreams surefire/failsafe
- ne provoque pas a lui seul un echec du build

Action:
- ignorer si les tests et la couverture sont au vert

## 6) Dependances `foundation-*` introuvables

Symptome:
- Maven ne trouve pas `foundation-parent` / `foundation-bom` / starters

Correctif:

```bash
cd ../foundation-platform
mvn clean install
cd ../demo-service
mvn clean verify
```

## Approche et choix techniques

## 1) Architecture hexagonale

Choix:
- les ports `domain.port.in` exposent les cas d'usage
- les ports `domain.port.out` abstraient les dependances techniques
- les adapters `infrastructure.adapter.*` branchent web, persistence, messaging, REST externe et SOAP

Pourquoi:
- separer clairement metier et technique
- simplifier les tests (mock des ports)
- faciliter le remplacement d'une techno (ex: REST client) sans casser le metier

## 2) Starters foundation (composition par dependance)

Choix:
- utilisation des starters `foundation-*` (core, api, logging, observability, mapping, test, data, nats, http-client, soap-client)

Pourquoi:
- alignement sur les standards de la plateforme
- conventions transverses reutilisables
- demarrage rapide avec peu de code d'infrastructure custom

## 3) Contrats d'entree/sortie explicites (DTO + MapStruct)

Choix:
- DTO web (`CreateItemRequest`, `ItemResponse`) separes des entites JPA
- mappers MapStruct (`ItemMapper`, `ItemPersistenceMapper`)

Pourquoi:
- eviter d'exposer les entites techniques
- rendre les mappings explicites, testables et robustes

## 4) Observabilite et tracabilite

Choix:
- propagation du `X-Correlation-Id` cote HTTP entrant/sortant
- metriques Micrometer (ex: compteur `items.created`)
- endpoints Actuator/Prometheus

Pourquoi:
- tracer un flux bout-en-bout (API -> DB -> messaging -> appels externes)
- faciliter diagnostic et monitoring

## 5) Strategie de resilience sur les integrations

Choix:
- REST externe: erreurs mappees en `502 Bad Gateway` ou `503 Service Unavailable`
- SOAP externe: faute SOAP mappee en `502 Bad Gateway`
- publication NATS: echec de publication logge sans bloquer la creation d'item

Pourquoi:
- proteger l'experience API locale meme en cas de panne partielle externe
- rendre les erreurs techniques explicites et observables

## Flux fonctionnels detailles

## Flux A - Creation d'un item (`POST /api/items`)

1. Entree:
    - body JSON valide (`name` obligatoire)
    - header optionnel `X-Correlation-Id`
2. Mapping:
    - `CreateItemRequest` -> `Item` (domain)
3. BDD:
    - sauvegarde via `ItemPersistenceAdapter` -> `ItemRepository` (JPA)
    - table `items` (migration Flyway `V1__create_items_table.sql`)
4. Messaging:
    - publication d'un evenement NATS `item.created` (payload `itemId`, `name`)
5. Sortie:
    - HTTP `201 Created`
    - header `Location: /api/items/{id}`
    - body `ItemResponse`

## Flux B - Synchronisation externe (`POST /api/items/sync`)

1. Entree:
    - requete HTTP `POST` sans body
2. Appel externe REST:
    - `ExternalItemRestAdapter` appelle `ExternalItemsApi` (client genere OpenAPI)
3. Mapping:
    - `ExternalItem` -> `Item` (domain)
4. BDD:
    - insertion des items recuperes
5. Sortie:
    - HTTP `200`
    - body `{ "synced": <nombre> }`

## Flux C - Verification SOAP (`GET /api/items/{id}/soap-check`)

1. Entree:
    - `id` en path param
2. Appel externe SOAP:
    - `ItemSoapAdapter` via `ItemServicePortType` (client genere CXF)
3. Mapping:
    - `GetItemResponse` SOAP -> `Item` (domain)
4. Sortie:
    - HTTP `200`
    - body `ItemResponse`

## Stack technique

- Java 21
- Spring Boot 4.1.x
- Spring Web / Validation
- Spring Data JPA + Flyway + PostgreSQL
- NATS (`jnats`)
- WebClient + client OpenAPI genere
- Apache CXF + client SOAP genere
- MapStruct
- Micrometer + Prometheus + Actuator
- JUnit 5, Spring Boot Test, Testcontainers
