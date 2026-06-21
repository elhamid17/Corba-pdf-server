# Prompt de passation — V1 / Étape 1.1 — Module `pdf-engine` in-process

> À copier-coller tel quel dans une **nouvelle conversation dédiée**. Cette conversation exécute
> UNIQUEMENT l'étape 1.1. Le Tech Lead (conversation principale) validera le retour avant l'étape 1.2.

---

## §1. NORTH STAR (immuable)
- **Vision :** SaaS PDF open-core, self-host-first, local-first. Fer de lance = workflows chaînés.
- **Modèle :** open-core / self-host-first (l'utilisateur porte le compute).
- **Stratégie :** ÉVOLUTION chirurgicale — **jamais** de réécriture from scratch.
- **Archi cible :** monolithe modulaire Spring Boot, Java 21. Pas de microservices, pas de CORBA.
- **Réf. décisions :** `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`.

## §2. ÉTAT ACTUEL
- 3 modules Maven : `corba-idl` (IDL 42 ops), `corba-server` (JacORB 3.9 + `PDFServiceImpl` + 41 handlers
  dans `sn.ussein.pdfserver.handlers`, util dans `.util`), `api-gateway` (Spring Boot 3, Java 17).
- La gateway parle aux handlers via CORBA (`CorbaClientService` → proxy `sn.ussein.pdf.PDFService`).
- Chaque handler a un test unitaire dans `corba-server/src/test/.../handlers/`.
- Branche de départ : `main`. Crée une branche de travail dédiée.

## §3. OBJECTIF DE CETTE ÉTAPE (scope strict)
Extraire la logique PDF de CORBA pour qu'elle soit appelable **en in-process** par du code Java standard.

**Dans le périmètre :**
- Créer un module `pdf-engine` (ou package) contenant les 41 handlers + util + types métier, **sans aucune
  dépendance CORBA/JacORB**.
- Définir une interface Java propre `PdfEngine` exposant les ~42 opérations (signatures avec `byte[]`,
  `String`, types métier — PAS les types générés CORBA).
- Migrer les tests unitaires des handlers vers ce module ; ils doivent rester **verts**.

**HORS périmètre (ne pas toucher à cette étape) :**
- La gateway (`PDFController`, `CorbaClientService`) — c'est l'étape 1.2.
- La suppression de `corba-idl` / `corba-server` / JacORB — étape 1.2.
- L'infra Docker / Render / supervisord — étape 1.3.
- Le frontend.

## §4. INVARIANTS À NE JAMAIS CASSER
1. Les 41 tests de handlers restent verts après migration.
2. Aucune logique métier PDF n'est modifiée — on **déplace et on adapte les signatures**, on ne réécrit pas.
3. **Validation locale avant tout commit/push** : l'utilisateur valide d'abord (`docker compose` / `mvn test`).
4. Java 21 (upgrade autorisé dans cette étape si nécessaire) ; PDFBox peut rester en 2.x pour l'instant.

## §5. LIVRABLES ATTENDUS + DEFINITION OF DONE
- Module/package `pdf-engine` compilable indépendamment, sans import `org.omg.*` ni `org.jacorb.*`.
- Interface `PdfEngine` + implémentation `PdfEngineImpl` déléguant aux 41 handlers.
- Tests des handlers migrés et verts (`mvn test` sur le module).
- Une note de synthèse listant le mapping IDL-op → méthode `PdfEngine` et les points d'attention pour 1.2.
- **DoD :** `mvn -pl pdf-engine test` est vert ; `grep -r "org.omg\|jacorb" pdf-engine/src` ne retourne rien.

## §6. CRITÈRES DE VALIDATION (par le Tech Lead)
- Le module compile et teste seul, zéro dépendance CORBA.
- L'interface `PdfEngine` est exhaustive (couvre les 42 ops de l'IDL) et idiomatique Java.
- Aucun handler n'a vu sa logique métier altérée (diff = déplacement + adaptation de signature uniquement).

## §7. GARDE-FOUS / ANTI-PATTERNS
- ❌ NE PAS réécrire les handlers « parce que tu peux faire mieux ». On migre, point.
- ❌ NE PAS encore brancher la gateway ni supprimer les modules CORBA (étapes suivantes).
- ❌ NE PAS introduire de framework/lib non justifié (pas de Spring dans le module engine si évitable —
  garde-le en POJO réutilisable).
- ❌ NE PAS commiter/pusher sans validation locale de l'utilisateur.

## §8. PROTOCOLE DE RETOUR (pour la rétrospective)
Rapporter au Tech Lead :
1. Structure finale du module + signature de `PdfEngine`.
2. Résultat `mvn test` (copie de la ligne de synthèse).
3. Difficultés rencontrées (types CORBA récalcitrants, deps, etc.).
4. Risques identifiés pour l'étape 1.2 (branchement gateway).
5. Tout écart par rapport au scope, justifié.

---
## ⚠️ REPRISE — première tentative incomplète (coupée par limite de session)

Un premier sous-agent a démarré dans le worktree `worktree-agent-a510e30017820fe82`
(commit WIP `wip(pdf-engine): étape 1.1 INCOMPLÈTE`). **Ne pas merger en l'état.**

**Déjà fait :** copie des 41 handlers + `util/` + package `sn.ussein.pdfengine.model`
(WatermarkOptions, ConvertOptions, CompressOptions, PDFMetadata, PDFResult, et les
exceptions PDFException/InvalidPageException/PasswordException) dans `pdf-engine/src`.

**RESTE À FAIRE (par ordre) :**
1. [ ] Créer `pdf-engine/pom.xml` (s'inspirer de `corba-server/pom.xml` pour les deps
       PDFBox/Tess4J/BouncyCastle/POI/ZXing/commonmark/jsoup/odfdom ; retirer toute dep JacORB/corba-idl).
2. [ ] Retirer les types CORBA des **37 handlers** qui importent encore `org.omg.*` ou
       `sn.ussein.pdf.*` : remplacer PDFData/PDFResult→`byte[]`, PDFList→`byte[][]`/`List<byte[]>`,
       PageList→`int[]`, options CORBA→POJO de `sn.ussein.pdfengine.model`.
3. [ ] Écrire l'interface `sn.ussein.pdfengine.PdfEngine` (42 ops) + `PdfEngineImpl` qui délègue aux handlers.
4. [ ] Enregistrer `<module>pdf-engine</module>` dans le `pom.xml` racine.
5. [ ] `mvn -pl pdf-engine test` vert ; `grep -rE "org\.omg|jacorb" pdf-engine/src` → vide.
6. [ ] Ne pas pousser ; rapporter au Tech Lead (cf. §8).

Repartir du worktree existant OU recréer un module propre depuis `main` en réutilisant la copie déjà faite.

---
*Rétrospective (à remplir en fin d'étape) :*
- Livré : …
- Écarts : …
- Prompt d'amorçage 1.2 : …
