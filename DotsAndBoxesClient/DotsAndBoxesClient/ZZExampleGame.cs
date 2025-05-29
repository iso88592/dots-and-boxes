using DotsAndBoxesLib;

namespace DotsAndBoxesClient;

public class ZzExampleGame: IDotsAndBoxes
{
    public ZzExampleGame()
    {
        Console.WriteLine("Creating example new game");
    }
    
    public GameState Turn(GameState oldState)
    {
        return oldState;
    }

    public string[] Names()
    {
        return  new [] { "Várkonyi Tibor" } ;
    }
}