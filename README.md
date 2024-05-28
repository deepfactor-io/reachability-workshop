# Java Agent code for the Deepfactor OWASP Reachability Workshop

This code is a sample [javassist](https://www.javassist.org) based agent that records which
classes have been loaded (eg, used). At periodic intervals, it
prints the list of classes as a JSON blob to stdout.

## Building

You will need [Gradle](https://gradle.org/releases) 6.1 or later installed. To build, run
_gradle shawdowJar_ from the root of the repository.

## Workshop Materials

Use this [presentation](https://github.com/zbraiterman/reachability-workshop/blob/main/OWASP%20Reachability%20Workshop.pdf) to learn about vulnerability reachability, including how to use this Java Agent.
