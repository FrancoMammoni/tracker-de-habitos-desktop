package ui.components;

import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * Utilidad para permitir el redimensionamiento suave de ventanas con estilo UNDECORATED.
 * Detecta bordes y esquinas dentro de un margen perimetral y ajusta el tamaño del Stage.
 */
public class WindowResizeHelper {

    private static final int RESIZE_MARGIN = 8;

    private final Stage stage;
    private final Scene scene;

    private Cursor currentCursor = Cursor.DEFAULT;
    private ResizeDirection resizeDirection = ResizeDirection.NONE;

    private double startScreenX = 0;
    private double startScreenY = 0;
    private double startStageX = 0;
    private double startStageY = 0;
    private double startWidth = 0;
    private double startHeight = 0;

    private enum ResizeDirection {
        NONE, N, S, E, W, NW, NE, SW, SE
    }

    private WindowResizeHelper(Stage stage, Scene scene) {
        this.stage = stage;
        this.scene = scene;
        initListeners();
    }

    public static void attach(Stage stage, Scene scene) {
        new WindowResizeHelper(stage, scene);
    }

    private void initListeners() {
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, this::handleMouseMoved);
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
    }

    private boolean isMaximized() {
        return stage.isMaximized() || Boolean.TRUE.equals(stage.getProperties().get("custom-maximized"));
    }

    private void handleMouseMoved(MouseEvent event) {
        if (isMaximized()) {
            if (currentCursor != Cursor.DEFAULT) {
                scene.setCursor(Cursor.DEFAULT);
                currentCursor = Cursor.DEFAULT;
                resizeDirection = ResizeDirection.NONE;
            }
            return;
        }

        double mouseX = event.getSceneX();
        double mouseY = event.getSceneY();
        double sceneW = scene.getWidth();
        double sceneH = scene.getHeight();

        boolean west = mouseX <= RESIZE_MARGIN;
        boolean east = mouseX >= sceneW - RESIZE_MARGIN;
        boolean north = mouseY <= RESIZE_MARGIN;
        boolean south = mouseY >= sceneH - RESIZE_MARGIN;

        if (north && west) {
            currentCursor = Cursor.NW_RESIZE;
            resizeDirection = ResizeDirection.NW;
        } else if (north && east) {
            currentCursor = Cursor.NE_RESIZE;
            resizeDirection = ResizeDirection.NE;
        } else if (south && west) {
            currentCursor = Cursor.SW_RESIZE;
            resizeDirection = ResizeDirection.SW;
        } else if (south && east) {
            currentCursor = Cursor.SE_RESIZE;
            resizeDirection = ResizeDirection.SE;
        } else if (west) {
            currentCursor = Cursor.W_RESIZE;
            resizeDirection = ResizeDirection.W;
        } else if (east) {
            currentCursor = Cursor.E_RESIZE;
            resizeDirection = ResizeDirection.E;
        } else if (north) {
            currentCursor = Cursor.N_RESIZE;
            resizeDirection = ResizeDirection.N;
        } else if (south) {
            currentCursor = Cursor.S_RESIZE;
            resizeDirection = ResizeDirection.S;
        } else {
            currentCursor = Cursor.DEFAULT;
            resizeDirection = ResizeDirection.NONE;
        }

        scene.setCursor(currentCursor);
    }

    private void handleMousePressed(MouseEvent event) {
        if (isMaximized() || resizeDirection == ResizeDirection.NONE) {
            return;
        }

        startScreenX = event.getScreenX();
        startScreenY = event.getScreenY();
        startStageX = stage.getX();
        startStageY = stage.getY();
        startWidth = stage.getWidth();
        startHeight = stage.getHeight();
        event.consume();
    }

    private void handleMouseDragged(MouseEvent event) {
        if (isMaximized() || resizeDirection == ResizeDirection.NONE) {
            return;
        }

        event.consume();

        double deltaX = event.getScreenX() - startScreenX;
        double deltaY = event.getScreenY() - startScreenY;

        double minWidth = Math.max(stage.getMinWidth(), 960);
        double minHeight = Math.max(stage.getMinHeight(), 600);

        switch (resizeDirection) {
            case E -> {
                double newW = Math.max(startWidth + deltaX, minWidth);
                stage.setWidth(newW);
            }
            case S -> {
                double newH = Math.max(startHeight + deltaY, minHeight);
                stage.setHeight(newH);
            }
            case W -> {
                double newW = Math.max(startWidth - deltaX, minWidth);
                if (newW > minWidth || deltaX < 0) {
                    stage.setX(startStageX + (startWidth - newW));
                    stage.setWidth(newW);
                }
            }
            case N -> {
                double newH = Math.max(startHeight - deltaY, minHeight);
                if (newH > minHeight || deltaY < 0) {
                    stage.setY(startStageY + (startHeight - newH));
                    stage.setHeight(newH);
                }
            }
            case SE -> {
                stage.setWidth(Math.max(startWidth + deltaX, minWidth));
                stage.setHeight(Math.max(startHeight + deltaY, minHeight));
            }
            case SW -> {
                double newW = Math.max(startWidth - deltaX, minWidth);
                if (newW > minWidth || deltaX < 0) {
                    stage.setX(startStageX + (startWidth - newW));
                    stage.setWidth(newW);
                }
                stage.setHeight(Math.max(startHeight + deltaY, minHeight));
            }
            case NE -> {
                stage.setWidth(Math.max(startWidth + deltaX, minWidth));
                double newH = Math.max(startHeight - deltaY, minHeight);
                if (newH > minHeight || deltaY < 0) {
                    stage.setY(startStageY + (startHeight - newH));
                    stage.setHeight(newH);
                }
            }
            case NW -> {
                double newW = Math.max(startWidth - deltaX, minWidth);
                if (newW > minWidth || deltaX < 0) {
                    stage.setX(startStageX + (startWidth - newW));
                    stage.setWidth(newW);
                }
                double newH = Math.max(startHeight - deltaY, minHeight);
                if (newH > minHeight || deltaY < 0) {
                    stage.setY(startStageY + (startHeight - newH));
                    stage.setHeight(newH);
                }
            }
            case NONE -> {}
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        if (resizeDirection != ResizeDirection.NONE) {
            event.consume();
        }
    }
}
