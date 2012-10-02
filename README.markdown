This tiny library is intended to make a java program multithreaded in very fast and easy way - a unix pipes like implementation in java.

let's assume simple workflow example:

![workflow-schema-example-higlevel](https://raw.github.com/mousator/pipeline-workflow/master/workflow-sample-highlevel.png "Simple image processing workflow")

... that can correspondent to image processing, the input is several files representing the images, we want to perform some transformation to each of them and save the result. Each image has independent transformation so it's perfect for data independent chunking and processing.

in unix bash it could look like:

	find . -name "*.jpg" | xargs -I {} convert {} -resize 50% out/{}
	
that will convert image by image, but the whole transformation will happen in a single process.

Assuming single JVM, the processing can be implemented as the schema below:

![workflow-schema-example](https://raw.github.com/mousator/pipeline-workflow/master/workflow-sample.png "Simple schema of pipeline workflow")

* one thread for reading the images from the filesystem - data producer
* four threads for parallel image transformations, each thread for separate image - data processors
* one thread to save image by image to a file system - data consumer

the tricky part is a thread communication, creating blocking queues and making sure the threads will terminate once all images were processed.

Here comes this library as a handy tool, the code would look like:

```java
// creating filesystem crawler
Worker fsCrawler = new FsCrawler();

// data processors, each one will be executed in
// a separate thread, each one will receive data as
// they become available from the crawler
int numberOfProcessors = 4;
Worker[] processors = new Worker[numberOfProcessors];
for(int i=0;i < numberOfProcessors;i++){
	processors[i]=new ImageTransformer();
}

// a workflow writer, in this case it collects all processed data
// - images as save them to the filesystem
Worker writer = new FsWriter();

// create pipeline similar to UNIX pipeline
IPipe readerPipe = new SynchronousPipe().addInputs(dataReader).addOutputs(processors);
IPipe writerPipe = new SynchronousPipe().addInputs(processors).addOutputs(writer);

// register pipes and execute workflow
Workflow workflow = new Workflow();
workflow.registerPipes(readerPipe, writerPipe);
// do blocking execution
workflow.execute();
```

The implementation of ImageTransformer (other classes are similar) would look like:

```java
public class ImageTransformer extends Worker {
	@Override
	protected void doJob(Object data, Map<String, IPipe> outPipes)
		throws Exception {
		Image image = (Image)data;
		Image transformedImage = transformImage(image)
		outPipes.get(DEFAULT_PIPE_TYPE).send(transformedImage);
	}
```

Some remarks:
* the workflow automatically finds workers which don't have any input and schedule them
* all instances defined as workers are reused, for instance if there are 16 images and 4 image transformers, then each image transformer gets in average fout times an input token = four images.
* when there's no active worker, the workflow is terminated
* each worker is running in a separate thread
* if the worker cannot send data via pipe (all connected workers are busy) the worker gets blocked and waits until at least one connected worker gets available
* pipe can by synchronous or asynchronous, asynchronous means it can store some preprocessed data on the pipe

The available example is can be find at:
https://github.com/mousator/pipeline-workflow/blob/master/src/test/java/sk/emandem/pipeline/example/SimpleOne.java

by [Michal Antolik]
