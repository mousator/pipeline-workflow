/**
 * 
 */
package sk.emandem.pipeline.core.pipe;

import java.util.List;

import sk.emandem.pipeline.core.worker.IWorker;


/**
 * @author Michal Antolik (michal@emandem.sk)
 *
 */
public interface IPipe {

	public boolean send(Object data);
	public IPipe addInputs(IWorker ... workers);
	public IPipe addOutputs(IWorker ... workers);
	public IPipe addInputs(String type, IWorker ... workers);
	public IPipe addOutputs(String type, IWorker ... workers);
	public void receiveEchoFromOutput(IWorker worker);
	public void init();
	public List<IWorker> getStandaloneWorkers();
	public List<IWorker> getEndWorkers();
}
