/**
 * 
 */
package sk.emandem.pipeline.example;

import java.util.Map;

import sk.emandem.pipeline.core.Workflow;
import sk.emandem.pipeline.core.pipe.IPipe;
import sk.emandem.pipeline.core.pipe.SynchronousPipe;
import sk.emandem.pipeline.core.worker.Worker;

/**
 * @author Michal Antolik (michal@emandem.sk)
 *
 */
public class SimpleOne {

	public static void main(String[] args) throws Exception{
		// a workflow reader, which creates data 
		// and will send them through pipeline
		Worker dataReader = createReader();
		
		// a workflow processors, each one will be executed in
		// a separate thread, each one will receive data as
		// they become available from the reader
		int numberOfProcessors = 4;
		Worker[] processors = new Worker[numberOfProcessors];
		for(int i=0;i<numberOfProcessors;i++){
			processors[i]=createProcessor();
		}
		
		// a workflow writer, in this case it collects all processed data
		// and it will iteratively create statistics from them
		Worker writer = createWriter();
		
		// create pipeline similar to UNIX pipeline
		IPipe readerPipe = new SynchronousPipe().addInputs(dataReader).addOutputs(processors);
		IPipe writerPipe = new SynchronousPipe().addInputs(processors).addOutputs(writer);
		
		// register pipes and execute workflow
		Workflow workflow = new Workflow();
		workflow.registerPipes(readerPipe, writerPipe);
		// do blocking execution
		workflow.execute();
	}
	
	private static Worker createReader(){
		return new Worker() {
			@Override
			protected void doJob(Object data, Map<String, IPipe> outPipes)
					throws Exception {
				int max=1000;
				for(int i=0; i< max;i++){
					outPipes.get(DEFAULT_PIPE_TYPE).send(new DataToken(Math.random()));
				}
			}
		};
	}
	
	private static Worker createProcessor(){
		return new Worker() {
			@Override
			protected void doJob(Object data, Map<String, IPipe> outPipes)
					throws Exception {
				DataToken dataToken = (DataToken)data;
				dataToken.setToken(Math.floor(dataToken.getToken()*10));
				outPipes.get(DEFAULT_PIPE_TYPE).send(dataToken);
			}
		};
	}
	
	private static Worker createWriter(){
		return new Worker() {
			private final int range=10;
			int[] receivedData = new int[range];
			@Override
			protected void doJob(Object data, Map<String, IPipe> outPipes)
					throws Exception {
				DataToken dataToken = (DataToken)data;
				receivedData[(int)dataToken.getToken()]++;
			}
			@Override
			public void close() throws Exception {
				//print out the stats
				for(int i=0; i<range;i++){
					System.out.println(String.format("Bucket %d: %d", i, receivedData[i]));
				}
			}
		};
	}
	
	private static class DataToken {
		private double token;
		public DataToken(double token) {
			this.token=token;
		}
		public double getToken() {
			return token;
		}
		public void setToken(double token) {
			this.token = token;
		}
	}
}
