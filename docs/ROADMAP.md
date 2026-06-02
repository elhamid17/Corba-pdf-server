# Roadmap — PDF Suite

> Pilotée par le Tech Lead. Chaque version se découpe en étapes ; chaque étape majeure =
> une conversation dédiée pilotée par un prompt de passation (`docs/handoffs/`).
> Dernière mise à jour : 2026-06-02.

## North Star
SaaS PDF **open-core, self-host-first, local-first**, dont le fer de lance est l'**automatisation
par workflows chaînés**. Évolution chirurgicale (jamais de rewrite). Monolithe modulaire Spring Boot, Java 21.

## Échelle des versions

| Version | Thème | Objectif macro | État |
|---|---|---|---|
| **V1** | 🔪 Excision de CORBA (Walking Skeleton) | Monolithe Spring Boot unique, handlers in-process, mêmes features, tests verts, déploiement simplifié. Zéro régression. | 🟡 À démarrer |
| **V2** | 🧱 Socle qualité & domaine | Casser le contrôleur de 1190 lignes, frontières de modules, OpenAPI, versioning d'API, durcissement sécurité/auth. | ⚪ Prévu |
| **V3** | ⚙️ Production-ready | Queue de jobs (OCR/gros fichiers), progression SSE, stockage objet, observabilité (OTel, métriques, logs structurés). | ⚪ Prévu |
| **V4** | 🗡️ Workflows chaînés | Moteur de pipelines réutilisables (ex. OCR→anonymise→signe→tampon). Le différenciateur produit. | ⚪ Prévu |
| **V5** | 🔒 Local-first / confidentialité | Traitement client WASM pour ops sensibles, zéro-rétention garantie, auto-hébergement 1 commande, RGPD. | ⚪ Prévu |
| **V6** | 💎 IA + commercialisation | IA documentaire (BYO-key/modèle local), multi-tenant, facturation, SLA, SRE. Verticale = packaging GTM. | ⚪ Prévu |

---

## Découpage V1 — Excision de CORBA

> But : prouver que tout fonctionne **sans CORBA**, sans rien perdre. Le geste fondateur du projet.

| Étape | Intitulé | Livrable clé |
|---|---|---|
| **1.1** | Module `pdf-engine` in-process | Les 41 handlers + util + types déplacés dans un module Java sans CORBA, exposés via une interface `PdfEngine` propre. Tests handlers migrés et verts. |
| **1.2** | Branchement gateway | `PDFController` appelle `PdfEngine` en direct ; suppression de `CorbaClientService`, JacORB, `corba-idl`, `corba-server`. Tests gateway verts. |
| **1.3** | Nettoyage infra | Un seul Dockerfile, `docker-compose` sans corba-server ni supervisord, `render.yaml` mono-service. Validation locale + non-régression e2e. |

**Definition of Done V1 :** `docker compose up` lance frontend + 1 backend + mongo ; les 40+ outils
fonctionnent comme avant ; toute la suite de tests est verte ; plus aucune trace de CORBA/JacORB/IOR/supervisord.

---

## Suivi des passations
Voir `docs/handoffs/` — un fichier par étape, avec le prompt prêt à injecter et la rétrospective en fin d'étape.
