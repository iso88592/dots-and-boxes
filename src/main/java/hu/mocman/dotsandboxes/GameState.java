package hu.mocman.dotsandboxes;

public class GameState implements Cloneable {

    public static GameState NewGame(int boardSize) {
        return null;
    }

    public boolean gameOver() {
        return false;
    }

    @Override
    public GameState clone() {
        try {
            return (GameState) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public boolean isValidMove(GameState gameState) {
        return false;
    }
}
