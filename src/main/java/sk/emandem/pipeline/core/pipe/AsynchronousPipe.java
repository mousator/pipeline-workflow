/**
 * 
 */
package sk.emandem.pipeline.core.pipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.emandem.pipeline.core.worker.IWorker;


/**
 * @author Michal Antolik (michal@emandem.sk)
 *
 */
public class AsynchronousPipe implements IPipe {

	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private IWorker[] inputWorkers;
	private IWorker[] outputWorkers;
	
	private final int capacity;
	
	private Semaphore freeReceivers;
	private final Queue<Object> dataAsyncBuffer;
	private Lock bufferAccessLock = new ReentrantLock();
	private Condition bufferFull  = bufferAccessLock.newCondition(); 

	public AsynchronousPipe(int capacity) {
		this.capacity = capacity;
		this.dataAsyncBuffer = new ArrayDeque<Object>(capacity);
	}
	
	public boolean send(Object data) {
		bufferAccessLock.lock();
		try {
			if(freeReceivers.tryAcquire()){
				//free receiver => send data immediately
				boolean sent = trySendData(data);
				if(!sent){
					logger.error("Data not sent!");
				}
				return sent;
			} else {
				//we need to cache data
				if(dataAsyncBuffer.size()==capacity){
					//block current thread
					bufferFull.await();
				}
				//insert data to buffer
				dataAsyncBuffer.add(data);
				return true;
			}
		} catch (InterruptedException ie){
			ie.printStackTrace();
		} finally {
			bufferAccessLock.unlock();
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
		Object newData = null;
		bufferAccessLock.lock();
		try {
			if(dataAsyncBuffer.size() > 0){
				//we have data waiting to be send out, take first data
				// and send it to worker which is free (current one)
				newData = dataAsyncBuffer.poll();
				if(dataAsyncBuffer.size() == capacity-1){
					//was full before = (capacity - 1)
					bufferFull.signal(); // wait thread which wanted to put data to buffer
				}
			}
		} finally {
			bufferAccessLock.unlock();
		}
		if(newData!=null){
			worker.receive(newData, this);
		} else {
			freeReceivers.release();
		}
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

	/**
	 * @return the capacity
	 */
	public int getCapacity() {
		return capacity;
	}

	@Override
	public List<IWorker> getEndWorkers() {
		List<IWorker> endWorkers = new ArrayList<IWorker>();
		Collections.addAll(endWorkers, outputWorkers);
		endWorkers.addAll(getStandaloneWorkers());
		return endWorkers;
	}

}
