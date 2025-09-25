package jeux;

/**
 * Une interface (un "contrat") pour connecter l'UI (GameUI) à la logique de contrôle
 * (GameServer ou GameClient). Cela permet à l'UI d'envoyer des événements
 * (clic, fermeture) sans savoir qui la contrôle.
 */
public interface GameController {
    /** Appelé quand l'utilisateur clique sur une case de la grille. */
    void onGridCellClicked(int x, int y);

    /** Appelé quand l'utilisateur ferme la fenêtre. */
    void onWindowClosed();

    /** Appelé quand l'utilisateur clique sur le bouton "Rejouer". */
    void onPlayAgainRequested();
}