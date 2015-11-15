/*
 * ====================================================================
 *
 * Copyright 2008 (c) Daims.co.kr.  All rights reserved.
 *
 */
package truecut.net;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.PoolUtils;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * org.apache.commons.pool.impl.GenericObjectPool ???�속 받아 MessageBuffer????�� Pool ?�공
 * <p>
 *
 * @see #MessageBuffer MessageBuffer
 * @see #MessageBufferFactory MessageBufferFactory
 *
 * @author ?�갈 ??
 * @version $Date: 2010/02/26 02:18:48 $ 
 */
public class MessageBufferPool extends GenericObjectPool {

	/** MessageBuffer ?�성 Factory */
	private MessageBufferFactory factory;	
	
	/**
	 * ?�성??
	 * @param _capacity			MessageBuffer ?�량
	 * @param _max_active		MessageBuffer??최�? �?��
	 * @param _when_exhausted	pool ?�의 MessageBuffer?��? ???�용?�어 졌을 경우, 0 - fail, 1 - gorw, 2 - block
	 */
	public MessageBufferPool(int _capacity, int _max_active, byte _when_exhausted) {

		super();
		
		factory = new MessageBufferFactory(_capacity);
		super.setFactory(PoolUtils.synchronizedPoolableFactory(factory));
		super.setMaxActive(_max_active);
		super.setTestOnBorrow(true);
		super.setTestOnReturn(false);
		super.setWhenExhaustedAction(_when_exhausted);
		/*
		if ( _when_exhausted == 1 ) {	// grow
			super.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_GROW);
		}		
		else if ( _when_exhausted == 2 ) {	// block
			super.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
		}		
		else {	// fail
			super.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL);
		}
		*/
	}

	/**
	 * Obtain an instance from my pool. By contract,
	 * clients MUST return the borrowed instance using returnObject or a related method as defined in an implementation or sub-interface. 
	 * The behaviour of this method when the pool has been exhausted is not specified (although it may be specified by implementations). 
	 * <p>
	 * @param _capacity			capacity of ByteBuffer
	 * @param _max_active		maximum size of ByteBuffer
	 * @param _when_exhausted	pool ?�의 ByteBuffer�????�용?�어 졌을 경우, 0 - fail, 1 - gorw, 2 - block
	 * @return	MessageBuffer instalce
	 * @exception	Exception
	 */
	public MessageBuffer borrowBuffer () throws Exception {
		MessageBuffer buffer = (MessageBuffer)super.borrowObject();
		// System.out.println ( "[MessageBufferPool] borrow: " + getMaxActive() + " vs " + getNumActive() );
		return buffer;
	}

	/**
	 * Return an instance to my pool.
	 * By contract, obj MUST have been obtained using borrowObject or a related method as defined in an implementation or sub-interface. 
	 * <p>
	 * @param buffer			borrow ??ByteBuffer
	 * @exception	Exception
	 */	
	public void returnBuffer (MessageBuffer buffer) throws Exception {
		// System.out.println ( "[MessageBufferPool] return: " + getMaxActive() + " vs " + getNumActive() );
		super.returnObject(buffer);
	}


}


/**
 * org.apache.commons.pool.BaseKeyedPoolableObjectFactory ???�속 받아 MessageBuffer????�� ?�성 기능 ?�공
 * MessageBufferPool ?�서 ?�용
 * <p>
 *
 * @see #MessageBuffer MessageBuffer
 * @see #MessageBufferPool MessageBufferPool
 *
 * @author ?�갈 ??
 * @version $Date: 2010/02/26 02:18:48 $ 
 */
class MessageBufferFactory extends BasePoolableObjectFactory {

	/** 버퍼 ?�량, MessageBuffer 객체 ?�성???�라미터�??�용 */
	private int capacity;

	/**
	 * ?�성??
	 * @param _capacity		MessageBuffer ?�량
	 */
	public MessageBufferFactory(int _capacity) {
		this.capacity = _capacity;
	}
	
	/*
	activateObject is invoked on every instance before it is returned from the pool. 
	passivateObject is invoked on every instance when it is returned to the pool. 
	destroyObject is invoked on every instance when it is being "dropped" from the pool (whether due to the response from validateObject, or for reasons specific to the pool implementation.) 
	validateObject is invoked in an implementation-specific fashion to determine if an instance is still valid to be returned by the pool. It will only be invoked on an "activated" instance.
	*/
	
	/**
	 * Create an instance that can be served by the pool. <br>
	 * makeObject is called whenever a new instance is needed. <br>
	 *
	 * @return an MessageBuffer instance that can be served by the pool. 
	 */ 
	public Object makeObject() throws Exception {
		// System.out.println ("[MessageBufferFactory] call makeObject " );
		return new MessageBuffer(this.capacity);
		
	}
	
	/**
	 * activateObject is invoked on every instance before it is returned from the pool. 
	 *
	 * @param obj	the MessageBuffer instance to be activated 
	 */
	public void activeObject(Object obj) throws Exception {
		// System.out.println ("[MessageBufferFactory] call activeObject" + obj );
	}
	
	/**
	 * passivateObject is invoked on every instance when it is returned to the pool. 
	 *
	 * @param obj	the MessageBuffer instance to be passivated  
	 */
	public void passivateObject(Object obj) throws Exception {
		((MessageBuffer)obj).clear();
		// System.out.println ("[MessageBufferFactory] call passivateObject" + obj );
	}
	
	/**
	 * destroyObject is invoked on every instance when it is being "dropped" from the pool <br>
	 * (whether due to the response from validateObject, or for reasons specific to the pool implementation.) <br>
	 *
	 * @param obj	the MessageBuffer instance to be destroyed   
	 */
	public void destroyObject(Object obj) throws Exception {
		((MessageBuffer)obj).clear();
		// System.out.println ("[MessageBufferFactory] call destroyObject" + obj);
	}
	
	/**
	 * validateObject is invoked in an implementation-specific fashion to determine <br>
	 * if an instance is still valid to be returned by the pool. It will only be invoked on an "activated" instance.<br>
	 *
	 * @param obj	the MessageBuffer instance to be validated 
	 * @return	true
	 */
	public boolean validateObject(Object obj) {
		// System.out.println ("[MessageBufferFactory] call validateObject" + obj);
		return true;
	}
	
}


