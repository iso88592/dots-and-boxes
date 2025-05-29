using System.Reflection;
using DotsAndBoxesLib;

public static class DotsAndBoxesFactory
{
    private static List<IDotsAndBoxes> _dotsAndBoxes = new();
    
    public static void Register(this IServiceCollection dotsAndBoxes, Assembly assembly)
    {
        var handlers = assembly.GetTypes()
            .Where(t => typeof(IDotsAndBoxes).IsAssignableFrom(t) && t is {IsClass: true, IsAbstract: false});
        foreach (var type in handlers)
        {
            _dotsAndBoxes.Add(Activator.CreateInstance(type) as IDotsAndBoxes);
        }
    }
    

    public static IDotsAndBoxes GetInstance()
    {
        if (_dotsAndBoxes.Count == 0)
        {
            throw new NotImplementedException();
        }

        _dotsAndBoxes.Sort((a, b) => String.Compare(a.GetType().Name, b.GetType().Name, StringComparison.Ordinal));
        Console.WriteLine("Selected implementation {0}", _dotsAndBoxes.First().GetType().Name);
        return _dotsAndBoxes.First();
    }
}
