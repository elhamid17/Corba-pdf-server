# CORBA PDF Server

[![CI](https://github.com/elhamid17/Corba-pdf-server/actions/workflows/ci.yml/badge.svg)](https://github.com/elhamid17/Corba-pdf-server/actions/workflows/ci.yml)

Serveur distribué de traitement PDF basé sur CORBA/JacORB, exposé via une API REST Spring Boot, avec un frontend React.

## Stack technique

| Couche | Technologie |
|---|---|
| Frontend | React 18 + Vite + TailwindCSS |
| API | Spring Boot 3 + REST/JSON |
| Serveur CORBA | JacORB + Java 11 |
| Traitement PDF | Apache PDFBox 2.x + Tess4J + BouncyCastle |
| Infrastructure | Docker Compose + Nginx |

## Services PDF disponibles

- Fusion de PDF
- Découpage de PDF
- Extraction de pages
- Suppression de pages
- Compression
- Rotation
- Filigrane (watermark)
- Protection par mot de passe
- Conversion PDF → Images
- Extraction de texte
- OCR (Tesseract)
- Signature numérique
- Métadonnées XMP
- Création de PDF

## Lancement rapide

```bash
docker compose up --build
```

Frontend : http://localhost  
API : http://localhost/api

## Auteur

Projet académique — L2 AgroTIC, USSEIN
