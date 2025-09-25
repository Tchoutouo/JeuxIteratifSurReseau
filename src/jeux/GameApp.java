package jeux;

import javax.swing.JOptionPane;

/**
 * Le point d'entrée de l'application. Affiche la première fenêtre de choix
 * ("Héberger" ou "Rejoindre") et lance ensuite le serveur ou le client
 * avec les paramètres saisis par l'utilisateur.
 */
public class GameApp {
    public static void main(String[] args) {
        // Affiche la boîte de dialogue initiale.
        Object[] options = {"Héberger une partie", "Rejoindre une partie"};
        int choice = JOptionPane.showOptionDialog(null, "Bienvenue au Jeu de Carré !\nQue voulez-vous faire ?",
                "Menu Principal", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice == JOptionPane.YES_OPTION) { // L'utilisateur veut héberger.
            // Demande la taille de la grille avec validation.
            int gridSize = 0;
            while (gridSize < 5 || gridSize > 25) {
                String gridSizeStr = JOptionPane.showInputDialog(null, "Entrez la taille de la grille (entre 5 et 25):", "15");
                if (gridSizeStr == null) return; // L'utilisateur a annulé.
                try {
                    gridSize = Integer.parseInt(gridSizeStr);
                } catch (NumberFormatException e) { /* La boucle continue si la saisie est invalide. */ }
            }
            // Demande le pseudo avec validation.
            String pseudo = "";
            while (pseudo.trim().isEmpty()) {
                pseudo = JOptionPane.showInputDialog(null, "Entrez votre pseudo:", "Joueur 1");
                if (pseudo == null) return; // L'utilisateur a annulé.
            }
            // Lance le serveur.
            new GameServer(pseudo, gridSize).startServer();
        } else if (choice == JOptionPane.NO_OPTION) { // L'utilisateur veut rejoindre.
            // Demande l'IP du serveur avec validation.
            String serverIp = "";
            while (serverIp.trim().isEmpty()) {
                serverIp = JOptionPane.showInputDialog(null, "Entrez l'adresse IP du serveur:", "127.0.0.1");
                if (serverIp == null) return; // L'utilisateur a annulé.
            }
            // Demande le pseudo avec validation.
            String pseudo = "";
            while (pseudo.trim().isEmpty()) {
                pseudo = JOptionPane.showInputDialog(null, "Entrez votre pseudo:", "Joueur 2");
                if (pseudo == null) return; // L'utilisateur a annulé.
            }
            // Lance le client.
            new GameClient(pseudo, serverIp).startClient();
        }
    }
}