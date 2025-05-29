using System.Net;
using DotsAndBoxesLib;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
// Learn more about configuring OpenAPI at https://aka.ms/aspnet/openapi
builder.Services.AddOpenApi();


var app = builder.Build();

string uuid = Environment.GetEnvironmentVariable("UUID");
if (uuid == null)
{
    try
    {
        uuid = File.ReadAllText("/myname.is");
    }
    catch (IOException e)
    {
        uuid = Guid.NewGuid().ToString();
    }
}
string serverHost = Environment.GetEnvironmentVariable("SERVERHOST") ?? "http://127.0.0.1:8080/dab";

Console.WriteLine($"ServerHost: {serverHost}");

HttpClient client = new HttpClient();
client.BaseAddress = new Uri(serverHost);
HttpResponseMessage response = await client.GetAsync(client.BaseAddress + $"/hello?id={uuid}");

Console.WriteLine(response.StatusCode);

if (!response.IsSuccessStatusCode)
{
    Console.WriteLine("Failed to get server response");
    return -1;
}

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}
var loadedAssemblies = AppDomain.CurrentDomain.GetAssemblies();
for (int i = 0; i < loadedAssemblies.Length; i++)
{
    builder.Services.Register(loadedAssemblies[i]);
}

IDotsAndBoxes dotsAndBoxes = DotsAndBoxesFactory.GetInstance();

GameState state = new GameState();

app.MapGet("/turn", () =>
    {
        state = dotsAndBoxes.Turn(state);
        return state;
    })
    .WithName("Turn");

var host = Dns.GetHostEntry(Dns.GetHostName());

app.MapGet("/info", () => new BotInfo(dotsAndBoxes.Names(), host.AddressList[0].ToString()));

app.Run();

return 0;

record BotInfo(string[] names, string ip);
