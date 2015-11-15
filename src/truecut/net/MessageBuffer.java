/*
 * ====================================================================
 *
 * Copyright 2008 (c) Daims.co.kr.  All rights reserved.
 *
 */
package truecut.net;

import java.io.BufferedInputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;





/**
 * java.nio�??�용?�는 ?�켓 ?�로그램?�서 ?�용?�기 ?�한 버퍼 ?�의<br>
 * <p>
 *
 * @see #SingleSelectorServer SingleSelectorServer
 * @see #BasicProtocolHandler BasicProtocolHandler
 *
 * @author ?�갈 ??
 * @version $Date: 2010/03/23 04:33:00 $ 
 */
public class MessageBuffer {

	/** send??버퍼 */
	private ByteBuffer sendBuffer;
	
	/** ByteBuffer.allocateDirect ?�수�??�용?�서 ?�성?�는 버퍼 */
	private ByteBuffer directBuffer;
	/** ByteBuffer.allocate ?�수�??�용?�서 ?�성?�는 버퍼, ?�이??? directBuffer??capacity보다 ??경우 ?�용 */
	private ByteBuffer extendBuffer;

	/** extendBuffer ?�용 ?��? */
	private boolean isExtended;

	/** ?�켓 ?�결 ?�간 */
	private long connectedTime;
	
	/** 마�?�??�신 ?�간 */
	private long lastReadTime;
	
	/** remote IP Address */
	private String remoteHost;
	
	/** remote Port */
	private int remotePort;
	
	/** local information */
	private String localHost;
	
	/** local Port */
	private int localPort;
	
	private int initialCapacity;
	
	// 2011-02-23 추�?, SocketChannel ?�에 Socket ?�서 read?�수�??�출?�면 ??추�???
	/** directFlag=true ?�면 directBuffer �?��??allocateDirect �??�출?�여 ?�성, 그렇�??�으�?allocate method�??�출?�여 ?�성 */
	private boolean directFlag;
	
	private static Logger logger = Logger.getLogger("root");
	
	
	

	/**
	 * ?�성??
	 * <p>
	 * @param _capacity	direct buffer 초기 ?�기
	 * @param _dir_flag	allocate 방식 (true ??경우??allocateDirect ??, SocketChannel ???�용??경우??true�? Socket ???�용??경우??false �?주는 �?좋음
	 */	
	public MessageBuffer ( int _capacity, boolean _dir_flag ) {
		this.isExtended = false;
		this.initialCapacity = _capacity;
		this.directFlag = _dir_flag;
		this.sendBuffer = (_dir_flag ? ByteBuffer.allocateDirect(_capacity) : ByteBuffer.allocate(_capacity));
		this.directBuffer =(_dir_flag ? ByteBuffer.allocateDirect(_capacity) : ByteBuffer.allocate(_capacity));

	}

	public MessageBuffer ( int _capacity ) {
		this ( _capacity, true );
	}

	/*
	 * SocketChannel?�서 ?�이??? ?�어?�임
	 */
	public int read ( SocketChannel _channel ) throws Exception {
		this.lastReadTime = System.currentTimeMillis();
		
		int read_len = _channel.read(directBuffer);
		
		if ( DebugNet.DEBUG ) {
			if ( read_len > 0 ) {
				Socket sock = _channel.socket();
				logger.debug ( "[MessageBuffer] " + 
									sock.getLocalAddress().getHostAddress() + ":" + sock.getLocalPort() + " <- " +
									sock.getInetAddress().getHostAddress() + ":" + sock.getPort() + " :::: " +
									"read_len[" + read_len + "]\n" +
									getHexString(directBuffer, directBuffer.position()-read_len, read_len) );
			}
		}

		// extendDirectBuffer ?�는 extendBuffer ??�??�나�??�출?�야 ??
		// ?�다 ?�스????.
		
		extendDirectBuffer(read_len);
		// extendBuffer(read_len);
		
		return read_len;
	}

	/*
	 * BufferedInputStream ?�서 ?�이??? ?�어?�임
	 */
	public int read ( BufferedInputStream _in, Socket _sock ) throws Exception {
		this.lastReadTime = System.currentTimeMillis();
		
		
		int read_len = -1;
		if ( directBuffer.hasArray() ) {
			byte[] ba = directBuffer.array();
			read_len = _in.read(ba, directBuffer.position(), directBuffer.capacity()-directBuffer.position());
			if ( read_len > 0 ) {
				directBuffer.position(directBuffer.position()+read_len);
			}
		} else {
			byte[] ba = new byte[directBuffer.capacity()-directBuffer.position()];

			read_len = _in.read(ba, 0, ba.length);

			if ( read_len > 0 ) {
				directBuffer.put(ba, 0, read_len);
			}
		}
		
		if ( DebugNet.DEBUG ) {
			if ( read_len > 0 ) {
				logger.debug ( "[MessageBuffer] " + 
									_sock.getLocalAddress().getHostAddress() + ":" + _sock.getLocalPort() + " <- " +
									_sock.getInetAddress().getHostAddress() + ":" + _sock.getPort() + " :::: " +
									"read_len[" + read_len + "]\n" +
									getHexString(directBuffer, directBuffer.position()-read_len, read_len) );
			}
		}

		// extendDirectBuffer ?�는 extendBuffer ??�??�나�??�출?�야 ??
		// ?�다 ?�스????.
		
		extendDirectBuffer(read_len);
		// extendBuffer(read_len);
		
		return read_len;
	}



	/**
	 * 버퍼 ?�이�?체크 ?? ?�요 ???�장. ?�장??direct buffer �??�장??
	 *
	 * @return true(?�장), false(?�장?�함)
	 */
	private boolean extendDirectBuffer(int read_len) {
		
		if ( directBuffer.position() >= directBuffer.capacity() ) {	// 버퍼 ?�기 ?�리�?

			ByteBuffer new_buf = (this.directFlag ? ByteBuffer.allocateDirect(initialCapacity + directBuffer.capacity()) : ByteBuffer.allocate(initialCapacity + directBuffer.capacity()));
System.out.println ( "extend::: " + new_buf.capacity() );		

			directBuffer.flip();
			new_buf.put(directBuffer);

			directBuffer.clear();
			directBuffer = null;
			directBuffer = new_buf;

			return true;
		} else {
			return false;
		}
	}


	/**
	 * 버퍼 ?�이�?체크 ?? ?�요 ???�장. ?�장?�엔 extendBuffer ?�용
	 *
	 * @return true(?�장), false(?�장?�함)
	 */	
	private boolean extendBuffer(int read_len) {
		if ( directBuffer.position() >= directBuffer.capacity() ) {	// directBuffer�?�?찼을 경우, extendBuffer ?�성 ?�는 ?�리�?
			isExtended = true;
			if ( extendBuffer==null ) {	// first extend
				
//logger.debug ( "############# first extend::: " + (directBuffer.capacity()*2) );		
				extendBuffer = ByteBuffer.allocate(directBuffer.capacity()*2);
			} else {	// second or more extend
				ByteBuffer new_buf = ByteBuffer.allocate(extendBuffer.capacity() + directBuffer.capacity()*2);
//logger.debug ( "############# second extend::: " + new_buf.capacity() );		

				extendBuffer.flip();
				new_buf.put(extendBuffer);
				extendBuffer.clear();
				extendBuffer = null;
				extendBuffer = new_buf;
			}
			directBuffer.flip();
			extendBuffer.put(directBuffer);
			directBuffer.clear();
		} else if ( isExtended ) {		// extend 버퍼 ?�용중일 경우??copy directBuffer into extendBuffer
			int prev_pos = directBuffer.position();

//logger.debug ( "############# extend copy::: " );		
			
			directBuffer.flip();
			directBuffer.position(directBuffer.limit()-read_len);
			extendBuffer.put(directBuffer);
			
			directBuffer.clear();
			directBuffer.position(prev_pos);
		}
		return isExtended;
	}
	

	/**
	 * SocketChannel??attach???�신??버퍼 반환
	 */
	public ByteBuffer getSendBuffer() {
		return this.sendBuffer;
	}
	
	public void setSendBuffer(ByteBuffer _buf) {
		this.sendBuffer = _buf;
	}
	
	/**
	 * SocketChannel?�서 ?�이??? ?�어?�인 buffer 반환
	 * 버퍼�??�장?�었??경우 ?�장 버퍼 반환, 그렇�??��? 경우 direct 버퍼 반환
	 * <p>
	 * @return 버퍼
	 */
	public ByteBuffer getBuffer() {
		if ( isExtended	)	return this.extendBuffer;
		return this.directBuffer;
	}


	/**
	 * 버퍼 초기?? ?�장 버퍼�??�을 경우 ?�장 버퍼 ??��
	 */
	public void clear() {
		this.directBuffer.clear();
		if ( this.extendBuffer!=null )	this.extendBuffer.clear();
		this.extendBuffer = null;
		this.isExtended = false;
	}

	/**
	 * ?�라미터�?주어�??�덱???�후??버퍼 ?�이??? 버퍼 ?��???0�?���?shift
	 * <p>
	 * @param _buffer	버퍼
	 * @param _index	버퍼???�치
	 */
	public void shift (ByteBuffer _buffer, int _index ) {
		if ( _buffer==this.extendBuffer ) {
			byte[] msg = new byte[_buffer.position()-_index];
			_buffer.position(_index);
			_buffer.get(msg);

			if ( msg.length<this.directBuffer.capacity() ) {	// ?��? ?�용??directBuffer???�어�????�는 경우
				this.directBuffer.clear();
				this.directBuffer.put(msg);
				this.extendBuffer.clear();
				this.extendBuffer = null;
				this.isExtended = false;
			} else {
				this.extendBuffer.clear();
				this.extendBuffer.put(msg);
				this.directBuffer.clear();
			}
		} else {	// direct buffer
			byte[] msg = new byte[_buffer.position()-_index];
			_buffer.position(_index);
			_buffer.get(msg);
			_buffer.clear();
			_buffer.put(msg);
		}
	}
	
	/**
	 * ?�켓 ?�결 ?�간??�?��?�어 ?�을 경우, ?�켓 ?�결?�간 반환
	 * <p>
	 * @return	?�켓 ?�결?�간
	 */
	public long getConnectedTime() {
		return connectedTime;
	}


	/**
	 * ?�켓 ?�결 ?�간??�?��
	 * <p>
	 * @param _time	?�켓 ?�결?�간
	 */
	public void setConnectedTime(long _time) {
		connectedTime = _time;
	}

	/**
	 * 마�?�??�신 ?�간 반환
	 *
	 * @return 최종 ?�이???�신?�간
	 */
	public long getLastReadTime() {
		return lastReadTime;
	}
	 
	/**
	 * ?�결 ?�보 �?��, 로그 ?�길???�용 ?�해
	 * <p>
	 *
	 * @param _local_host	local host ip address
	 * @param _local_port	local port
	 * @param _remote_host	remote host ip address
	 * @param _remote_port	remote port
	 */
	 public void setConnectionInfo(String _local_host, int _local_port, String _remote_host, int _remote_port) {
	 	this.localHost = _local_host;
	 	this.localPort = _local_port;
	 	this.remoteHost = _remote_host;
	 	this.remotePort = _remote_port;
	 }
	 
	/**
	 * ?�라미터�?주어�?버퍼?�서 주어�?범위?�에??sindex<= ... <eindex) ?�이??2개의 바이?��? ?�치?�는 position �?��
	 * <p>
	 * @param buffer	�?�� ??�� 버퍼
	 * @param sindex	버퍼??�?�� 범위 ?�작 ?�치(?�함)
	 * @param eindex	버퍼??�?�� 범위 종료 ?�치(미포??
	 * @param target_b1	�?�� ??�� 첫째 바이??
	 * @param target_b2	�?�� ??�� ?�째 바이??
	 * @return	�?�� ??�� 첫째 바이?�의 버퍼 ?�의 ?�치
	 */
	public static int indexOf ( ByteBuffer buffer, int sindex, int eindex, byte target_b1, byte target_b2 ) {
		byte b1, b2;
		
		for ( int i=sindex; i<eindex-1; i++ ) {
			b1 = buffer.get(i);
			if ( b1==target_b1 ) {
				b2 = buffer.get(i+1);
				if ( b2==target_b2 ) {
					return i;
				}		
			}
		} 
		return -1;
	}
	
	public static int indexOf ( ByteBuffer buffer, int sindex, int eindex, byte target_b1 ) {
		byte b1;
		
		for ( int i=sindex; i<eindex; i++ ) {
			b1 = buffer.get(i);
			if ( b1==target_b1 ) {
				return i;
			}
		} 
		return -1;
		
	}

	/**
	 * ?�라미터�?주어�?버퍼?�서 주어�?범위??sindex<= ... <eindex)???�용 출력
	 * <p>
	 * @param buffer	출력 ??�� 버퍼
	 * @param sindex	버퍼??출력 범위 ?�작 ?�치(?�함)
	 * @param eindex	버퍼??출력 범위 종료 ?�치(미포??
	 * @return ?�사�?출력??문자??
	 */
	public static String getHexString ( ByteBuffer buffer, int sindex, int eindex ) {
		StringBuilder s = new StringBuilder();
		int total_length = (eindex-sindex);
		
		int j = 0; // we'll allow 20 bytes per line
		for (int i=0;i<total_length;i++)
		{
		    if (j++ > 19)
		    {
		        j=1;
		        s.append("\n");
		    }
		    String bs = Integer.toString((int)buffer.get(i+sindex)&0xFF, 16);
		    if (bs.length() < 2)
		    {
		        bs = "0" + bs;
		    }
		    s.append(bs+ " ");
		}
		return s.toString();
	}

	/**
	 * ?�라미터�?주어�?byte[]?�서 주어�?범위??sindex<= ... <eindex)???�용 출력
	 * <p>
	 * @param ba		출력 ??�� byte[]
	 * @param sindex	버퍼??출력 범위 ?�작 ?�치(?�함)
	 * @param eindex	버퍼??출력 범위 종료 ?�치(미포??
	 * @return ?�사�?출력??문자??
	 */
	public static String getHexString ( byte[] ba, int sindex, int eindex ) {
		StringBuilder s = new StringBuilder();
		int total_length = (eindex-sindex);
		
		int j = 0; // we'll allow 20 bytes per line
		for (int i=0;i<total_length;i++)
		{
		    if (j++ > 19)
		    {
		        j=1;
		        s.append("\n");
		    }
		    String bs = Integer.toString((int)ba[i+sindex]&0xFF, 16);
		    if (bs.length() < 2)
		    {
		        bs = "0" + bs;
		    }
		    s.append(bs+ " ");
		}
		return s.toString();
	}
	
	/**
	 * 로거 �?��
	 * <p>
	 * @param _logger	로거
	 */
	public static void setLogger (Logger _logger) {
		logger = _logger;
	}
		
	/*
	public static void printBuffer ( ByteBuffer buffer, int sindex, int eindex ) {
		System.out.println ( "[MessageBuffer]" + buffer.toString() );

		System.out.print ( "[MessageBuffer] " );
		for ( int i=sindex; i<eindex; i++) {
			System.out.print ( Integer.toHexString(Integer.parseInt(Byte.toString(buffer.get(i)))) + " " );
		}
		System.out.println();

	}
	*/

}
