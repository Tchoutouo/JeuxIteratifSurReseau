package jeux;

import java.io.*;
import java.net.*;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Le contrôleur côté Client.
 * - Gère l'UI pour le joueur distant (client).
 * - Se connecte au serveur et envoie les actions du joueur.
 * - Reçoit les messages du serveur et met à jour son UI en conséquence.
 */
public class GameClient implements GameController {
    // --- Attributs ---
    private String myPseudo;
    private String opponentPseudo;
    private final String serverIp;
    private GameUI ui;
    private PrintWriter out;
    private char mySymbol;
    // 'volatile' assure que les changements de variable sont visibles entre les threads.
    private volatile boolean myTurn = false;
    private volatile boolean gameOver = false;
    private volatile boolean gameStarted = false;
    private char[][] board; // Copie locale de la grille pour l'affichage.

    public GameClient(String pseudo, String serverIp) {
        this.myPseudo = pseudo;
        this.serverIp = serverIp;
    }

    /**
     * Lance le thread réseau du client.
     */
    public void startClient() {
        new Thread(this::runClientLogic).start();
    }
    
    /**
     * Logique principale du client : connexion et boucle d'écoute.
     * S'exécute dans un thread d'arrière-plan pour ne pas geler l'UI.
     */
    private void runClientLogic() {
        try (Socket socket = new Socket(serverIp, 6789)) {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Envoie le message de connexion initial.
            out.println("CONNECT:" + myPseudo);

            // Boucle d'écoute : attend en permanence les messages du serveur.
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                if ("DISCONNECT".equals(serverMessage)) {
                    handleDisconnect();
                    break;
                }
                if ("SERVER_BUSY".equals(serverMessage)) {
                    JOptionPane.showMessageDialog(null, "Le serveur est occupé. Réessayez plus tard.", "Serveur occupé", JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0);
                }
                // Transmet le message au thread de l'UI pour un traitement sécurisé.
                final String msg = serverMessage;
                SwingUtilities.invokeLater(() -> processServerMessage(msg));
            }
        } catch (IOException e) {
            handleDisconnect();
        }
    }

    /**
     * Interprète les messages reçus du serveur et met à jour l'état et l'UI.
     * Cette méthode est toujours exécutée sur le thread de l'UI (EDT).
     */
    private void processServerMessage(String msg) {
        // Décodage du message : COMMANDE:données
        String[] parts = msg.split(":", 2);
        String command = parts[0];
        String data = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "WELCOME":
                // Le serveur nous assigne notre symbole.
                this.mySymbol = data.charAt(0);
                break;

            case "START_GAME":
                String[] startData = data.split(";");
                String serverPseudo = startData[0];
                String clientPseudo = startData[1];
                this.opponentPseudo = this.myPseudo.equals(serverPseudo) ? clientPseudo : serverPseudo;
                char startPlayer = startData[2].charAt(0);
                int gridSize = Integer.parseInt(startData[3]);
                
                // Crée la grille et l'UI avec la bonne taille reçue du serveur.
                this.board = new char[gridSize][gridSize];
                for (int i = 0; i < gridSize; i++) for (int j = 0; j < gridSize; j++) board[i][j] = '-';
                if (ui == null) ui = new GameUI(gridSize, this);
                ui.updateBoard(this.board);
                ui.setTitle(myPseudo + " (" + mySymbol + ") vs " + opponentPseudo + " (" + (mySymbol == 'X' ? 'O' : 'X') + ")");
                
                gameStarted = true;
                myTurn = (mySymbol == startPlayer);
                ui.setStatusMessage(myTurn ? "La partie commence! C'est à vous." : "C'est le tour de " + opponentPseudo + ".");
                break;

            case "VALID_MOVE":
                 String[] moveData = data.split(";");
                 int x = Integer.parseInt(moveData[0]);
                 int y = Integer.parseInt(moveData[1]);
                 char playerWhoMoved = moveData[2].charAt(0);
                 
                 // 1. Mettre à jour la grille locale.
                 if (this.board != null) this.board[x][y] = playerWhoMoved;
                 ui.updateBoard(this.board);
                 
                 // 2. Déterminer à qui est le tour de manière EXPLICITE.
                 if (playerWhoMoved == this.mySymbol) {
                     this.myTurn = false; // C'est la confirmation de MON coup, donc ce n'est plus mon tour.
                     ui.setStatusMessage("C'est le tour de " + opponentPseudo + ".");
                 } else {
                     this.myTurn = true; // C'est le coup de l'ADVERSAIRE, donc c'est mon tour.
                     ui.setStatusMessage("C'est à votre tour.");
                 }
                break;

            case "GAME_OVER":
                gameOver = true;
                String[] endData = data.split(";");
                String finalMessage;
                
                if (endData[0].equals("VICTORY")) {
                    String winnerName = endData[1];
                    System.out.println("--- DEBUG (Client) ---" +myPseudo+ "Victoire détectée pour " + winnerName + ". Envoi du message GAME_OVER.");
                    if(myPseudo == winnerName) {
                    	finalMessage = "FIN DE PARTIE: Vous avez gagné !";
                    }else {
                    	finalMessage = "FIN DE PARTIE: " + winnerName + " a gagné !";
                    }
                    
                } else {
                    finalMessage = "FIN DE PARTIE: Match nul !";
                }
                JOptionPane.showMessageDialog(ui, finalMessage, "Partie terminée", JOptionPane.INFORMATION_MESSAGE);
                ui.showEndGameOptions();
                break;

            case "PLAY_AGAIN_REQUEST":
                int choice = JOptionPane.showConfirmDialog(ui, opponentPseudo + " veut rejouer. Accepter ?", "Demande de revanche", JOptionPane.YES_NO_OPTION);
                out.println("PLAY_AGAIN_RESPONSE:" + (choice == JOptionPane.YES_OPTION ? "OUI" : "NON"));
                if (choice == JOptionPane.YES_OPTION) ui.setStatusMessage("Vous avez accepté. En attente du serveur...");
                break;
            case "PLAY_AGAIN_RESPONSE":
                if(!data.equals("OUI")) ui.setStatusMessage(opponentPseudo + " a refusé. La partie est terminée.");
                break;

            case "RESET_GAME":
                resetGame();
                break;

            case "INVALID_MOVE":
                ui.setStatusMessage("Serveur: " + data + " Réessayez.");
                myTurn = true; // Si mon coup est invalide, c'est toujours à moi de jouer.
                break;
        }
    }

    /** Réinitialise l'état du client pour une nouvelle partie. */
    private void resetGame() {
        gameOver = false;
        gameStarted = true;
        for (int i = 0; i < board.length; i++) for (int j = 0; j < board.length; j++) board[i][j] = '-';
        ui.updateBoard(board);
        ui.hideEndGameOptions();
        myTurn = (mySymbol == 'X'); // Le serveur (X) recommence toujours.
        ui.setTitle(myPseudo + " (" + mySymbol + ") vs " + opponentPseudo + " (" + (mySymbol == 'X' ? 'O' : 'X') + ")");
        ui.setStatusMessage(myTurn ? "Nouvelle partie ! C'est à vous." : "Nouvelle partie ! Tour de " + opponentPseudo + ".");
    }

    /** Gère la déconnexion de l'adversaire. */
    private void handleDisconnect() {
        if (!gameOver) {
            SwingUtilities.invokeLater(() -> {
                if (ui != null) {
                    JOptionPane.showMessageDialog(ui, "L'adversaire a quitté la partie.");
                    ui.setTitle("Adversaire déconnecté");
                    ui.showEndGameOptions();
                }
            });
        }
        gameOver = true;
    }
    
    /** Appelé par l'UI quand le joueur clique sur la grille. */
    @Override
    public void onGridCellClicked(int x, int y) {
        if (gameStarted && myTurn && !gameOver) {
            out.println("MOVE:" + x + ";" + y);
            myTurn = false; // Désactive immédiatement le tour pour éviter les double-clics.
        }
    }

    /** Appelé par l'UI quand le joueur ferme la fenêtre. */
    @Override
    public void onWindowClosed() {
        if (out != null) out.println("DISCONNECT"); // Informe le serveur.
        System.exit(0);
    }
    
    /** Appelé par l'UI quand le joueur clique sur "Rejouer". */
    @Override
    public void onPlayAgainRequested() {
        if (gameOver) {
            out.println("PLAY_AGAIN_REQUEST");
            ui.setStatusMessage("Demande de revanche envoyée...");
        }
    }
}