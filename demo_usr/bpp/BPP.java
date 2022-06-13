// BPP.java
// Author: Stuart Clayman
// Email: s.clayman@ucl.ac.uk
// Date: August 2021

package demo_usr.bpp;

/*
 * A BPP Packet
 *
 * Overview:
 * BPP Block Header:  4 bytes (32 bits)
 *  - 32 bits (mix of fields)
 *
 * Command Block:     3 bytes (24 bits)
 *  - 5 bits (Command) + 8 bits (Condition) + 8 bits (Threshold) + 3 bits (PAD)
 *
 * Metadata Block:    6 bytes (48 bits) times no of chunks
 *  - 22 bits (OFFi [5 bits (Chunk Offset) + 12 bits (Source Frame No) + 5 bits (Frag No)])
 *    + 14 bits (CSi) + 4 bits (SIGi) + 1 bit (OFi) + 1 bit (FFi)
 *    + 1 bit (1 = VCL / 0 = NONVCL)
 *    + 5 bits (PAD)
 
    +------------------+--------------------+--------------------+-----------------+
    | BPP Block Header (32 bits)                                                   *
    +------------------+--------------------+--------------------+-----------------+
    | Command (5) |  Condition (8) | Threshold (8)   |  PAD (3)  *SeqNo (8)        *
    +------------------+--------------------+--------------------+-----------------+
    | SeqNo (24)                                                 *   OFFi (8)      |
    +------------------+--------------------+--------------------+-----------------+
    | OFFi (14)                       | CSi (14)                        | SIGi (4) |
    +------------------+--------------------+--------------------+-----------------+
    | O F V | P(5) *  <NEXT>                                                   |
    +------------------+--------------------+--------------------+-----------------+
      ^ ^ ^
      |  \  ----------\
  OFi (1)  FFi (1)    VCL(1)


// Chunk count is written in the Metadata offset field. The size of the field is 5 bits. 
// CSi is Chunk Sizei
// Checksum is in BPP Block Header
*/
public class BPP {

    public static final int BLOCK_HEADER_SIZE = 4;

    public static final int COMMAND_BLOCK_SIZE = 7;

    public static final int METADATA_BLOCK_SIZE = 6;
}
