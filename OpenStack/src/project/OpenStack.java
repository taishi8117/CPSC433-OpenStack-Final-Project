package project;

import java.util.HashMap;

import lib.Debug;
import lib.ReadConfig;

/**
 * 
 * Main class for OpenStack
 * # java OpenStack -config <config_file_name>
 *
 */
public class OpenStack {
	static HashMap<String, String> config_map;

	public static void main(String[] args) {
		if (args.length != 2 || !args[0].equals("-config")) {
			System.out.println("# java OpenStack -config <config_file_name>");
			System.exit(1);
		}
		
		// Reading config file
		String config_file = args[1];
		try {
			ReadConfig parse = new ReadConfig(config_file);
			config_map = parse.getConfig();
			Debug.debug(config_map.toString());
			if (config_map == null) {
				throw new Exception("Incomplete configuration");
				
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(2);
		}
		
		
		try {
			run(config_map);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Debug.debug(e.getMessage());
			
			System.exit(3);
		}
	}
	

	private static void run(HashMap<String, String> config) throws Exception {
		//TODO create a listening server

		Controller controller = new Controller(config);
		
	}

}
