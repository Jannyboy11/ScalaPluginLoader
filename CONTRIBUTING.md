## Contributing to ScalaLoader

Hello!

So you want to contribute to ScalaLoader? Great! You've come to the right place!

## General rules

If you have an idea for a new feature, make sure to create an issue first.
That way I can spit my opinion on whether this would be a good idea to have,
and I can mention some things to consider (e.g. interactions with other parts of the plugin).

For small additions that fix incompleteness issues (e.g. new ScalaVersions) or dependency upgrades you can just open a PR directly without creating an issue ticket first.

If you've got my approval, then there's a few hard rules that I apply when writing this plugin, them being:
- The loader itself must be strictly Java-only code with no dependency on the Scala library. I want this project to remain usable by *all* versions of Scala.
- Try to not introduce new dependencies. If something can be done using Java's standard library or a library bundled with Bukkit, please do so.
- This project must remain compatible with Bukkit. Non-reflective usage of APIs that are only available in Spigot or Paper is not allowed.
- The ScalaLoader plugin must be able to run on Java 8. (May be subject to change, see [#3](https://github.com/Jannyboy11/ScalaPluginLoader/issues/3).)

## Guidelines for new features

Here's a few guidelines that I stick to when writing new features:
- Make sure to test/showcase new features in one of the demo plugins.

## Code style

I'm not super strict when it comes to this, but in general:
- Opening braces go on the same line as the class or method definition.
- Indent using 4 spaces. This applies to Scala code as well!
- Explain non-trivial code using comments, especially when it comes to niche areas of the plugin (e.g. classloading).
- Prefer long descriptive names of short ones.
- Variables that remain unchanged in long methods should be marked as `final`.

## Final words

I (Jannyboy11) am the [BDFL](https://en.wikipedia.org/wiki/Benevolent_dictator_for_life) of this project and have the right to reject your contributions without explanation.
Of course you're free to fork the project and adjust it to your own needs as you wish.

Other than that, have fun! 😄
