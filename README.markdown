This tiny library is intended to make a java program multithreaded in very fast way - a java like implementation of unix pipes.

let's assume simple workflow:

![workflow-schema-example](https://raw.github.com/mousator/pipeline-workflow/master/workflow-sample.png "Simple schema of pipeline workflow")

The schema correspondents to single data producent, multiple instances of same processors and one consumer. For instance image processing, the producent is crawling file system and looking for the images. The processor is doing and image transformations. And the consumer is saving altered images back to filesystem. 

So the implementation part would be to create a java thread that reads a filesystem. Separate four threads for image processing so we can transform four images concurrently. And the writer - consumer to save images back to filesystem, that's another separate thread. Now having total 6 threads, each one needs to communicate with each other. Implementing that needs proper solution for concurrent access to shared memory and proper handling of threads termination.

Also adding another pipe would need quite some reimplementation.

This library is intended to make such development easier and faster.

the code sample can be find at:
https://github.com/mousator/pipeline-workflow/blob/master/src/test/java/sk/emandem/pipeline/example/SimpleOne.java

by [Michal Antolik]
