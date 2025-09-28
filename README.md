# Jeu de Carré en Réseau (Java) 🎮

Ce projet est une implémentation en Java d'un jeu de carré (semblable au Gomoku ou Morpion à 5) jouable en réseau local (LAN). Il met en œuvre une architecture client-serveur, un protocole de communication personnalisé et une interface graphique réalisée avec Swing.

Ce projet a été développé dans le cadre du module de **Conception et Architecture des Réseaux**.

## Aperçu du Jeu

*(Capture d'écran de deux fenêtres de jeu, une pour le serveur et une pour le client, en cours de partie.)*

## Fonctionnalités ✨
- **Mode Client-Serveur** : Un joueur héberge la partie et l'autre s'y connecte via une adresse IP.
- **Grille Configurable** : Le joueur qui héberge la partie peut choisir la taille de la grille (entre 5x5 et 25x25).
- **Noms de Joueurs Personnalisés** : Les messages et le titre de la fenêtre affichent les pseudonymes des joueurs.
- **Option "Rejouer"** : À la fin d'une partie, les joueurs peuvent choisir de lancer une nouvelle partie sans redémarrer l'application.
- **Gestion Robuste des Connexions** :
    - Notification claire en cas de déconnexion d'un adversaire.
    - Le serveur se réinitialise après une déconnexion pour accueillir un nouveau joueur.
    - Un client ne peut pas rejoindre une partie déjà en cours ou terminée.
- **Interface Graphique** : Interface simple et intuitive développée avec Java Swing.

## Architecture et Conception 🔧
- **Langage** : Java 21 (compatible JDK 9+).
- **Réseau** :
    - Modèle : **Client-Serveur**.
    - Protocole de Transport : **TCP** pour garantir une communication fiable et ordonnée des coups.
    - Sockets : Utilisation de `java.net.Socket` et `java.net.ServerSocket`.
- **Interface Graphique (GUI)** :
    - Bibliothèque : **Java Swing**.
    - Threading : Utilisation de `SwingUtilities.invokeLater` pour toutes les mises à jour de l'interface afin de garantir la sécurité des threads.
- **Modèle de Conception** : Le projet suit une architecture inspirée de **Modèle-Vue-Contrôleur (MVC)** pour séparer la logique du jeu (`GameLogic`), l'affichage (`GameUI`) et la gestion des actions/réseau (`GameServer`, `GameClient`).

## Prérequis
- [Java Development Kit (JDK)](https://www.oracle.com/java/technologies/downloads/) - Version 21 (compatible avec la version 9 ou supérieure).

## Installation et Compilation
1.  Clonez ou téléchargez ce dépôt sur votre machine locale.
2.  Ouvrez un terminal et naviguez jusqu'au dossier racine du projet.

## Comment Jouer 🚀

Pour jouer, vous devez lancer l'application deux fois : une pour le serveur et une pour le client.

**Procédure :** Deux méthodes sont possibles pour compiler le projet depuis son dossier racine :
### 1.Méthode Recommandée (Avec les Scripts) :
- Sous Linux ou macOS : Ouvrez un terminal et exécutez le script
  ```bash
  ./compiler.sh
  ```
- Sous Windows : Ouvrez une invite de commandes et exécutez le script compiler.bat. Ces scripts exécutent la commande de compilation pour vous.
### 2.Méthode Manuelle :
- Ouvrez un terminal ou une invite de commandes.
- Assurez-vous d'être dans le dossier racine du projet:
```bash
cd src/
```
- Exécutez la commande suivante :
```bash
javac jeux/*.java
```

À la fin de ce processus, les fichiers .class correspondants seront générés dans le dossier jeux.
## B.Exécution de l'Application
### Étape 1 : Lancement
- **Avec les scripts :**
  Exécutez
  ```bash
  ./lancer.sh #(Linux/macOS) ou
  lancer.bat #(Windows).
  ```
- **Manuellement :**
  Exécutez
  ```bash
  java jeux.GameApp
  ```
Une boîte de dialogue s'ouvrira, vous demandant de choisir un rôle.
### Étape 2 : Pour le Joueur 1 (Hôte / Serveur)
1. Cliquez sur **"Héberger une partie"**.
2. Dans les fenêtres suivantes, entrez la taille de la grille souhaitée, puis votre pseudonyme.
3. La fenêtre du jeu s'ouvrira. Le serveur est maintenant en attente.
4. Communiquez votre adresse **IP locale (ex: 192.168.1.25)** à l'autre joueur.
### Étape 3 : Pour le Joueur 2 (Client)
1. Lancez également l'application.
2. Cliquez sur **"Rejoindre une partie"**.
3. Dans la fenêtre qui s'ouvre, entrez l'adresse IP fournie par le Joueur 1.
4. Entrez votre pseudonyme.
5. La connexion s'établit et la partie commence !
Note sur les Tests en Local : Pour tester l'application sur une seule machine, lancez une instance en tant que serveur, puis une seconde en tant que client. Pour l'adresse IP, utilisez **127.0.1.1**.
