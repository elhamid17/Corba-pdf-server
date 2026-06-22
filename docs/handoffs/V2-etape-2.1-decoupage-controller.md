# Prompt de passation — V2 / Étape 2.1 — Découpage du `PDFController`

> À copier-coller dans une conversation dédiée. Prérequis : V1 livrée et mergée dans `main`
> (monolithe mono-service, `mvn clean package` vert, runtime validé).
> ⚠️ Déléguer SANS isolation worktree (ou merger d'abord) — cf. mémoire « Délégation worktree ».
> Travailler sur une branche dédiée à partir de `main`, ex. `v2/quality`.

---

## §1. NORTH STAR (immuable)
- SaaS PDF open-core, self-host-first, local-first. Fer de lance = workflows chaînés.
- Stratégie : ÉVOLUTION chirurgicale, JAMAIS de réécriture.
- Archi : monolithe modulaire Spring Boot, Java 21, in-process (`PdfEngine`).
- Réf. : `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`.

## §2. ÉTAT ACTUEL
- `api-gateway/.../controller/PDFController.java` ≈ **1190 lignes**, ~43 endpoints, fortement répétitifs :
  chaque endpoint refait validation + try/catch + `pdfResponse`/`zipResponse`/`buildAndRecord` + mapping d'erreurs.
- Le moteur est injecté via l'interface `sn.ussein.pdfengine.PdfEngine`.
- Tests : `PDFControllerTest`, `PDFControllerReadOnlyTest`, `PDFControllerTransformTest`,
  `PDFControllerOrganisationTest` (base commune `support/AbstractPDFControllerTest`). Tous verts.

## §3. OBJECTIF STRICT
Rendre le code maintenable en cassant le contrôleur monolithique — **sans changer le comportement**.

DANS LE PÉRIMÈTRE :
1. Scinder `PDFController` en contrôleurs par domaine, ex. : `OrganisationController` (merge/split/extract/
   delete/reorder/reverse/rotate…), `ConversionController` (pdf↔word/excel/pptx/markdown/html/odt/images…),
   `SecurityController` (password/unlock/sign/signature-image/redact/anonymize…), `AnalysisController`
   (extract-text/ocr/stats/compare/metadata/verify-signature…), `GenerationController` (create/cv/qr/barcode/
   cover/page-numbers/watermark/stamp…). Le découpage exact est au jugement, mais cohérent par domaine métier.
2. Extraire la logique transverse répétée (validation de fichier, construction de réponse PDF/ZIP,
   enregistrement de job, mesure de durée) dans une **base commune** ou un **service/composant partagé**
   (ex. `AbstractPdfController` ou `PdfResponseSupport`), pour supprimer la duplication.
3. Adapter/scinder les tests en conséquence ; tous restent verts.

HORS PÉRIMÈTRE :
- Changer les routes, params ou formats de réponse (contrat REST GELÉ — le frontend ne doit rien voir).
- OpenAPI/versioning (étape 2.2), sécurité (2.3), CI (2.4).
- Toucher au moteur `pdf-engine` ou au frontend.

## §4. INVARIANTS À NE JAMAIS CASSER
1. **Contrat REST strictement identique** : mêmes URLs, mêmes paramètres, mêmes codes/corps de réponse.
2. `mvn clean package` vert (unitaires + intégration).
3. Aucun changement de comportement observable (mêmes 400/422/500 qu'avant).
4. **Validation locale (docker compose) avant tout commit/push** ; l'utilisateur valide d'abord.

## §5. DEFINITION OF DONE
- `PDFController` n'existe plus en l'état (ou ne garde que `/ping`) ; logique répartie par domaine.
- Duplication transverse factorisée dans une base/service partagé.
- `mvn clean package` vert ; aucun endpoint perdu (comparer la liste des routes avant/après).
- Diff = restructuration pure, pas de modification de logique métier.

## §6. GARDE-FOUS / ANTI-PATTERNS
- ❌ NE PAS « améliorer » le comportement des endpoints au passage.
- ❌ NE PAS introduire de couche d'abstraction spéculative (pas de sur-ingénierie) — juste retirer la duplication.
- ❌ NE PAS modifier le contrat REST « tant qu'à faire ».
- ❌ NE PAS pousser sans validation locale de l'utilisateur.

## §7. PROTOCOLE DE RETOUR
1. Liste des nouveaux contrôleurs + répartition des endpoints (tableau domaine → routes).
2. Ce qui a été factorisé (base/service partagé) et le gain de duplication.
3. Comparaison des routes avant/après (preuve : aucun endpoint perdu).
4. Résultat `mvn clean package`.
5. Risques pour l'étape 2.2 (OpenAPI/versioning).
6. Écarts au scope, justifiés.

---
*Rétrospective (à remplir en fin d'étape) :*
- Livré : …
- Écarts : …
- Prompt d'amorçage 2.2 : …
