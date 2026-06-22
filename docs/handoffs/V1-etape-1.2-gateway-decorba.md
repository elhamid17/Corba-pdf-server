# Prompt de passation — V1 / Étape 1.2 — Brancher la gateway sur `PdfEngine` & supprimer CORBA

> À copier-coller dans une nouvelle conversation dédiée. Prérequis : l'étape 1.1 est terminée et validée
> (module `pdf-engine`, 62 tests verts, BUILD SUCCESS).

---

## §1. NORTH STAR (immuable)
- SaaS PDF open-core, self-host-first, local-first. Fer de lance = workflows chaînés.
- Stratégie : ÉVOLUTION chirurgicale, JAMAIS de réécriture.
- Archi cible : monolithe modulaire Spring Boot, **Java 21**, SANS CORBA.
- Réf. : `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`.

## §2. ÉTAT ACTUEL (sortie de 1.1)
- Nouveau module `pdf-engine` : interface `sn.ussein.pdfengine.PdfEngine` (54 méthodes, 42 ops IDL + utilitaires),
  `PdfEngineImpl` déléguant aux 41 handlers, POJO dans `sn.ussein.pdfengine.model`. `mvn -pl pdf-engine test` = 62/62 vert.
- `api-gateway` parle ENCORE à CORBA via `CorbaClientService` → proxy `sn.ussein.pdf.PDFService`.
- `corba-idl` et `corba-server` existent toujours et compilent.
- `PDFController` = 1190 LOC qui appelle `corba.getPdfService().xxx(...)`.

## §3. OBJECTIF DE CETTE ÉTAPE (scope strict)
Faire que la gateway utilise `PdfEngine` **en in-process** et supprimer toute trace de CORBA.

DANS LE PÉRIMÈTRE :
1. Ajouter une dépendance Maven `api-gateway` → `pdf-engine`.
2. Exposer `PdfEngine` comme bean Spring (`@Bean PdfEngine pdfEngine() { return new PdfEngineImpl(); }`).
3. Réécrire `PDFController` pour appeler `pdfEngine.xxx(...)` au lieu de `corba.getPdfService().xxx(...)`.
   Adapter les types : `byte[]`/`byte[][]`/`int[]` et POJO `.model` au lieu des types CORBA.
4. Supprimer `CorbaClientService`, la conf CORBA (host/port/ior), la lecture du fichier IOR.
5. Adapter `GlobalExceptionHandler` aux exceptions `sn.ussein.pdfengine.model.{PDFException, InvalidPageException, PasswordException}`.
6. Supprimer les modules `corba-idl` et `corba-server` (et leurs `<module>` du pom racine), retirer les deps JacORB.
7. Tests gateway verts (`mvn -pl api-gateway test`) + build complet vert (`mvn clean package`).

HORS PÉRIMÈTRE :
- L'infra Docker / Render / supervisord / docker-compose → étape 1.3.
- Le frontend (le contrat REST ne change PAS).
- Tout refactor du `PDFController` au-delà du strict rebranchement (le grand découpage = V2).

## §4. INVARIANTS À NE JAMAIS CASSER
1. **Contrat REST identique** : mêmes routes, mêmes params, mêmes réponses → le frontend ne doit rien voir.
2. Tous les tests restent verts (gateway + pdf-engine).
3. Aucune régression fonctionnelle des 40+ outils.
4. **Validation locale (docker compose / mvn) AVANT tout commit/push** : l'utilisateur valide d'abord.

## §5. RISQUES IDENTIFIÉS EN 1.1 (à traiter ici)
1. **Java 21 vs 17** : `pdf-engine` est en `release 21`. Brancher la gateway dessus impose Java 21 à la gateway
   et au reactor. → **Passer `api-gateway` en Java 21** et vérifier que le JDK de build/CI/Render est ≥ 21.
2. **`PDFResult.data` peut être `null` si `success=false`** → gérer le cas dans le controller (l'ancien chemin
   CORBA renvoyait peut-être un tableau vide).
3. **Mapping d'exceptions** : adapter `GlobalExceptionHandler` aux nouvelles exceptions `.model`.
4. **Dépendances** : ne dépendre que de `sn.ussein.pdfengine.{PdfEngine, model.*}`, jamais des handlers directement.
5. **slf4j/logback** : `pdf-engine` n'apporte que `slf4j-api` ; le binding vient de Spring Boot — vérifier l'absence de conflit.

## §6. LIVRABLES + DEFINITION OF DONE
- `api-gateway` compile et teste sans aucune dépendance CORBA.
- `grep -rE "org\.omg|jacorb|CorbaClientService|PDFServiceHelper" api-gateway/src` → vide.
- `corba-idl` et `corba-server` supprimés ; pom racine = `pdf-engine` + `api-gateway` uniquement.
- `mvn clean package` global vert.
- **DoD :** build complet vert + contrat REST inchangé (vérifiable via les tests de `PDFController*Test`).

## §7. GARDE-FOUS / ANTI-PATTERNS
- ❌ NE PAS modifier le contrat REST ni le frontend.
- ❌ NE PAS faire le grand refactor du controller de 1190 lignes (c'est la V2) — rebranchement minimal.
- ❌ NE PAS toucher à l'infra Docker/Render (étape 1.3).
- ❌ NE PAS pousser sans validation locale de l'utilisateur.

## §8. PROTOCOLE DE RETOUR
1. Diff de principe `PDFController` (avant/après sur 2-3 endpoints représentatifs).
2. Résultat exact `mvn clean package` (lignes Tests run).
3. Confirmation grep CORBA vide + liste des fichiers/modules supprimés.
4. Risques résiduels pour l'étape 1.3 (Docker mono-conteneur → mono-service).
5. Écarts au scope, justifiés.

---
*Rétrospective (à remplir en fin d'étape) :*
- Livré : …
- Écarts : …
- Prompt d'amorçage 1.3 : …
