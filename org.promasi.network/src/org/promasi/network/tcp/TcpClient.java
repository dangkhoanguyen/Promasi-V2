/**
 *
 */
package org.promasi.network.tcp;
import java.net.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.IllegalArgumentException;

import org.promasi.utilities.exceptions.NullArgumentException;

/**
 * @author m1cRo
 * TCPClient class.
 *
 */
public class TcpClient
{
	/**
	 *
	 */
	private Socket _clientSocket;

	/**
	 *
	 */
	private boolean _isConnected;

	/**
	 *
	 */
	private Thread _recvThread;
	
	/**
	 *
	 */
	private BufferedReader _socketStreamReader;
	
	/**
	 *
	 */
	private BufferedWriter _socketStreamWriter;
	
	/**
	 * 
	 */
	private List<ITcpClientListener> _listeners;
	
	/**
	 * 
	 */
	private Lock _lockObject;
	
	/**
	 * 
	 * @param hostname
	 * @param portNumber
	 * @throws IllegalArgumentException
	 * @throws IOException
	 * @throws NetworkException 
	 */
	public TcpClient(String hostname,int portNumber)throws NetworkException{
		if(hostname==null){
			throw new NetworkException("Wrong argument hostname==null");
		}
		
		if(portNumber<=0){
			throw new NetworkException("Wrong argument portNumber<=0");
		}
		
		try {
			_clientSocket=new Socket(hostname,portNumber);
			_socketStreamReader=new BufferedReader(new InputStreamReader(_clientSocket.getInputStream()));
			_socketStreamWriter=new BufferedWriter(new OutputStreamWriter(_clientSocket.getOutputStream()));
			_isConnected=true;
			_listeners=new LinkedList<ITcpClientListener>();
			_lockObject=new ReentrantLock();
			
			_recvThread=new Thread(new Runnable() {
				
				@Override
				public void run() {
					connectionLoop();
					_isConnected=false;
				}
			});
			
		} catch (UnknownHostException e) {
			throw new NetworkException(e.toString());
		} catch (IOException e) {
			throw new NetworkException(e.toString());
		}

		_recvThread.start();
	}
	
	/**
	 *
	 * @param socket
	 * @throws IllegalArgumentException
	 * @throws IOException
	 * @throws NetworkException 
	 */
	public TcpClient(Socket socket) throws NetworkException
	{
		if(socket==null){
			throw new NetworkException("Wrong socket==null");
		}
		
		_clientSocket=socket;
		try {
			_socketStreamReader=new BufferedReader(new InputStreamReader(_clientSocket.getInputStream()));
			_socketStreamWriter=new BufferedWriter(new OutputStreamWriter(_clientSocket.getOutputStream()));
			_isConnected=true;
			_listeners=new LinkedList<ITcpClientListener>();
			_lockObject=new ReentrantLock();
			
			_recvThread=new Thread(new Runnable() {
				
				@Override
				public void run() {
					connectionLoop();
					_isConnected=false;
				}
			});
			
			_recvThread.start();
		} catch (IOException e) {
			throw new NetworkException(e.toString());
		}

	}
	
	/**
	 * 
	 */
	private void connectionLoop(){
		try
		{
			try{
				_lockObject.lock();
				for(ITcpClientListener listener : _listeners){
					listener.onConnect();
				}
			}finally{
				_lockObject.unlock();
			}

			String line=null;
			do{
				line=_socketStreamReader.readLine();
				if( line != null){
					try{
						_lockObject.lock();
						for(ITcpClientListener listener : _listeners){
							listener.onReceive(line);
						}	
					}finally{
						_lockObject.unlock();
					}
				}
			}while(line!=null);
			
			_socketStreamReader.close();
			_socketStreamWriter.close();
			_clientSocket.shutdownInput();
			_clientSocket.shutdownOutput();
			_clientSocket.close();
			
			try{
				_lockObject.lock();
				for(ITcpClientListener listener : _listeners){
					listener.onDisconnect();
				}
			}finally{
				_lockObject.unlock();
			}
		}catch(IOException e){
			synchronized(_lockObject){
				for(ITcpClientListener listener : _listeners){
					listener.onConnectionError();
				}
			}
		}
	}

	/**
	 *
	 * @return
	 */
	public boolean isConnected()
	{
		return _isConnected;
	}

	/**
	 *
	 * @param tcpEventHandler
	 */
	public boolean addListener(ITcpClientListener listener)throws NetworkException
	{
		if(listener==null){
			throw new NetworkException("Wrong argument listener==null");
		}
		
		try{
			_lockObject.lock();
			if(_listeners.contains(listener)){
				return false;
			}else{
				_listeners.add(listener);
				return true;
			}
		}finally{
			_lockObject.unlock();
		}
	}
	
	/**
	 * 
	 * @param listener
	 * @return
	 * @throws NullArgumentException
	 */
	public boolean removeListener(ITcpClientListener listener)throws NullArgumentException
	{
		if(listener==null){
			throw new NullArgumentException("Wrong argument listener==null");
		}
		
		try{
			_lockObject.lock();
			if(_listeners.contains(listener)){
				_listeners.remove(listener);
				return true;
			}
		}finally{
			_lockObject.unlock();
		}
	
		return false;
	}

	/**
	 *
	 * @return
	 */
	public boolean disconnect()
	{
		try{
			_lockObject.lock();
			_socketStreamReader.close();
			_socketStreamWriter.close();
			_clientSocket.shutdownInput();
			_clientSocket.shutdownOutput();
			_clientSocket.close();
			_isConnected=false;
		}catch(IOException e){
			_isConnected=false;
			return false;
		}finally{
			_lockObject.unlock();
		}
			
		return true;
	}

	/**
	 *
	 * @param message
	 * @return
	 */
	public boolean sendMessage(String message){
		if(message==null){
			return false;
		}

		try{
			_lockObject.lock();
			_socketStreamWriter.write(message,0,message.length());
			_socketStreamWriter.flush();
		}catch(IOException e){
			return false;
		}finally{
			_lockObject.unlock();
		}
		
		return true;
	}
}