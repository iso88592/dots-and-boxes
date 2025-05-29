namespace DotsAndBoxesLib;

public class GameState
{
    public static GameState NewGame(int width, int height)
    {
        GameState gameState = new()
        {
            Width = width,
            Height = height,
            CurrentPlayerIndex = 0,
            Level = new int[width * 2 + 1, height * 2 + 1]
        };
        for (int i = 0; i < width * 2 + 1; i++)
        {
            for (int j = 0; j < height * 2 + 1; j++)
            {
                gameState.Level[i, j] = -1;
            }
        }

        return gameState;
    }

    public int Width { get; set; }
    public int Height { get; set; }
    public int CurrentPlayerIndex { get; set; }

    public int[,] Level { get; set; }

    public enum Edge
    {
        Left,
        Right,
        Top,
        Bottom,
        Ceter
    }

    public void SetEdge(int x, int y, Edge edge)
    {
        GetCoordinates(x, y, edge, out int x1, out int y1);

        Level[x1, y1] = CurrentPlayerIndex;
    }

    private void GetCoordinates(int x, int y, Edge edge, out int x1, out int y1)
    {
        int dx = 0;
        int dy = 0;
        switch (edge)
        {
            case Edge.Left: dx = -1; break;
            case Edge.Right: dx = 1; break;
            case Edge.Top: dy = -1; break;
            case Edge.Bottom: dy = 1; break;
        }

        x1 = x * 2 + dx + 1;
        y1 = y * 2 + dy + 1;
    }

    public int GetEdge(int x, int y, Edge edge)
    {
        GetCoordinates(x, y, edge, out int x1, out int y1);
        return Level[x1, y1];
    }

    public bool IsValidMove(GameState newState)
    {
        if (Width != newState.Width || Height != newState.Height) return false;
        if (CurrentPlayerIndex != newState.CurrentPlayerIndex) return false;
        int count = 0;
        for (int i = 0; i < Width * 2 + 1; i++)
        {
            for (int j = 0; j < Height * 2 + 1; j++)
            {
                if (i % 2 + j % 2 == 0) continue;
                if (i % 2 + j % 2 == 2)
                {
                    if (Level[i,j] != newState.Level[i, j]) return false;
                }

                if (i % 2 + j % 2 == 1)
                {
                    if (Level[i, j] != newState.Level[i, j])
                    {
                        if (newState.Level[i, j] == -1) return false;
                        if (Level[i, j] != -1) return false;
                        if (newState.Level[i, j] != CurrentPlayerIndex) return false;
                        count++;
                        if (count > 1) return false;
                    }
                }
            }
        }

        return count == 1;
    }

    public bool IsGameOver()
    {
        for (int i = 0; i < Width * 2 + 1; i++)
        {
            for (int j = 0; j < Height * 2 + 1; j++)
            {
                if (i % 2 + j % 2 != 1) continue;
                if (Level[i, j] == -1) return false;
            }
        }

        return true;
    }

    public void NextTurn()
    {
        CurrentPlayerIndex++;
        CurrentPlayerIndex %= 2;
    }

    public bool Fill()
    {
        bool result = false;
        for (int i = 0; i < Width; i++)
        {
            for (int j = 0; j < Height; j++)
            {
                result |= FillAt(i, j);
            }
        }
        return result;
    }
    
    private bool FillAt(int x, int y)
    {
        if (GetEdge(x,y, Edge.Ceter) != -1) return false;
        if (GetEdge(x, y, Edge.Left) == -1) return false;
        if (GetEdge(x, y, Edge.Top) == -1) return false;
        if (GetEdge(x, y, Edge.Right) == -1) return false;
        if (GetEdge(x, y, Edge.Bottom) == -1) return false;
        SetEdge(x,y, Edge.Ceter);
        return true;
    }

    public GameState Clone()
    {
        GameState gameState = new()
        {
            Width = Width,
            Height = Height,
            CurrentPlayerIndex = CurrentPlayerIndex,
            Level = new int[Width * 2 + 1, Height * 2 + 1]
        };
        for (int i = 0; i < Width * 2 + 1; i++)
        {
            for (int j = 0; j < Height * 2 + 1; j++)
            {
                gameState.Level[i, j] = Level[i, j];
            }
        }

        return gameState;
    }

    public string Print()
    {
        string result = "";
        for (int j = 0; j < Height * 2 + 1; j++)
        {
            for (int i = 0; i < Width * 2 + 1; i++)
            {
                if (i % 2 + j % 2 == 2) result += F(Level[i, j]);
                if (i % 2 + j % 2 == 1) result += E(Level[i, j], i % 2 == 0);
                if (i % 2 + j % 2 == 0) result += M(i,j);
            }

            result += "\n";
        }

        return result;
    }

    private string F(int v)
    {
        switch (v)
        {
            case -1: return "   ";
            case 0: return "\x1b[41m 1 \x1b[0m";
            case 1: return "\x1b[42m 2 \x1b[0m";
        }

        return "%";
    }

    private String E(int v, bool isHorizontal)
    {
        switch (v)
        {
            case -1: return isHorizontal ? " " : "   ";
            case 0:
                return isHorizontal ? "\x1b[31m\u2502\x1b[0m" : "\x1b[31m\u2500\u2500\u2500\x1b[0m";
            case 1: return isHorizontal ? "\x1b[32m\u2502\x1b[0m" : "\x1b[32m\u2500\u2500\u2500\x1b[0m";
        }

        return "%";
    }

    private String M(int x, int y)
    {
        if (x == 0 && y == 0) return "\u250c";
        if (x == 0 && y == Height * 2) return "\u2514";
        if (x == 0) return "\u251C";
        if (x == Width * 2 && y == 0) return "\u2510";
        if (x == Width * 2 && y == Height * 2) return "\u2518";
        if (x == Width * 2) return "\u2524";
        if (y == 0)  return "\u252C";
        if (y == Height * 2) return "\u2534";
        return "\u253C";
    }
}