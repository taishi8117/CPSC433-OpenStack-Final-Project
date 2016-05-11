package lib;

/**
 * Some colorful debug class
 * @author TAISHI
 *
 */
public class Debug {
	public final static boolean IS_DEBUG = true;

	final static String ANSI_BOLD = "\u001B[1m";
	final static String ANSI_GREEN = "\u001B[32m";
	final static String ANSI_RED = "\u001B[31m";
	final static String ANSI_RESET = "\u001B[0m";
	
	/**
	 * Debug with red output
	 * @param str Message
	 */
	public static void redDebug(String str){
		String message = str;
		debug_helper(message, ANSI_RED);
	}
	
	/**
	 * Debug with bold output
	 * @param str Message
	 */
	public static void boldDebug(String str) {
		String message = str;
		debug_helper(message, ANSI_BOLD);
		
	}
	
	/**
	 * Debug with green output
	 * @param str Message
	 */
	public static void debug(String str) {
		String message = "[DEBUG] " + str;
		debug_helper(message, ANSI_GREEN);
	}
	
	/**
	 * Debug with type
	 * @param type Any object can be passed, represented by toString()
	 * @param str Message
	 */
	public static void debug(Object type, String str) {
		String message = ANSI_BOLD + ANSI_RED + "[" + type.toString() + "]" +
							ANSI_RESET + ANSI_GREEN + " " + str;
		debug_helper(message, "");
		
	}
	
	
	private static void debug_helper(String str, String color) {
		if (IS_DEBUG) {
			System.out.print(color);
			System.out.println(str + ANSI_RESET);
		}
		
	}

}
