package hu.mocman.dotsandboxes;

import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.junit.jupiter.api.Test;

public class GameStateTransferTest {
    @Test
    public void fromJsonHasExpectedFields() throws JSONException {
        GameState testState = GameState.FromJson("{\"currentPlayerIndex\":42,\"level\":{\"0\":[-1,0,1,2,3,4,5,6,7,8,9],\"1\":[10,11,12,13,14,15,16,17,18,19,20],\"2\":[21,22,23,24,25,26,27,28,29,30,31],\"3\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],\"4\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],\"5\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],\"6\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],\"7\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],\"8\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],\"9\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],\"10\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1]},\"width\":5,\"height\":5}");
        Assertions.assertThat(testState.getWidth()).isEqualTo(5);
        Assertions.assertThat(testState.getHeight()).isEqualTo(5);
        Assertions.assertThat(testState.getCurrentPlayerIndex()).isEqualTo(42);
        Assertions.assertThat(testState.getEdge(0,0, GameState.Edge.Center)).isEqualTo(11);
        Assertions.assertThat(testState.getEdge(0,0, GameState.Edge.Left)).isEqualTo(10);
        Assertions.assertThat(testState.getEdge(0,0, GameState.Edge.Right)).isEqualTo(12);
        Assertions.assertThat(testState.getEdge(0,0, GameState.Edge.Top)).isEqualTo(0);
        Assertions.assertThat(testState.getEdge(0,0, GameState.Edge.Bottom)).isEqualTo(22);
    }

    @Test
    public void fillTest() {
        GameState testState = GameState.NewGame(5);
        testState.setEdge(1,1, GameState.Edge.Left);
        testState.setEdge(1,1, GameState.Edge.Right);
        testState.setEdge(1,1, GameState.Edge.Top);
        testState.setEdge(1,1, GameState.Edge.Bottom);
        Assertions.assertThat(testState.getEdge(1,1, GameState.Edge.Center)).isEqualTo(-1);
        testState.fill();
        Assertions.assertThat(testState.getEdge(1,1, GameState.Edge.Center)).isEqualTo(0);
    }

    @Test
    public void invalidMoveFillCell() {
        GameState originalState = GameState.NewGame(5);
        GameState testState = originalState.clone();
        testState.setEdge(1,1, GameState.Edge.Center);
        Assertions.assertThat(originalState.isValidMove(testState)).isFalse();
    }

    @Test
    public void invalidMoveNoMove() {
        GameState originalState = GameState.NewGame(5);
        GameState testState = originalState.clone();
        Assertions.assertThat(originalState.isValidMove(testState)).isFalse();
    }

    @Test
    public void invalidMoveEraseEdge() {
        GameState originalState = GameState.NewGame(5);
        originalState.setEdge(1,1, GameState.Edge.Right);
        GameState testState = originalState.clone();
        testState.setCurrentPlayerIndex(-1);
        testState.setEdge(1,1, GameState.Edge.Right);
        testState.setCurrentPlayerIndex(0);
        Assertions.assertThat(originalState.isValidMove(testState)).isFalse();
    }

    @Test
    public void invalidMoveOverwriteEdge() {
        GameState originalState = GameState.NewGame(5);
        originalState.setEdge(1,1, GameState.Edge.Right);
        GameState testState = originalState.clone();
        testState.setCurrentPlayerIndex(1);
        testState.setEdge(1,1, GameState.Edge.Right);
        testState.setCurrentPlayerIndex(0);
        Assertions.assertThat(originalState.isValidMove(testState)).isFalse();
    }

    @Test
    public void invalidMoveTwoEdges() {
        GameState originalState = GameState.NewGame(5);
        GameState testState = originalState.clone();
        testState.setCurrentPlayerIndex(1);
        testState.setEdge(1,1, GameState.Edge.Right);
        testState.setEdge(1,1, GameState.Edge.Left);
        testState.setCurrentPlayerIndex(0);
        Assertions.assertThat(originalState.isValidMove(testState)).isFalse();
    }

    @Test
    public void testJsonTransfer() throws JSONException {
        String testJson = "{\"currentPlayerIndex\":42,\"level\":{\"0\":[-1,0,1,2,3,4,5,6,7,8,9],\"1\":[10,11,12,13,14,15,16,17,18,19,20],\"2\":[21,22,23,24,25,26,27,28,29,30,31],\"3\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],\"4\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],\"5\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],\"6\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],\"7\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],\"8\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],\"9\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],\"10\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1]},\"width\":5,\"height\":5}";
        GameState testState = GameState.FromJson(testJson);
        String resultJson = testState.convertStateToJson();
        Assertions.assertThat(resultJson).isEqualTo(testJson);

    }

}
