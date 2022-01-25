// NALType.java
// Author: Stuart Clayman
// Email: s.clayman@ucl.ac.uk
// Date: August 2021


package demo_usr.bpp;


/**
 * This holds info about the temporal layer relationships
 * for encodings with groups of 16 GOBs.
 */
public enum NALType {
    // NALTypes
    VCL(0),                      // NALType 0 VCL - video
    NONVCL(1);                   // NALType 1 non-VCL


    private final int value;

    NALType(final int length) {
        value = length;
    }

    public int getValue() { return value; }    
}
