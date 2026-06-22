# Architecture — PDF Suite

> Vue d'ensemble tenue par le Tech Lead. Dernière mise à jour : 2026-06-02.

## Principe directeur
**Monolithe modulaire** Spring Boot, déployable en un seul artefact. Frontières de modules nettes,
pas de microservices (cf. [ADR-0002](DECISIONS.md)). Évolution chirurgicale de l'existant.

## Architecture ACTUELLE (avant V1) — à démanteler

```
[React/Vite]──HTTP──>[Nginx]──>[API Gateway Spring Boot]──IIOP/CORBA──>[corba-server JVM]
                                       │                                   (41 handlers PDFBox)
                                   [MongoDB]                            fichier IOR partagé
```
- `corba-idl` : 42 opérations dans une interface IDL monolithique.
- `corba-server` : 2e JVM, JacORB 3.9, `PDFServiceImpl` + 41 handlers.
- `api-gateway` : REST, JWT, quotas, rate-limit, admin, MongoDB (users/jobs/GridFS). `PDFController` = 1190 LOC.
- Prod Render : hack mono-conteneur, supervisord lance les 2 JVM dans 512 Mo.

**Dette :** CORBA n'apporte rien (deux JVM Java qui s'échangent des `byte[]`). C'est le cadavre académique.

## Architecture CIBLE (à partir de V1)

```
[React/Vite]──HTTP/JSON──>[API Gateway Spring Boot, JVM unique]
                                  │  ├── module web (REST controllers, auth, quotas)
                                  │  ├── module pdf-engine (41 handlers, in-process)
                                  │  └── module persistence (Mongo : users, jobs)
                              [MongoDB]
```
- Un seul JVM, un seul Dockerfile. Plus d'IIOP, d'IOR, de supervisord.
- L'ancienne interface IDL devient une **interface Java interne** `PdfEngine`.
- Le frontend ne change pas (contrat REST inchangé).

## Évolution prévue par version
- **V2** : découpage du `PDFController` monolithique, OpenAPI, versioning d'API, durcissement sécurité.
- **V3** : queue de jobs asynchrone (Mongo→Redis), progression SSE, stockage objet, OpenTelemetry.
- **V4** : moteur de workflows chaînés au-dessus de `PdfEngine`.
- **V5** : chemin de traitement client WASM pour ops sensibles (local-first fort).
- **V6** : couche IA documentaire, multi-tenant, facturation.

## Invariants permanents
1. Aucune réécriture from scratch (ADR-0001).
2. Tests verts à chaque étape ; aucune régression utilisateur.
3. Validation locale (`docker compose`) **avant** tout commit/push.
4. Contrat REST stable tant qu'une version ne le change pas explicitement (frontend protégé).
