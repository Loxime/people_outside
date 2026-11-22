# PeopleOutside

API REST permettant de signaler et de consulter l'affluence
dans des lieux extérieurs.

**Stack :** Java 25, Spring Boot 4, Spring Data JPA,
Oracle Database Free, Docker Compose.

## Fonctionnalités

- Création de signalements d'affluence.
- Recherche géographique avec la formule Haversine.
- Filtrage des signalements de la journée en Europe/Paris.
- Persistance des données dans Oracle.
- Validation stricte des entrées HTTP et JSON.

L'application ne nécessite ni compte utilisateur
ni authentification.

## Prérequis

- Docker et Docker Compose
- Git
- curl

Java, Maven et Oracle fonctionnent exclusivement dans Docker.

## Installation

Cloner le dépôt :

```bash
git clone https://github.com/Loxime/people_outside.git
cd people_outside
```

Créer la configuration locale :

```bash
cp .env.example .env
```

Configurer les deux mots de passe dans `.env` :
`ORACLE_PASSWORD` pour l'administration Oracle et
`APP_DB_PASSWORD` pour le compte applicatif `PO_APP`.
Utiliser deux mots de passe forts et distincts.

Le script `setup.sh` impose à `APP_DB_PASSWORD` le format
`P0_` suivi de 40 caractères hexadécimaux minuscules.

Pour générer une valeur compatible, exécuter :

`python3 -c 'import secrets; print("P0_" + secrets.token_hex(20))'`

Exécuter cette commande deux fois pour obtenir deux mots de
passe distincts, puis renseigner les deux variables dans `.env`.

Si la base Oracle existe déjà, modifier `.env` ne change pas
automatiquement le mot de passe du compte `PO_APP`.

Ne jamais versionner `.env`.

### Démarrer Oracle

```bash
sudo docker compose up -d --wait oracle
```

Initialiser le schéma :

```bash
sudo docker compose exec -T oracle \
  bash /opt/oracle/scripts/setup/peopleoutside-setup.sh
```

### Construire l'application

```bash
sudo docker compose --progress=plain build app
```

### Démarrer l'API

```bash
sudo docker compose up -d app
```

Consulter les journaux :

```bash
sudo docker compose logs --tail=80 app
```

Attendre le message `Started DmdApplication`.

L'API écoute sur `http://127.0.0.1:8080`.

## API REST

### POST /reports

Crée un signalement d'affluence.

```bash
curl -i -X POST http://127.0.0.1:8080/reports \
  -H 'Content-Type: application/json' \
  -d '{
    "latitude": 48.8566,
    "longitude": 2.3522,
    "placeType": "PARK",
    "crowdLevel": 2
  }'
```

Réponse : HTTP 201 avec les coordonnées, le type de lieu,
le niveau d'affluence, un identifiant et un horodatage.

L'identifiant et l'horodatage sont générés par le serveur.

### GET /reports

Recherche les signalements du jour à proximité
d'une position.

```bash
curl -i -G http://127.0.0.1:8080/reports \
  --data-urlencode 'latitude=48.8566' \
  --data-urlencode 'longitude=2.3522' \
  --data-urlencode 'radiusKm=10'
```

Réponse : HTTP 200 avec un tableau JSON.

La recherche utilise une distance Haversine à vol d'oiseau.
Le rayon est inclusif, et les bornes temporelles sont
calculées en Europe/Paris.

Un résultat vide est représenté par `[]`.

## Validation

| Paramètre | Contraintes |
| --- | --- |
| latitude | Nombre entre -90 et 90 |
| longitude | Nombre entre -180 et 180 |
| placeType | PARK, BEACH, PLAYGROUND, SPORTS_FIELD, PICNIC_AREA |
| crowdLevel | Entier de 1 à 5 |
| radiusKm | Nombre strictement positif, au maximum 100 |

Les paramètres obligatoires manquants, les valeurs invalides
et les types JSON non conformes entraînent HTTP 400.

## Tests et couverture

La construction de l'image exécute les 67 tests Java
sans dépendre d'une base Oracle accessible :

```bash
sudo docker compose --progress=plain build app
```

Pour exécuter les 78 tests, dont les tests d'intégration
avec Oracle, et contrôler la couverture JaCoCo :

```bash
sudo docker compose \
  -f compose.yaml \
  -f compose.verify.yaml \
  run --rm verify
```

Le profil Maven `full-coverage` impose au minimum :

- 95 % des lignes couvertes ;
- 95 % des branches couvertes.

Ces seuils concernent la suite complète, avec Oracle.
Ils ne sont pas appliqués au build Docker autonome.

Le rapport JaCoCo est généré dans le volume Docker
`verify_target`. Pour consulter les résultats CSV :

```bash
sudo docker compose \
  -f compose.yaml \
  -f compose.verify.yaml \
  run --rm verify \
  cat /workspace/target/site/jacoco/jacoco.csv
```

Les tests d'intégration utilisent de vraies transactions
Oracle, annulées à la fin de chaque test.

## Données de démonstration

Le script `scripts/oracle/demo_reports.sql` prévoit :

- Deux signalements proches de Paris, aujourd'hui.
- Un signalement éloigné de Paris, aujourd'hui.
- Un signalement proche de Paris, hier.

Exécution :

```bash
sudo docker compose exec -T oracle bash -c '
{
    printf "CONNECT PO_APP/\"%s\"@//localhost:1521/FREEPDB1\n" \
      "$APP_DB_PASSWORD"
    cat
} | sqlplus -L -s /nolog
' < scripts/oracle/demo_reports.sql
```

Le script évite de réinsérer les mêmes exemples pour
une date locale identique.

Les données restent conservées après l'exécution.

### Démonstration HTTP

Après avoir initialisé Oracle, chargé les données
de démonstration et démarré l'API :

- `http/demo.http` : création, recherche et tableau vide.
- `http/validation.http` : requêtes invalides et réponses 400.

Ces fichiers sont compatibles avec REST Client pour VS Code.

Lors de la soutenance, présenter une création HTTP 201,
puis une recherche HTTP 200, et comparer les résultats
avec les données du jour et celles de la veille.

## Démonstration pour l'évaluation (15 minutes)

### Préparation avant la soutenance

1. Démarrer Oracle et l'API avec Docker Compose.
2. Initialiser le schéma Oracle si nécessaire.
3. Exécuter `scripts/oracle/demo_reports.sql`.
4. Vérifier que les services sont opérationnels :

```bash
sudo docker compose ps
```

Le jeu de données comprend des signalements du jour et
de la veille, proches et éloignés de Paris.

### Déroulement

| Durée | Démonstration | Résultat attendu |
| --- | --- | --- |
| 0–2 min | Présenter l'architecture Spring / Oracle | Expliquer les composants |
| 2–5 min | `POST /reports` via `http/demo.http` | HTTP 201, ID et timestamp serveur |
| 5–9 min | `GET /reports` | HTTP 200, filtrage du jour et du rayon, tableau vide |
| 9–11 min | Requêtes de `http/validation.http` | HTTP 400 |
| 11–13 min | Redémarrer uniquement l'API, puis rechercher le signalement créé | Signalement conservé |
| 13–15 min | Présenter les tests et JaCoCo | 78 tests et seuils de couverture validés |

### Points à expliquer

- Les coordonnées, le type de lieu et l'affluence sont
  vérifiés avant l'enregistrement.
- L'identifiant et l'horodatage sont attribués par le serveur.
- Chaque observation est indépendante, même au même endroit.
- Les distances sont calculées avec la formule Haversine.
- La date du jour est déterminée dans le fuseau Europe/Paris.
- Les données anciennes et les observations hors rayon sont exclues.
- Les tests d'intégration utilisent une véritable base Oracle.

Le nombre exact de signalements et leurs identifiants
peuvent varier : ne pas utiliser de valeurs codées en dur
pour interpréter les résultats de la démonstration.

### Vérification complète

```bash
sudo docker compose   -f compose.yaml   -f compose.verify.yaml   run --rm verify
```

La validation complète doit exécuter 78 tests et respecter
les seuils JaCoCo de 95 % des lignes et des branches.

## Architecture

- `domain` : modèles métier et invariants.
- `application` : cas d'utilisation et ports.
- `infrastructure` : configuration et persistance Oracle.
- `web` : contrôleurs REST et validation HTTP.

Le domaine et les cas d'utilisation ne dépendent pas
de Spring ou d'Oracle.

## Arrêt

```bash
sudo docker compose down
```

Cette commande préserve les volumes persistants.

Ne pas utiliser `docker compose down -v` :
cette option supprimerait notamment les données Oracle.
