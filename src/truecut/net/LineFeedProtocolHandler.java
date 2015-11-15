/*
 * ====================================================================
 *
 * Copyright 2008 (c) Daims.co.kr.  All rights reserved.
 *
 */
package truecut.net;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

/**
 *
 * InputListener�??�속받아 ?�신 ?�이??? ?�킷별로 처리?�기 ?�한 ?�래??br>
 * ?�킷 구조???�음�?같음: ?�일(0x10)<br>
 * ?�신 ?�이??? ?�킷별로 구분?�여 handleData 메소?��? ?�출?�다. handleData 메소???�에?�는 ?�이??? 출력?�기�??��?�?br>
 * Application별로 ?�킷??처리?�기 ?�해?�는 StartEndProtocolHandler�??�속받아 handleData 메소?��? override ?�야 ??br>
 * <p>
 *
 * @author ?�갈 ??
 * @version $Date: 2010/02/26 02:18:45 $ 
 */
public class LineFeedProtocolHandler<E> implements InputListener<E> {

	/** ?�킷 ?�일 ?�의 : ?�일 = LineFeed(\n) */
	public static final byte LINEFEED = 0x0A;
	
	private int startIndex = 0;		// ?�더 발견 ?�치
	private int endIndex = -1;		// ?�일 발견 ?�치
	
	private static Logger logger = Logger.getLogger("root");


	
	/**
	 * ?�더???�일?�이???�이??? 처리, ?�제 Application?�서??handleData 메소?��? ???�의?�여 ?�용
	 * <p>
	 * @param _channel	?�이??? ?�신???�켓 채녈
	 * @param _message_buffer	?�켓 채널??attach ??MessageBuffer
	 * @param _ba		?�이??�� byte array
	 * @return 처리결과=0
	 */		
	protected int handleData ( E _channel_or_socket, MessageBuffer _message_buffer, byte[] _ba ) {
		System.out.println ( "[StartEndProtocolHandler] handle data: " + new String(_ba) );
		return 0;
	}

	/**
	 * ?�신???�이??? ?�고 ?�는 _message_buffer?�서
	 * ?�더???�일??발견??경우 �??�이???�이??? {$link #handleData handleData} 메소?��? ?�출
	 * <p>
	 * SocketThread??action method??같�? ?�고리즘?�로 ?�행?�다.
	 * @param _channel			?�이??? ?�신???�켓 채녈
	 * @param _message_buffer	?�신???�이??? ?�고 ?�는 MessageBuffer
	 * @param _len				?�신 ?�이???�기
	 * @return 0
	 */	
	public int action ( E _channel_or_socket, MessageBuffer _message_buffer, int _len ) {

		if ( _len==0 ) {
			logger.warn ( "[StartEndProtocolHandler] " + "LEN is zero " );
			return 0;
		}
		
		ByteBuffer buffer = _message_buffer.getBuffer();

		int before_pos = buffer.position();

 System.out.println ( "message\n" + MessageBuffer.getHexString(buffer, 0, before_pos) );

		int current_index = buffer.position()-_len;
//		if ( current_index > 0 ) {	// ?�에??ESC 까�?�??�을 ?????�기 ?�문?? ?�로 ?�어?�인 �?보다.. ?�나 ?�전???�덱?��???조사
//			current_index -= 1;
//		}
		
		// ?�킷 ?�더???�일??�?��?�며, 발견??경우 handleData 메소???�출
		while ( true ) {

			if ( startIndex < 0 ) {
//				startIndex = MessageBuffer.indexOf ( buffer, current_index, buffer.position(), HEADER_TAG[0], HEADER_TAG[1]  );
				startIndex = current_index;
			}

			if ( startIndex < 0 ) {
				if ( current_index >= buffer.position() ) {		// 모든 ?�이??? 처리?��???경우, 버퍼 초기??
					_message_buffer.clear();
				} else {										// 처리???�이???�후???�이??? 버퍼???�쪽?�로 shift
					_message_buffer.shift(buffer, current_index);
				}
				break;
			}
			
			current_index = startIndex;
			endIndex = MessageBuffer.indexOf ( buffer, current_index, buffer.position(), LINEFEED  );

			if ( endIndex < 0 ) {		// ?�킷???�더??발견 ?�었?�나, ?�일??발견?��? ?�을 경우
				if ( startIndex > 0 ) {		// ?�더?�치�?0보다 ?�경?? 버퍼???�쪽?�로 shift
					_message_buffer.shift ( buffer, startIndex );
					startIndex = 0;
				}
				break;
			}
			
			// ?�킷 ?�더 ???�일 ?�이???�이??? ?�께 handleData ?�출
			byte[] msg = new byte[endIndex-startIndex];
			buffer.position(startIndex);
			buffer.get(msg);
			
			handleData ( _channel_or_socket, _message_buffer, msg );
			
			current_index = endIndex+1;
			startIndex = endIndex = -1;
			buffer.position(before_pos);
		}

		return 0;
	}


	/**
	 * ?�켓채널?�로 메시�??�송
	 * <p>
	 * @param _channel	?�켓 채널
	 * @param _buffer	메시�?? ?�고 ?�는 ByteBuffer (버퍼??0 �?�� position???�치까�? 메시�??�송)
	 * @return	0
	 */	
	public static int sendMessage (SocketChannel _channel, ByteBuffer _buffer ) throws Exception {

		_buffer.flip();
		_buffer.position(0);
		
		_channel.write(_buffer);
		return 0;
	}


	/**
	 * 로거 �?��
	 * <p>
	 * @param _logger	로거
	 */
	public static void setLogger (Logger _logger) {
		logger = _logger;
	}
	
	public static void main (String[] args) {
		try {
			byte b = '\n';
			System.out.println(b);
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
	}

}
