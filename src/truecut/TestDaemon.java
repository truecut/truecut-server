package truecut;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.taimos.daemon.DaemonLifecycleAdapter;
import de.taimos.daemon.DaemonProperties;
import de.taimos.daemon.DaemonStarter;
import de.taimos.daemon.ILoggingConfigurer;
public class TestDaemon extends DaemonLifecycleAdapter implements ILoggingConfigurer {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleLogTest.class);



	public long currentTime;
	@Override
	public void doStart() throws Exception {

		System.out.println("Start Daemon");
		 
		int cnt = 0;
		while (true) {

			Thread.sleep(1000);

			cnt ++;
			System.out.println("doing");
			currentTime = System.currentTimeMillis();
			
			if ( cnt >10) {
				break;
			}
		}
	}

	@Override
	public void doStop() throws Exception {
		System.out.println("Daemon Stop");
		
	}

	@Override
	public void signalUSR2() {

		System.out.println("현재시간 : " + currentTime);
	}
	
	public static void main(String[] args) throws Exception {
		
		System.setProperty(DaemonProperties.STARTUP_MODE, DaemonProperties.STARTUP_MODE_RUN);
		DaemonStarter.startDaemon("Test Daemon", new TestDaemon());
		
		Thread.sleep(1000);
		
		LOGGER.info(DaemonStarter.getInstanceId());
//		System.out.println(DaemonStarter.getInstanceId());
		LOGGER.debug(DaemonStarter.getDaemonName());
//		System.out.println(DaemonStarter.getHostname());
//		System.out.println(DaemonStarter.getStartupMode());
//		System.out.println(DaemonStarter.getCurrentPhase());
//		System.out.println(DaemonStarter.getDaemonProperties());
//		System.out.println(td.getPropertyProvider().loadProperties());
		
		Thread.sleep(1000);
		
//		td.signalUSR2();
		
		Thread.sleep(3000); 
		
//		DaemonStarter.stopService();
	}

	@Override
	public void initializeLogging() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reconfigureLogging() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void simpleLogging() throws Exception {
		// TODO Auto-generated method stub
		
	}

}
