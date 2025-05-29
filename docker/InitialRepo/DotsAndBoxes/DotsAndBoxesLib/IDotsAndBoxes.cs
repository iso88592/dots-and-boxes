namespace DotsAndBoxesLib;

public interface IDotsAndBoxes
{
    GameState Turn(GameState state);
    String[] Names();
}