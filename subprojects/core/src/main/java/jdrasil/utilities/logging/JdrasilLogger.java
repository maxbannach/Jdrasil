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
package jdrasil.utilities.logging;

import java.util.logging.*;

/**
 * This class represents the logger used by Jdrasil. It essentially supports two modes:
 * <p>
 * 1) if Jdrasil is used as standalone, log information are printed as DIMACS-styled comment (with a leading 'c')
 * to the System.out;</p>
 * <p>
 * 2) if Jdrasil is used as library, log informations are piped to System.err with richer information.
 * </p>
 *
 * The default configuration is 2), in order to switch to the first configuration use @see jdrasil.utilities.logging.JdrasilLogger#setToDimacsLogging(),
 * in order to switch back to the second configuration use @see jdrasil.utilities.logging.JdrasilLogger#setToClassicLogging().
 *
 * <p>
 * To (globally) change the log behavior of Jdrasil,
 * modify a logger instance of this logger, @see java.util.logging.Logger#getLogger(java.lang.String).
 * </p>
 *
 * @author Max Bannach
 */
public class JdrasilLogger {

    /** Name of this Logger, use with @see java.util.logging.Logger#getLogger(java.lang.String) */
    private static final String NAME = "JdrasilLogger";

    /** Local instance of the Logger. */
    private final static Logger LOGGER = Logger.getLogger(NAME);

    /*
     * Static constructor to set up logger configuration.
     *
     * Default configuration is:
     * Log with rich information to System.err (library mode), Loglvl: ALL
     */
    static {
        setToClassicLogging();
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.OFF);
    }

    /**
     * Get the name of JDrasils default logger. Use with @see java.util.logging.Logger#getLogger(java.lang.String)
     * to obtain an instance.
     *
     * @return name of the logger
     */
    public static String getName() {
        return NAME;
    }

    /**
     * Auxillary method to delete all handlers attached to
     * Jdrasils default logger.
     */
    private static void removeHandlers() {
        Handler[] handlers = LOGGER.getHandlers();
        for (Handler h : handlers) {
            h.flush();
            LOGGER.removeHandler(h);
        }
    }

    /**
     * Set the logging level of Jdrasil.
     * @see Logger#setLevel(java.util.logging.Level)
     * @param lvl
     */
    public static void setLoglevel(Level lvl) {
        LOGGER.setLevel(lvl);
    }

    /**
     * Set the log mode to dimacs, this will:
     * 1) log to System.out,
     * 2) write log messages simply with a leading 'c' (DIMACS comment style).
     *
     * @see jdrasil.utilities.logging.DimacsLogFormatter for details.
     */
    public static void setToDimacsLogging() {
        removeHandlers();
        Handler ch = new StreamHandler(System.out, new DimacsLogFormatter()) {
            @Override
            public synchronized void publish(final LogRecord record) {
                super.publish(record);
                flush();
            }
        };
        ch.setLevel(java.util.logging.Level.ALL);
        LOGGER.addHandler(ch);
    }

    /**
     * Set the log mode to classic, this will:
     * 1) log to System.err,
     * 2) print richer informations
     *
     * @see JdrasilLogFormatter for more informations
     */
    public static void setToClassicLogging() {
        removeHandlers();
        Handler ch = new StreamHandler(System.err, new JdrasilLogFormatter()) {
            @Override
            public synchronized void publish(final LogRecord record) {
                super.publish(record);
                flush();
            }
        };
        ch.setLevel(java.util.logging.Level.ALL);
        LOGGER.addHandler(ch);
    }

    /**
     * Sets the handler to a custom handler. This method will remove all other handlers,
     * and add the provided one.
     *
     * This is a shortcut for @see java.util.logging.Logger#removeHandler(java.util.logging.Handler)
     * and @see java.util.logging.Logger#addHandler(java.util.logging.Handler) .
     *
     * @param ch the handler to be used
     */
    public static void setCustomHandler(Handler ch) {
        removeHandlers();
        LOGGER.addHandler(ch);
    }

}
