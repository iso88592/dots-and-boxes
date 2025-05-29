using DotsAndBoxesLib;

namespace DotsAndBoxes;

public class ExampleGame: IDotsAndBoxes
{
    private Random random = new Random();
    public ExampleGame()
    {
        Console.WriteLine("Creating example new game");
    }
    
    public GameState Turn(GameState state)
    {
        // TODO: make the next step
        GameState.Edge edge = (GameState.Edge)random.Next(4);
        state.SetEdge(random.Next(state.Width), random.Next(state.Height), edge);
        return state;
    }

    public string[] Names()
    {
        return  new [] { "Várkonyi Tibor" } ;
    }
}