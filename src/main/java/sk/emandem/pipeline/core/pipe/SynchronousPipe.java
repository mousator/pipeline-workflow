/**
 * 
 */
package sk.emandem.pipeline.core.pipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.emandem.pipeline.core.worker.IWorker;


/**
 * @author Michal Antolik (antomi000)
 *
 */
public class SynchronousPipe implements IPipe {

	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private IWorker[] inputWorkers;
	private IWorker[] outputWorkers;
	
//	private long waitTime = 10L;
	
//	private Lock echoLock = new ReentrantLock();
//	private boolean echoReceived = false;
//	private CountDownLatch echo = new CountDownLatch(1);
	
	private Semaphore freeReceivers;
	
	public boolean send(Object data) {
		try{
			freeReceivers.acquire();
			return trySendData(data);
		} catch (InterruptedException e) {
			logger.error("synchronous pipe interrupted", e);
		}
		return false;
	}
	
	private boolean trySendData(Object data){
		for(IWorker worker : outputWorkers){
			if(worker.receive(data, this)){
				//worker received new data
				return true;
			}
		}
		return false;
	}

	@Override
	public IPipe addInputs(IWorker... workers) {
		return addInputs(null, workers);
	}

	@Override
	public IPipe addOutputs(IWorker... workers) {
		return addOutputs(null, workers);
	}

	@Override
	public IPipe addInputs(String type, IWorker... workers) {
		this.inputWorkers = workers;
		for(IWorker worker : inputWorkers){
			if(type==null){
				worker.registerOutPipe(this);
			} else {
				worker.registerOutPipe(type,this);
			}
		}
		return this;
	}

	@Override
	public IPipe addOutputs(String type, IWorker... workers) {
		this.outputWorkers = workers;
		for(IWorker worker : outputWorkers){
			if(type==null){
				worker.registerInPipe(this);
			} else {
				worker.registerInPipe(type, this);
			}
		}
		return this;
	}

	@Override
	public void receiveEchoFromOutput(IWorker worker) {
		freeReceivers.release();
	}

	@Override
	public void init() {
		freeReceivers = new Semaphore(outputWorkers.length);
	}

	@Override
	public List<IWorker> getStandaloneWorkers() {
		List<IWorker> standaloneWorkers = new ArrayList<IWorker>();
		for(IWorker worker: inputWorkers){
			if(worker.isStandalone()){
				standaloneWorkers.add(worker);
			}
		}
		return standaloneWorkers;
	}

	@Override
	public List<IWorker> getEndWorkers() {
		List<IWorker> endWorkers = new ArrayList<IWorker>();
		Collections.addAll(endWorkers, outputWorkers);
		endWorkers.addAll(getStandaloneWorkers());
		return endWorkers;
	}

}
