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
- [Java Development Kit (JDK)](https://www.oracle.com/java/technologies/downloads/) - Version 9 ou supérieure.

## Installation et Compilation
1.  Clonez ou téléchargez ce dépôt sur votre machine locale.
2.  Ouvrez un terminal et naviguez jusqu'au dossier racine du projet.
3.  Compilez tous les fichiers source Java en exécutant la commande suivante :
    ```bash
    javac src/jeux/*.java
    ```

## Comment Jouer 🚀

Pour jouer, vous devez lancer l'application deux fois : une pour le serveur et une pour le client.

#### 1. Lancer le Serveur (Joueur 1)
- Dans un terminal, exécutez la commande :
  ```bash
  java jeux.GameApp
