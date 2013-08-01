package usr.common;

/**
 * Generate some ANSI terminal codes.
 */
public class ANSI {
    private ANSI() {
    }

    // There is a single-character CSI (155/0x9B/0233) as well.
    // The ESC+[ \033 two-character sequence is more often used

    static String CSI = "\033[";

    /**
     * Full Reset
     */
    public static String FULL_RESET = CSI + "c";

    /**
     * Reset the colours
     */
    public static String RESET_COLOUR = CSI + "0m";

    /**
     * Bright / bold colours
     */
    public static String BRIGHT_COLOUR = CSI + "1m";

    /**
     * Faint colours
     */
    public static String FAINT_COLOUR = CSI + "2m";

    /**
     * Crossed out
     */
    public static String CROSSED = CSI + "9m";

    /**
     * Bright / bold colours off
     */
    public static String BRIGHT_OFF = CSI + "21m";

    /**
     * Normal intensity - bo bright, bold or faint
     */
    public static String FAINT_OFF = CSI + "22m";

    /**
     * Crossed out off
     */
    public static String CROSSED_OFF = CSI + "29m";


    // 30-37	Set text color	30 + x, where x is from the color table
    // Black	Red	Green	Yellow	Blue	Magenta	Cyan	White

    /**
     * Black
     */
    public static String BLACK = CSI + "30m";

    /**
     * Red
     */
    public static String RED = CSI + "31m";

    /**
     * Green
     */
    public static String GREEN = CSI + "32m";

    /**
     * Yellow
     */
    public static String YELLOW = CSI + "33m";

    /**
     * BLue
     */
    public static String BLUE = CSI + "34m";

    /**
     * Magenta
     */
    public static String MAGENTA = CSI + "35m";

    /**
     * Cyan
     */
    public static String CYAN = CSI + "36m";

    /**
     * White
     */
    public static String WHITE = CSI + "37m";


    // 40-47	Set background color	40 + x, where x is from the color table

    /**
     * Black background
     */
    public static String BLACK_BG = CSI + "40m";

    /**
     * Red background
     */
    public static String RED_BG = CSI + "41m";

    /**
     * Green background
     */
    public static String GREEN_BG = CSI + "42m";

    /**
     * Yellow background
     */
    public static String YELLOW_BG = CSI + "43m";

    /**
     * Blue background
     */
    public static String BLUE_BG = CSI + "44m";

    /**
     * Magenta background
     */
    public static String MAGENTA_BG = CSI + "45m";

    /**
     * Cyan background
     */
    public static String CYAN_BG = CSI + "46m";

    /**
     * White background
     */
    public static String WHITE_BG = CSI + "47m";


    /**
     * Underline
     */
    public static String UNDERLINE = CSI + "4m";

    /**
     * Underline off
     */
    public static String UNDERLINE_OFF = CSI + "24m";

    /**
     * Conceal
     */
    public static String CONCEAL = CSI + "8m";

    /**
     * Conceal off
     */
    public static String CONCEAL_OFF = CSI + "28m";

    /**
     * Cursor up
     */
    public static String UP = CSI + "A";

    /**
     * Move cursor up
     */
    public static String UP(int n) {
        return CSI + n + "A";
    }

    /**
     * Cursor down
     */
    public static String DOWN = CSI + "B";

    /**
     * Move cursor down
     */
    public static String DOWN(int n) {
        return CSI + n + "B";
    }

    /**
     * Cursor right
     */
    public static String RIGHT = CSI + "C";

    /**
     * Move cursor right
     */
    public static String RIGHT(int n) {
        return CSI + n + "C";
    }

    /**
     * Cursor left
     */
    public static String LEFT = CSI + "D";

    /**
     * Move cursor left
     */
    public static String LEFT(int n) {
        return CSI + n + "D";
    }

    /**
     * Cursor begining of next line
     */
    public static String CURSOR_NEXT_LINE = CSI + "1E";

    /**
     * Cursor begining of next line
     */
    public static String CURSOR_NEXT_LINE(int n) {
        return CSI + n + "E";
    }

    /**
     * Cursor begining of previous line
     */
    public static String CURSOR_PREV_LINE = CSI + "1F";

    /**
     * Cursor begining of previous line
     */
    public static String CURSOR_PREV_LINE(int n) {
        return CSI + n + "F";
    }

    /**
     * Cursor goto column
     */
    public static String COLUMN(int n) {
        return CSI + n + "G";
    }

    /**
     * Clear to end of screen.
     */
    public static String CLEAR_EOS = CSI + "0J";

    /**
     * Clear to top of screen.
     */
    public static String CLEAR_TOS = CSI + "1J";

    /**
     * Clear whole screen.
     */
    public static String CLEAR = CSI + "2J";

    /**
     * Clear to end of line.
     */
    public static String CLEAR_EOL = CSI + "0K";

    /**
     * Clear to begining of line.
     */
    public static String CLEAR_BOL = CSI + "1K";

    /**
     * Clear whole line.
     */
    public static String CLEAR_LINE = CSI + "2K";

    /**
     * Move cursor to a position
     */
    public static String POS(int n, int m) {
        return CSI + n +";" + m +"H";
    }

    /**
     * Scroll up
     */
    public static String SCROLL_UP = CSI + "1S";

    /**
     * Scroll up
     */
    public static String SCROLL_UP(int n) {
        return CSI + n + "S";
    }

    /**
     * Scroll down
     */
    public static String SCROLL_DOWN = CSI + "1T";

    /**
     * Scroll down
     */
    public static String SCROLL_DOWN(int n) {
        return CSI + n + "T";
    }

}