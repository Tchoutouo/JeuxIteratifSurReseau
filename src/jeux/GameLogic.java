package jeux;

/**
 * Le "cerveau" du jeu. Cette classe gère l'état de la grille, les règles du jeu,
 * la validation des coups et la détection de la victoire ou du match nul.
 * Elle est totalement indépendante de l'interface graphique et du réseau.
 */
public class GameLogic {
    // --- Attributs ---
    private final int boardSize;
    private char[][] board;
    private char currentPlayerSymbol;
    private boolean isGameOver;

    private static final char EMPTY_CELL = '-';
    private static final int WINNING_STREAK = 5;

    /**
     * Constructeur de la logique du jeu.
     * @param size La taille de la grille (ex: 15 pour une grille 15x15).
     */
    public GameLogic(int size) {
        this.boardSize = size;
        this.board = new char[boardSize][boardSize];
        reset(); // Initialise la grille et les variables d'état.
    }

    /**
     * Réinitialise la grille et l'état du jeu pour une nouvelle partie.
     * Met toutes les cases à vide, redonne le tour au joueur 'X' et réactive la partie.
     */
    public void reset() {
        this.isGameOver = false;
        this.currentPlayerSymbol = 'X'; // Le joueur 'X' (serveur) commence toujours.
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                board[i][j] = EMPTY_CELL;
            }
        }
    }

    /**
     * Tente de placer le symbole du joueur courant aux coordonnées données.
     * @param x Coordonnée de la ligne.
     * @param y Coordonnée de la colonne.
     * @return true si le coup est valide, false sinon (case occupée, hors grille, etc.).
     */
    public boolean placeSymbol(int x, int y) {
        // Vérification 1: La partie n'est-elle pas déjà terminée ?
        if (isGameOver) return false;
        // Vérification 2: Les coordonnées sont-elles dans la grille ?
        if (x < 0 || x >= boardSize || y < 0 || y >= boardSize) return false;
        // Vérification 3: La case est-elle vide ?
        if (board[x][y] != EMPTY_CELL) return false;

        // Si toutes les vérifications passent, on place le symbole.
        board[x][y] = currentPlayerSymbol;
        return true;
    }

    /**
     * Vérifie si le dernier coup aux coordonnées (x,y) est un coup gagnant.
     * @param x La ligne du dernier coup.
     * @param y La colonne du dernier coup.
     * @return true si le joueur courant a gagné, false sinon.
     */
    public boolean checkWin(int x, int y) {
        // On vérifie les 4 axes possibles à partir de la dernière pièce jouée.
        if (countConsecutiveSymbols(x, y, 1, 0) >= WINNING_STREAK || // Horizontal (--)
            countConsecutiveSymbols(x, y, 0, 1) >= WINNING_STREAK || // Vertical (|)
            countConsecutiveSymbols(x, y, 1, 1) >= WINNING_STREAK || // Diagonale (\)
            countConsecutiveSymbols(x, y, 1, -1) >= WINNING_STREAK) { // Anti-diagonale (/)
            
            this.isGameOver = true; // La partie est terminée.
            return true;
        }
        return false;
    }
    
    /**
     * Méthode utilitaire pour compter les symboles consécutifs dans une direction.
     * @param x, y Coordonnées de départ.
     * @param dx, dy Vecteur de direction (ex: dx=1, dy=0 pour l'horizontale).
     */
    private int countConsecutiveSymbols(int x, int y, int dx, int dy) {
        char symbol = board[x][y];
        if (symbol == EMPTY_CELL) return 0;
        int count = 1; // On compte la pièce de départ.

        // On regarde dans la direction (dx, dy).
        for (int i = 1; i < WINNING_STREAK; i++) {
            int newX = x + i * dx;
            int newY = y + i * dy;
            if (newX >= 0 && newX < boardSize && newY >= 0 && newY < boardSize && board[newX][newY] == symbol) {
                count++;
            } else {
                break; // La chaîne est rompue.
            }
        }
        // On regarde dans la direction opposée (-dx, -dy).
        for (int i = 1; i < WINNING_STREAK; i++) {
            int newX = x - i * dx;
            int newY = y - i * dy;
            if (newX >= 0 && newX < boardSize && newY >= 0 && newY < boardSize && board[newX][newY] == symbol) {
                count++;
            } else {
                break; // La chaîne est rompue.
            }
        }
        return count;
    }

    /**
     * Vérifie si la grille est entièrement remplie (condition de match nul).
     */
    public boolean isBoardFull() {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (board[i][j] == EMPTY_CELL) {
                    return false; // Il reste au moins une case vide.
                }
            }
        }
        this.isGameOver = true; // Si aucune case vide, la partie est terminée.
        return true;
    }

    /**
     * Passe le tour au joueur suivant.
     */
    public void switchPlayer() {
        this.currentPlayerSymbol = (this.currentPlayerSymbol == 'X') ? 'O' : 'X';
    }

    // --- Getters et Setters ---
    public char[][] getBoard() { return this.board; }
    public char getCurrentPlayerSymbol() { return this.currentPlayerSymbol; }
    public boolean isGameOver() { return this.isGameOver; }
    public void setGameOver(boolean isOver) { this.isGameOver = isOver; }
}