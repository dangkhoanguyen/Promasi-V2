/**
 *
 */
package org.promasi.multiplayer.server.clientstate;

import java.net.ProtocolException;

import org.apache.commons.lang.NullArgumentException;
import org.promasi.multiplayer.AbstractClientState;
import org.promasi.multiplayer.ProMaSiClient;
import org.promasi.multiplayer.game.Game;
import org.promasi.network.protocol.client.request.RequestBuilder;
import org.promasi.network.protocol.client.response.InternalErrorResponse;
import org.promasi.network.protocol.server.response.StartGameResponse;
import org.promasi.network.protocol.client.response.WrongProtocolResponse;


/**
 * @author m1cRo
 *
 */
public class WaitingGameClientState extends AbstractClientState {
	/**
	 *
	 */
	private Game _game;

	/**
	 *
	 * @param promasi
	 * @param game
	 * @throws NullArgumentException
	 */
	public WaitingGameClientState(Game game)throws NullArgumentException
	{
		if(game==null)
		{
			throw new NullArgumentException("Wrong argument game==null");
		}
		
		_game=game;
	}

	/* (non-Javadoc)
	 * @see org.promasi.server.client.state.IClientState#onReceive(org.promasi.server.core.ProMaSiClient, java.lang.String)
	 */
	@Override
	public void onReceive(ProMaSiClient client, String recData)throws  NullArgumentException {
		if(client==null)
		{
			throw new NullArgumentException("Wrong argument client==null");
		}

		if(recData==null)
		{
			throw new NullArgumentException("Wrong argument client==null");
		}
		try
		{
			Object object=RequestBuilder.buildRequest(recData);
			if(object instanceof StartGameResponse)
			{
				changeClientState(client,new PlayingGameClientState( _game ) );
			} 
			else
			{
				client.sendMessage(new WrongProtocolResponse().toProtocolString());
				client.disconnect();
			}
		}
		catch(ProtocolException e)
		{
			client.sendMessage(new WrongProtocolResponse().toProtocolString());
			client.disconnect();
		}
		catch(NullArgumentException e)
		{
			client.sendMessage(new InternalErrorResponse().toProtocolString());
			client.disconnect();
		}
		catch(IllegalArgumentException e)
		{
			client.sendMessage(new InternalErrorResponse().toProtocolString());
			client.disconnect();
		}
	}

}
