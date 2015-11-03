package truecut.common;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import de.taimos.daemon.DaemonLifecycleAdapter;
import de.taimos.daemon.ILoggingConfigurer;
import truecut.util.ConfigUtil;

public class Service extends DaemonLifecycleAdapter implements ILoggingConfigurer {

	protected Logger LOG;

	public Service() {
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void init() throws Exception {
		initializeLogging();
	}

	@Override
	public void initializeLogging() throws Exception {

		String properties = "log4j." + getClass().getSimpleName() + ".properties";
		Path pathLog4j = Paths.get("deploy", "conf", "log4j", properties);

		PropertyConfigurator.configure(pathLog4j.toString());
		PropertyConfigurator.configureAndWatch(pathLog4j.toString(), 10000);

		this.LOG = Logger.getLogger(getClass());

		this.LOG.info("*****************************************************************");
		this.LOG.info("* " + getClass().getName() + " STARTUP");
		this.LOG.info("*****************************************************************");

	}

	public void loadMyBaseCfg(Class<?> cls) throws Exception {

		String properties = getClass().getSimpleName() + ".cfg";
		Path pathCfg = Paths.get("deploy", "conf", "cfg", properties);

		ConfigUtil.getInstance().readAllProperty(cls, pathCfg.toString());
	}

	@Override
	public void reconfigureLogging() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void simpleLogging() throws Exception {
		initializeLogging();
	}

}
