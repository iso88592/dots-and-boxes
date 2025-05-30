package hu.mocman.dotsandboxes;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.util.Tuple;

import java.awt.*;

@Slf4j
@Data
public class GameState implements Cloneable {

    private int width;
    private int height;
    private int currentPlayerIndex;
    private int[][] level;

    public static GameState FromJson(String response) throws JSONException {
        GameState result = new GameState();
        JSONObject json = new JSONObject(response);
        result.setWidth(json.getInt("width"));
        result.setHeight(json.getInt("height"));
        result.setCurrentPlayerIndex(json.getInt("currentPlayerIndex"));

        JSONObject levelJson = json.getJSONObject("level");
        int[][] newLevel = new int[result.getWidth() * 2 + 1][result.getHeight() * 2 + 1];
        for (int i = 0; i < result.getWidth() * 2 + 1; i++) {
            for (int j = 0; j < result.getHeight() * 2 + 1; j++) {
                newLevel[i][j] = levelJson.getJSONArray(String.valueOf(j)).getInt(i);
            }
        }
        result.setLevel(newLevel);

        return result;
    }

    public void nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % 2;
    }

    public int countScores(int kind) {
        int count = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (getEdge(i, j, Edge.Center) == kind) count++;
            }
        }
        return count;
    }

    String convertStateToJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("width", getWidth());
        json.put("height", getHeight());
        json.put("currentPlayerIndex", getCurrentPlayerIndex());

        JSONObject levelJson = new JSONObject();
        for (int i = 0; i < getWidth() * 2 + 1; i++) {
            JSONArray row = new JSONArray();
            for (int j = 0; j < getHeight() * 2 + 1; j++) {
                row.put(getLevel()[j][i]);
            }
            levelJson.put(String.valueOf(i), row);
        }
        json.put("level", levelJson);
        return json.toString();
    }

    public void draw(Graphics graphics) {
        int cellSize = 360/width;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int v = getEdge(i, j, Edge.Center);
                setColor(graphics, v);
                graphics.fillRect(i * cellSize, j * cellSize + 40, cellSize, cellSize);

                v = getEdge(i, j, Edge.Left);
                setColor(graphics, v);
                graphics.drawLine(i * cellSize, j * cellSize + 40, i * cellSize + cellSize, j * cellSize + 40);
                v = getEdge(i, j, Edge.Right);
                setColor(graphics, v);
                graphics.drawLine(i * cellSize + cellSize, j * cellSize + 40, i * cellSize + cellSize, j * cellSize + cellSize + 40);
                v = getEdge(i, j, Edge.Top);
                setColor(graphics, v);
                graphics.drawLine(i * cellSize, j * cellSize + 40, i * cellSize, j * cellSize + cellSize + 40);
                v = getEdge(i, j, Edge.Bottom);
                setColor(graphics, v);
                graphics.drawLine(i * cellSize, j * cellSize + cellSize + 40, i * cellSize + cellSize, j * cellSize + cellSize + 40);
            }
        }
    }

    private void setColor(Graphics graphics, int v) {
        switch (v) {
            case -1:
                graphics.setColor(Color.LIGHT_GRAY);
                break;
            case 0:
                graphics.setColor(Color.RED);
                break;
            case 1:
                graphics.setColor(Color.GREEN);
                break;
        }

    }

    public enum Edge {
        Left,
        Right,
        Top,
        Bottom,
        Center
    }

    public void setEdge(int x, int y, Edge edge) {
        Tuple<Integer, Integer> coordinates = GetCoordinates(x, y, edge);
        level[coordinates._1()][coordinates._2()] = currentPlayerIndex;
    }

    private Tuple<Integer, Integer> GetCoordinates(int x, int y, Edge edge) {
        int dx = 0;
        int dy = 0;
        switch (edge) {
            case Left:
                dx = -1;
                break;
            case Right:
                dx = 1;
                break;
            case Top:
                dy = -1;
                break;
            case Bottom:
                dy = 1;
                break;
            case Center:
                break;
            default:
        }
        return new Tuple<>(x * 2 + dx + 1, y * 2 + dy + 1);
    }

    public int getEdge(int x, int y, Edge edge) {
        Tuple<Integer, Integer> coordinates = GetCoordinates(x, y, edge);
        return level[coordinates._1()][coordinates._2()];
    }

    public boolean isValidMove(GameState newState) {
        if (width != newState.width || height != newState.height) {
            log.info("Wrong dimensions!");
            return false;
        }
        if (currentPlayerIndex != newState.currentPlayerIndex) {
            log.info("Wrong player index!");
            return false;
        }
        int count = 0;
        for (int i = 0; i < width * 2 + 1; i++) {
            for (int j = 0; j < height * 2 + 1; j++) {
                if (i % 2 + j % 2 == 0) continue;
                if (i % 2 + j % 2 == 2) {
                    if (level[i][j] != newState.level[i][j]) {
                        log.info("Bot tried to fill a cell!");
                        return false;
                    }
                }
                if (i % 2 + j % 2 == 1) {
                    if (level[i][j] != newState.level[i][j]) {
                        if (newState.level[i][j] == -1) {
                            log.info("Bot tried to clear an edge!");
                            return false;
                        }
                        if (level[i][j] != -1) {
                            log.info("Bot tried to rewrite an edge!");
                            return false;
                        }
                        count++;
                        if (count > 1) {
                            log.info("Bot tried to write more than one edge!");
                            return false;
                        }
                    }
                }
            }
        }
        return count == 1;
    }

    public static GameState NewGame(int boardSize) {
        GameState state = new GameState();
        state.width = boardSize;
        state.height = boardSize;
        state.currentPlayerIndex = 0;
        state.level = new int[boardSize * 2 + 1][boardSize * 2 + 1];
        for (int i = 0; i < boardSize * 2 + 1; i++) {
            for (int j = 0; j < boardSize * 2 + 1; j++) {
                state.level[i][j] = -1;
            }
        }
        return state;
    }

    public boolean isGameOver() {
        for (int i = 0; i < width * 2 + 1; i++) {
            for (int j = 0; j < height * 2 + 1; j++) {
                if (i % 2 + j % 2 != 1) continue;
                if (level[i][j] == -1) return false;
            }
        }
        return true;
    }

    @Override
    public GameState clone() {
        try {
            GameState cloned = (GameState) super.clone();
            cloned.level = new int[width * 2 + 1][height * 2 + 1];
            for (int i = 0; i < width * 2 + 1; i++) {
                for (int j = 0; j < height * 2 + 1; j++) {
                    cloned.level[i][j] = this.level[i][j];
                }
            }
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    boolean fill() {
        boolean result = false;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                result |= fillAt(i, j);
            }
        }
        return result;
    }

    private boolean fillAt(int x, int y) {
        if (getEdge(x, y, Edge.Center) != -1) return false;
        if (getEdge(x, y, Edge.Left) == -1) return false;
        if (getEdge(x, y, Edge.Right) == -1) return false;
        if (getEdge(x, y, Edge.Top) == -1) return false;
        if (getEdge(x, y, Edge.Bottom) == -1) return false;
        setEdge(x, y, Edge.Center);
        return true;
    }

    public String toHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"game\">");
        for (int j = 0; j < height; j++) {
            sb.append("<div class=\"game-row\">");
            for (int i = 0; i < width; i++) {
                sb.append("<div class=\"game-cell fill-");
                int v = getEdge(i, j, Edge.Center);
                sb.append(v);
                sb.append(" border-l");
                sb.append(getEdge(i, j, Edge.Left));
                sb.append(" border-t");
                sb.append(getEdge(i, j, Edge.Top));
                sb.append(" border-r");
                sb.append(getEdge(i, j, Edge.Right));
                sb.append(" border-b");
                sb.append(getEdge(i, j, Edge.Bottom));
                sb.append("\">");
                if (v != -1) {
                    sb.append(v + 1);
                } else {
                    sb.append("&nbsp;");
                }
                sb.append("</div>");
            }
            sb.append("</div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

}
