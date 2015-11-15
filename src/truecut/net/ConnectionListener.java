/*
 * ====================================================================
 *
 * Copyright 2008 (c) Daims.co.kr.  All rights reserved.
 *
 */
package truecut.net;

import java.nio.channels.SocketChannel;

/**
 * SocketChannelClient ?´ë˜?¤ì—???¬ìš©?˜ê¸° ?„í•œ ëª©ì ?? 
 * ?Œì¼“ ?°ê²°/ì¤‘ì? ?? ì²˜ë¦¬?˜ê¸° ?„í•œ ?¸í„°?˜ì´???•ì˜
 * <p>
 * @see #SocketChannelClient SocketChannelClient
 *
 * @author ?œê°ˆ ??
 * @version $Date: 2010/02/26 02:18:47 $ 
 */
public interface ConnectionListener {

	/**
	 * Socket ?°ê²°???±ê³µ ??ê²½ìš° ?¸ì¶œ?˜ëŠ” ë©”ì†Œ??
	 *
	 * @param _channel	?Œì¼“ ì²´ë„
	 * @return N/A
	 */
	public void whenConnected ( String _host, SocketChannel _channel );
	
	/**
	 * Socket ?°ê²°???Šê¸¸ ê²½ìš° ?¸ì¶œ?˜ëŠ” ë©”ì†Œ??
	 *
	 * @param _channel	?Œì¼“ ì²´ë„
	 * @return N/A
	 */
	public void whenDisconnected ( String _host );
	
}