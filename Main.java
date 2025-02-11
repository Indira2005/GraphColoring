import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

class GraphVisualization extends JPanel {
    private final List<Point> vertices;
    private final List<int[]> edges;
    private final Map<Integer, Color> vertexColors;
    private List<Color> availableColors;
    private Color selectedColor;
    private int score;
    private boolean gameActive;
    private int level;
    private int minColors;
    private JLabel scoreLabel;
    private JLabel levelLabel;
    private JLabel minColorsLabel;
    private JPanel colorPalette;
    private JButton nextLevelButton;
    private JPanel sidePanel;
    private Timer hintTimer;
    private int hintVertex = -1;
    private Stack<ColoringAction> undoStack;
    private JButton undoButton;

    public GraphVisualization() {
        undoStack = new Stack<>();
        vertices = new ArrayList<>();
        edges = new ArrayList<>();
        vertexColors = new HashMap<>();
        availableColors = new ArrayList<>();
        score = 0;
        level = 1;
        gameActive = false;

        setLayout(new BorderLayout());
        setupUI();

        // Add mouse listener for coloring vertices
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (gameActive) {
                    handleVertexColoring(e.getPoint());
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (gameActive) {
                    highlightHoverVertex(e.getPoint());
                }
            }
        });

        // Setup hint timer
        hintTimer = new Timer(500, e -> {
            if (gameActive && !isGraphComplete()) {
                showHint();
            }
        });
        hintTimer.setRepeats(false);
    }

    private void setupUI() {
        JPanel gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGraph(g);
            }
        };
        gamePanel.setPreferredSize(new Dimension(800, 600));
        gamePanel.setBackground(new Color(240, 240, 250));

        sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        sidePanel.setBackground(new Color(230, 230, 240));
        sidePanel.setPreferredSize(new Dimension(200, 600));

        JButton startButton = createStyledButton("Start Game");
        startButton.addActionListener(e -> startGame());

        scoreLabel = createStyledLabel("Score: 0");
        levelLabel = createStyledLabel("Level: 1");
        minColorsLabel = createStyledLabel("Min Colors: 0");

        colorPalette = new JPanel();
        colorPalette.setLayout(new FlowLayout());
        colorPalette.setBackground(new Color(230, 230, 240));

        JButton helpButton = createStyledButton("Help");
        helpButton.addActionListener(e -> showHelp());

        JButton hintButton = createStyledButton("Hint");
        hintButton.addActionListener(e -> hintTimer.restart());

        undoButton = createStyledButton("Undo");  // Use the class field
        undoButton.addActionListener(e -> undo());
        undoButton.setVisible(false);

        nextLevelButton = createStyledButton("Next Level");
        nextLevelButton.addActionListener(e -> startNextLevel());
        nextLevelButton.setVisible(false);

        sidePanel.add(startButton);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        sidePanel.add(scoreLabel);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        sidePanel.add(levelLabel);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        sidePanel.add(minColorsLabel);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        sidePanel.add(new JLabel("Available Colors:"));
        sidePanel.add(colorPalette);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        sidePanel.add(helpButton);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        sidePanel.add(hintButton);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        sidePanel.add(undoButton);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        sidePanel.add(nextLevelButton);

        add(gamePanel, BorderLayout.CENTER);
        add(sidePanel, BorderLayout.EAST);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(150, 30));
        button.setBackground(new Color(100, 149, 237));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        return button;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        return label;
    }

    private void generateColorPalette() {
        colorPalette.removeAll();
        availableColors.clear();

        minColors = Math.max(3, level + 1);
        for (int i = 0; i < minColors; i++) {
            Color color = Color.getHSBColor(i * (1.0f / minColors), 0.8f, 0.9f);
            availableColors.add(color);

            JPanel colorButton = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.setColor(color);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    if (color.equals(selectedColor)) {
                        g.setColor(Color.BLACK);
                        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                        g.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
                    }
                }
            };

            colorButton.setPreferredSize(new Dimension(30, 30));
            colorButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            colorButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedColor = color;
                    colorPalette.repaint();
                }
            });

            colorPalette.add(colorButton);
        }

        if (!availableColors.isEmpty()) {
            selectedColor = availableColors.get(0);
        }
        colorPalette.revalidate();
        colorPalette.repaint();
        minColorsLabel.setText("Min Colors: " + minColors);
    }

    private boolean isGraphComplete() {
        for (int i = 0; i < vertices.size(); i++) {
            if (!vertexColors.containsKey(i)) {
                return false;
            }
        }
        return true;
    }

    private boolean canColorVertex(int vertex, Color color) {
        for (int[] edge : edges) {
            if ((edge[0] == vertex && vertexColors.containsKey(edge[1]) &&
                    vertexColors.get(edge[1]).equals(color)) ||
                    (edge[1] == vertex && vertexColors.containsKey(edge[0]) &&
                            vertexColors.get(edge[0]).equals(color))) {
                return false;
            }
        }
        return true;
    }

    private void handleVertexColoring(Point clickPoint) {
        if (selectedColor == null) {
            showMessage("Please select a color first!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (int i = 0; i < vertices.size(); i++) {
            Point p = vertices.get(i);
            if (clickPoint.distance(p) <= 15) {
                if (!vertexColors.containsKey(i) && canColorVertex(i, selectedColor)) {
                    Color oldColor = vertexColors.getOrDefault(i, Color.LIGHT_GRAY);
                    vertexColors.put(i, selectedColor);
                    score += level * 10;
                    scoreLabel.setText("Score: " + score);

                    undoStack.push(new ColoringAction(i, oldColor, selectedColor));
                    undoButton.setEnabled(true);

                    if (isGraphComplete()) {
                        handleLevelComplete();
                    }
                } else if (vertexColors.containsKey(i)) {
                    showMessage("Vertex already colored!", "Invalid Move", JOptionPane.ERROR_MESSAGE);
                }
                else if (!hasValidMovesRemaining()) {
                    handleGameOver();
                }
                else {
                    showMessage("Color not allowed due to adjacency.", "Color Conflict", JOptionPane.ERROR_MESSAGE);
                }
                repaint();
                break;
            }
        }
    }


    private boolean hasValidMovesRemaining() {
        // Check each uncolored vertex
        for (int i = 0; i < vertices.size(); i++) {
            if (!vertexColors.containsKey(i)) {
                // Check if any available color can be used
                for (Color color : availableColors) {
                    if (canColorVertex(i, color)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public void undo() {
        if (!undoStack.isEmpty()) {
            ColoringAction action = undoStack.pop();
            if (action.getOldColor() == Color.LIGHT_GRAY) {
                vertexColors.remove(action.getVertex());  // Remove the color instead of setting it to LIGHT_GRAY
            } else {
                vertexColors.put(action.getVertex(), action.getOldColor());
            }
            score -= level * 10;
            scoreLabel.setText("Score: " + score);
            repaint();
        }
        if (undoStack.isEmpty()) {
            undoButton.setEnabled(false);
        }
    }

    private void handleLevelComplete() {
        nextLevelButton.setVisible(true);
        gameActive = false;
        int bonus = calculateBonus();
        score += bonus;
        scoreLabel.setText("Score: " + score);

        showMessage(
                String.format("Level %d completed!\nScore: %d\nBonus: +%d\nTotal Score: %d",
                        level, score - bonus, bonus, score),
                "Level Complete",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void handleGameOver() {
        gameActive = false;
        String message = String.format("Game Over!\nNo valid moves remaining.\nFinal Score: %d", score);
        showMessage(message, "Game Over", JOptionPane.INFORMATION_MESSAGE);

        // Reset everything except the final score
        vertices.clear();
        edges.clear();
        vertexColors.clear();
        undoStack.clear();
        nextLevelButton.setVisible(false);
        undoButton.setVisible(false);

        // Show start button again
        for (Component comp : sidePanel.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                if (button.getText().equals("Start Game")) {
                    button.setVisible(true);
                    break;
                }
            }
        }

        repaint();
    }

    private int calculateBonus() {
        Set<Color> usedColors = new HashSet<>(vertexColors.values());
        int colorBonus = (minColors - usedColors.size()) * 100;
        return Math.max(0, colorBonus) * level;
    }

    private void generateRandomGraph() {
        Random ran = new Random();
        int nvertices = Math.min(5 + level, 15);
        double edgeProbability = Math.min(0.5, 0.2 + 0.1 * level);
        vertices.clear();
        edges.clear();
        vertexColors.clear();

        for (int i = 0; i < nvertices; i++) {
            vertices.add(new Point(ran.nextInt(600) + 100, ran.nextInt(400) + 100));
        }

        for (int i = 0; i < nvertices; i++) {
            for (int j = i + 1; j < nvertices; j++) {
                if (ran.nextDouble() < edgeProbability) {
                    edges.add(new int[]{i, j});
                }
            }
        }
    }

    private void drawGraph(Graphics g) {
        g.setColor(Color.GRAY);
        for (int[] edge : edges) {
            Point p1 = vertices.get(edge[0]);
            Point p2 = vertices.get(edge[1]);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        for (int i = 0; i < vertices.size(); i++) {
            Point p = vertices.get(i);
            Color color = vertexColors.getOrDefault(i, Color.LIGHT_GRAY);
            if (i == hintVertex) {
                g.setColor(Color.YELLOW);
                g.fillOval(p.x - 16, p.y - 16, 32, 32);
            }
            g.setColor(color);
            g.fillOval(p.x - 15, p.y - 15, 30, 30);
            g.setColor(Color.BLACK);
            g.drawOval(p.x - 15, p.y - 15, 30, 30);
        }
    }

    private void showHelp() {
        showMessage("Color each vertex with a color such that no two adjacent vertices share the same color.\n" +
                        "Select a color from the palette and click on a vertex to color it.",
                "Game Help", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showHint() {
        hintVertex = -1;
        for (int i = 0; i < vertices.size(); i++) {
            if (!vertexColors.containsKey(i)) {
                hintVertex = i;
                repaint();
                break;
            }
        }
    }

    private void startNextLevel() {
        level++;
        gameActive = true;
        scoreLabel.setText("Score: " + score);
        levelLabel.setText("Level: " + level);
        nextLevelButton.setVisible(false);
        generateRandomGraph();
        generateColorPalette();
        repaint();
    }

    private void startGame() {
        level = 1;
        score = 0;
        gameActive = true;
        scoreLabel.setText("Score: " + score);
        levelLabel.setText("Level: " + level);
        generateRandomGraph();
        generateColorPalette();
        undoButton.setVisible(true);

        sidePanel.revalidate();
        sidePanel.repaint();
        repaint();
    }

    private void highlightHoverVertex(Point hoverPoint) {
        for (int i = 0; i < vertices.size(); i++) {
            Point p = vertices.get(i);
            if (hoverPoint.distance(p) <= 15) {
                hintVertex = i;
                repaint();
                return;
            }
        }
        hintVertex = -1;
        repaint();
    }

    private void showMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    private static class ColoringAction {
        private final int vertex;
        private final Color oldColor;
        private final Color newColor;

        public ColoringAction(int vertex, Color oldColor, Color newColor) {
            this.vertex = vertex;
            this.oldColor = oldColor;
            this.newColor = newColor;
        }

        public int getVertex() {
            return vertex;
        }

        public Color getOldColor() {
            return oldColor;
        }

        public Color getNewColor() {
            return newColor;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Graph Coloring Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        frame.add(new GraphVisualization());
        frame.setVisible(true);
    }
}