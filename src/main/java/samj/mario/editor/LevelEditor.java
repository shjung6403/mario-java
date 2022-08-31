package samj.mario.editor;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import samj.mario.editor.data.Level;
import samj.mario.editor.data.Tile;
import samj.mario.editor.data.TileData;
import samj.mario.editor.data.TileMatrix;
import samj.mario.editor.io.FileIO;
import samj.mario.editor.io.IconLoader;
import samj.mario.editor.io.JsonLevelFormat;
import samj.mario.editor.io.LevelFormat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import java.util.Stack;

public class LevelEditor implements ActionListener {

    private static JFrame FRAME;

    private JPanel mainPanel;
    private JScrollPane levelScrollPane;
    private JScrollPane toolScrollPane;
    private JPanel levelPanel;
    private JPanel tilePalettePanel;
    private JPanel toolControlPanel;
    private JPanel selectedTilePreviewPanel;

    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenu editMenu;
    private JMenu viewMenu;
    private JMenuItem newMenuItem;
    private JMenuItem openMenuItem;
    private JMenuItem saveMenuItem;
    private JMenuItem quitMenuItem;
    private JMenuItem undoMenuItem;
    private JMenuItem propertiesMenuItem;

    private final int gridSize = 16;
    private final int paletteColumns = 12;

    private final LevelFormat levelFormat = new JsonLevelFormat();
    private final FileIO fileIO = new FileIO(levelFormat);
    private final IconLoader iconLoader = new IconLoader(gridSize);

    private int levelPanelWidth;
    private int levelPanelHeight;
    private Level level;
    private Tile selectedTile = Tile.EMPTY_TILE;
    private boolean isGridEnabled = true;

    private Stack<EditorCommand> undoStack = new Stack<>();

    public LevelEditor() {
        $$$setupUI$$$();
        levelPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                handleLevelPanelMouseEvent(e);
            }
        });
        tilePalettePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                handleTilePalettePanelMouseEvent(e);
            }
        });

        // By default, create an empty level on startup
        createNewLevel();
    }

    public JPanel getLevelPanel() {
        return levelPanel;
    }

    public Level getLevel() {
        return level;
    }

    private void createUIComponents() {
        levelPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawLevel(g);
            }
        };

        tilePalettePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawPalette(g);
            }
        };

        selectedTilePreviewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawPreview(g);
            }
        };

        createMenuBar();
    }

    private void createMenuBar() {
        menuBar = new JMenuBar();

        fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        newMenuItem = new JMenuItem("New");
        newMenuItem.setActionCommand("new");
        newMenuItem.addActionListener(this);
        fileMenu.add(newMenuItem);

        fileMenu.addSeparator();

        openMenuItem = new JMenuItem("Open");
        openMenuItem.setActionCommand("open");
        openMenuItem.addActionListener(this);
        fileMenu.add(openMenuItem);

        saveMenuItem = new JMenuItem("Save");
        saveMenuItem.setActionCommand("save");
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.META_DOWN_MASK));
        saveMenuItem.addActionListener(this);
        fileMenu.add(saveMenuItem);

        fileMenu.addSeparator();

        quitMenuItem = new JMenuItem("Quit");
        quitMenuItem.setActionCommand("quit");
        quitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.META_DOWN_MASK));
        quitMenuItem.addActionListener(this);
        fileMenu.add(quitMenuItem);

        editMenu = new JMenu("Edit");
        menuBar.add(editMenu);

        undoMenuItem = new JMenuItem("Undo");
        undoMenuItem.setActionCommand("undo");
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.META_DOWN_MASK));
        undoMenuItem.addActionListener(this);
        editMenu.add(undoMenuItem);

        editMenu.addSeparator();

        propertiesMenuItem = new JMenuItem("Properties");
        propertiesMenuItem.setActionCommand("properties");
        propertiesMenuItem.addActionListener(this);
        editMenu.add(propertiesMenuItem);

        FRAME.setJMenuBar(menuBar);
    }

    private void drawLevel(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, levelPanelWidth, levelPanelHeight);
        if (level != null) {
            drawTiles(g);
        }
        if (isGridEnabled) {
            drawGrid(g);
        }
    }

    private void drawTiles(Graphics g) {
        for (int x = 0; x < level.getWidth(); x++) {
            for (int y = 0; y < level.getHeight(); y++) {
                Tile tile = level.getTileMatrix().getTile(x, y);
                if (tile.getPrimaryDisplayIcon() != null) {
                    int panelX = x * gridSize;
                    int panelY = y * gridSize;
                    Image iconImage = iconLoader.getImageForIcon(tile.getPrimaryDisplayIcon());
                    g.drawImage(iconImage, panelX, panelY, null);
                }
            }
        }
    }

    private void drawGrid(Graphics g) {
        int width = levelPanelWidth;
        int height = levelPanelHeight;

        g.setColor(Color.CYAN);

        // vertical lines
        for (int i = gridSize; i < width; i += gridSize) {
            g.drawLine(i, 0, i, height);
        }

        // horizontal lines
        for (int i = gridSize; i < height; i += gridSize) {
            g.drawLine(0, i, width, i);
        }
    }

    private void drawPalette(Graphics g) {
        final List<Tile> fgTiles = TileData.TILES;

        for (int i = 0; i < fgTiles.size(); i++) {
            Tile tile = fgTiles.get(i);
            Image iconImage = iconLoader.getImageForIcon(tile.getPrimaryDisplayIcon());
            int x = (i % paletteColumns) * gridSize;
            int y = (i / paletteColumns) * gridSize;
            g.drawImage(iconImage, x, y, null);
        }
    }

    private void drawPreview(Graphics g) {
        Image iconImage = iconLoader.getImageForIcon(selectedTile.getPrimaryDisplayIcon());
        g.drawImage(iconImage, 0, 0, gridSize * 2, gridSize * 2, null);
    }

    public void doCommand(EditorCommand command) {
        command.execute();
        undoStack.push(command);
    }

    private void createNewLevel() {
        final int width = 16;
        final int height = 16;

        level = new Level();
        level.setDimensions(width, height);
        level.setTileMatrix(new TileMatrix(width, height));
        levelPanelWidth = width * gridSize;
        levelPanelHeight = height * gridSize;

        undoStack.clear();
        repaintLevel();
    }

    private void loadExistingLevel(Level level) {
        this.level = level;
        levelPanelWidth = level.getWidth() * gridSize;
        levelPanelHeight = level.getHeight() * gridSize;

        undoStack.clear();
        repaintLevel();
    }

    public void changeLevelDimensions(int width, int height) {
        level.setDimensions(width, height);
        levelPanelWidth = width * gridSize;
        levelPanelHeight = height * gridSize;

        // TODO: Validate that no tiles are being deleted

        level.setTileMatrix(new TileMatrix(width, height, level.getTileMatrix()));

        repaintLevel();
    }

    private void repaintLevel() {
        // Resize components & repaint
        levelPanel.setPreferredSize(new Dimension(levelPanelWidth, levelPanelHeight));
        JViewport viewport = levelScrollPane.getViewport();
        viewport.setViewSize(new Dimension(levelPanelWidth, levelPanelHeight));
        levelScrollPane.revalidate();
        levelScrollPane.repaint();
    }

    private boolean getDialogConfirmation() {
        if (undoStack.isEmpty()) {
            // this means the user hasn't done anything yet.
            return true;
        }

        ConfirmationDialog dialog = new ConfirmationDialog();
        dialog.setAlwaysOnTop(true);
        dialog.setLocationRelativeTo(mainPanel);
        dialog.pack();
        dialog.setVisible(true);
        return dialog.isConfirmed();
    }

    private void handleLevelPanelMouseEvent(MouseEvent e) {
        int x = e.getX() / gridSize;
        int y = e.getY() / gridSize;
        if (x >= 0 && x < level.getWidth() && y >= 0 && y < level.getHeight()) {
            Tile oldTile = level.getTileMatrix().getTile(x, y);
            EditorCommand command = new ChangeTileCommand(x, y, selectedTile, oldTile, this);
            doCommand(command);
        }
    }

    private void handleTilePalettePanelMouseEvent(MouseEvent e) {
        List<Tile> fgTiles = TileData.TILES;
        int x = e.getX() / gridSize;
        int y = e.getY() / gridSize;
        int index = (y * paletteColumns) + x;
        if (index >= 0 && index < fgTiles.size()) {
            selectedTile = fgTiles.get(index);
        }
        selectedTilePreviewPanel.repaint();
    }

    private void handleNewRequested() {
        if (getDialogConfirmation()) {
            createNewLevel();
        }
    }

    private void handleOpenRequested() {
        if (!getDialogConfirmation()) {
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(mainPanel);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            Level level = fileIO.readLevelFile(selectedFile);
            if (level != null) {
                loadExistingLevel(level);
            }
        }
    }

    private void handleSaveRequested() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(mainPanel);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            fileIO.writeLevelFile(selectedFile, level);
        }
    }

    private void handleUndoRequested() {
        if (!undoStack.isEmpty()) {
            EditorCommand command = undoStack.pop();
            command.undo();
        }
    }

    private void handlePropertiesRequested() {
        PropertiesDialog dialog = new PropertiesDialog(this);
        dialog.setAlwaysOnTop(true);
        dialog.setLocationRelativeTo(mainPanel);
        dialog.pack();
        dialog.setVisible(true);
    }

    private void handleQuitRequested() {
        if (getDialogConfirmation()) {
            System.exit(0);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        switch (e.getActionCommand()) {
            case "new" -> handleNewRequested();
            case "open" -> handleOpenRequested();
            case "save" -> handleSaveRequested();
            case "undo" -> handleUndoRequested();
            case "properties" -> handlePropertiesRequested();
            case "quit" -> handleQuitRequested();
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Level Editor");
        FRAME = frame;
        frame.setContentPane(new LevelEditor().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(400, 300));
        frame.setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(1, 2, new Insets(10, 10, 10, 10), -1, -1));
        mainPanel.setPreferredSize(new Dimension(800, 600));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        levelScrollPane = new JScrollPane();
        levelScrollPane.setAlignmentX(0.5f);
        levelScrollPane.setAutoscrolls(false);
        levelScrollPane.setFocusable(true);
        panel1.add(levelScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(9999999, -1), null, 0, false));
        levelPanel.setAutoscrolls(false);
        levelPanel.setPreferredSize(new Dimension(-1, -1));
        levelScrollPane.setViewportView(levelPanel);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        toolScrollPane = new JScrollPane();
        toolScrollPane.setAlignmentX(0.5f);
        toolScrollPane.setAutoscrolls(false);
        toolScrollPane.setVisible(true);
        panel2.add(toolScrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(200, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        toolScrollPane.setViewportView(tilePalettePanel);
        toolControlPanel = new JPanel();
        toolControlPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(toolControlPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        toolControlPanel.add(selectedTilePreviewPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(32, 32), new Dimension(32, 32), null, 0, false));
        final Spacer spacer1 = new Spacer();
        toolControlPanel.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        toolControlPanel.add(spacer2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
