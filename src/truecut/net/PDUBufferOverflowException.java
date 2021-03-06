/*
 * ====================================================================
 *
 * Copyright 2008 (c) Daims.co.kr.  All rights reserved.
 *
 */
package truecut.net;

import java.nio.BufferOverflowException;

/**
 * ByteBuffer ??CommonPDU???΄μ©??put? λ ?¬κΈ°κ°?λͺ¨μ??κ²½μ°??λ°μ?λ Exception
 * <p>
 *
 *
 * @author ?κ° ??
 * @version $Date: 2010/02/26 02:18:49 $ 
 */
public class PDUBufferOverflowException extends BufferOverflowException { 

	/** CommonPDU???΄μ©???°λ ???μ??κΈΈμ΄ */
	private int requiredLen;
	
	/**
	 * ?μ±??
	 * @param _len	CommonPDU???΄μ©???°λ ???μ??κΈΈμ΄	
	 */
	public PDUBufferOverflowException (int _len) {
		super();
		this.requiredLen = _len;
	}

	/**
	 * @return	CommonPDU???΄μ©???°λ ???μ??κΈΈμ΄	
	 */
	public int getRequiredLen() {
		return this.requiredLen;
	}

	/**
	 * CommonPDU???΄μ©???°λ ???μ??κΈΈμ΄ μ§? 
	 * @param _len	CommonPDU???΄μ©???°λ ???μ??κΈΈμ΄	
	 */	
	public void setRequiredLen (int _len) {
		this.requiredLen = _len;
	}

}