package org.promasi.sdsystem;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.promasi.sdsystem.sdobject.FlowSdObject;
import org.promasi.sdsystem.sdobject.ISdObject;
import org.promasi.sdsystem.sdobject.InputSdObject;
import org.promasi.sdsystem.sdobject.OutputSdObject;
import org.promasi.sdsystem.sdobject.StockSdObject;
import org.promasi.sdsystem.sdobject.TimeSdObject;
import org.promasi.utilities.exceptions.NullArgumentException;
import org.promasi.utilities.serialization.SerializationException;

/**
 * 
 * @author m1cRo
 *
 */
public class SdSystem
{
	/**
	 * 
	 */
	public static final String CONST_TIME_SDOBJECT_NAME="time";
	
	/**
	 * 
	 */
	public static final Pattern CONST_ALPHABET_PATTERN = Pattern.compile("[A-Z0-9a-z]+");
	
	/**
	 * 
	 */
	protected Map<String, ISdObject> _sdObjects;
	
	/**
	 * 
	 */
	private Lock _lockObject;
	
	/**
	 * 
	 * @param _sdObjects
	 * @throws NullArgumentException
	 * @throws IllegalArgumentException
	 */
	public SdSystem( final Map<String, ISdObject> sdObjects)throws SdSystemException{
		if(sdObjects==null){
			throw new SdSystemException("Wrong argument sdObjects==null");
		}
		
		if(sdObjects.isEmpty()){
			throw new SdSystemException("Wrong argument sdObjects is empty");
		}
		
		if(sdObjects.containsKey(CONST_TIME_SDOBJECT_NAME)){
			throw new SdSystemException("Wrong argument sdObjects contains time");
		}
		
		for(Map.Entry<String, ISdObject> entry : sdObjects.entrySet()){
			if(entry.getKey()==null || entry.getValue()==null){
				throw new SdSystemException("Wrong argument sdObjects contains null");
			}
			
			Matcher matcher=CONST_ALPHABET_PATTERN.matcher(entry.getKey());
			if(!matcher.matches()){
				throw new SdSystemException("Wrong argument sdObjects contains invalid objectId");
			}
		}
		
		Map<String, ISdObject> systemObjects=new TreeMap<String, ISdObject>(sdObjects);
		systemObjects.put(CONST_TIME_SDOBJECT_NAME, new TimeSdObject());
		
		for(Map.Entry<String, ISdObject> entry : systemObjects.entrySet()){
			if(!entry.getValue().executeStep(systemObjects)){
				throw new SdSystemException("Wrong argument sdObjects invalid system");
			}
		}
		
		_sdObjects=systemObjects;
		
		for(Map.Entry<String, ISdObject> entry : _sdObjects.entrySet()){
			if(!entry.getValue().executeStep(_sdObjects)){
				throw new SdSystemException("Wrong argument sdObjects invalid system step execution failed");
			}
		}
		
		_lockObject = new ReentrantLock();
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean executeStep(){
		boolean result = true;
		
		try{
			_lockObject.lock();
			for(Map.Entry<String, ISdObject> entry : _sdObjects.entrySet()){
				if( entry.getValue() instanceof FlowSdObject){
					result &= entry.getValue().executeStep(_sdObjects);
				}
			}
			
			for(Map.Entry<String, ISdObject> entry : _sdObjects.entrySet()){
				if(entry.getValue() instanceof StockSdObject ){
					result &= entry.getValue().executeStep(_sdObjects);
				}
			}
			
			for(Map.Entry<String, ISdObject> entry : _sdObjects.entrySet()){
				if(entry.getValue() instanceof OutputSdObject ){
					result &= entry.getValue().executeStep(_sdObjects);
				}
			}
			
			for(Map.Entry<String, ISdObject> entry : _sdObjects.entrySet()){
				System.out.print(String.format("%S, %.2f\n", entry.getKey(), entry.getValue().getValue()));
			}
			
			ISdObject time=_sdObjects.get(CONST_TIME_SDOBJECT_NAME);
			result &= time.executeStep(_sdObjects);
		}finally{
			_lockObject.unlock();
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param inputName
	 * @param value
	 * @return
	 */
	public boolean setInput(final String inputName, final Double value){
		boolean result = false;
		
		try{
			_lockObject.lock();
			if( inputName != null || value != null)
			{
				if(_sdObjects.containsKey(inputName)){
					ISdObject sdObject=_sdObjects.get(inputName);
					if( sdObject instanceof InputSdObject ){
						InputSdObject inputSdObject=(InputSdObject)sdObject;
						result = inputSdObject.setValue(value);
					}
				}
			}
		}finally{
			_lockObject.unlock();
		}

		return result;
	}
	
	/**
	 * 
	 * @param sdObjectName
	 * @return
	 * @throws NullArgumentException
	 * @throws IllegalArgumentException
	 */
	public Double getValue(final String sdObjectName)throws SdSystemException{
		if(sdObjectName==null){
			throw new SdSystemException("Wrong argument sdObjectName==null");
		}
		
		if(!_sdObjects.containsKey(sdObjectName)){
			throw new SdSystemException("Wrong argument sdObjectName no in sdObjects list");
		}
		
		try{
			_lockObject.lock();
			return _sdObjects.get(sdObjectName).getValue();
		}finally{
			_lockObject.unlock();
		}
	}
	
	/**
	 * 
	 * @return
	 * @throws SerializationException
	 */
	public SdSystemMemento getMemento() {
		return new SdSystemMemento(this);
	}
	
	/**
	 * 
	 * @param inputId
	 * @return
	 * @throws NullArgumentException
	 */
	protected InputSdObject getInput(String inputId)throws SdSystemException{
		if(inputId==null){
			throw new SdSystemException("Wrong argument inputId==null");
		}
		
		if(!_sdObjects.containsKey(inputId)){
			throw new SdSystemException("Wrong argument inputId");
		}
		
		ISdObject sdObject=_sdObjects.get(inputId);
		if(sdObject instanceof InputSdObject){
			return (InputSdObject)sdObject;
		}else{
			throw new SdSystemException("Wrong argument inputId");
		}
	}

	/**
	 * 
	 * @param outputId
	 * @return
	 * @throws NullArgumentException
	 * @throws IllegalArgumentException
	 */
	protected OutputSdObject getOutputSdObject(String outputId)throws SdSystemException{
		if(outputId==null){
			throw new SdSystemException("Wrong argument outputId==null");
		}
		
		if(!_sdObjects.containsKey(outputId)){
			throw new SdSystemException("Wrong argument outputId");
		}
		
		ISdObject sdObject=_sdObjects.get(outputId);
		if(sdObject instanceof OutputSdObject){
			return (OutputSdObject)sdObject;
		}else{
			throw new SdSystemException("Wrong argument outputId");
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public List<String> getOutputs() {
		List<String> outputs=new Vector<String>();
		for(Map.Entry<String, ISdObject> entry : _sdObjects.entrySet()){
			if(entry.getValue() instanceof OutputSdObject){
				outputs.add(entry.getKey());
			}
		}
		
		return outputs;
	}
	
	/**
	 * 
	 * @return
	 */
	public Map< String, Double > getSystemValues(){
		Map<String, Double> values=new TreeMap<String, Double>();
		for(Map.Entry<String, ISdObject> entry : _sdObjects.entrySet()){
			values.put(entry.getKey(), entry.getValue().getValue());
		}
		
		return values;
	}

	/**
	 * 
	 * @return
	 */
	public List<String> getInputs() {
		List<String> inputs=new Vector<String>();
		for(Map.Entry<String, ISdObject> entry : _sdObjects.entrySet()){
			if(entry.getValue() instanceof InputSdObject){
				inputs.add(entry.getKey());
			}
		}
		
		return inputs;
	}

	/**
	 * 
	 * @param inputName
	 * @return
	 * @throws NullArgumentException
	 */
	public boolean hasInput(String inputName) throws SdSystemException {
		if(inputName==null){
			throw new SdSystemException("Wrong argument inputName==null");
		}
		
		if(!_sdObjects.containsKey(inputName)){
			return false;
		}
		
		if(_sdObjects.get(inputName) instanceof InputSdObject){
			return true;
		}
		
		return false;
	}

	/**
	 * 
	 * @param outputName
	 * @return
	 * @throws NullArgumentException
	 */
	public boolean hasOutput(String outputName) throws SdSystemException {
		if(outputName==null){
			throw new SdSystemException("Wrong argument outputName==null");
		}
		
		if(!_sdObjects.containsKey(outputName)){
			return false;
		}
		
		if(_sdObjects.get(outputName) instanceof OutputSdObject){
			return true;
		}
		
		return false;
	}
}