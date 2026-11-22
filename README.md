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

## Tests

Tests unitaires et HTTP :

```bash
sudo docker compose --progress=plain build app
```

Tests d'intégration avec Oracle :

```bash
sudo docker compose \
  -f compose.yaml \
  -f compose.verify.yaml \
  run --rm verify
```

Les tests d'intégration utilisent de vraies transactions
Oracle, annulées après chaque test.

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
