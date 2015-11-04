package truecut.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigUtil {

	private Logger LOG;

	private static ConfigUtil me = new ConfigUtil();

	private ConfigUtil() {
		LOG = LoggerFactory.getLogger(getClass());

	}

	public static ConfigUtil getInstance() {
		return me;
	}

	/**
	 * ������Ƽ ������ ������ �ε��Ѵ�.
	 * 
	 * @param path
	 *            String ������ / ����� ��� ����ص� �����ϴ�.
	 * @return Properties
	 */
	public Properties getProperties(String path) {
		Properties pty = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream(new File(path));
			pty.load(input);
		} catch (FileNotFoundException ex) {
			LOG.error(ex.toString(), ex);
		} catch (IOException ex) {
			LOG.error(ex.toString(), ex);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException ioe) {
					LOG.error(ioe.toString(), ioe);
				}
			}

		}
		return pty;
	}

	/**
	 *
	 * @param cls
	 *            Class CFG ���ϰ� ������ Ŭ���� �ڷᱸ���� ��Ÿ��.
	 * @param cfgFileName
	 *            String CFG ���ϰ�� ( ������ , ����� ��� ��밡�� )
	 * @return Object
	 * @throws Exception
	 */
	public Object readAllProperty(Class<?> cls, String cfgFileName) throws Exception {
		Object obj = null;
		Properties pty = ConfigUtil.getInstance().getProperties(cfgFileName);
		Field[] fields = null;
		String cfgKey = null;
		try {

			fields = cls.getFields();
			for (Enumeration<Object> eKey = pty.keys(); eKey.hasMoreElements();) {
				cfgKey = (String) eKey.nextElement();
				for (int i = 0; i < fields.length; i++) {

					if (fields[i].getName().equals(cfgKey)) {

						if (fields[i].getType().isPrimitive()) {
							fields[i].setInt(new Integer((String) pty.get(cfgKey)), Integer.parseInt((String) pty.get(cfgKey)));
						} else if (fields[i].getType().isInstance(new String())) {
							fields[i].set((String) pty.get(cfgKey), (String) pty.get(cfgKey));
						}
						// log.debug( "KEY="+fields[i].getName()+" , VALUE="+( String )pty.get( cfgKey ) );
					}
				}

			}
			obj = cls.newInstance();

		} catch (Exception e) {
			LOG.error(e.toString(), e);
			throw e;
		}
		return obj;

	}

}
