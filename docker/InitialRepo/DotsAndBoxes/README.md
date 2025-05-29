# Dots And Boxes
This is your project. Write your implementation and push it to the container to see your implementation's scores.

Please do not modify `DotsAndBoxesLib` project It's for your own good!

## Implementation

I have prepared a random implementation for you in DotsAndBoxes project.
Please see [ExampleGame.cs](DotsAndBoxes/ExampleGame.cs) for further details.
I have also prepared a Program.cs that you can use to test different implementations of the samge game.

## Deployment

Note that when deployed, all classes that implement the interface `IDotsAndBoxes` will be included, but only the first 
one alphabetically will be contested in the tournament. So if you don't see your updated code, you can always rename the
contesting classes.

## Debugging

You can always ssh into your container. You can either SSH or whatever you want to the container given you are using the
correct IP address. You can also set up your container to attach your dotnet to a debugger.

## Penalties

Slow and not responding implementations will get penalized by the server. If your implementation does insane things, the 
server will automatically disqualify that version. If your game cannot respond with a valid moves in 3 tries, it also
gets a penalty.
Please mind that the server resources are not infinite so please don't try to calculate heavy loads. It's still a game.

## Best Wishes
I hope you had fun on this activity. Feel free to leave your valuable feedback for me to improve on our next session!