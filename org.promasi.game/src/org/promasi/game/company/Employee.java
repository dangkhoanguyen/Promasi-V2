package org.promasi.game.company;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.promasi.game.GameException;
import org.promasi.utilities.exceptions.NullArgumentException;
import org.promasi.utilities.serialization.SerializationException;


/**
 * 
 * Represents an employee.(Developer,Tester,Designer etc). All the fields of the
 * employee(experienced etc) have a range of 0.0-10.0
 * 
 * @author m1cRo
 * 
 */
public class Employee 
{	
	/**
     * The name of the person.
     */
    protected String _firstName;

    /**
     * The last name of the person.
     */
    protected String _lastName;

    /**
     * 
     */
    protected String _employeeId;
    
	/**
     * The salary of the employee.
     */
    protected double _salary;

    
    /**
     * The CV of the employee.
     */
    protected String _curriculumVitae;
    
    /**
     * 
     */
    protected Map<String, Double> _employeeSkills;
    
	/**
	 * 
	 */
	private List<IEmployeeListener> _listeners;
    
    /**
     * 
     */
    protected Map<Integer ,EmployeeTask> _employeeTasks;
    
    /**
     * 
     */
    private String _supervisor;
    
    /**
     * 
     */
    private Lock _lockObject;
    
    /**
     * Initializes the object.
     */
    public Employee(String firstName, String lastName, String employeeId, String curriculumVitae, double salary,Map<String, Double> employeeSkills)throws GameException
   {
    	
    	if(firstName==null){
    		throw new GameException("Wrong argument firstName==null");
    	}
    	
        if(lastName==null){
        	throw new GameException("Wrong argument lastName==null");
        }
        
        if(employeeId==null){
        	throw new GameException("Wrong argument employeeId==null");
        }
        
        if(curriculumVitae==null){
        	throw new GameException("Wrong argument curriculumVitae==null");
        }

        if(employeeSkills==null){
        	throw new GameException("Wrong argument employeeSkills==null");
        }
        
        for(Map.Entry<String, Double> entry : employeeSkills.entrySet()){
        	if(entry.getKey()==null || entry.getValue()==null){
        		throw new IllegalArgumentException("Wrong argument employeeSkills contains null");
        	}
        }
        
        _firstName=firstName;
        _lastName=lastName;
        _employeeId=employeeId;
        _curriculumVitae=curriculumVitae;
        _salary=salary;
        _employeeSkills=employeeSkills;
        _lockObject = new ReentrantLock();
        _employeeTasks=new TreeMap<Integer, EmployeeTask>();
        _listeners = new LinkedList<IEmployeeListener>();
    }

    /**
     * @return The {@link #_salary}.
     */
    public double getSalary ( ){
        return _salary;
    }

    /**
     * @return The {@link #_curriculumVitae}.
     */
    public String getCurriculumVitae ( ){
        return _curriculumVitae;
    }
    
    /**
     * 
     * @return
     */
    public String getFirstName(){
    	return _firstName;
    }
    
    /**
     * 
     * @return
     */
    public String getLastName(){
    	return _lastName;
    }
    
    /**
     * 
     * @return
     */
    public String getEmployeeId(){
    	return _employeeId;
    }
    
    /**
     * 
     * @return
     */
    public String getSupervisor(){
    	try{
    		_lockObject.lock();
    		return _supervisor;
    	}finally{
    		_lockObject.unlock();
    	}
    }
    
    /**
     * 
     * @param supervisor
     */
    public void setSupervisor( String supervisor ){
    	try{
    		_lockObject.lock();
    		_supervisor = supervisor;
    	}finally{
    		_lockObject.unlock();
    	}
    }
    
	/**
	 * 
	 * @return
	 * @throws SerializationException
	 */
	public EmployeeMemento getSerializableEmployee()throws SerializationException{
		return new EmployeeMemento(this);
	}

    /**
     * 
     * @param employeeTask
     * @return
     * @throws SerializationException 
     */
    public boolean assignTasks(List<EmployeeTask> employeeTasks){
    	boolean result = false;
    	
    	try{
    		_lockObject.lock();
        	if(employeeTasks!=null){
            	for(EmployeeTask task : employeeTasks){
                	for(Map.Entry<Integer , EmployeeTask> entry: _employeeTasks.entrySet()){
                		if( entry.getValue().conflictsWithTask(task) ){
                			return false;
                		}
                	}
            	}
            	
            	List<EmployeeTask> tmp=new LinkedList<EmployeeTask>(employeeTasks);
            	for(EmployeeTask tmpTask : tmp){
            		for(EmployeeTask employeeTask : employeeTasks){
            			if(tmpTask!=employeeTask && tmpTask.conflictsWithTask(employeeTask)){
            				return false;
            			}
            		}
            	}

            	List<EmployeeTaskMemento> serializableTasks=new LinkedList<EmployeeTaskMemento>();
            	for(EmployeeTask task : employeeTasks){
            		_employeeTasks.put(task.getFirstStep(), task);
            		serializableTasks.add( task.getSerializableEmployeeTask() );
            	}
        		
            	for( IEmployeeListener listener : _listeners ){
            		listener.taskAttached(_supervisor, getSerializableEmployee(), serializableTasks);
            	}
            	
            	result = true;
        	}
    	}catch( SerializationException e){
    		result = false;
    	}finally{
    		_lockObject.unlock();
    	}
	
		return result;
    }
    
    /**
     * 
     * @return
     */
    public boolean removeAllTasks(){
    	boolean result = false;
    	try{
    		_lockObject.lock();
        	for(Map.Entry<Integer , EmployeeTask> entry : _employeeTasks.entrySet()){
        		result = true;
    			for ( IEmployeeListener listener : _listeners ){
    				listener.taskDetached(_supervisor, getSerializableEmployee(), entry.getValue().getSerializableEmployeeTask());
    			}
        	}
        	
        	_employeeTasks.clear();
        	result = true;
    	}catch (SerializationException e){
    		result = false;
    	}finally{
    		_lockObject.unlock();
    	}

    	return result;
    }
    
    /**
     * 
     * @param employeeTask
     * @return
     * @throws NullArgumentException
     * @throws SerializationException 
     */
    public boolean removeEmployeeTask(EmployeeTask employeeTask){
    	boolean result = false;
    	try{
    		_lockObject.lock();
        	if(employeeTask!=null){
            	if(_employeeTasks.containsKey(employeeTask.getFirstStep() ) ){
            		EmployeeTask task=_employeeTasks.get(employeeTask.getFirstStep() );
            		if(task==employeeTask){
            			_employeeTasks.remove(task.getFirstStep());
            			result = true;
            			for ( IEmployeeListener listener : _listeners ){
            				listener.taskDetached(_supervisor, getSerializableEmployee(), task.getSerializableEmployeeTask());
            			}
            		}
            	}
        	}
    	}catch(SerializationException e){
    		result = false;
    	}finally{
    		_lockObject.unlock();
    	}
    	
    	return result;
    }
    
    /**
     * 
     * @return
     */
    public boolean  executeTasks(int currentStep){
    	boolean result = false;
    	
    	try{
    		_lockObject.lock();
        	if(!_employeeTasks.isEmpty()){
            	Map<Integer, EmployeeTask> employeeTasks=new TreeMap<Integer, EmployeeTask> ();
            	for(Map.Entry<Integer ,EmployeeTask> entry: _employeeTasks.entrySet()){
            		try{
            			entry.getValue().executeTask(_employeeSkills, currentStep);
            			if(entry.getValue().isValid(currentStep)){
            				employeeTasks.put(entry.getKey(), entry.getValue());
            			}
            			
            			result = true;
            		}catch(NullArgumentException e){
            			return false;
            		}
            	}
            	
            	_employeeTasks=employeeTasks;
        	}
        	
    	}finally{
    		_lockObject.unlock();
    	}

    	return result;
    }

    /**
     * 
     * @param listener
     * @return
     */
    public boolean addListener(IEmployeeListener listener){
    	boolean result = false;
    	
    	try{
    		_lockObject.lock();
    		if( !_listeners.contains(listener) ){
    			result = _listeners.add(listener);
    		}
    	}finally{
    		_lockObject.unlock();
    	}
    	
    	return result;
    }
    

    /**
     * 
     * @param listener
     * @return
     */
    public boolean removeListener(IEmployeeListener listener){
    	boolean result = false;
    	try{
    		_lockObject.lock();
    		if( _listeners.contains(listener) ){
    			result = _listeners.remove(listener);
    		}
    	}finally{
    		_lockObject.unlock();
    	}
    	
    	return result;
    }
    
    /**
     * 
     */
    public void removeListeners(){
    	try{
    		_lockObject.lock();
    		_listeners.clear();
    	}finally{
    		_lockObject.unlock();
    	}
    }
}