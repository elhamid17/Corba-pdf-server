# Journal des décisions d'architecture (ADR)

> Registre des décisions structurantes du projet. Chaque ADR est immuable une fois acté ;
> une décision qu'on révise donne lieu à un nouvel ADR qui *supersede* l'ancien.
> Tenu par le Tech Lead. Dernière mise à jour : 2026-06-02.

---

## ADR-0001 — Évolution chirurgicale plutôt que réécriture
**Statut :** Acté
**Contexte :** ~22 000 LOC fonctionnelles et testées (41 handlers PDF + frontend React riche).
La tentation initiale était une « refonte totale, nouveau stack ».
**Décision :** On **n'effectue aucune réécriture from scratch**. On fait évoluer chirurgicalement :
on garde le frontend et les 41 handlers testés, on retire le mort (CORBA), on monte en grade par versions.
**Conséquences :** Risque minimal, valeur préservée. Interdiction permanente de « tout reprendre à zéro ».
**Justification :** Le mythe du rewrite est l'erreur d'ingénierie la plus documentée. Le bottleneck
du traitement PDF est la librairie (PDFBox), pas le langage.

## ADR-0002 — Suppression de CORBA, monolithe modulaire
**Statut :** Acté
**Contexte :** JacORB 3.9 (≈2017) relie deux process Java qui s'échangent des `byte[]`. Coût : IDL à
maintenir, sérialisation IIOP, fichier IOR partagé, 2e JVM, hack supervisord en prod.
**Décision :** Retirer entièrement CORBA. Les 41 handlers fusionnent dans la gateway comme couche
service **in-process**. Architecture cible = **monolithe modulaire** Spring Boot (un seul déployable).
**Conséquences :** Disparition de `corba-idl`, `corba-server`, JacORB, IOR, supervisord, mono-conteneur Render.
**Justification :** « Grade entreprise » ≠ microservices. Pour un solo, le monolithe modulaire est le
choix senior correct. Les microservices seraient un poison organisationnel à cette échelle.

## ADR-0003 — Modèle économique : open-core / self-host-first
**Statut :** Acté
**Contexte :** Ambition SaaS commercial, mais ressources = solo + budget ~0.
**Décision :** Produit ouvert et auto-hébergeable (modèle Stirling-PDF / Plausible / Supabase early).
L'utilisateur porte le compute en self-host. Monétisation ultérieure = tier hébergé + features pro.
**Justification :** Seul modèle SaaS viable sans capital. Aligné avec le positionnement local-first.

## ADR-0004 — Local-first séquencé
**Statut :** Acté
**Décision :** « Local-first » s'entend en deux temps :
- **Maintenant (A)** : zéro-rétention + auto-hébergeable, traitement serveur (on garde les 41 handlers).
- **V5 (B)** : traitement navigateur WASM pour les opérations sensibles uniquement.
**Justification :** A est compatible avec l'évolution chirurgicale et finançable à budget 0
(le compute est chez l'utilisateur). B est le moat ultime mais radical — on l'introduit quand le socle est prêt.

## ADR-0005 — Décisions de stack
**Statut :** Acté
**Garde :** React/Vite, Spring Boot/Java, PDFBox + handlers, MongoDB.
**Upgrade :** Java 17 → **21 LTS** (virtual threads, idéal I/O PDF + free tier) ; PDFBox 2.x → 3.x.
**Ajoute par versions :** queue de jobs, OpenTelemetry, OpenAPI, CI/CD.
**Rejette :** réécriture Rust/Go, microservices, Kafka, Kubernetes (avant V5).
**Justification :** Éviter la « langue de bois techno ». Optimiser la vitesse de livraison, pas la mode.

## ADR-0006 — Différenciation séquencée (pas simultanée)
**Statut :** Acté
**Contexte :** Les 4 axes (confidentialité, workflows, IA, verticale) choisis en même temps = 4 entreprises
de travail pour une personne. Impossible de front.
**Décision :** Séquencer, sans en abandonner aucun :
1. **Confidentialité / local-first** = colonne vertébrale + modèle éco.
2. **Workflows chaînés** = fer de lance produit.
3. **IA documentaire** = couche premium (BYO-key / modèle local pour respecter budget 0).
4. **Verticale métier** = packaging go-to-market, pas une décision d'architecture.
