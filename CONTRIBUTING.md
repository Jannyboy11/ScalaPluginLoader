## Contributing to ScalaLoader

Hello!

So you want to contribute to ScalaLoader? Great! You've come to the right place!

## General rules

There's a few hard rules that I apply when writing this plugin, them being:
- The loader itself must be strictly Java-only code with no dependency on the Scala library. I want this project to remain usable by *all* versions of Scala.
- Try to not introduce new dependencies. If something can be done using Java's standard library or a library bundled with Bukkit, please do so.
- This project must remain compatible with Bukkit. Non-reflective usage of APIs that are only available in Spigot or Paper is not allowed. 

## Guidelines for new features

Here's a few guidelines that I stick to when writing new features:
- Make sure to test/showcase new features in one of the demo plugins. 

## Code style

I'm not super strict when it comes to this, but in general:
- Opening braces go on the same line as the class or method definition.
- Indent using 4 spaces. This applies to Scala code as well!
- explain non-trivial code using comments, especially when it comes to niche areas of the plugin (for example: classloading).

## Final words

I (Jannyboy11) am the [BDFL](https://en.wikipedia.org/wiki/Benevolent_dictator_for_life) of this project and have the right to reject your contributions without explanation.

Other than that, have fun! 😄
