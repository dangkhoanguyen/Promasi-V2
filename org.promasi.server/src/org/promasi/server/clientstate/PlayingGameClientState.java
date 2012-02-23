package org.promasi.server.clientstate;

import java.beans.XMLDecoder;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.promasi.game.GameModelMemento;
import org.promasi.game.company.CompanyMemento;
import org.promasi.game.company.EmployeeMemento;
import org.promasi.game.company.EmployeeTaskMemento;
import org.promasi.game.company.MarketPlaceMemento;
import org.promasi.game.multiplayer.IMultiPlayerGame;
import org.promasi.game.multiplayer.IServerGameListener;
import org.promasi.game.multiplayer.MultiPlayerGame;
import org.promasi.game.project.ProjectMemento;
import org.promasi.protocol.client.AbstractClientState;
import org.promasi.protocol.client.ProMaSiClient;
import org.promasi.protocol.messages.AssignEmployeeTasksRequest;
import org.promasi.protocol.messages.DischargeEmployeeRequest;
import org.promasi.protocol.messages.EmployeeDischargedRequest;
import org.promasi.protocol.messages.EmployeeHiredRequest;
import org.promasi.protocol.messages.GameFinishedRequest;
import org.promasi.protocol.messages.GameStartedResponse;
import org.promasi.protocol.messages.HireEmployeeRequest;
import org.promasi.protocol.messages.InternalErrorResponse;
import org.promasi.protocol.messages.LeaveGameRequest;
import org.promasi.protocol.messages.LeaveGameResponse;
import org.promasi.protocol.messages.OnExecuteStepRequest;
import org.promasi.protocol.messages.OnTickRequest;
import org.promasi.protocol.messages.ProjectAssignedRequest;
import org.promasi.protocol.messages.ProjectFinishedRequest;
import org.promasi.protocol.messages.WrongProtocolResponse;
import org.promasi.server.ProMaSiServer;
import org.promasi.utilities.exceptions.NullArgumentException;
import org.promasi.utilities.serialization.SerializationException;

/**
 * 
 * @author m1cRo
 *
 */
public class PlayingGameClientState extends AbstractClientState implements IServerGameListener
{
	/**
	 * 
	 */
	private MultiPlayerGame _game;
	
	/**
	 * 
	 */
	private String _clientId;
	
	/**
	 * 
	 */
	private ProMaSiClient _client;
	
	/**
	 * 
	 */
	private ProMaSiServer _server;
	
	/**
	 * 
	 * @param clientId
	 * @param game
	 * @throws NullArgumentException
	 */
	public PlayingGameClientState(ProMaSiServer server, ProMaSiClient client, String clientId, MultiPlayerGame game)throws NullArgumentException{
		if(game==null){
			throw new NullArgumentException("Wrong argument game==null");
		}
		
		if(clientId==null){
			throw new NullArgumentException("Wrong argument clientId==null");
		}
		
		if(client==null){
			throw new NullArgumentException("Wrong argument client==null");
		}
		
		if(server==null){
			throw new NullArgumentException("Wrong argument server==null");
		}
		
		_client=client;
		_server=server;
		_game=game;
		_game.addListener(this);
		_clientId=clientId;
	}
	
	
	@Override
	public void onReceive(ProMaSiClient client, String recData) {
		try{
			Object object=new XMLDecoder(new ByteArrayInputStream(recData.getBytes())).readObject();
			if(object instanceof HireEmployeeRequest){
				HireEmployeeRequest request=(HireEmployeeRequest)object;
				if(request.getEmployeeId()==null){
					client.sendMessage(new WrongProtocolResponse().serialize());
					client.disconnect();
				}
				
				try{
					_game.hireEmployee(_clientId, request.getEmployeeId());
				}catch(IllegalArgumentException e){
					//Logger
				}
			}else if(object instanceof DischargeEmployeeRequest){
				DischargeEmployeeRequest request=(DischargeEmployeeRequest)object;
				if(request.getEmployeeId()==null){
					client.sendMessage(new WrongProtocolResponse().serialize());
					client.disconnect();
				}
				
				try{
					_game.dischargeEmployee(_clientId, request.getEmployeeId());
				}catch(IllegalArgumentException e){
					//Logger
				}
			}else if(object instanceof GameStartedResponse){
			}else if(object instanceof AssignEmployeeTasksRequest){
				AssignEmployeeTasksRequest request=(AssignEmployeeTasksRequest)object;
				if(request.getEmployeeId()==null || request.getTasks()==null){
					client.sendMessage(new WrongProtocolResponse().serialize());
					client.disconnect();
				}
				
				//ToDo if assign tasks failed response with valid message
				_game.assignTasks(_clientId, request.getEmployeeId(), request.getTasks());
			}else if(object instanceof LeaveGameRequest){
				_game.removeListener(this);
				changeClientState(_client, new ChooseGameClientState(_server, _clientId));
				client.sendMessage(new LeaveGameResponse().serialize());
			}else{
				client.sendMessage(new WrongProtocolResponse().serialize());
				client.disconnect();
			}
		}catch(IllegalArgumentException e){
			client.sendMessage(new InternalErrorResponse().serialize());
			client.disconnect();
		} catch (NullArgumentException e) {
			client.sendMessage(new InternalErrorResponse().serialize());
			client.disconnect();
		} catch (SerializationException e) {
			client.sendMessage(new InternalErrorResponse().serialize());
			client.disconnect();
		}
	}

	
	@Override
	public void onSetState(ProMaSiClient client) {
		// TODO Auto-generated method stub

	}

	
	@Override
	public void gameStarted(String clientId, IMultiPlayerGame game,
			GameModelMemento gameModel, DateTime dateTime) {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	public void projectAssigned(String clientId, IMultiPlayerGame game,
			CompanyMemento company, ProjectMemento project,
			DateTime dateTime) {
		if(clientId.equals(_clientId)){
			_client.sendMessage(new ProjectAssignedRequest(company, project, dateTime.toString()).serialize());
		}
	}

	
	@Override
	public void projectFinished(String clientId, IMultiPlayerGame game,
			CompanyMemento company, ProjectMemento project,
			DateTime dateTime) {
		if(clientId.equals(_clientId)){
			_client.sendMessage(new ProjectFinishedRequest(project).serialize());
		}
		
	}

	
	@Override
	public void employeeHired(String clientId, IMultiPlayerGame game,MarketPlaceMemento marketPlace, CompanyMemento company,EmployeeMemento employee, DateTime dateTime) {
		if(clientId.equals(_clientId)){
			_client.sendMessage(new EmployeeHiredRequest(marketPlace, company, employee, dateTime.toString()).serialize());
		}	
	}

	
	@Override
	public void employeeDischarged(String clientId, IMultiPlayerGame game,MarketPlaceMemento marketPlace, CompanyMemento company, EmployeeMemento employee, DateTime dateTime) {
		if(clientId.equals(_clientId)){
			_client.sendMessage(new EmployeeDischargedRequest(marketPlace, company, employee, dateTime.toString()).serialize());
		}	
	}

	
	@Override
	public void employeeTasksAssigned(String clientId, IMultiPlayerGame game,
			CompanyMemento company, EmployeeMemento employee,
			List<EmployeeTaskMemento> employeeTasks, DateTime dateTime) {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	public void employeeTaskDetached(String clientId, IMultiPlayerGame game,
			MarketPlaceMemento marketPlace, CompanyMemento company,
			EmployeeMemento employee,
			EmployeeTaskMemento employeeTask, DateTime dateTime) {}

	
	@Override
	public void companyIsInsolvent(String clientId, IMultiPlayerGame game,CompanyMemento company, DateTime dateTime) {}
	
	@Override
	public void onExecuteStep( String playerId, IMultiPlayerGame game, CompanyMemento company, DateTime dateTime){}
	
	@Override
	public void onExecuteWorkingStep(String clientId, IMultiPlayerGame game,
			CompanyMemento company, ProjectMemento assignedProject, DateTime dateTime) {
		if(clientId.equals(_clientId)){
			_client.sendMessage(new OnExecuteStepRequest(assignedProject, company, dateTime.toString()).serialize());
		}
	}

	
	@Override
	public void onTick(String clientId, IMultiPlayerGame game, DateTime dateTime) {
		if(clientId.equals(_clientId)){
			_client.sendMessage(new OnTickRequest(dateTime.toString()).serialize());
		}
	}

	
	@Override
	public void messageSent(String clientId, IMultiPlayerGame game, String message) {}

	
	@Override
	public void onDisconnect(ProMaSiClient client) {
		_game.leaveGame(_clientId);
	}

	
	@Override
	public void onConnect(ProMaSiClient client) {}

	
	@Override
	public void onConnectionError(ProMaSiClient client) {
		_game.leaveGame(_clientId);
	}

	
	@Override
	public void playersListUpdated( IMultiPlayerGame game, List<String> gamePlayers) {}

	
	@Override
	public void gameFinished(Map<String, GameModelMemento> gameModels) {
		if(gameModels.containsKey(_clientId)){
			GameModelMemento gameModel=gameModels.get(_clientId);
			Map<String, GameModelMemento> models=new TreeMap<String, GameModelMemento>(gameModels);
			models.remove(_clientId);
			GameFinishedRequest request=new GameFinishedRequest(_clientId, gameModel, models);
			_client.sendMessage(request.serialize());
		}
	}

}
