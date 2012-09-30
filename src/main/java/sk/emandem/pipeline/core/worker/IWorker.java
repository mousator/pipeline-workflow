/**
 * 
 */
package sk.emandem.pipeline.core.worker;

import sk.emandem.pipeline.core.MultiThreadedExecutor;
import sk.emandem.pipeline.core.pipe.IPipe;


/**
 * @author Michal Antolik (antomi000)
 *
 */
public interface IWorker {

	public boolean receive(Object data, IPipe sender);
	
	public void registerInPipe(IPipe inPipe);
	public void registerOutPipe(IPipe outPipe);
	
	public void registerInPipe(String type, IPipe inPipe);
	public void registerOutPipe(String type, IPipe outPipe);
	
	public boolean isStandalone();
	
	public void open() throws Exception;
	public void close() throws Exception;
	
	public long getTotalProcessingTime();
	public void setExecutor(MultiThreadedExecutor executor);
}
