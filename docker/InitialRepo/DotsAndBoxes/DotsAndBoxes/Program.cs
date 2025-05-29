// See https://aka.ms/new-console-template for more information

using DotsAndBoxes;
using DotsAndBoxesLib;

ExampleGame[] players = new ExampleGame[2];

for (int i = 0; i < players.Length; i++)
{
    players[i] = new ExampleGame();
}

int currentPlayer = 0;

GameState currentState = GameState.NewGame(5, 5);

while (!currentState.IsGameOver())
{
    GameState newState = players[currentPlayer].Turn(currentState.Clone());
    if (currentState.IsValidMove(newState))
    {
        bool scored = newState.Fill();
        currentState = newState;
        Console.Clear();
        Console.WriteLine(currentState.Print());
        Thread.Sleep(100);
        if (!scored)
        {
            currentState.NextTurn();
            currentPlayer++;
            currentPlayer %= 2;
        }
    }
}
