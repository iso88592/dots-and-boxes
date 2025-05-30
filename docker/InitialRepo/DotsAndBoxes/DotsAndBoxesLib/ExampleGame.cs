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
        while (true)
        {
            // TODO: make the next step
            GameState.Edge edge = (GameState.Edge)random.Next(4);
            int x = random.Next(state.Width);
            int y = random.Next(state.Height);
            if (state.GetEdge(x, y, edge) != -1) continue;
            state.SetEdge(x, y, edge);
            return state;
        }
    }

    public string[] Names()
    {
        return  new [] { "Várkonyi Tibor" } ;
    }
}