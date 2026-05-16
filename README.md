# La fabrique de lunettes

Système distribué de commande et de fabrication de lunettes connectées
(frontend JavaFX + backend MQTT + usine + fabricateur).

Projet M1 MIAGE — cours **Programmation Avancée**.

## Équipe

- Abdellahi Mohamed Lemine
- Taha Araar
- Hakim Si Ameur
- Sanae Acharkaoui

Détails dans le rapport PDF joint au rendu.

## Prérequis

- Java 24
- Mosquitto (broker MQTT) sur `localhost:1883`, sans authentification

## Lancement

Depuis la dernière release GitHub, télécharger :
- `serveur-fabrique.jar`
- `lunettes-connectees-frontend.jar`

Puis dans deux terminaux :

```bash
java -jar serveur-fabrique.jar
java -jar lunettes-connectees-frontend.jar
```

Le backend lit `config.properties` à côté du jar s'il existe, sinon utilise les
valeurs par défaut. Le frontend embarque sa config dans le jar et peut être
surchargé par variable d'environnement (ex : `MQTT_BROKER_URL`).

## Lancement depuis les sources

```bash
mvn package -DskipTests
java -jar backend/target/serveur-fabrique.jar
cd frontend && mvn javafx:run
```

`bernard-flou:fabricateur` étant sur un dépôt Maven privé,
l'authentification doit être dans `~/.m2/settings.xml`.

## Protocole

Topics : `orders/{commandeId}`, `orders/{commandeId}/{validated,cancelled,delivery,error,status}`,
`serials/{serial}/check`, `serials/{serial}`.

Format de sérialisation maison `CHAMP=valeur|CHAMP=valeur`, exemple :

```
CMD_ID=abc-123|LUNETTES=CHATGPT:2,CLAUDE:1|TOTAL=3
```

Voir le rapport pour la spécification complète.

## Industrialisation

Workflows GitHub Actions dans `.github/workflows/` :
- build du projet sur chaque push/PR
- les jars frontend et backend attachés à chaque release
- publication de l'usine sur GitHub Packages à chaque release
