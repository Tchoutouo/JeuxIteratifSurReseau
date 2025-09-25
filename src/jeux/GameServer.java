package jeux;

import java.io.*;
import java.net.*;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Le contrôleur côté Serveur. Il est le "maître du jeu".
 * - Gère l'instance de GameLogic et l'UI pour le joueur local (serveur).
 * - Ouvre une connexion réseau et attend les clients.
 * - Valide les coups et maintient l'état de la partie.
 */
public class GameServer implements GameController {
    // --- Attributs ---
    private GameLogic game;
    private GameUI ui;
    private PrintWriter outToClient;
    private String myPseudo;
    private String opponentPseudo;
    private final int gridSize;
    private final char mySymbol = 'X';
    private volatile boolean gameStarted = false;

    // États du serveur pour une gestion robuste des connexions.
    private enum ServerState { WAITING, PLAYING, GAME_OVER }
    private volatile ServerState currentState = ServerState.WAITING;

    public GameServer(String pseudo, int gridSize) {
        this.myPseudo = pseudo;
        this.gridSize = gridSize;
    }

    /**
     * Démarre l'UI et le thread réseau du serveur.
     */
    public void startServer() {
        SwingUtilities.invokeLater(() -> {
            game = new GameLogic(gridSize);
            ui = new GameUI(gridSize, this);
        });
        new Thread(this::runServerLogic).start();
    }
    
    /**
     * Boucle principale du serveur qui attend les connexions.
     */
    private void runServerLogic() {
        try (ServerSocket serverSocket = new ServerSocket(6789)) {
            String ip = InetAddress.getLocalHost().getHostAddress();
            SwingUtilities.invokeLater(() -> ui.setStatusMessage("En attente d'un adversaire sur " + ip + "..."));

            // Le serveur tourne indéfiniment pour accepter plusieurs parties.
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Attend une connexion.
                if (currentState != ServerState.WAITING) {
                    // Si une partie est en cours ou terminée (mais non relancée), on refuse.
                    new PrintWriter(clientSocket.getOutputStream(), true).println("SERVER_BUSY");
                    clientSocket.close();
                } else {
                    // Un client est accepté, on passe en mode "JEU".
                    currentState = ServerState.PLAYING;
                    this.outToClient = new PrintWriter(clientSocket.getOutputStream(), true);
                    // On lance un thread dédié pour gérer ce client.
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gère les clics du joueur local (serveur) sur la grille.
     */
    @Override
    public void onGridCellClicked(int x, int y) {
        if (gameStarted && game.getCurrentPlayerSymbol() == mySymbol && !game.isGameOver()) {
            synchronized (game) { // Accès synchronisé à l'objet partagé 'game'.
                if (game.placeSymbol(x, y)) {
                    ui.updateBoard(game.getBoard());
                    outToClient.println("VALID_MOVE:" + x + ";" + y + ";" + mySymbol);
                    
                    // On vérifie si ce coup termine la partie.
                    if (checkEndGame(x, y)) return;
                    
                    game.switchPlayer();
                    ui.setStatusMessage("C'est le tour de " + opponentPseudo + "...");
                }
            }
        }
    }
    
    /**
     * Vérifie la fin de partie (victoire/nul) et met à jour l'état et l'UI.
     * @param x, y Les coordonnées du dernier coup pour une détection efficace.
     */
    private boolean checkEndGame(int x, int y) {
        // On appelle game.checkWin avec les bonnes coordonnées.
        boolean isWin = game.checkWin(x, y);
        System.out.println("--- DEBUG (Serveur) --- checkEndGame appelé pour (" + x + "," + y + "). Résultat de isWin: " + isWin);
        boolean isDraw = !isWin && game.isBoardFull();

        if (isWin) {
            String winnerName = game.getCurrentPlayerSymbol() == mySymbol ? myPseudo : opponentPseudo;
            SwingUtilities.invokeLater(() -> {
                // On utilise une boîte de dialogue pour annoncer le résultat.
                String finalMessage = "FIN DE PARTIE: " + winnerName + " a gagné !";
                JOptionPane.showMessageDialog(ui, finalMessage, "Partie terminée", JOptionPane.INFORMATION_MESSAGE);
                ui.showEndGameOptions();
            });
            outToClient.println("GAME_OVER:VICTORY;" + winnerName);
            currentState = ServerState.GAME_OVER;
            return true;
        } else if (isDraw) {
            SwingUtilities.invokeLater(() -> {
                // On utilise une boîte de dialogue pour annoncer le résultat.
                JOptionPane.showMessageDialog(ui, "FIN DE PARTIE: Match Nul !", "Partie terminée", JOptionPane.INFORMATION_MESSAGE);
                ui.showEndGameOptions();
            });
            outToClient.println("GAME_OVER:DRAW;NULL");
            currentState = ServerState.GAME_OVER;
            return true;
        }
        return false;
    }

    /**
     * Gère la fermeture de la fenêtre par le joueur local.
     */
    @Override
    public void onWindowClosed() {
        if (outToClient != null) outToClient.println("DISCONNECT");
        System.exit(0);
    }
    
    /**
     * Gère la demande de revanche du joueur local.
     */
    @Override
    public void onPlayAgainRequested() {
        if (currentState == ServerState.GAME_OVER) {
            outToClient.println("PLAY_AGAIN_REQUEST");
            ui.setStatusMessage("Demande de revanche envoyée...");
        }
    }

    /**
     * Thread interne qui gère la communication avec un client.
     */
    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private BufferedReader in;

        public ClientHandler(Socket socket) throws IOException {
            this.clientSocket = socket;
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }

        @Override
        public void run() {
            try {
                // Étape 1: Échange des informations initiales
                opponentPseudo = in.readLine().split(":")[1];
                SwingUtilities.invokeLater(() -> ui.setTitle(myPseudo + " (" + mySymbol + ") vs " + opponentPseudo + " (O)"));

                // Étape 2: Envoi des paramètres de la partie
                outToClient.println("START_GAME:" + myPseudo + ";" + opponentPseudo + ";" + game.getCurrentPlayerSymbol() + ";" + gridSize);
                gameStarted = true;
                SwingUtilities.invokeLater(() -> ui.setStatusMessage("Partie commencée! C'est à vous de jouer."));

                // Étape 3: Boucle de réception des messages du client
                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    if ("DISCONNECT".equals(clientMessage)) {
                        handleDisconnect(true); // Déconnexion volontaire
                        break;
                    }
                    final String finalMessage = clientMessage;
                    SwingUtilities.invokeLater(() -> processClientMessage(finalMessage));
                }
            } catch (IOException e) {
                handleDisconnect(false); // Déconnexion brutale (crash, etc.)
            }
        }

        private void processClientMessage(String message) {
            if (message.startsWith("MOVE:")) handleMove(message);
            else if ("PLAY_AGAIN_REQUEST".equals(message)) handlePlayAgainRequest();
            else if (message.startsWith("PLAY_AGAIN_RESPONSE:")) handlePlayAgainResponse(message);
        }

        private void handleMove(String message) {
            if (game.getCurrentPlayerSymbol() == 'O') {
                String[] parts = message.split(":")[1].split(";");
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                synchronized (game) {
                    if (game.placeSymbol(x, y)) {
                        ui.updateBoard(game.getBoard());
                        outToClient.println("VALID_MOVE:" + x + ";" + y + ";O");
                        if (!checkEndGame(x, y)) {
                            game.switchPlayer();
                            ui.setStatusMessage("C'est à votre tour de jouer...");
                        }
                    }
                }
            }
        }

        private void handlePlayAgainRequest() {
            int choice = JOptionPane.showConfirmDialog(ui, opponentPseudo + " veut rejouer. Accepter ?", "Demande de revanche", JOptionPane.YES_NO_OPTION);
            outToClient.println("PLAY_AGAIN_RESPONSE:" + (choice == JOptionPane.YES_OPTION ? "OUI" : "NON"));
            if (choice == JOptionPane.YES_OPTION) {
                resetGame();
            } else {
                ui.setStatusMessage("Vous avez refusé. La partie est terminée.");
            }
        }

        private void handlePlayAgainResponse(String message) {
             if (message.endsWith("OUI")) {
                 resetGame();
             } else {
                 ui.setStatusMessage(opponentPseudo + " a refusé. La partie est terminée.");
             }
        }
        
        private void resetGame() {
            game.reset();
            currentState = ServerState.PLAYING;
            ui.updateBoard(game.getBoard());
            ui.hideEndGameOptions();
            ui.setStatusMessage("Nouvelle partie ! C'est à vous.");
            outToClient.println("RESET_GAME");
        }
        
        /**
         * CORRECTION : Gère la déconnexion de l'adversaire.
         * Le serveur ne s'arrête pas, il se réinitialise pour attendre un nouveau joueur.
         */
        private void handleDisconnect(boolean graceful) {
            if (!game.isGameOver()) {
                String message = graceful ? "L'adversaire a quitté la partie." : "L'adversaire s'est déconnecté brutalement.";
                JOptionPane.showMessageDialog(ui, message);
            }
            // Réinitialisation de l'état du serveur
            gameStarted = false;
            currentState = ServerState.WAITING; // Le serveur est de nouveau en attente
            game.reset(); // On nettoie la grille
            
            // Mise à jour de l'UI du serveur pour refléter le nouvel état
            SwingUtilities.invokeLater(() -> {
                ui.updateBoard(game.getBoard());
                ui.hideEndGameOptions();
                ui.setTitle("Jeu de Carré en Réseau");
                try {
                    ui.setStatusMessage("En attente d'un nouveau joueur sur " + InetAddress.getLocalHost().getHostAddress() + "...");
                } catch (UnknownHostException e) {}
            });
        }
    }
}