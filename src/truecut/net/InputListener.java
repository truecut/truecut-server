package truecut.net;


/**
 * SingleSelectorServer 클래스에서 사용하기 위한 목적의, 
 * SocketChannel에서 데이타가 수신될 경우 처리하기 위한 인터페이스 정의
 * <p>
 * InputListener 구현시 주의 사항 (또는 구현한 클래스 - e.g: FixedLengthProtocolHandler 를 상속받아 구현할 경우에도 마찬가지임)  :
 * <br>
 * SingleSelectorServer 에선 여러 클라이언트 소켓에서 데이터를 수신할 때,
 * 한개의 동일한 InputListener를 상속받은 인스턴스의 action method를 호출하여 처리하다. 
 * (물론 이는, 동시에 호출되진 않고 순차적으로 호출되긴 하지만)<Br>
 * 따라서 InputLitener를 구현하는 클래스에서는 이를 주의할 필요가 있다.
 * <br>
 * 예를 들어, 다음과 같은 코드를 예를 들자<br>
   public class Foo implements InputListener {
     private InetAddress clientAddress;
  
     public int action (  SocketChannel _channel, MessageBuffer _buffer, int _len ) {
     	if ( this.clientAddress != null ) {
     		System.out.println ( "previous client address : " + this.clientAddress.getHostAddress() );
     	}
     	this.clientAddress = _channel.socket().getInetAddress();
     	System.out.println ( "current client address : " + this.clientAddress.getHostAddress() );
     }
   }
 * <br>
 * 위와 같은 InputListener 를 구현하여, SingleSelectorServer와 함께 사용하고, 동시에 여러 위치의 client에서 접속한다면
 * previous client address 와, current client address 는 다르게 찍힐 것이다.
 *
 * @see #SingleSelectorServer SingleSelectorServer
 *
 * @author 제갈 영
 * @version $Date: 2010/02/26 02:18:48 $ 
 */
public interface InputListener<E> {

	/**
	 * SocketChannel에서 에서 새로운 데이타가 수신될 경우, 호출되는 메소드
	 *
	 * @param _channel	소켓 체널
	 * @param _buffer	수신된 데이타를 담고 있는 MessageBuffer 인스턴스
	 * @param _len		수신된 데이타 길이
	 * @return 처리 값
	 */
	public int action ( E _channel_or_socket, MessageBuffer _buffer, int _len );
}