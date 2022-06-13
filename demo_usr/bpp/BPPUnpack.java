package demo_usr.bpp;

import java.io.PrintStream;
import usr.net.Datagram;
import usr.protocol.Protocol;
import usr.common.ANSI;

/**
 * Unpack a packet containing BPP data
 */
public class BPPUnpack {
    // Available bandwidth (in bytes)
    int availableBandwidth = 0;
    // Available bandwidth (in bits)
    int availableBandwidthBits = 0;

    int packetsPerSecond = 0;
    
    // counts
    int count = 0;
    int chunkCount = 0;
    int totalIn = 0;
    int totalOut = 0;
    int countThisSec = 0;  // packet count this second
    int recvThisSec = 0;   // amount sent this second
    int sentThisSec = 0;   // amount sent this second


    // timing
    int seconds = 0;       // no of seconds
    long secondStart = 0;   // when did the second start
    long now = 0;
    long timeOffset = 0;


    // payload
    byte[] payload = null;
    int packetLength = 0;

    
    // Datagram contents
    int command = 0;
    int condition = 0;
    int threshold = 0;
    int sequence = 0;

    int [] contentSizes = null;
    int [] contentStartPos = null;
    int [] significance = null;
    int [] fragments = null;
    boolean [] lastFragment = null;
    boolean [] isDropped = null;
    int nalCount = 0;
    int nalNo = 0;
    NALType nalType = null;


    // PrintStream
    PrintStream outStream;

    public BPPUnpack(int availableBandwidthBits, int packetsPerSecond, PrintStream outStream) {
        this.availableBandwidthBits = availableBandwidthBits;
        this.availableBandwidth = availableBandwidthBits >> 3;
        this.packetsPerSecond = packetsPerSecond;
        this.outStream = outStream;

        // set secondStart
        secondStart = System.currentTimeMillis();
        
    }

    /**
     * Unpack a Datagram
     * @throws UnsupportedOperationException if it can't work out what to do
     */
    public byte[] convert(int count, Datagram packet) throws UnsupportedOperationException {
        return convertB(count, packet);
    }


    /**
     * Unpack a Datagram
     * Based on bandwidth
     * @throws UnsupportedOperationException if it can't work out what to do
     */
    public byte[] convertB(int count, Datagram packet) throws UnsupportedOperationException {
        this.count = count;
        countThisSec++;
        
        // timing
        now = System.currentTimeMillis();
        timeOffset = now - secondStart;
        float secondPart = (float)timeOffset / 1000;
        
        // check bandwidth is enough
        payload = packet.getPayload();
        packetLength = payload.length;

        totalIn += packetLength;
        recvThisSec += packetLength;

        int idealSendThisSec = (int) (availableBandwidth * secondPart);
        int behind = idealSendThisSec - sentThisSec;

        if (Verbose.level >= 2) {
            outStream.printf("BPPUnpack: " + count + " secondPart: " + secondPart + " countThisSec " + countThisSec +  " recvThisSec " + recvThisSec + " idealSendThisSec " + idealSendThisSec + " behind " + behind + " sentThisSec " + sentThisSec);
        }

        
        if (timeOffset >= 1000) {
            // we crossed a second boundary
            seconds++;
            secondStart = now;
            countThisSec = 0;
            recvThisSec = 0;
            sentThisSec = 0;
            secondPart = 0;
        }


        // work out packetDropLevel
        int packetDropLevel = 0;

        if (behind > 0) {
            // fine
            packetDropLevel = 0;
            if (Verbose.level >= 2) {
                outStream.printf(" NO DROP\n");
            }
        } else {
            packetDropLevel =  behind;
            if (Verbose.level >= 2) {
                outStream.printf("  drop " + packetDropLevel + "\n");
            }
        }


        // Look into the packet headers
        unpackDatagramHeaders(packet);

        if (packetDropLevel < 0) {
            // If we need to drop something, we need to look at the content
            unpackDatagramContent(packet);

            int droppedAmount = dropContent(-packetDropLevel);

            int size = packetLength - droppedAmount;
            totalOut += size;
            sentThisSec += size;

            // Now rebuild the packet payload, from the original packet
            byte[] newPayload = packContent();

            return newPayload;

        } else {
            // Get the network function to forward the packet
            int size = packetLength;
            totalOut += size;
            sentThisSec += size;
            return null;
        }

        
    }

    

    /**
     * Unpack a Datagram
     * Used in recent papers
     * @throws UnsupportedOperationException if it can't work out what to do
     */
    public byte[] convert0(int count, Datagram packet) throws UnsupportedOperationException {
        this.count = count;

        // check bandwidth is enough
        payload = packet.getPayload();
        packetLength = payload.length;

        totalIn += packetLength;

        int averagePacketLen = totalIn / count;

        // Look into the packet headers
        unpackDatagramHeaders(packet);

        int cond = packetsPerSecond;
        
        int packetDropLevel = ((packetLength * 8) - (availableBandwidthBits / cond)) / 8;

        

        if (Verbose.level >= 2) {
            outStream.printf("BPPUnpack: " + count + ": availableBandwidthBits: %-10d availableBandwidth: %-10d len: %-5d, avg: %-5d condition: %-5d packetDropLevel: %d bytes\n", availableBandwidthBits, availableBandwidth, packetLength, averagePacketLen, cond, packetDropLevel);
        }


        if (availableBandwidth < (cond * averagePacketLen)) {
            // If we need to drop something, we need to look at the content
            unpackDatagramContent(packet);

            int droppedAmount = dropContent(-packetDropLevel);

            // Now rebuild the packet payload
            byte[] newPayload = packContent();

            return newPayload;

        } else {
            // Get the network function to forward the packet
            return null;
        }

        
    }

    /**
     * Unpack a Datagram
     * Similar to algorithm used in early papers
     * @throws UnsupportedOperationException if it can't work out what to do
     */
    public byte[] convert1(int count, Datagram packet) throws UnsupportedOperationException {
        this.count = count;

        // check bandwidth is enough
        payload = packet.getPayload();
        packetLength = payload.length;

        // Look into the packet headers
        unpackDatagramHeaders(packet);

        // trafficDropLevel is no of Kbps to drop in a second
        int trafficDropLevel = (availableBandwidthBits / 1024) - (condition * 10);

        // we need to convert packetDropLevel, which is in Kbps, to a level for each packet
        // need packets / per second to workout the correct trim level
        int packetDropLevel = ((trafficDropLevel * 1024) / 8) / packetsPerSecond;


        if (Verbose.level >= 2) {

            //outStream.printf("BPPUnpack: " + count + ": availableBandwidthBits (Kbps): %-10d condition (Kbps): %-5d trafficDropLevel (Kbps): %d\n", availableBandwidthBits / 1024, condition * 10, trafficDropLevel);
            //outStream.printf("BPPUnpack: " + count + ": trafficDropLevel (bits): %d trafficDropLevel (bytes): %d\n", trafficDropLevel * 1024, (trafficDropLevel * 1024) / 8);

            outStream.printf("BPPUnpack: " + count + ": availableBandwidthBits: %-10d availableBandwidth: %-10d len: %-5d, condition: %-5d packetDropLevel: %d bytes\n", availableBandwidthBits, availableBandwidth, packetLength, condition, packetDropLevel);
        }


        if (packetDropLevel < 0) {
            // If we need to drop something, we need to look at the content
            unpackDatagramContent(packet);

            int droppedAmount = dropContent(-packetDropLevel);

            // Now rebuild the packet payload, from the original packet
            byte[] newPayload = packContent();

            return newPayload;

        } else {
            // Get the network function to forward the packet
            return null;
        }

        
    }

    /**
     * Set no of packetsPerSecond
     */
    public void setPacketsPerSecond(int val) {
        packetsPerSecond = val;
    }

    /**
     * Unpack  the Datagram into objects
     */
    protected void unpackDatagramHeaders(Datagram packet) {
        
        byte[] packetBytes = payload;

        /* Unpack the Datagram */
        int bufPos = 0;
        
        // Now extract BPP header - 32 bits for BPP
        byte b0 = packetBytes[0];
        byte b1 = packetBytes[1];
        byte b2 = packetBytes[2];
        byte b3 = packetBytes[3];

        bufPos += BPP.BLOCK_HEADER_SIZE;

        // Check version pattern
        int version = (b0 & 0xF0) >> 4;
        chunkCount = (b2 & 0xF8) >> 3;

        //System.err.printf(" 0x%02X 0x%02X 0x%02X 0x%02X \n",  packetBytes[0], packetBytes[1], packetBytes[2], packetBytes[3]);

        // Now extract the Command Block
        byte b4 = packetBytes[4];
        byte b5 = packetBytes[5];
        byte b6 = packetBytes[6];

        // Get the Sequence No bytes
        byte b7 = packetBytes[7];
        byte b8 = packetBytes[8];
        byte b9 = packetBytes[9];
        byte b10 = packetBytes[10];
        
        bufPos += BPP.COMMAND_BLOCK_SIZE;
        

        // command is top 5 bits of b4
        command = (b4 & 0xFC) >> 3;

        // condition is bottom 3 bits of b4 and top 5 bits of b5
        condition = ((b4 & 0x07) << 5) | (b5 & 0xFC) >> 3;

        // threshold is bottom 3 bits of b5 and top 5 bits of b6
        threshold = ((b5 & 0x07) << 5) | (b6 & 0xFC) >> 3;
        
        // sequence no
        sequence = ((b7 & 0xFF) << 24) | ((b8  & 0xFF) << 16) | ((b9  & 0xFF) << 8) | (b10  & 0xFF) ;
        

        //System.err.printf("%-6d ver: 0x%04X chunkCount: %d command: 0x%05X condition: %d threshold: %d\n", count, version, chunkCount, command, condition, threshold);
    }

    
    protected void unpackDatagramContent(Datagram packet) {
        
        byte[] packetBytes = payload;

        int bufPos = 0;
        
        // skip the Block Header
        bufPos += BPP.BLOCK_HEADER_SIZE;
        // skip the Command Block
        bufPos += BPP.COMMAND_BLOCK_SIZE;

        // Visit each ChunkContent in the packet
        // and try to get the data out
        contentSizes = new int[chunkCount];
        contentStartPos = new int[chunkCount];
        significance = new int[chunkCount];
        fragments = new int[chunkCount];
        lastFragment = new boolean[chunkCount];
        isDropped = new boolean[chunkCount];

        
        for (int c=0; c<chunkCount; c++) {
        
            // Find per-chunk Metadata Block - 48 bits / 6 bytes 
            //  -  22 bits (OFFi [5 bits (Chunk Offset) + 12 bits (Source Frame No) + 5 bits (Frag No)])
            //   + 14 bits (CSi) + 4 bits (SIGi) + 1 bit (OFi) + 1 bit (FFi)
            //   + 6 bits (PAD)
                
            int offI = 0;
            int csI = 0;
            int sigI = 0;
            int fragment = 0;
            boolean ofI = false;
            boolean ffI = false;

            // first get bytes into structural elements

            // OFFi
            // 8 bits
            offI =  ((packetBytes[bufPos] & 0xFF) << 14);
            // 8 bits
            offI |= ((packetBytes[bufPos+1] & 0xFF) << 6);
            // 6 bits
            offI |= ((packetBytes[bufPos+2] & 0xFC) >> 2);

            //System.err.printf(" offI = %d  0x%5X \n", offI, offI);
            
            // CSi
            // 2 bits
            csI = ((packetBytes[bufPos+2] & 0x3) << 12);
            // 8 bits
            csI |= ((packetBytes[bufPos+3] & 0xFF) << 4);
            // 4 bits
            csI |= ((packetBytes[bufPos+4] & 0xF0) >> 4);

            // SIGi
            sigI = (packetBytes[bufPos+4] & 0x0F);

            ofI = (packetBytes[bufPos+5] & 0x80) == 0 ? false : true;
            ffI = (packetBytes[bufPos+5] & 0x40) == 0 ? false : true;

            int type = ((packetBytes[bufPos+5] & 0x20) >> 5) ;

            bufPos += BPP.METADATA_BLOCK_SIZE;
            
            // now unpack values
            nalCount = (offI >> 17) & 0x0000001F;
            nalNo = (offI >> 5) & 0x00000FFF;
            fragment = (offI & 0x0000001F);
        
            if (type == 0 || type == 1)  {
                nalType = (type == 0 ? NALType.VCL : NALType.NONVCL);
            } else {
                throw new Error("Invalid NALType number " + type);
            }
            
            
            //System.err.printf("  %-3dOFFi: nalNo: %d nalCount: %d fragment: %d \n", (c+1), nalNo, nalCount, fragment);
            //System.err.printf("     CSi: contentSize: %d  SIGi:  %d\n", csI, sigI);
            //System.err.printf("     OFi: %s  FFi: %s  NAL: %s\n", ofI, ffI, nalType);

            // save the contentSize
            contentSizes[c] = csI;

            // fragmentation info
            fragments[c] = fragment;
            lastFragment[c] = ffI;

            // significance
            significance[c] = sigI;

            // dropped
            isDropped[c] = ofI;
        }

        // Save start point of each content
        // bufPos now should be at first content

        for (int c=0; c<chunkCount; c++) {
            contentStartPos[c] = bufPos;

            if (Verbose.level >= 2) {
                //outStream.printf(" 0x%02X 0x%02X 0x%02X 0x%02X \n",  packetBytes[bufPos+0], packetBytes[bufPos+1], packetBytes[bufPos+2], packetBytes[bufPos+3]);
                //outStream.printf(" 0x%02X 0x%02X 0x%02X 0x%02X \n",  packetBytes[bufPos+4], packetBytes[bufPos+5], packetBytes[bufPos+6], packetBytes[bufPos+7]);
            }
            
            bufPos += contentSizes[c];
        }        

    }


    /**
     * Drop some content chunks
     */
    protected int dropContent(int packetDropLevel) {
        if (Verbose.level >= 3) {
            outStream.println("BPPUnpack: " + count + " Trim needed of " + packetDropLevel);
        }
            
        int dropped = 0;
            
        // now we try to drop some chunk content
        // try from the highest to the lowest
        for (int c=chunkCount-1; c>=0; c--) {
            if (Verbose.level >= 3) {
                outStream.println("isDropped[" + c + "] = " + isDropped[c]);
            }

            // can we delete this content
            
            if (significance[c] > threshold) {
                // it's a candidate
                // mark it as dropped
                isDropped[c] = true;
                // update the dropped count
                dropped += contentSizes[c];

                if (Verbose.level >= 3) {
                    outStream.println("BPPUnpack: dropped chunk " + c + " significance " + significance[c] + " size: " + contentSizes[c]);
                }
            }

            if (dropped >= packetDropLevel) {
                // we've achieved the target
                // so no need to do any more
                break;
            }
        }

        if (Verbose.level >= 3) {
            outStream.println("BPPUnpack: dropped " + dropped);
        }
        
        return dropped;
    }            

    /**
     * Pack some headers and content into a new BPP byte[]
     */
    protected byte[] packContent() {

        // The new size is the incoming size - the dropped content chunks
        int droppedSize = 0;

        for (int c=0; c<chunkCount; c++) {
            if (isDropped[c]) {
                droppedSize += contentSizes[c];
            }
        }
        
        byte[] packetBytes = new byte[packetLength - droppedSize];

        int bufPos = 0;


        // 32 bits for BPP header
        packetBytes[0] = (byte)((0x0C << 4) & 0xFF);
        packetBytes[1] = (byte)(0x00);
        packetBytes[2] = (byte)((chunkCount & 0x3F) << 3);
        packetBytes[3] = (byte)(0x00);

        // increase bufPos
        bufPos += BPP.BLOCK_HEADER_SIZE;


        // 24 bits for Command Block
        // build int, then pack into bytes
        int commandBlock = 0;

        commandBlock = (command << 19) | ((byte)(condition & 0x000000FF) << 11) | ((byte)(threshold & 0x000000FF) << 3);
            
        packetBytes[4] = (byte)(((commandBlock & 0x00FF0000) >> 16) & 0xFF);
        packetBytes[5] = (byte)(((commandBlock & 0x0000FF00) >> 8) & 0xFF);
        packetBytes[6] = (byte)(((commandBlock & 0x000000FF) >> 0) & 0xFF);

        // Add Sequence no
        packetBytes[7] = (byte)(((sequence & 0xFF000000) >> 24) & 0xFF);
        packetBytes[8] = (byte)(((sequence & 0x00FF0000) >> 16) & 0xFF);
        packetBytes[9] = (byte)(((sequence & 0x0000FF00) >> 8) & 0xFF);
        packetBytes[10] = (byte)(((sequence & 0x000000FF) >> 0) & 0xFF);
            
        if (Verbose.level >= 2) {
            outStream.println("Chunk data: seq: " + sequence + " nalNo: " + nalNo + " nalCount: " + nalCount);
        }

        // increase bufPos
        bufPos += BPP.COMMAND_BLOCK_SIZE;

        // Visit the Content
        for (int c=0; c<chunkCount; c++) {
                
            // Get the payload info
            int contentSize = contentSizes[c];

            // get fragment from content
            int fragment = fragments[c];
            boolean isLastFragment = lastFragment[c];
            boolean isDroppedChunk = isDropped[c];


            if (isDroppedChunk) {
                // it's dropped, so send no content
                // set contentSize to 0
                contentSize = 0;
            }

            //System.err.println("BPP: contentSize = " + contentSize);


            // Add per-chunk Metadata Block - 48 bits / 6 bytes 
            //  -  22 bits (OFFi [5 bits (Chunk Offset) + 12 bits (Source Frame No) + 5 bits (Frag No)])
            //   + 14 bits (CSi) + 4 bits (SIGi) + 1 bit (OFi) + 1 bit (FFi)
            //   + 6 bits (PAD)
                
            int offI = 0;
            int csI = 0;
            // significance probably calculated on-the-fly, from the NAL
            int sigI = significance[c];
                
            
            offI = ((nalCount & 0x0000001F) << 17) | ((nalNo & 0x00000FFF) << 5) | ((fragment & 0x0000001F) << 0);

            //System.err.printf(" offI = %d  0x%5X \n", offI, offI);

            // chunk size - 14 bits
            csI = (contentSize & 0x00003FFF);

            // now build the next 6 bytes
                
            // need 8 bits: 14 - 21 of offI
            packetBytes[bufPos] = (byte)(((offI & 0x003FC000) >> 14) & 0xFF);
            // need 8 bits: 6 - 13 of offI
            packetBytes[bufPos+1] = (byte)(((offI & 0x00003FC0) >> 6) & 0xFF);
            // need 6 bits: 0 - 5 of offI
            packetBytes[bufPos+2] = (byte)((((offI & 0x0000003F) >> 0) << 2) & 0xFF);


            // need 2 bits: 12 - 13 of csI
            packetBytes[bufPos+2] |= (byte)(((csI & 0x00003000) >> 12) & 0x03);
            // need 8 bits: 4 - 11 of csI
            packetBytes[bufPos+3] = (byte)(((csI &  0x00000FF0) >> 4) & 0xFF);
            // need 4 bits: 0 - 3 of csI
            packetBytes[bufPos+4] = (byte)((((csI &  0x0000000F) >> 0) << 4) & 0xFF);

            // need 4 bits: 0 - 3 of sigI
            packetBytes[bufPos+4] |= (byte)(((sigI & 0x0000000F) >> 0) & 0x0F);

            // need 1 bit for OFi
            packetBytes[bufPos+5] = (byte)(((isDroppedChunk ? 1 : 0) << 7) & 0xFF);
            // need 1 bit for FFi
            packetBytes[bufPos+5] |= (byte)(((isLastFragment ? 1 : 0) << 6) & 0xFF);
            // need 1 bit for VCL/NONVCL
            packetBytes[bufPos+5] |= (byte)((nalType.getValue() & 0x01) << 5);

            // need 5 bits of PAD

            // increase bufPos
            bufPos += BPP.METADATA_BLOCK_SIZE;

        }


        // Now add in the content
        for (int c=0; c<chunkCount; c++) {
            boolean isDroppedChunk = isDropped[c];
                
            if (!isDroppedChunk) {
                // send content chunks which are not dropped
                // now add the bytes to the packetBytes
                // source_arr,  sourcePos,  dest_arr,  destPos, len
                System.arraycopy(payload, contentStartPos[c], packetBytes, bufPos, contentSizes[c]);

                if (Verbose.level >= 3) {
                    //outStream.printf(" 0x%02X 0x%02X 0x%02X 0x%02X \n",  packetBytes[bufPos+0], packetBytes[bufPos+1], packetBytes[bufPos+2], packetBytes[bufPos+3]);
                    //outStream.printf(" 0x%02X 0x%02X 0x%02X 0x%02X \n",  packetBytes[bufPos+4], packetBytes[bufPos+5], packetBytes[bufPos+6], packetBytes[bufPos+7]);
                }
                
                bufPos += contentSizes[c];

            } else {
                if (Verbose.level >= 3) {
                    outStream.println("packContent: content " + c + " is dropped");
                }
            }

        }

        //outStream.println("BPP: bufPos = " + bufPos);
                
        return packetBytes;
        
    }

}
