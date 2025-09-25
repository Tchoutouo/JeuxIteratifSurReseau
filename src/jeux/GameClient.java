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
    private volatile boolean myTurn = false;
    private volatile boolean gameOver = false;
    private volatile boolean gameStarted = false;
    private char[][] board;

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
     */
    private void runClientLogic() {
        try (Socket socket = new Socket(serverIp, 6789)) {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("CONNECT:" + myPseudo);

            // Boucle de réception des messages
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
            	System.out.println("--- DEBUG (Client) --- Message reçu: " + serverMessage);
            	
                if ("DISCONNECT".equals(serverMessage)) {
                    handleDisconnect();
                    break;
                }
                if ("SERVER_BUSY".equals(serverMessage)) {
                    JOptionPane.showMessageDialog(null, "Le serveur est occupé. Réessayez plus tard.", "Serveur occupé", JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0);
                }
                final String msg = serverMessage;
                SwingUtilities.invokeLater(() -> processServerMessage(msg));
            }
        } catch (IOException e) {
            handleDisconnect();
        }
    }

    /**
     * Interprète les messages du serveur et met à jour l'état et l'UI.
     */
    private void processServerMessage(String msg) {
        String[] parts = msg.split(":", 2);
        String command = parts[0];
        String data = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "START_GAME":
                String[] startData = data.split(";");
                this.myPseudo = this.myPseudo.equals(startData[0]) ? startData[0] : startData[1];
                this.opponentPseudo = this.myPseudo.equals(startData[0]) ? startData[1] : startData[0];
                char startPlayer = startData[2].charAt(0);
                int gridSize = Integer.parseInt(startData[3]);
                
                // Crée la grille et l'UI avec la bonne taille.
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
                this.board[Integer.parseInt(moveData[0])][Integer.parseInt(moveData[1])] = moveData[2].charAt(0);
                ui.updateBoard(this.board);
                myTurn = (moveData[2].charAt(0) != mySymbol);
                ui.setStatusMessage(myTurn ? "C'est à votre tour." : "C'est le tour de " + opponentPseudo + ".");
                break;
            case "GAME_OVER":
            	gameOver = true;
                String[] endData = data.split(";");
                // On prépare le message puis on l'affiche dans une boîte de dialogue.
                String finalMessage;
                if (endData[0].equals("VICTORY")) {
                    String winnerName = endData[1];
                    finalMessage = "FIN DE PARTIE: " + winnerName + " a gagné !";
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
            case "WELCOME":
                this.mySymbol = data.charAt(0);
                break;
            case "INVALID_MOVE":
                ui.setStatusMessage("Serveur: " + data + " Réessayez.");
                myTurn = true;
                break;
        }
    }

    private void resetGame() {
        gameOver = false;
        gameStarted = true;
        for (int i = 0; i < board.length; i++) for (int j = 0; j < board.length; j++) board[i][j] = '-';
        ui.updateBoard(board);
        ui.hideEndGameOptions();
        myTurn = (mySymbol == 'X'); // Le serveur (X) recommence.
        ui.setStatusMessage(myTurn ? "Nouvelle partie ! C'est à vous." : "Nouvelle partie ! Tour de " + opponentPseudo + ".");
    }

    private void handleDisconnect() {
        if (!gameOver) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(ui, "L'adversaire a quitté la partie.");
                ui.setStatusMessage("Adversaire déconnecté.");
                // On ne peut pas rejouer seul, donc on n'affiche que le panneau de fin.
                ui.showEndGameOptions(); 
            });
        }
        gameOver = true;
    }
    
    @Override
    public void onGridCellClicked(int x, int y) {
        if (gameStarted && myTurn && !gameOver) {
            out.println("MOVE:" + x + ";" + y);
            myTurn = false; // Désactive le tour en attendant la validation.
        }
    }

    @Override
    public void onWindowClosed() {
        if (out != null) out.println("DISCONNECT");
        System.exit(0);
    }
    
    @Override
    public void onPlayAgainRequested() {
        if (gameOver) {
            out.println("PLAY_AGAIN_REQUEST");
            ui.setStatusMessage("Demande de revanche envoyée...");
        }
    }
}