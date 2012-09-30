/**
 * 
 */
package sk.emandem.pipeline.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.emandem.pipeline.core.pipe.IPipe;
import sk.emandem.pipeline.core.worker.IWorker;
import sk.emandem.pipeline.core.worker.Worker;


/**
 * @author Michal Antolik (michal@emandem.sk)
 *
 */
public class Workflow {

	private static Logger logger = LoggerFactory.getLogger(Workflow.class);
	
	private MultiThreadedExecutor executor = new MultiThreadedExecutor();
	
	private IPipe[] pipes;
	
	public void registerPipes(IPipe ... pipes) throws Exception{
		this.pipes = pipes;
		for(IPipe pipe: pipes){
			pipe.init();
		}
		for(IPipe pipe: pipes){
			for(IWorker worker : pipe.getEndWorkers()){
				worker.setExecutor(executor);
				worker.open();
			}
		}
	}
	
	public void execute() throws Exception{
		
		for(IPipe pipe: pipes){
			for(IWorker worker: pipe.getStandaloneWorkers()){
				((Worker)worker).startMe(null);
			}
		}
		
		while(!isFinished()){
			logger.info("waiting to finish jobs, #active jobs: " + executor.getNumberOfActiveTasks());
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				logger.error("Waiting for workflow end interrupted", e);
			}
		}
		
		double totalExecTime = (executor.getTotalExecutionTime())/1000.0;
		double totalProcTime = (executor.getTotalProcessingTime())/1000.0;
		logger.info("Workflow ended, took " + totalExecTime + " seconds, " +
				"total processing time took " + totalProcTime);
		
		logger.info("Total processing time for each worker:");
		for(IPipe pipe: pipes){
			for(IWorker worker : pipe.getEndWorkers()){
				double time = (worker.getTotalProcessingTime()/1000.0);
				int ratioTime = (int)(time * 100.0/totalExecTime);
				logger.info("\t" + worker.getClass().getSimpleName() + ": " + time + ", " + ratioTime + "%");;
			}
		}
		
		for(IPipe pipe: pipes){
			for(IWorker worker : pipe.getEndWorkers()){
				worker.close();
			}
		}
		executor.shutdown();
	}
	
	public boolean isFinished(){
		return executor.isFinished();
	}
	
	public void executeWithoutBlocking(){
		throw new UnsupportedOperationException();
	}
}
