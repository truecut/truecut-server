/*
 * ====================================================================
 *
 * Copyright 2008 (c) Daims.co.kr.  All rights reserved.
 *
 */
package truecut.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;


/**
 * java.nio.channels.ServerSocketChannel �?java.nio.channels.SocketChannel???�해 non-blocking 모드�??�켓??처리?�기 ?�한 ?�래??br>
 * java.nio.channels.Selector�??�용?�여, ?�결 ?�청 ?�신 �??�이???�신 처리�??�다.<br>
 * <p>
 * 만약 10000 ?�트�?listen?�고, ?�킷??추출?�여 ?�더 �??�일 ?�의 ?�이??�� 출력?�고 ?�다�??�음�?같이 ?�용?�다.
 * <pre>
 *		// 1024byte??초기 ?�량??�????최�? 10개의 MessageBuffer�?�????Pool ?�성, 최�? 10�??�상 ?�어갈경?? MessageBuffer ??증�??�킴
 *		MessageBufferPool pool = new MessageBufferPool(1024, 10, MessageBufferPool.WHEN_EXHAUSTED_GROW);
 *		// 고정???�더로�????�킷??구분?�여 추출?�기 ?�한 FixedLengthProtocolHandler ?�스?�스 ?�성,
 *		FixedLengthProtocolHandler listener = new FixedLengthProtocolHandler();
 *		SingleSelectorServer svr_obj = new SingleSelectorServer(pool);
 *		svr_obj.addInputListener(listener);
 *		svr_obj.initServer(10000);		// 10000 ?�트�?listen
 *		svr_obj.runServer();
 * </pre>
 *
 * @see #InputListener InputListener
 * @see #FixedLengthProtocolHandler FixedLengthProtocolHandler
 * @see #StartEndProtocolHandler StartEndProtocolHandler
 * @see @MessageBufferPool MessageBufferPool
 *
 * @author ?�갈 ??
 * @version $Date: 2010/04/08 09:25:31 $ 
 */
public class SingleSelectorServer implements Runnable {

	private int portNo;		// ?�버 listen ?�트
	
	private Selector selector;

	private ServerSocketChannel serverSocketChannel;

	/** ?�결???�라?�언??리스??*/
	private Vector<SocketChannel> connectionList = null;		// element: SocketChannel

	/** ?�결???�라?�언??리스??�?multicast �?받을 리스??*/
	private Vector<SocketChannel> multicastList = null;	
		
	/** ?�라?�언?��? MessageBuffer??매핑 */
	private HashMap<SocketChannel, MessageBuffer> connectionToBuffer = null;
	
	/** 
	    ?�켓 ?�결???�인?�기 ?�한 메시�?? ?�송?�는 버퍼 
	    주기?�으�?echo ?�청 ?�킷???�송??경우????��?�여 코드?�에 만들?�으??
	    ?�용?��? ?�음 
	 */
	private ByteBuffer dummyBuffer = null;
	
	/** 브로??�?��?�용 버퍼 */
	private ByteBuffer broadcastBuffer = null;	
	

	/** client ?�서 ?�어?�는 ?�이?��? ?�고 ?�는 buffer??*/
	private MessageBufferPool messageBufferPool;	// �?SocketChannel�?MessageBuffer�??�당?�기 ?�한 MessageBufferPool

	/**
	 * ?�이???�신 ?�는 ?�결 종료??InputListener??action 메소?��? ?�출??
	 * (Application�?action??처리?�기 ?�한 ?�터?�이?�로, Application�?InputListener�?구현) 
	 */
	private InputListener<SocketChannel> inputListener;

	/**
	 * false ??경우, ?�이???�신 발생??InputListener??action 메소???�출, true??경우, ?�켓 ?�결 종료 ??action 메소???�출
	 */
	private boolean callActionWhenSocketClosed = false;

	/**
	 * timeout, timeout(msec) ?�안 ?�킷???�신?��? ?�으�??�켓???�절?�다. 0?�경?�엔 체크?��? ?�음 
	 */
	private long timeoutMsec = 0;
	
	/**
	 * logger
	 */ 		
	private static Logger logger = Logger.getLogger("root");

	
	/**
	 * ?�성??
	 * <p>
	 * @param _pool		�?SocketChannel�?MessageBuffer�??�당?�기 ?�한 MessageBufferPool ?�스?�스
	 */		
	public SingleSelectorServer(MessageBufferPool _pool) {
		this.messageBufferPool = _pool;

		connectionList = new Vector<SocketChannel>();
		multicastList = new Vector<SocketChannel>();
		connectionToBuffer = new HashMap<SocketChannel, MessageBuffer>();
		broadcastBuffer = ByteBuffer.allocateDirect ( 4096 );
	}

	/**
	 * ?�이???�신 ?�는 ?�결 종료???�신???�이?��? 처리??InputListener ?�록
	 */
	public void addInputListener ( InputListener<SocketChannel> _listener ) {
		this.inputListener = _listener;
	}
	
	/**
	 * ?�이???�신?�마??InputListener??action 메소?��? ?�출??것인�?
	 * ?�켓 ?�결 종료??action 메소?��? ?�출??것인�?�?��
	 *
	 * @param _flag	false: ?�이???�신??default), true: ?�결 종류??
	 */
	public void setCallActionWhenSocketClosed(boolean _flag) {
		callActionWhenSocketClosed = _flag;
	}
	
	/**
	 * timeout
	 */
	public long getTimeoutMsec() {
		return this.timeoutMsec;
	}
	
	public void setTimeoutMsec(long _timeout) {
		this.timeoutMsec = _timeout;
	}
	
	/**
	 * @see dummyBuffer
	 */
	public void setDummyBuffer ( byte[] _ba ) {
		dummyBuffer = ByteBuffer.allocateDirect ( _ba.length );
		dummyBuffer.put ( _ba );
	}
	
	
	public void initServer(int _port) {
		initServer(null, _port);
	}
	
	/**
	 * Selector�??�성?�고, ServerSocketChannel???�후, ?�라미터�?받�? ?�트�?bind ?�키�? Selector�??�록?�다.
	 * <p>
	 * @param _host		?�버 주소 : null ??경우, InetAddress.getLocalHost() ?�용, ?�터?�이??카드�?2�??�을 경우 0.0.0.0 ?�용?�다. 	  
	 * @param _port		?�버 bind port
	 */	
	public void initServer(String _host, int _port) {

		portNo = _port;
		
		try {
			
			logger.info ( "[SingleSelectorServer] Server Init(" + _host + ":" + this.portNo + ") ");

			// Selector for incoming requests
			this.selector = SelectorProvider.provider().openSelector();	// null 발생??경우�??�음 -_-

			// Create a new non-blocking ServerSocketChannel, bind it to port, and register it with the Selector
			this.serverSocketChannel = ServerSocketChannel.open();
			this.serverSocketChannel.configureBlocking(false);
			// Bind the server socket to the local host and port
			if ( _host==null ) {
				this.serverSocketChannel.socket().bind ( new InetSocketAddress(this.portNo) );
			} else {
				this.serverSocketChannel.socket().bind ( new InetSocketAddress(_host, this.portNo) );
			}
		

			// ServerSocketChannel??Selector??OP_ACCEPT�??�록
			// OP_ACCEPT�??�록?�여 ServerSocketChannel�??�청??Selection Key�?OP_ACCEPT 값을 갖게?�어 key.isAcceptable() 조건문으�?구별?�여 처리
			this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch(IOException ex) {
			logger.error ( "[SingleSelectorServer] " + this.portNo + "::::Server Init Fail : " + ex.getMessage() );
			ex.printStackTrace();
		}

	}
	
	/**
	 * Runnable ?�터?�이?�의 run method 구현
	 */
	public void run() {
		try {
			runServer();
		} catch ( Exception ex ) {
			logger.error ( "Running Error: " + ex.getMessage() );
			ex.printStackTrace();
		}
	}

	/**
	 * 루프�??�면??Selector??select메소?��? ?�출?�여, ?�결 ?�청???�거???�이???�신??발생??경우 처리<br>
	 * ?�결 ?�청???�을 경우, ?�결???�락?�고 {@link #MessageBufferPool}?�서 {$link @MessageBuffer}�??�어???�결(SocketChannel)??버퍼???�께 Selector�??�록?�다.<br>
	 * ?�이???�신??발생??경우,  callActionWhenSocketClosed 값이 false??경우 InputListener??action 메소?��? ?�출?�다.<br>
	 * ?�결??종료??경우, callActionWhenSocketClosed 값이 true??경우  InputListener??action 메소?��? ?�출?�다.<br>
	 * <p>
	 */
	public void runServer() throws IOException, ClosedChannelException {

		int keys_num = 0;
		int read_len = -1;
		
		while (true) {

			//  It returns only after at least one channel is selected, 
			// this selector's wakeup method is invoked, or the current thread is interrupted, whichever comes first
			keys_num = this.selector.select(10*1000);
			
			if ( keys_num<=0 ) {
				continue;
			}
			
			for ( Iterator<SelectionKey> it = this.selector.selectedKeys().iterator(); it.hasNext(); ) {
				SelectionKey sk = it.next();

				it.remove();
				
				if ( !sk.isValid() )	continue;
				
				if ( sk.isReadable() ) {		// receive data
					SocketChannel socket_channel = (SocketChannel)sk.channel();

					MessageBuffer buffer = (MessageBuffer)sk.attachment();
					
					if ( buffer==null ) {
						logger.error ( "[SingleSelectorServer] " + this.portNo + "::::buffer is null from " + socket_channel.socket().getInetAddress().getHostAddress() );
						continue;
					}
					
					try {
						read_len = buffer.read(socket_channel);
						if ( read_len > 0 ) {
							if ( inputListener!=null ) {
								if ( !callActionWhenSocketClosed ) {	// when packets arrived, do action with new read data
									inputListener.action ( socket_channel, buffer, read_len );
								}
							} else {		// �?��??inputListener �??�을경우, 버퍼�?그냥 비운?? 그렇�??�으�?�?��?�으�?버퍼???�이??? ?�임
								buffer.clear();
							}
						}
					} catch ( Exception ex ) {
						logger.info ( "[SingleSelectorServer] " + this.portNo + "::::read exception " + socket_channel.socket().getInetAddress().getHostAddress() );
						ex.printStackTrace();
						read_len = -1;
					} finally {
						if ( read_len < 0 ) {		// socket ?�결 ?��?
							// ?�결 리스?�에????��
							logger.info ( "[SingleSelectorServer] " + this.portNo + "::::connection closed from " + socket_channel.socket().getInetAddress().getHostAddress() );

							closeConnection(socket_channel);

							if ( inputListener!=null ) {
								if ( callActionWhenSocketClosed ) {		// when socket is closed, do action with whole data
									inputListener.action ( socket_channel, buffer, buffer.getBuffer().position() );
								}
							}

						}
					}
				} else if ( sk.isAcceptable() ) {	// receive connection request

//logger.debug ( "[SingleSelectorServer] someone tries to access $$$$$$$$$$$$$$$$$$$$$$$$$$$$" );

					MessageBuffer buffer = null;
					SocketChannel socket_channel = null;
										
					try {
						ServerSocketChannel server_channel = (ServerSocketChannel)sk.channel();    // 주어�??��? ?�용?�여 ServerSocketChannel ?�성
						socket_channel = server_channel.accept();
						
						socket_channel.configureBlocking(false);    // Set new channel nonblocking

						Socket sock = socket_channel.socket();
						buffer = messageBufferPool.borrowBuffer();

						// ?�결 리스?�에 추�?
						addConnection ( socket_channel, buffer );
						
						if ( buffer!=null ) {
							buffer.setConnectedTime(System.currentTimeMillis());
							buffer.setConnectionInfo(sock.getLocalAddress().getHostAddress(), sock.getLocalPort(), sock.getInetAddress().getHostAddress(), sock.getPort());

							socket_channel.register(this.selector, SelectionKey.OP_READ, buffer );    // Register it with the selector
							logger.info ( "[SingleSelectorServer] " + this.portNo + "::::connection accepted from " + sock.getInetAddress().getHostAddress() );
							System.out.println ( "[SingleSelectorServer] " + this.portNo + "::::connection accepted from " + sock.getInetAddress().getHostAddress() );

						} else {
							logger.warn ( "[SingleSelectorServer] " + this.portNo + "::::connection accepted from " + sock.getInetAddress().getHostAddress() + ", but buffer can't be borrowed" );
						}

					} catch ( Exception ex ) {
						logger.error ( "[SingleSelectorServer] " + this.portNo + "::::error while accept  :: " + ex.getMessage() );
						
						if ( socket_channel!=null ) {
							closeConnection ( socket_channel );
						}
					}

				}
				
			}

			// timeout 체크
			if ( this.timeoutMsec > 0 ) {
				long cur_time = System.currentTimeMillis();
				for ( Iterator<SelectionKey> jt = this.selector.keys().iterator(); jt.hasNext(); ) {
					SelectionKey sk = jt.next();
					if ( sk.isValid() && sk.isReadable() ) {
						SocketChannel socket_channel = (SocketChannel)sk.channel();
						MessageBuffer buffer = (MessageBuffer)sk.attachment();
						
						if ( buffer!=null ) {
							if ( (cur_time - buffer.getLastReadTime()) > this.timeoutMsec ) {
								// ?�결 종료
								logger.info ( "[SingleSelectorServer] " + this.portNo + "::::connection close by timeout " + socket_channel.socket().getInetAddress().getHostAddress() );
								closeConnection(socket_channel);
							}
						}
					}
					// System.out.println ( "V:" + sk.isValid() );
					// System.out.println ( "V:" + sk.isValid() + ", C: " + sk.isConnectable() + ", A: " + sk.isAcceptable() + ", R: " + sk.isReadable() );
				}
			}	
		}

	}

	private void addConnection ( SocketChannel _channel, MessageBuffer _buffer ) {
		// ?�결 리스?�에 추�?
		this.connectionList.add(_channel);
		this.connectionToBuffer.put(_channel, _buffer);
		
	}
	
	public void addMulticastClient ( SocketChannel _channel ) {
		if ( !this.multicastList.contains(_channel) ) {
			this.multicastList.add(_channel);
		}
	}
	
	private void closeConnection ( SocketChannel _channel ) {
		this.connectionList.remove(_channel);
		this.multicastList.remove(_channel);
		MessageBuffer buffer = this.connectionToBuffer.remove(_channel);

		try {
			_channel.socket().shutdownInput();
		} catch ( Exception close_ex ) {}
		try {
			_channel.socket().shutdownOutput();
		} catch ( Exception close_ex ) {}
		try {
			_channel.close();
		} catch ( Exception close_ex ) {}
			
		try {
			if ( buffer!=null ) {
				messageBufferPool.returnBuffer(buffer);
			}
		} catch ( Exception return_ex ) {
		}
	}
	


															
	/**
	 * ?�더 �??�이?��? broadcastBuffer �?��???�아??
	 * ?�결?�어 ?�는 client?�에 broadcast<br>
	 * broadcastBuffer ???�기�?초기??2048�?고정?�어 ?�음
	 * broadcast ??버퍼 ?�기�?2048???�을 경우, ?�시 allocate ??
	 *
	 * @return ?�송??client ??
	 */
    public int broadcast (byte[] _header, byte[] _body) {
    	int result = -1;
    	synchronized ( broadcastBuffer ) {
	    	if ( broadcastBuffer.capacity() < (_header.length + _body.length) ) {	// 보낼 ?�용???�당??버퍼 ?�기보다 ??경우
	    		broadcastBuffer = ByteBuffer.allocateDirect(_header.length + _body.length);
				logger.info ( "[SingleSelectorServer] " + this.portNo + "::::broadcast:: re-allocate broadcast Buffer, capacity = " + broadcastBuffer.capacity() );
	    	}
	    	
			broadcastBuffer.clear();
			
			broadcastBuffer.put(_header);
			broadcastBuffer.put(_body);
			broadcastBuffer.flip();
			
			result = broadcast ( broadcastBuffer );
		}
		return result;
	}
	
	/**
	 * ?�더 �??�이?��? broadcastBuffer �?��???�아??
	 * multicast 받을 리스?�의 client?�에 broadcast<br>
	 * broadcastBuffer ???�기�?초기??2048�?고정?�어 ?�음
	 * broadcast ??버퍼 ?�기�?2048???�을 경우, ?�시 allocate ??
	 *
	 * @return ?�송??client ??
	 */
	public int multicast (byte[] _header, byte[] _body) {
    	int result = -1;
    	synchronized ( broadcastBuffer ) {
	    	if ( broadcastBuffer.capacity() < (_header.length + _body.length) ) {	// 보낼 ?�용???�당??버퍼 ?�기보다 ??경우
	    		broadcastBuffer = ByteBuffer.allocateDirect(_header.length + _body.length);
				logger.info ( "[SingleSelectorServer] " + this.portNo + "::::multicast:: re-allocate broadcast Buffer, capacity = " + broadcastBuffer.capacity() );
	    	}
	    	
			broadcastBuffer.clear();
			
			broadcastBuffer.put(_header);
			broadcastBuffer.put(_body);
			broadcastBuffer.flip();
			
			result = multicast ( broadcastBuffer );
		}
		return result;
	}
	
	

	/**
	 * ?�결?�어 ?�는 client?�에 broadcast<br>
	 * ?�송 ?�류?? ?�결 ?�제
	 *
	 * @return ?�송??client ??
	 */
    public int broadcast (ByteBuffer _send_buffer) {

		if ( (_send_buffer.limit() - _send_buffer.position())==0 )	return 0;		// 보낼 ?�용???�음
		int before_pos = _send_buffer.position();
		int before_limit = _send_buffer.limit();
		
		if ( DebugNet.DEBUG ) {
			logger.debug (  "[SingleSelectorServer] " + this.portNo + "::::broadcast:: target count = " + connectionList.size() + "\n" + MessageBuffer.getHexString(_send_buffer, before_pos, before_limit) );
		}
System.out.println (  "[SingleSelectorServer] " + this.portNo + "::::broadcast:: target count = " + connectionList.size() + "\n" + MessageBuffer.getHexString(_send_buffer, before_pos, before_limit) );
		
		int send_count = 0;
		int send_result = -1;
		synchronized ( connectionList ) {
			// for ( int i=0; i<connectionList.size(); i++ ) {
			for ( java.util.Enumeration<SocketChannel> ie=connectionList.elements(); ie.hasMoreElements(); ) {
// System.out.println ( "connection num = " + connectionList.size() );
				//SocketChannel socket_channel = (SocketChannel)connectionList.get(i);
				SocketChannel socket_channel = ie.nextElement();
				
				send_result = sendTo ( socket_channel, _send_buffer );
				if ( send_result < 0 ) {
					logger.warn ( "[SingleSelectorServer] " + this.portNo + "::::broadcast::fail to send to " + socket_channel.socket().getInetAddress().getHostAddress() );
					closeConnection(socket_channel);
				} else {
					send_count++;
				}
				
				_send_buffer.position(before_pos).limit(before_limit);
			}
		}

		return send_count;
	}

	/**
	 *  multicast 받을 리스?�의 client?�에 broadcast<br>
	 * ?�송 ?�류?? ?�결 ?�제
	 *
	 * @return ?�송??client ??
	 */
    public int multicast (ByteBuffer _send_buffer) {

		if ( (_send_buffer.limit() - _send_buffer.position())==0 )	return 0;		// 보낼 ?�용???�음
		int before_pos = _send_buffer.position();
		int before_limit = _send_buffer.limit();
		
		if ( DebugNet.DEBUG ) {
			logger.debug (  "[SingleSelectorServer] " + this.portNo + "::::multicast:: target count = " + multicastList.size() + "\n" + MessageBuffer.getHexString(_send_buffer, before_pos, before_limit) );
		}
		
		int send_count = 0;
		int send_result = -1;
		synchronized ( multicastList ) {
			// for ( int i=0; i<multicastList.size(); i++ ) {
			for ( java.util.Enumeration<SocketChannel> ie=multicastList.elements(); ie.hasMoreElements(); ) {
				SocketChannel socket_channel = ie.nextElement();
				
				send_result = sendTo ( socket_channel, _send_buffer );
				if ( send_result < 0 ) {
					logger.warn ( "[SingleSelectorServer] " + this.portNo + "::::multicast::fail to send to " + socket_channel.socket().getInetAddress().getHostAddress() );
					closeConnection(socket_channel);
				} else {
					send_count++;
				}
				
				_send_buffer.position(before_pos).limit(before_limit);
			}
		}

		return send_count;
	}


	/**
	 * SocketChannel???�송
	 *
	 * @return ?�송??바이????
	 */	
    private int sendTo (SocketChannel _socket_channel, ByteBuffer _send_buffer) {
		int n = -1;
		try {

			int before_pos = _send_buffer.position();
			
			n = _socket_channel.write(_send_buffer);
			
			if ( DebugNet.DEBUG ) {
				logger.debug ( "[SingleSelectorServer] " + this.portNo + "::::sendTo:: " + n + " bytes to "
					+ _socket_channel.socket().getInetAddress().getHostAddress() + "\n" 
					+ MessageBuffer.getHexString ( _send_buffer, before_pos, n ));
			}
			
		} catch ( Exception ex ) {
			logger.error ( "[SingleSelectorServer] " + this.portNo + "::::sendTo:: error: " + ex.getMessage() );
			return -1;
		}
		return n;
	}


	
	/**
	 * 로거 �?��
	 * <p>
	 * @param _logger	로거
	 */
	public static void setLogger (Logger _logger) {
		logger = _logger;
	}
	
	
	/** ?�스????메인 */	
	public static void main(String[] args) {
		
		try {
			MessageBufferPool pool = new MessageBufferPool(10,2,MessageBufferPool.WHEN_EXHAUSTED_GROW);
			
			SingleSelectorServer svr_obj = new SingleSelectorServer(pool);
			svr_obj.addInputListener(new LineFeedProtocolHandler());
			svr_obj.initServer(8900);
			svr_obj.runServer();
			
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		
	}

}
