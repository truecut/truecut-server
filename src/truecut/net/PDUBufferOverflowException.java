/*
 * ====================================================================
 *
 * Copyright 2008 (c) Daims.co.kr.  All rights reserved.
 *
 */
package truecut.net;

import java.nio.BufferOverflowException;

/**
 * ByteBuffer ??CommonPDU???�용??put?�때 ?�기�?모자??경우??발생?�는 Exception
 * <p>
 *
 *
 * @author ?�갈 ??
 * @version $Date: 2010/02/26 02:18:49 $ 
 */
public class PDUBufferOverflowException extends BufferOverflowException { 

	/** CommonPDU???�용???�는 ???�요??길이 */
	private int requiredLen;
	
	/**
	 * ?�성??
	 * @param _len	CommonPDU???�용???�는 ???�요??길이	
	 */
	public PDUBufferOverflowException (int _len) {
		super();
		this.requiredLen = _len;
	}

	/**
	 * @return	CommonPDU???�용???�는 ???�요??길이	
	 */
	public int getRequiredLen() {
		return this.requiredLen;
	}

	/**
	 * CommonPDU???�용???�는 ???�요??길이 �?��
	 * @param _len	CommonPDU???�용???�는 ???�요??길이	
	 */	
	public void setRequiredLen (int _len) {
		this.requiredLen = _len;
	}

}