package jeux;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Gère toute l'interface graphique (la "Vue").
 * Affiche la fenêtre, la grille, les symboles, et les messages.
 * Détecte les actions de l'utilisateur (clics) et les transmet au contrôleur.
 */
public class GameUI extends JFrame {
    private final GameBoardPanel boardPanel;
    private final JLabel statusLabel;
    private final GameController controller;
    private final JPanel endPanel;

    public GameUI(int gridSize, GameController controller) {
        this.controller = controller;

        setTitle("Jeu de Carré en Réseau");
        // On intercepte la fermeture pour envoyer un message de déconnexion.
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.onWindowClosed();
            }
        });

        setLayout(new BorderLayout());

        boardPanel = new GameBoardPanel(gridSize);
        statusLabel = new JLabel("Initialisation...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));

        add(boardPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Prépare le panneau avec les boutons "Rejouer" et "Quitter"
        endPanel = createEndGamePanel();

        pack(); // Ajuste la taille de la fenêtre au contenu.
        setLocationRelativeTo(null); // Centre la fenêtre.
        setVisible(true);
    }
    
    /**
     * Crée le panneau affiché en fin de partie.
     */
    private JPanel createEndGamePanel() {
        JPanel panel = new JPanel();
        JButton playAgainButton = new JButton("Rejouer");
        JButton quitButton = new JButton("Quitter");
        playAgainButton.addActionListener(e -> controller.onPlayAgainRequested());
        quitButton.addActionListener(e -> controller.onWindowClosed());
        panel.add(playAgainButton);
        panel.add(quitButton);
        return panel;
    }
    
    /**
     * Affiche les options de fin de partie ("Rejouer", "Quitter").
     */
    public void showEndGameOptions() {
        remove(statusLabel);
        add(endPanel, BorderLayout.SOUTH);
        revalidate(); // Met à jour l'affichage des composants.
        repaint();
    }
    
    /**
     * Cache les options de fin de partie pour une nouvelle partie.
     */
    public void hideEndGameOptions() {
        remove(endPanel);
        add(statusLabel, BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    /**
     * Met à jour la grille avec les nouvelles données et la redessine.
     * @param board L'état actuel de la grille.
     */
    public void updateBoard(char[][] board) {
        boardPanel.setBoard(board);
        repaint(); // Demande à Swing de redessiner le composant.
    }

    /**
     * Affiche un message dans la barre de statut en bas de la fenêtre.
     */
    public void setStatusMessage(String message) {
        statusLabel.setText(message);
    }

    /**
     * Le panneau interne qui gère le dessin de la grille et la détection des clics.
     */
    private class GameBoardPanel extends JPanel {
        private final int gridSize;
        private char[][] board;

        public GameBoardPanel(int size) {
            this.gridSize = size;
            setPreferredSize(new Dimension(600, 600));

            // Ajoute un écouteur pour les clics de souris.
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int cellSize = getWidth() / gridSize;
                    // Calcule les coordonnées (x,y) de la case cliquée.
                    int row = e.getY() / cellSize;
                    int col = e.getX() / cellSize;
                    if(controller != null) controller.onGridCellClicked(row, col);
                }
            });
        }

        public void setBoard(char[][] boardData) { this.board = boardData; }

        /**
         * La méthode magique de Swing où tout le dessin est effectué.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            // Active l'anti-aliasing pour des dessins plus lisses.
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int cellSize = width / gridSize;
            
            // Fond blanc
            g.setColor(Color.WHITE);
            g.fillRect(0,0, width, height);

            // Lignes de la grille
            g.setColor(Color.BLACK);
            for (int i = 0; i <= gridSize; i++) {
                g.drawLine(i * cellSize, 0, i * cellSize, height);
                g.drawLine(0, i * cellSize, width, i * cellSize);
            }

            // Symboles des joueurs
            if (board != null) {
                for (int i = 0; i < gridSize; i++) {
                    for (int j = 0; j < gridSize; j++) {
                        if (board[i][j] == 'X') drawX(g2d, j * cellSize, i * cellSize, cellSize);
                        else if (board[i][j] == 'O') drawO(g2d, j * cellSize, i * cellSize, cellSize);
                    }
                }
            }
        }

        private void drawX(Graphics2D g, int x, int y, int size) {
            g.setColor(Color.BLUE);
            g.setStroke(new BasicStroke(3));
            int p = size / 5; // Marge intérieure
            g.drawLine(x + p, y + p, x + size - p, y + size - p);
            g.drawLine(x + size - p, y + p, x + p, y + size - p);
        }

        private void drawO(Graphics2D g, int x, int y, int size) {
            g.setColor(Color.RED);
            g.setStroke(new BasicStroke(3));
            int p = size / 5; // Marge intérieure
            g.drawOval(x + p, y + p, size - 2 * p, size - 2 * p);
        }
    }
}