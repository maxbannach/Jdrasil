/*
 * Copyright (c) 2016-present, Max Bannach, Sebastian Berndt, Thorsten Ehlers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package jdrasil.utilities;

import jdrasil.utilities.logging.JdrasilLogger;

import java.util.Properties;
import java.util.logging.Level;

/**
 * Static overlay of the properties class. This is globally used to configurate Jdrasil.
 * If Jdrasil is used as standalone, this is done within the main method by the arguments provided to Jdrasil.
 * If Jdrasil is used as library, properties have to be set manually.
 * @author Max Bannach
 */
public class JdrasilProperties {

    /** Global properties object used by Jdrasil. */
    private static Properties properties;

    /** Version of the program. */
    private static final float VERSION = 0.1f;

    /* Static constructor for the properties object. */
    static {
        properties = new Properties();
    }

    /**
     * Checks if the property for 'key' is set.
     * @param key
     * @return true if there is an entry for the key
     */
    public static boolean containsKey(String key) {
        return properties.containsKey(key);
    }

    /**
     * Get the entry stored for the given key.
     * @param key
     * @return the entry
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Sets the property 'key' to the given 'value'.
     * @param key
     * @param value
     */
    public static void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
    
    /**
    * If a timeout is specified, return whether this has been reached or not. 
    */
    public static boolean timeout(){
      if(!properties.containsKey("t"))
        return false;
      return System.nanoTime() > Long.parseLong(properties.getProperty("t")); 
    }

    /**
     * Parsing the programs argument and store them in parameter map.
     * @param args the arguments of the program
     */
    public static void parseArguments(String[] args) {

        // parse the arguments
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.charAt(0) == '-') {

                // help is a special case
                if (a.equals("-h")) {
                    printHelp();
                    System.exit(0);
                }

                // catch format errors
                if (a.length() < 2 || (a.length() == 2 && i == args.length-1)) {
                    System.err.println("Error parsing arguments.");
                    System.exit(-1);
                }

                if (a.length() == 2) { // arguments of length one are followed by a value
                    JdrasilProperties.setProperty(a.substring(1, a.length()), args[i+1]);
                } else { // others are just flags
                    String cmd = a.substring(1, a.length());
                    switch (cmd) {
                        case "log":
                            JdrasilLogger.setLoglevel(Level.INFO);
                            break;
                        case "debug":
                            JdrasilLogger.setLoglevel(Level.ALL);
                            break;
                        default:
                            JdrasilProperties.setProperty(cmd, "");
                    }
                }
            }
        }

        // if we found a seed apply it the the random number generator
        if (JdrasilProperties.containsKey("s")) {
            RandomNumberGenerator.seed(Long.parseLong(JdrasilProperties.getProperty("s")));
        }
        
        // Set timeout
        if(JdrasilProperties.containsKey("t")){
          setProperty("t", "" + (System.nanoTime() + 1000 * 1000 * 1000 * Long.parseLong(JdrasilProperties.getProperty("t"))));
        }
    }

    /**
     * Print a static help message.
     */
    public static void printHelp() {
        System.out.println("Jdrasil");
        System.out.println("Authors: Max Bannach, Sebastian Berndt, and Thorsten Ehlers");
        System.out.println("Version: " + VERSION);
        System.out.println();
        System.out.println("Parameters:");
        System.out.println("  -h : print this dialog");
        System.out.println("  -s <seed> : set a random seed");
        System.out.println("  -t <timeout> : set a time limit");
        System.out.println("  -parallel : enable parallel processing");
        System.out.println("  -instant : computes solution directly (only heuristic mode)");
        System.out.println("  -log : enable log output");
        System.out.println("  -debug : Run some more debugging");
    }

}
