/**
 * 
 */
package sk.emandem.pipeline.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import sk.emandem.pipeline.core.worker.Worker;


/**
 * @author Michal Antolik (michal@emandem.sk)
 *
 */
public class MultiThreadedExecutor {
	
//	private static MultiThreadedExecutor instance = null;
	
	private AtomicInteger numberOfActiveTasks = new AtomicInteger(0);
	
	private AtomicLong totalProcessingTime = new AtomicLong(0);
	private long startTime = -1L;
	private long endTime = -1L;
	
//	public static MultiThreadedExecutor getInstance(){
//		if(instance == null){
//			instance = new MultiThreadedExecutor();
//			instance.startTime = System.currentTimeMillis();
//		}
//		return instance;
//	}
	
	public MultiThreadedExecutor(){
		executorService = Executors.newCachedThreadPool();
	}

	private final ExecutorService executorService;
	
	public void executeTask(Worker worker, Object data){
		if(startTime==-1L){
			startTime=System.currentTimeMillis();
		}
		numberOfActiveTasks.incrementAndGet();
		executorService.execute(new InnerTask(worker, data));
	}
	
	public void shutdown(){
		executorService.shutdown();
	}
	
	public boolean isFinished(){
		return numberOfActiveTasks.get() == 0;
	}
	
	public long getTotalProcessingTime(){
		return totalProcessingTime.get();
	}
	
	public long getTotalExecutionTime(){
		return endTime-startTime;
	}
	
	class InnerTask implements Runnable{

		private Worker worker; 
		private Object data;
		
		public InnerTask(Worker worker, Object data) {
			this.worker = worker;
			this.data = data;
		}
		
		@Override
		public void run() {
			long start = System.currentTimeMillis();
			worker.executeInBackground(data);
			numberOfActiveTasks.decrementAndGet();
			totalProcessingTime.addAndGet(System.currentTimeMillis()-start);
			if(numberOfActiveTasks.get() == 0){
				MultiThreadedExecutor.this.endTime = System.currentTimeMillis();
			}
		}
		
	}

	public int getNumberOfActiveTasks() {
		return numberOfActiveTasks.get();
	}
}

