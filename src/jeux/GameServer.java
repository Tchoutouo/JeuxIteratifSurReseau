package jeux;

import java.io.*;
import java.net.*;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Le contrôleur côté Serveur. Il est le "maître du jeu".
 * - Gère l'instance de GameLogic (le "cerveau" du jeu).
 * - Gère l'interface graphique (GameUI) pour le joueur local (serveur).
 * - Ouvre une connexion réseau et attend un client.
 * - Gère un "ClientHandler" pour communiquer avec le client distant.
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
    private final char mySymbol = 'X'; // Le serveur est toujours le joueur 'X'.
    private volatile boolean gameStarted = false;

    /**
     * États possibles du serveur pour gérer les connexions de manière robuste.
     * volatile assure que les changements de cet état sont visibles par tous les threads.
     */
    private enum ServerState { WAITING, PLAYING, GAME_OVER }
    private volatile ServerState currentState = ServerState.WAITING;

    public GameServer(String pseudo, int gridSize) {
        this.myPseudo = pseudo;
        this.gridSize = gridSize;
    }

    /**
     * Démarre l'interface graphique et lance le thread réseau du serveur.
     */
    public void startServer() {
        // Crée l'UI sur le thread dédié de Swing (Event Dispatch Thread) pour éviter les conflits.
        SwingUtilities.invokeLater(() -> {
            game = new GameLogic(gridSize);
            ui = new GameUI(gridSize, this);
        });
        // Lance la logique réseau dans un thread séparé pour ne pas geler l'UI.
        new Thread(this::runServerLogic).start();
    }
    
    /**
     * La boucle principale du serveur qui attend les connexions des clients.
     * S'exécute dans un thread d'arrière-plan.
     */
    private void runServerLogic() {
        try (ServerSocket serverSocket = new ServerSocket(6789)) {
            String ip = InetAddress.getLocalHost().getHostAddress();
            SwingUtilities.invokeLater(() -> ui.setStatusMessage("En attente d'un adversaire sur " + ip + "..."));

            // Le serveur tourne indéfiniment pour pouvoir accepter de nouvelles parties après une déconnexion.
            while (true) {
                // Opération bloquante : le thread attend ici qu'un client se connecte.
                Socket clientSocket = serverSocket.accept();
                
                if (currentState != ServerState.WAITING) {
                    // Si une partie est en cours ou terminée (non relancée), on refuse poliment le nouveau client.
                    new PrintWriter(clientSocket.getOutputStream(), true).println("SERVER_BUSY");
                    clientSocket.close();
                } else {
                    // Un client est accepté, on passe en mode "JEU".
                    currentState = ServerState.PLAYING;
                    this.outToClient = new PrintWriter(clientSocket.getOutputStream(), true);
                    // On lance un thread dédié pour gérer la communication avec ce client.
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gère les clics du joueur local (serveur) sur la grille.
     * Méthode de l'interface GameController.
     */
    @Override
    public void onGridCellClicked(int x, int y) {
        // On ne peut jouer que si la partie a commencé, que c'est notre tour et que la partie n'est pas finie.
        if (gameStarted && game.getCurrentPlayerSymbol() == mySymbol && !game.isGameOver()) {
            // 'synchronized' empêche les conflits si le client joue en même temps (protection contre les race conditions).
            synchronized (game) {
                if (game.placeSymbol(x, y)) {
                    ui.updateBoard(game.getBoard());
                    // On informe le client que le coup est valide.
                    outToClient.println("VALID_MOVE:" + x + ";" + y + ";" + mySymbol);
                    
                    // On vérifie si ce coup termine la partie.
                    if (checkEndGame(x, y)) return;
                    
                    // Si la partie continue, on passe le tour.
                    game.switchPlayer();
                    ui.setStatusMessage("C'est le tour de " + opponentPseudo + ".");
                }
            }
        }
    }
    
    /**
     * Vérifie si la partie est terminée (victoire/nul) et met à jour l'état et l'UI.
     * @param x, y Les coordonnées du dernier coup pour une détection efficace.
     * @return true si la partie est terminée, false sinon.
     */
    private boolean checkEndGame(int x, int y) {
        boolean isWin = game.checkWin(x, y);
        boolean isDraw = !isWin && game.isBoardFull();

        if (isWin) {
            String winnerName = game.getCurrentPlayerSymbol() == mySymbol ? myPseudo : opponentPseudo;
            SwingUtilities.invokeLater(() -> {
            	if(myPseudo == winnerName) {
            		String finalMessage = "FIN DE PARTIE: Vous avez gagné !";
                    JOptionPane.showMessageDialog(ui, finalMessage, "Partie terminée", JOptionPane.INFORMATION_MESSAGE);
            	}else {
            		String finalMessage = "FIN DE PARTIE: " + winnerName + " a gagné !";
                    JOptionPane.showMessageDialog(ui, finalMessage, "Partie terminée", JOptionPane.INFORMATION_MESSAGE);
            	}
                
                ui.showEndGameOptions();
            });
            outToClient.println("GAME_OVER:VICTORY;" + winnerName);
            currentState = ServerState.GAME_OVER;
            return true;
        } else if (isDraw) {
            SwingUtilities.invokeLater(() -> {
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
     * Thread interne qui gère toute la communication avec un client connecté.
     * Chaque client a son propre ClientHandler.
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
                opponentPseudo = in.readLine().split(":")[1]; // Attend le message CONNECT du client
                SwingUtilities.invokeLater(() -> ui.setTitle(myPseudo + " (" + mySymbol + ") vs " + opponentPseudo + " (O)"));

                // Étape 2: Envoi des paramètres de la partie au client
                outToClient.println("WELCOME:O"); // Informe le client de son symbole
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

        /** Traite un message reçu du client. */
        private void processClientMessage(String message) {
            if (message.startsWith("MOVE:")) handleMove(message);
            else if ("PLAY_AGAIN_REQUEST".equals(message)) handlePlayAgainRequest();
            else if (message.startsWith("PLAY_AGAIN_RESPONSE:")) handlePlayAgainResponse(message);
        }

        /** Gère un coup reçu du client. */
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
                            ui.setStatusMessage("C'est à votre tour.");
                        }
                    }
                }
            }
        }

        /** Gère une demande de revanche reçue du client. */
        private void handlePlayAgainRequest() {
            int choice = JOptionPane.showConfirmDialog(ui, opponentPseudo + " veut rejouer. Accepter ?", "Demande de revanche", JOptionPane.YES_NO_OPTION);
            outToClient.println("PLAY_AGAIN_RESPONSE:" + (choice == JOptionPane.YES_OPTION ? "OUI" : "NON"));
            if (choice == JOptionPane.YES_OPTION) {
                resetGame();
            } else {
                ui.setStatusMessage("Vous avez refusé. La partie est terminée.");
            }
        }

        /** Gère la réponse à une demande de revanche. */
        private void handlePlayAgainResponse(String message) {
             if (message.endsWith("OUI")) {
                 resetGame();
             } else {
                 ui.setStatusMessage(opponentPseudo + " a refusé. La partie est terminée.");
             }
        }
        
        /** Réinitialise le jeu pour une nouvelle partie. */
        private void resetGame() {
            game.reset();
            currentState = ServerState.PLAYING;
            ui.updateBoard(game.getBoard());
            ui.hideEndGameOptions();
            ui.setTitle(myPseudo + " (" + mySymbol + ") vs " + opponentPseudo + " (O)");
            ui.setStatusMessage("Nouvelle partie ! C'est à vous.");
            outToClient.println("RESET_GAME");
        }
        
        /** Gère la déconnexion de l'adversaire et remet le serveur en attente. */
        private void handleDisconnect(boolean graceful) {
            if (!game.isGameOver()) {
                String message = graceful ? "L'adversaire a quitté la partie." : "L'adversaire s'est déconnecté brutalement.";
                JOptionPane.showMessageDialog(ui, message);
            }
            // Réinitialisation de l'état du serveur
            gameStarted = false;
            currentState = ServerState.WAITING; // Le serveur est de nouveau en attente.
            game.reset(); // On nettoie la grille.
            
            // Mise à jour de l'UI du serveur pour refléter le nouvel état.
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