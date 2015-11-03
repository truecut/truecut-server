package truecut.service.test;

import de.taimos.daemon.DaemonStarter;
import truecut.common.Service;

public class TestService extends Service {

	public static final String serviceName ="TestService";
	public static TestService THIS;
	public static TestCfg cfg;
	
	
	public static void main(String[] args) throws Exception {
		THIS = new TestService();
		cfg = new TestCfg();
		THIS.loadMyBaseCfg(TestCfg.class);
		
		DaemonStarter.startDaemon(TestService.serviceName, THIS);
	}
	
	@Override
	public void doStart() throws Exception {
		System.out.println("Start Daemon");
		while(true) {
			THIS.LOG.info("TTTTT : " + System.currentTimeMillis());
			THIS.LOG.info(TestCfg.TEST);
			THIS.LOG.debug("===============");
			Thread.sleep(10000);
		}
			
	}
}
