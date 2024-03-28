# Java Agent code for the Deepfactor OWASP Reachability Workshop

This code is a sample javaassist-based agent that records which
classes have been loaded (eg, used). At periodic intervals, it
prints the list of classes as a JSON blob to stdout.

## Building

You will need gradle 6.1 or later installed. To build, run
_gradle shawdowJar_ from the root of the repository.
