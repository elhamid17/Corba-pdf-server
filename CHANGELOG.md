# Changelog

Toutes les versions notables de CORBA PDF Suite.

Format : [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) — Versions : [SemVer](https://semver.org/lang/fr/).

---

## [2.0.0] — 2026-05-22

### 🎉 Refonte majeure : 35 services + UX pro

**Tag git :** `v2.0.0`
**Branche backup avant overhaul :** `backup/v1.0-stable`
**Tag avant overhaul :** `v1.0-stable`

### ✨ Nouveaux services backend (14 ajouts)

#### Conversions
- **PDF → PowerPoint** (PPTX) — une slide par page via Apache POI XSLF
- **PDF → Markdown** — extraction avec heuristiques de mise en forme
- **Markdown → PDF** — parser CommonMark → rendu stylisé (titres, listes, code, citations)
- **HTML → PDF** — parser jsoup → rendu structure (h1-h6, p, ul/ol, blockquote, pre)
- **Excel → PDF** — XLSX → grille PDF paysage
- **ODT → PDF** — OpenDocument Text via odfdom

#### Sécurité
- **Déverrouillage** — retire la protection mot de passe (PDFBox `setAllSecurityToBeRemoved`)
- **Vérification signature** — liste les signatures PKCS#7 avec validité cryptographique (BouncyCastle)
- **PDF/A** — marqueur PDFA-1B + métadonnées XMP RDF/XML (intégré comme option de Compression)

#### Analyse
- **Comparer 2 PDFs** — diff textuel page par page (lignes ajoutées/retirées)
- **Statistiques** — pages, mots, caractères, lignes, paragraphes, langue détectée (intégré dans le panneau Aperçu)

#### Génération
- **QR code** — ZXing, position + taille configurables
- **Code-barres** — CODE_128, EAN_13/8, CODE_39, ITF, UPC_A, PDF_417
- **Générateur de CV** — formulaire structuré → PDF mis en page

### 📸 Scanner caméra (feature flagship)

- Capture multi-photos via `navigator.mediaDevices.getUserMedia()`
- Bascule caméra avant/arrière (mobile)
- Filtres pure JS Canvas : Couleur / Niveaux de gris / Noir & blanc
- Drag-drop pour réorganiser les pages capturées
- **Mode intelligent** (toggle) : OpenCV.js (lazy load 7 Mo)
  - Détection automatique des bords du document (Canny + approxPolyDP)
  - Correction de perspective (warpPerspective)
  - Éditeur SVG avec 4 coins draggables pour ajuster
  - Filtre adaptive threshold (effet "scan Adobe")
- Assemblage final en PDF via l'endpoint existant `/images-to-pdf`

### 🚀 Features pro UX

1. **PDF Preview inline** sur toutes les pages outils
   - pdf.js lazy-loaded (450 Ko, pas dans le bundle initial)
   - Navigation page par page (← →)
   - Bouton **Stats** intégré au panneau pour analyse contextuelle

2. **Progress bar temps réel** sur 41 services
   - XMLHttpRequest + `xhr.upload.onprogress` (fetch ne le permet pas)
   - 4 phases visuelles : `upload` / `processing` (shimmer) / `download` / `done`
   - Pourcentage live + icône de phase + remplissage du bouton

3. **Workflow chaining** — enchaînement intelligent
   - Capture du blob résultant après chaque opération
   - Bandeau "Et après ?" avec 3 suggestions contextuelles
   - **Auto-transfert** du fichier dans le DropZone de l'outil suivant

4. **Récemment utilisés + Favoris** (localStorage)
   - Sections personnelles sur la Home
   - Bouton ★ sur chaque page outil pour épingler
   - Hook `useToolHistory` avec pub-sub interne

5. **Onboarding tour** à la 1ère visite
   - 4 étapes : Bienvenue → Recherche → Scanner → Ctrl+K
   - Spotlight CSS + tooltip Framer Motion
   - Persistence via `corba_pdf_onboarded` (localStorage)

### 🎨 UI / UX overhaul

- **Home restructurée** : barre de recherche fuzzy (sans accents), 8 chips de catégorie, section "Nouveautés", grille avec `auto-rows-fr`
- **Motion (Framer Motion)** : fade-in pages, stagger des cartes, modals scale-up, hero avec 2 orbes animées + 6 icônes flottantes décoratives, halo pulse sur CTA
- **Illustrations SVG custom** : empty history (document + horloge), no results (loupe), 404 (gradient)
- **Personnalité visuelle** : hover lift + shadow coloré sur ServiceCard, bordures subtiles, dark mode polish
- **Mobile** : chips scrollables horizontalement, layout adaptatif

### 🧹 Cleanup : 43 → 35 services

#### Supprimés (5)
| Service | Raison |
|---|---|
| Inverser | Redondant avec Réorganisation |
| Suppression | Fusionné avec Extraction → "Sélection de pages" |
| Création (texte) | Markdown→PDF fait la même chose en mieux |
| Redimensionner | Niche (<1% des utilisations) |
| Statistiques | Devient bouton contextuel dans PdfPreview |

#### Fusionnés (4 → 2)
| Avant | Après |
|---|---|
| Filigrane + Tampon | **Marquage du document** (2 modes) |
| QR code + Code-barres | **Code-barres / QR** (type au choix) |

#### Déplacés (1)
| Service | Nouveau emplacement |
|---|---|
| PDF/A | Case à cocher dans Compression |

#### Renommés (1)
| Avant | Après |
|---|---|
| Extraction | **Sélection de pages** (toggle keep/remove) |

#### Redirects HTTP (préservation bookmarks)
`/watermark`, `/stamp` → `/marking`
`/qr`, `/barcode` → `/code`
`/extract-pages`, `/delete-pages` → `/select-pages`
`/to-pdfa` → `/compress`
`/create` → `/markdown-to-pdf`
`/reverse` → `/reorder`
`/resize`, `/stats` → `/`

### 🛠 Stack ajoutée

**Backend**
- `org.commonmark:commonmark` 0.22.0 — parser Markdown
- `org.jsoup:jsoup` 1.17.2 — parser HTML
- `org.odftoolkit:odfdom-java` 0.12.0 — lecture ODT
- `com.google.zxing:core` + `javase` 3.5.3 — QR/barcodes

**Frontend**
- `framer-motion` ^12 — animations
- `pdfjs-dist` ^5 — preview PDF inline (lazy load)
- OpenCV.js 4.10.0 — via CDN jsdelivr (lazy load pour scanner smart)

### 🔄 Rollback

**Catastrophe totale** :
```bash
git reset --hard v1.0-stable
git push -f origin main
```

**Bug spécifique** : `git revert <SHA>` du commit concerné (backend ou frontend).

**Récupération extrême** : `git checkout backup/v1.0-stable -b recovery`.

---

## [1.0.0] — 2026-05-18

### Version stable initiale (avant overhaul)

**Tag git :** `v1.0-stable`

- 43 services PDF basiques (organisation, conversions, édition, sécurité, OCR, génération)
- Architecture CORBA/JacORB + Spring Boot + React + Docker
- Authentification JWT + MongoDB + GridFS
- Historique utilisateur + admin panel
- Déploiement Render avec MongoDB Atlas
- Dark mode, PWA installable, command palette (Ctrl+K)
