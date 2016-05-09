package lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads OpenStack global configuration file 
 * 
 * Currently supporting config:
 * - LocScript: <Location of script directory>
 * - CtrlPort: <Port to talk to the controller from outside>
 * 
 * 
 * @author TAISHI
 *
 */
public class ReadConfig {
	File config_file;
	HashMap<String, String> config_map;

	// boolean to check if LocScript was there
	private boolean script_set = false;
	private boolean port_set = false;

	
	
	//regex
	private static final Matcher locScriptRegex = Pattern.compile("LocScript\\:\\s*([^\\s]+)").matcher("");
	private static final Matcher ctrlPortRegex = Pattern.compile("CtrlPort\\:\\s*(\\d+)").matcher("");
	private static final Matcher commentRegex = Pattern.compile("#.*").matcher("");
	
	public ReadConfig(String config_file) throws Exception {
		this.config_file = new File(config_file);
		if (!this.config_file.isFile()) {
			throw new Exception("Configuration not found");
		}
		this.config_map = new HashMap<String, String>();
	}
	
	private void parse() throws Exception {
		InputStream inputStream = new FileInputStream(config_file);
		BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream));
		String line;
		

		while ((line = buffer.readLine()) != null) {
			if (commentRegex.reset(line).find()) {
				// found comment
				continue;
			} else if (locScriptRegex.reset(line).find()) {
				// found "LocScript: <location to script directory>"
				// removes trailing slash
				String loc = locScriptRegex.group(1).trim().replaceAll("/$", "");
				script_set = true;
				config_map.put("LocScript", loc);
			} else if (ctrlPortRegex.reset(line).find()) {
				// found "CtrlPort: <port to talk to the controller from outside>"
				int ctrlPort;
				try {
					ctrlPort = Integer.parseInt(ctrlPortRegex.group(1).trim());
					if (ctrlPort < 65536) {
						port_set = true;
						config_map.put("CtrlPort", Integer.toString(ctrlPort));
					}
				} catch (Exception e) {
					//couldn't parse properly
					port_set = false;
				}
			}
		}
		buffer.close();

	}
	
	
	/**
	 * Returns mapped configuration
	 */
	public HashMap<String, String> getConfig() throws Exception {
		parse();
		if (script_set && port_set) {
			return config_map;
		}else {
			throw new Exception ("Error parsing configuration: LocScript / CtrlPort invalid");
		}
	}

}
