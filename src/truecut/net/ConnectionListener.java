/*
 * ====================================================================
 *
 * Copyright 2008 (c) Daims.co.kr.  All rights reserved.
 *
 */
package truecut.net;

import java.nio.channels.SocketChannel;

/**
 * SocketChannelClient ?�래?�에???�용?�기 ?�한 목적?? 
 * ?�켓 ?�결/중�? ?? 처리?�기 ?�한 ?�터?�이???�의
 * <p>
 * @see #SocketChannelClient SocketChannelClient
 *
 * @author ?�갈 ??
 * @version $Date: 2010/02/26 02:18:47 $ 
 */
public interface ConnectionListener {

	/**
	 * Socket ?�결???�공 ??경우 ?�출?�는 메소??
	 *
	 * @param _channel	?�켓 체널
	 * @return N/A
	 */
	public void whenConnected ( String _host, SocketChannel _channel );
	
	/**
	 * Socket ?�결???�길 경우 ?�출?�는 메소??
	 *
	 * @param _channel	?�켓 체널
	 * @return N/A
	 */
	public void whenDisconnected ( String _host );
	
}