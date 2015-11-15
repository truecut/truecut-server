/*
 * ====================================================================
 *
 * Copyright 2008 (c) Daims.co.kr.  All rights reserved.
 *
 */
package truecut.net;

import java.nio.BufferOverflowException;

/**
 * ByteBuffer ??CommonPDU???´ìš©??put? ë•Œ ?¬ê¸°ê°?ëª¨ì??ê²½ìš°??ë°œìƒ?˜ëŠ” Exception
 * <p>
 *
 *
 * @author ?œê°ˆ ??
 * @version $Date: 2010/02/26 02:18:49 $ 
 */
public class PDUBufferOverflowException extends BufferOverflowException { 

	/** CommonPDU???´ìš©???°ëŠ” ???„ìš”??ê¸¸ì´ */
	private int requiredLen;
	
	/**
	 * ?ì„±??
	 * @param _len	CommonPDU???´ìš©???°ëŠ” ???„ìš”??ê¸¸ì´	
	 */
	public PDUBufferOverflowException (int _len) {
		super();
		this.requiredLen = _len;
	}

	/**
	 * @return	CommonPDU???´ìš©???°ëŠ” ???„ìš”??ê¸¸ì´	
	 */
	public int getRequiredLen() {
		return this.requiredLen;
	}

	/**
	 * CommonPDU???´ìš©???°ëŠ” ???„ìš”??ê¸¸ì´ ì§? •
	 * @param _len	CommonPDU???´ìš©???°ëŠ” ???„ìš”??ê¸¸ì´	
	 */	
	public void setRequiredLen (int _len) {
		this.requiredLen = _len;
	}

}