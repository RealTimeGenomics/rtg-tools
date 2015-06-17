/*
 * Copyright (c) 2014. Real Time Genomics Limited.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This package is based on the work done by Keiron Liddle, Aftex Software
 * <keiron@aftexsw.com> to whom the Ant project is very grateful for his
 * great code.
 *
 * Modified by RTG.
 */
package com.rtg.util.io.bzip2;

import static com.rtg.util.io.bzip2.BZip2Constants.G_SIZE;
import static com.rtg.util.io.bzip2.BZip2Constants.MAX_ALPHA_SIZE;
import static com.rtg.util.io.bzip2.BZip2Constants.MAX_CODE_LEN;
import static com.rtg.util.io.bzip2.BZip2Constants.MAX_SELECTORS;
import static com.rtg.util.io.bzip2.BZip2Constants.N_GROUPS;
import static com.rtg.util.io.bzip2.BZip2Constants.RUNA;
import static com.rtg.util.io.bzip2.BZip2Constants.RUNB;

import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream that decompresses from the <code>BZip2</code> format (without the file
 * header chars) to be read as any other stream.
 *
 * <p>The decompression requires large amounts of memory. Thus you
 * should call the {@link #close() close()} method as soon as
 * possible, to force <tt>CBZip2InputStream</tt> to release the
 * allocated memory. </p>
 *
 * <p><tt>CBZip2InputStream</tt> reads bytes from the compressed
 * source stream via the single byte {@link java.io.InputStream#read()
 * read()} method exclusively. Thus you should consider to use a
 * buffered source stream.</p>
 *
 * <p>Instances of this class are not thread safe.</p>
 */
public class CBZip2InputStream extends InputStream {

    private static void reportCRCError() throws IOException {
        // The clean way would be to throw an exception.
        throw new IOException("BZip2 crc error");
    }

    private void makeMaps() {
        final boolean[] inUse   = this.mData.mInUse;
        final byte[] seqToUnseq = this.mData.mSeqToUnseq;

        int nInUseShadow = 0;

        for (int i = 0; i < 256; i++) {
          if (inUse[i]) {
            seqToUnseq[nInUseShadow++] = (byte) i;
          }
        }

        this.mNInUse = nInUseShadow;
    }

    /**
     * Index of the last char in the block, so the block size == last + 1.
     */
    private int  mLast;

    /**
     * Index in <code>zptr[]</code> of original string after sorting.
     */
    private int  mOrigPtr;

    /**
     * always: in the range 0 .. 9.
     * The current block size is 100000 * this number.
     */
    private int mBlockSize100k;

    private boolean mBlockRandomised;

    private int mBsBuff;
    private int mBsLive;
    private final CRC mCrc = new CRC();

    private int mNInUse;

    private InputStream mIn;

    private int mCurrentChar = -1;

    private static final int EOF                  = 0;
    private static final int START_BLOCK_STATE = 1;
    private static final int RAND_PART_A_STATE = 2;
    private static final int RAND_PART_B_STATE = 3;
    private static final int RAND_PART_C_STATE = 4;
    private static final int NO_RAND_PART_A_STATE = 5;
    private static final int NO_RAND_PART_B_STATE = 6;
    private static final int NO_RAND_PART_C_STATE = 7;

    private int mCurrentState = START_BLOCK_STATE;

    private int mStoredBlockCrc, mStoredCombinedCrc;
    private int mComputedBlockCrc, mComputedCombinedCrc;

    // Variables used by setup* methods exclusively

    private int mSuCount;
    private int mSuCh2;
    private int mSuChPrev;
    private int mSuI2;
    private int mSuJ2;
    private int mSuRNToGo;
    private int mSuRTPos;
    private int mSuTPos;
    private char mSuZ;

    /**
     * All memory intensive stuff.
     * This field is initialized by <code>initBlock()</code>.
     */
    private CBZip2InputStream.Data mData;

    /**
     * Constructs a new CBZip2InputStream which decompresses bytes read from
     * the specified stream.
     *
     * @param in stream to decompress
     * @throws IOException
     *  if the stream content is malformed or an I/O error occurs.
     * @throws NullPointerException
     *  if <tt>in == null</tt>
     */
    public CBZip2InputStream(final InputStream in) throws IOException {
        super();

        this.mIn = in;
        init();
    }

    @Override
    public int read() throws IOException {
        if (this.mIn != null) {
            return read0();
        } else {
            throw new IOException("stream closed");
        }
    }

    @Override
    public int read(final byte[] dest, final int offs, final int len)
        throws IOException {
        if (offs < 0) {
            throw new IndexOutOfBoundsException("offs(" + offs + ") < 0.");
        }
        if (len < 0) {
            throw new IndexOutOfBoundsException("len(" + len + ") < 0.");
        }
        if (offs + len > dest.length) {
            throw new IndexOutOfBoundsException("offs(" + offs + ") + len("
                                                + len + ") > dest.length("
                                                + dest.length + ").");
        }
        if (this.mIn == null) {
            throw new IOException("stream closed");
        }

        final int hi = offs + len;
        int destOffs = offs;
        for (int b; (destOffs < hi) && ((b = read0()) >= 0);) {
            dest[destOffs++] = (byte) b;
        }

        return (destOffs == offs) ? -1 : (destOffs - offs);
    }

    private int read0() throws IOException {
        final int retChar = this.mCurrentChar;

        switch (this.mCurrentState) {
        case EOF:
            return -1;

        case START_BLOCK_STATE:
            throw new IllegalStateException();

        case RAND_PART_A_STATE:
            throw new IllegalStateException();

        case RAND_PART_B_STATE:
            setupRandPartB();
            break;

        case RAND_PART_C_STATE:
            setupRandPartC();
            break;

        case NO_RAND_PART_A_STATE:
            throw new IllegalStateException();

        case NO_RAND_PART_B_STATE:
            setupNoRandPartB();
            break;

        case NO_RAND_PART_C_STATE:
            setupNoRandPartC();
            break;

        default:
            throw new IllegalStateException();
        }

        return retChar;
    }

    private void checkMagicHeader(char expected, int actual, int position) throws IOException {
      final int magic2 = (char) actual;
      if (magic2 != expected) {
        final String positionString = position != -1 ? (" at position " + position) : "";
        throw new IOException("Stream is not BZip2 formatted: expected '" + expected + "'"
                + " as byte" + positionString + " but got '" + (char) magic2
                + "'");
      }
    }
    private void init() throws IOException {
        if (null == mIn) {
            throw new IOException("No InputStream");
        }
        if (mIn.available() == 0) {
            throw new IOException("Empty InputStream");
        }
        checkMagicHeader('B', this.mIn.read(), 0);
        checkMagicHeader('Z', this.mIn.read(), 1);
        checkMagicHeader('h', this.mIn.read(), 2);

        final int blockSize = this.mIn.read();
        if ((blockSize < '1') || (blockSize > '9')) {
            throw new IOException("Stream is not BZip2 formatted: illegal "
                                  + "blocksize " + (char) blockSize);
        }

        this.mBlockSize100k = blockSize - '0';

        initBlock();
        setupBlock();
    }

    /**
     * Read a single byte, if available from the <code>mBsBuff</code> then read from
     * it, otherwise read straight from <code>mIn</code>. If discard is true we discard the first
     * up to 7 bits to become byte aligned again.
     * @return as with {@link InputStream#read()}
     * @throws IOException when an IO error occurs
     * @throws IllegalStateException if we have &gt; 0 but &lt; 8 bits of data available
     */
    private int fileBoundaryRead(boolean discard) throws IOException {
      if (discard) {
        bsR(this.mBsLive & 7); //discard padding bits
      }
      if (this.mBsLive >= 8) {
        return bsR(8);
      } else if (this.mBsLive > 0) {
        throw new IllegalStateException("Bad input buffer state in Bzip2 decompression");
      }
      return this.mIn.read();
    }

    private boolean checkFileBoundaryHeader() throws IOException {
      final int c = fileBoundaryRead(true);
      if (c != -1) {
        checkMagicHeader('B', c, -1);
        checkMagicHeader('Z', fileBoundaryRead(false), -1);
        checkMagicHeader('h', fileBoundaryRead(false), -1);
        final int blockSize = fileBoundaryRead(false);
        if ((blockSize < '1') || (blockSize > '9')) {
            throw new IOException("Stream is not BZip2 formatted: illegal "
                                  + "blocksize " + (char) blockSize);
        }
        //reset variables
        this.mBlockSize100k = blockSize - '0';
        mBsBuff = 0;
        mBsLive = 0;
        mStoredBlockCrc = 0;
        mStoredCombinedCrc = 0;
        mComputedBlockCrc = 0;
        mComputedCombinedCrc = 0;
        return true;
      }
      return false;
    }

    private void initBlock() throws IOException {
        final char magic0 = bsGetUByte();
        final char magic1 = bsGetUByte();
        final char magic2 = bsGetUByte();
        final char magic3 = bsGetUByte();
        final char magic4 = bsGetUByte();
        final char magic5 = bsGetUByte();

        if (magic0 == 0x17
            && magic1 == 0x72
            && magic2 == 0x45
            && magic3 == 0x38
            && magic4 == 0x50
            && magic5 == 0x90) {
            complete(); // end of file
            if (checkFileBoundaryHeader()) {
              initBlock();
            }
        } else if (magic0 != 0x31 || // '1'
                   magic1 != 0x41 || // ')'
                   magic2 != 0x59 || // 'Y'
                   magic3 != 0x26 || // '&'
                   magic4 != 0x53 || // 'S'
                   magic5 != 0x59   // 'Y'
                   ) {
            this.mCurrentState = EOF;
            throw new IOException("bad block header");
        } else {
            this.mStoredBlockCrc = bsGetInt();
            this.mBlockRandomised = bsR(1) == 1;

            /**
             * Allocate data here instead in constructor, so we do not
             * allocate it if the input file is empty.
             */
            if (this.mData == null) {
                this.mData = new Data(this.mBlockSize100k);
            }

            // currBlockNo++;
            getAndMoveToFrontDecode();

            this.mCrc.initialiseCRC();
            this.mCurrentState = START_BLOCK_STATE;
        }
    }

    private void endBlock() throws IOException {
        this.mComputedBlockCrc = this.mCrc.getFinalCRC();

        // A bad CRC is considered a fatal error.
        if (this.mStoredBlockCrc != this.mComputedBlockCrc) {
            // make next blocks readable without error
            // (repair feature, not yet documented, not tested)
            this.mComputedCombinedCrc
                = (this.mStoredCombinedCrc << 1)
                | (this.mStoredCombinedCrc >>> 31);
            this.mComputedCombinedCrc ^= this.mStoredBlockCrc;

            reportCRCError();
        }

        this.mComputedCombinedCrc
            = (this.mComputedCombinedCrc << 1)
            | (this.mComputedCombinedCrc >>> 31);
        this.mComputedCombinedCrc ^= this.mComputedBlockCrc;
    }

    private void complete() throws IOException {
        this.mStoredCombinedCrc = bsGetInt();
        this.mCurrentState = EOF;
        this.mData = null;

        if (this.mStoredCombinedCrc != this.mComputedCombinedCrc) {
            reportCRCError();
        }
    }

    @Override
    public void close() throws IOException {
        final InputStream inShadow = this.mIn;
        if (inShadow != null) {
            try {
                if (inShadow != System.in) {
                    inShadow.close();
                }
            } finally {
                this.mData = null;
                this.mIn = null;
            }
        }
    }

    private int bsR(final int n) throws IOException {
        int bsLiveShadow = this.mBsLive;
        int bsBuffShadow = this.mBsBuff;

        if (bsLiveShadow < n) {
            final InputStream inShadow = this.mIn;
            do {
                final int thech = inShadow.read();

                if (thech < 0) {
                    throw new IOException("unexpected end of stream");
                }

                bsBuffShadow = (bsBuffShadow << 8) | thech;
                bsLiveShadow += 8;
            } while (bsLiveShadow < n);

            this.mBsBuff = bsBuffShadow;
        }

        this.mBsLive = bsLiveShadow - n;
        return (bsBuffShadow >> (bsLiveShadow - n)) & ((1 << n) - 1);
    }

    private boolean bsGetBit() throws IOException {
        int bsLiveShadow = this.mBsLive;
        int bsBuffShadow = this.mBsBuff;

        if (bsLiveShadow < 1) {
            final int thech = this.mIn.read();

            if (thech < 0) {
                throw new IOException("unexpected end of stream");
            }

            bsBuffShadow = (bsBuffShadow << 8) | thech;
            bsLiveShadow += 8;
            this.mBsBuff = bsBuffShadow;
        }

        this.mBsLive = bsLiveShadow - 1;
        return ((bsBuffShadow >> (bsLiveShadow - 1)) & 1) != 0;
    }

    private char bsGetUByte() throws IOException {
        return (char) bsR(8);
    }

    private int bsGetInt() throws IOException {
        return (((((bsR(8) << 8) | bsR(8)) << 8) | bsR(8)) << 8) | bsR(8);
    }

    /**
     * Called by <code>createHuffmanDecodingTables()</code> exclusively.
     */
    private static void hbCreateDecodeTables(final int[] limit,
                                             final int[] base,
                                             final int[] perm,
                                             final char[] length,
                                             final int minLen,
                                             final int maxLen,
                                             final int alphaSize) {
        for (int i = minLen, pp = 0; i <= maxLen; i++) {
            for (int j = 0; j < alphaSize; j++) {
                if (length[j] == i) {
                    perm[pp++] = j;
                }
            }
        }

        for (int i = MAX_CODE_LEN; --i > 0;) {
            base[i] = 0;
            limit[i] = 0;
        }

        for (int i = 0; i < alphaSize; i++) {
            base[length[i] + 1]++;
        }

        for (int i = 1, b = base[0]; i < MAX_CODE_LEN; i++) {
            b += base[i];
            base[i] = b;
        }

        for (int i = minLen, vec = 0, b = base[i]; i <= maxLen; i++) {
            final int nb = base[i + 1];
            vec += nb - b;
            b = nb;
            limit[i] = vec - 1;
            vec <<= 1;
        }

        for (int i = minLen + 1; i <= maxLen; i++) {
            base[i] = ((limit[i - 1] + 1) << 1) - base[i];
        }
    }

    private void recvDecodingTables() throws IOException {
        final Data dataShadow     = this.mData;
        final boolean[] inUse     = dataShadow.mInUse;
        final byte[] pos          = dataShadow.mRecvDecodingTables_pos;
        final byte[] selector     = dataShadow.mSelector;
        final byte[] selectorMtf  = dataShadow.mSelectorMtf;

        int inUse16 = 0;

        /* Receive the mapping table */
        for (int i = 0; i < 16; i++) {
            if (bsGetBit()) {
                inUse16 |= 1 << i;
            }
        }

        for (int i = 256; --i >= 0;) {
            inUse[i] = false;
        }

        for (int i = 0; i < 16; i++) {
            if ((inUse16 & (1 << i)) != 0) {
                final int i16 = i << 4;
                for (int j = 0; j < 16; j++) {
                    if (bsGetBit()) {
                        inUse[i16 + j] = true;
                    }
                }
            }
        }

        makeMaps();
        final int alphaSize = this.mNInUse + 2;

        /* Now the selectors */
        final int nGroups = bsR(3);
        final int nSelectors = bsR(15);

        for (int i = 0; i < nSelectors; i++) {
            int j = 0;
            while (bsGetBit()) {
                j++;
            }
            selectorMtf[i] = (byte) j;
        }

        /* Undo the MTF values for the selectors. */
        for (int v = nGroups; --v >= 0;) {
            pos[v] = (byte) v;
        }

        for (int i = 0; i < nSelectors; i++) {
            int v = selectorMtf[i] & 0xff;
            final byte tmp = pos[v];
            while (v > 0) {
                // nearly all times v is zero, 4 in most other cases
                pos[v] = pos[v - 1];
                v--;
            }
            pos[0] = tmp;
            selector[i] = tmp;
        }

        final char[][] len  = dataShadow.mTemp_charArray2d;

        /* Now the coding tables */
        for (int t = 0; t < nGroups; t++) {
            int curr = bsR(5);
            final char[] lenT = len[t];
            for (int i = 0; i < alphaSize; i++) {
                while (bsGetBit()) {
                    curr += bsGetBit() ? -1 : 1;
                }
                lenT[i] = (char) curr;
            }
        }

        // finally create the Huffman tables
        createHuffmanDecodingTables(alphaSize, nGroups);
    }

    /**
     * Called by <code>recvDecodingTables()</code> exclusively.
     */
    private void createHuffmanDecodingTables(final int alphaSize,
                                             final int nGroups) {
        final Data dataShadow = this.mData;
        final char[][] len  = dataShadow.mTemp_charArray2d;
        final int[] minLens = dataShadow.mMinLens;
        final int[][] limit = dataShadow.mLimit;
        final int[][] base  = dataShadow.mBase;
        final int[][] perm  = dataShadow.mPerm;

        for (int t = 0; t < nGroups; t++) {
            int minLen = 32;
            int maxLen = 0;
            final char[] lenT = len[t];
            for (int i = alphaSize; --i >= 0;) {
                final char lent = lenT[i];
                if (lent > maxLen) {
                    maxLen = lent;
                }
                if (lent < minLen) {
                    minLen = lent;
                }
            }
            hbCreateDecodeTables(limit[t], base[t], perm[t], len[t], minLen,
                                 maxLen, alphaSize);
            minLens[t] = minLen;
        }
    }

    private void getAndMoveToFrontDecode() throws IOException {
        this.mOrigPtr = bsR(24);
        recvDecodingTables();

        final InputStream inShadow = this.mIn;
        final Data dataShadow   = this.mData;
        final byte[] ll8        = dataShadow.mLl8;
        final int[] unzftab     = dataShadow.mUnzftab;
        final byte[] selector   = dataShadow.mSelector;
        final byte[] seqToUnseq = dataShadow.mSeqToUnseq;
        final char[] yy         = dataShadow.mGetAndMoveToFrontDecode_yy;
        final int[] minLens     = dataShadow.mMinLens;
        final int[][] limit     = dataShadow.mLimit;
        final int[][] base      = dataShadow.mBase;
        final int[][] perm      = dataShadow.mPerm;
        final int limitLast     = this.mBlockSize100k * 100000;

        /*
          Setting up the unzftab entries here is not strictly
          necessary, but it does save having to do it later
          in a separate pass, and so saves a block's worth of
          cache misses.
        */
        for (int i = 256; --i >= 0;) {
            yy[i] = (char) i;
            unzftab[i] = 0;
        }

        int groupNo     = 0;
        int groupPos    = G_SIZE - 1;
        final int eob   = this.mNInUse + 1;
        int nextSym     = getAndMoveToFrontDecode0(0);
        int bsBuffShadow      = this.mBsBuff;
        int bsLiveShadow      = this.mBsLive;
        int lastShadow        = -1;
        int zt          = selector[groupNo] & 0xff;
        int[] baseZt   = base[zt];
        int[] limitZt  = limit[zt];
        int[] permZt   = perm[zt];
        int minLensZt  = minLens[zt];

        while (nextSym != eob) {
            if ((nextSym == RUNA) || (nextSym == RUNB)) {
                int s = -1;

                for (int n = 1; true; n <<= 1) {
                    if (nextSym == RUNA) {
                        s += n;
                    } else if (nextSym == RUNB) {
                        s += n << 1;
                    } else {
                        break;
                    }

                    if (groupPos == 0) {
                        groupPos    = G_SIZE - 1;
                        zt          = selector[++groupNo] & 0xff;
                        baseZt     = base[zt];
                        limitZt    = limit[zt];
                        permZt     = perm[zt];
                        minLensZt  = minLens[zt];
                    } else {
                        groupPos--;
                    }

                    int zn = minLensZt;

                    // Inlined:
                    // int zvec = bsR(zn);
                    while (bsLiveShadow < zn) {
                        final int thech = inShadow.read();
                        if (thech >= 0) {
                            bsBuffShadow = (bsBuffShadow << 8) | thech;
                            bsLiveShadow += 8;
                            continue;
                        } else {
                            throw new IOException("unexpected end of stream");
                        }
                    }
                    int zvec = (bsBuffShadow >> (bsLiveShadow - zn)) & ((1 << zn) - 1);
                    bsLiveShadow -= zn;

                    while (zvec > limitZt[zn]) {
                        zn++;
                        while (bsLiveShadow < 1) {
                            final int thech = inShadow.read();
                            if (thech >= 0) {
                                bsBuffShadow = (bsBuffShadow << 8) | thech;
                                bsLiveShadow += 8;
                                continue;
                            } else {
                                throw new IOException("unexpected end of stream");
                            }
                        }
                        bsLiveShadow--;
                        zvec = (zvec << 1) | ((bsBuffShadow >> bsLiveShadow) & 1);
                    }
                    nextSym = permZt[zvec - baseZt[zn]];
                }

                final byte ch = seqToUnseq[yy[0]];
                unzftab[ch & 0xff] += s + 1;

                while (s-- >= 0) {
                    ll8[++lastShadow] = ch;
                }

                if (lastShadow >= limitLast) {
                    throw new IOException("block overrun");
                }
            } else {
                if (++lastShadow >= limitLast) {
                    throw new IOException("block overrun");
                }

                final char tmp = yy[nextSym - 1];
                unzftab[seqToUnseq[tmp] & 0xff]++;
                ll8[lastShadow] = seqToUnseq[tmp];

                /*
                  This loop is hammered during decompression,
                  hence avoid native method call overhead of
                  System.arraycopy for very small ranges to copy.
                */
                if (nextSym <= 16) {
                    for (int j = nextSym - 1; j > 0;) {
                        yy[j] = yy[--j];
                    }
                } else {
                    System.arraycopy(yy, 0, yy, 1, nextSym - 1);
                }

                yy[0] = tmp;

                if (groupPos == 0) {
                    groupPos    = G_SIZE - 1;
                    zt          = selector[++groupNo] & 0xff;
                    baseZt     = base[zt];
                    limitZt    = limit[zt];
                    permZt     = perm[zt];
                    minLensZt  = minLens[zt];
                } else {
                    groupPos--;
                }

                int zn = minLensZt;

                // Inlined:
                // int zvec = bsR(zn);
                while (bsLiveShadow < zn) {
                    final int thech = inShadow.read();
                    if (thech >= 0) {
                        bsBuffShadow = (bsBuffShadow << 8) | thech;
                        bsLiveShadow += 8;
                        continue;
                    } else {
                        throw new IOException("unexpected end of stream");
                    }
                }
                int zvec = (bsBuffShadow >> (bsLiveShadow - zn)) & ((1 << zn) - 1);
                bsLiveShadow -= zn;

                while (zvec > limitZt[zn]) {
                    zn++;
                    while (bsLiveShadow < 1) {
                        final int thech = inShadow.read();
                        if (thech >= 0) {
                            bsBuffShadow = (bsBuffShadow << 8) | thech;
                            bsLiveShadow += 8;
                            continue;
                        } else {
                            throw new IOException("unexpected end of stream");
                        }
                    }
                    bsLiveShadow--;
                    zvec = (zvec << 1) | ((bsBuffShadow >> bsLiveShadow) & 1);
                }
                nextSym = permZt[zvec - baseZt[zn]];
            }
        }

        this.mLast = lastShadow;
        this.mBsLive = bsLiveShadow;
        this.mBsBuff = bsBuffShadow;
    }

    private int getAndMoveToFrontDecode0(final int groupNo)
        throws IOException {
        final InputStream inShadow  = this.mIn;
        final Data dataShadow  = this.mData;
        final int zt          = dataShadow.mSelector[groupNo] & 0xff;
        final int[] limitZt  = dataShadow.mLimit[zt];
        int zn = dataShadow.mMinLens[zt];
        int zvec = bsR(zn);
        int bsLiveShadow = this.mBsLive;
        int bsBuffShadow = this.mBsBuff;

        while (zvec > limitZt[zn]) {
            zn++;
            while (bsLiveShadow < 1) {
                final int thech = inShadow.read();

                if (thech >= 0) {
                    bsBuffShadow = (bsBuffShadow << 8) | thech;
                    bsLiveShadow += 8;
                    continue;
                } else {
                    throw new IOException("unexpected end of stream");
                }
            }
            bsLiveShadow--;
            zvec = (zvec << 1) | ((bsBuffShadow >> bsLiveShadow) & 1);
        }

        this.mBsLive = bsLiveShadow;
        this.mBsBuff = bsBuffShadow;

        return dataShadow.mPerm[zt][zvec - dataShadow.mBase[zt][zn]];
    }

    private void setupBlock() throws IOException {
        if (this.mData == null) {
            return;
        }

        final int[] cftab = this.mData.mCftab;
        final int[] tt    = this.mData.initTT(this.mLast + 1);
        final byte[] ll8  = this.mData.mLl8;
        cftab[0] = 0;
        System.arraycopy(this.mData.mUnzftab, 0, cftab, 1, 256);

        for (int i = 1, c = cftab[0]; i <= 256; i++) {
            c += cftab[i];
            cftab[i] = c;
        }
        final int lastShadow = this.mLast;
        for (int i = 0; i <= lastShadow; i++) {
            tt[cftab[ll8[i] & 0xff]++] = i;
        }

        if ((this.mOrigPtr < 0) || (this.mOrigPtr >= tt.length)) {
            throw new IOException("stream corrupted");
        }

        this.mSuTPos = tt[this.mOrigPtr];
        this.mSuCount = 0;
        this.mSuI2 = 0;
        this.mSuCh2 = 256;   /* not a char and not EOF */

        if (this.mBlockRandomised) {
            this.mSuRNToGo = 0;
            this.mSuRTPos = 0;
            setupRandPartA();
        } else {
            setupNoRandPartA();
        }
    }

    private void setupRandPartA() throws IOException {
        if (this.mSuI2 <= this.mLast) {
            this.mSuChPrev = this.mSuCh2;
            int suCh2Shadow = this.mData.mLl8[this.mSuTPos] & 0xff;
            this.mSuTPos = this.mData.mTt[this.mSuTPos];
            if (this.mSuRNToGo == 0) {
                this.mSuRNToGo = BZip2Constants.R_NUMS[this.mSuRTPos] - 1;
                if (++this.mSuRTPos == 512) {
                    this.mSuRTPos = 0;
                }
            } else {
                this.mSuRNToGo--;
            }
            this.mSuCh2 = suCh2Shadow ^= (this.mSuRNToGo == 1) ? 1 : 0;
            this.mSuI2++;
            this.mCurrentChar = suCh2Shadow;
            this.mCurrentState = RAND_PART_B_STATE;
            this.mCrc.updateCRC(suCh2Shadow);
        } else {
            endBlock();
            initBlock();
            setupBlock();
        }
    }

    private void setupNoRandPartA() throws IOException {
        if (this.mSuI2 <= this.mLast) {
            this.mSuChPrev = this.mSuCh2;
            final int suCh2Shadow = this.mData.mLl8[this.mSuTPos] & 0xff;
            this.mSuCh2 = suCh2Shadow;
            this.mSuTPos = this.mData.mTt[this.mSuTPos];
            this.mSuI2++;
            this.mCurrentChar = suCh2Shadow;
            this.mCurrentState = NO_RAND_PART_B_STATE;
            this.mCrc.updateCRC(suCh2Shadow);
        } else {
            this.mCurrentState = NO_RAND_PART_A_STATE;
            endBlock();
            initBlock();
            setupBlock();
        }
    }

    private void setupRandPartB() throws IOException {
        if (this.mSuCh2 != this.mSuChPrev) {
            this.mCurrentState = RAND_PART_A_STATE;
            this.mSuCount = 1;
            setupRandPartA();
        } else if (++this.mSuCount >= 4) {
            this.mSuZ = (char) (this.mData.mLl8[this.mSuTPos] & 0xff);
            this.mSuTPos = this.mData.mTt[this.mSuTPos];
            if (this.mSuRNToGo == 0) {
                this.mSuRNToGo = BZip2Constants.R_NUMS[this.mSuRTPos] - 1;
                if (++this.mSuRTPos == 512) {
                    this.mSuRTPos = 0;
                }
            } else {
                this.mSuRNToGo--;
            }
            this.mSuJ2 = 0;
            this.mCurrentState = RAND_PART_C_STATE;
            if (this.mSuRNToGo == 1) {
                this.mSuZ ^= 1;
            }
            setupRandPartC();
        } else {
            this.mCurrentState = RAND_PART_A_STATE;
            setupRandPartA();
        }
    }

    private void setupRandPartC() throws IOException {
        if (this.mSuJ2 < this.mSuZ) {
            this.mCurrentChar = this.mSuCh2;
            this.mCrc.updateCRC(this.mSuCh2);
            this.mSuJ2++;
        } else {
            this.mCurrentState = RAND_PART_A_STATE;
            this.mSuI2++;
            this.mSuCount = 0;
            setupRandPartA();
        }
    }

    private void setupNoRandPartB() throws IOException {
        if (this.mSuCh2 != this.mSuChPrev) {
            this.mSuCount = 1;
            setupNoRandPartA();
        } else if (++this.mSuCount >= 4) {
            this.mSuZ = (char) (this.mData.mLl8[this.mSuTPos] & 0xff);
            this.mSuTPos = this.mData.mTt[this.mSuTPos];
            this.mSuJ2 = 0;
            setupNoRandPartC();
        } else {
            setupNoRandPartA();
        }
    }

    private void setupNoRandPartC() throws IOException {
        if (this.mSuJ2 < this.mSuZ) {
            final int suCh2Shadow = this.mSuCh2;
            this.mCurrentChar = suCh2Shadow;
            this.mCrc.updateCRC(suCh2Shadow);
            this.mSuJ2++;
            this.mCurrentState = NO_RAND_PART_C_STATE;
        } else {
            this.mSuI2++;
            this.mSuCount = 0;
            setupNoRandPartA();
        }
    }

    private static final class Data {

        // (with blockSize 900k)
        final boolean[] mInUse   = new boolean[256];                                   //      256 byte

        final byte[] mSeqToUnseq   = new byte[256];                                    //      256 byte
        final byte[] mSelector     = new byte[MAX_SELECTORS];                          //    18002 byte
        final byte[] mSelectorMtf  = new byte[MAX_SELECTORS];                          //    18002 byte

        /**
         * Freq table collected to save a pass over the data during
         * decompression.
         */
        final int[] mUnzftab = new int[256];                                           //     1024 byte

        final int[][] mLimit = new int[N_GROUPS][MAX_ALPHA_SIZE];                      //     6192 byte
        final int[][] mBase  = new int[N_GROUPS][MAX_ALPHA_SIZE];                      //     6192 byte
        final int[][] mPerm  = new int[N_GROUPS][MAX_ALPHA_SIZE];                      //     6192 byte
        final int[] mMinLens = new int[N_GROUPS];                                      //       24 byte

        final int[]     mCftab     = new int[257];                                     //     1028 byte
        final char[]    mGetAndMoveToFrontDecode_yy = new char[256];                   //      512 byte
        final char[][]  mTemp_charArray2d  = new char[N_GROUPS][MAX_ALPHA_SIZE];       //     3096 byte
        final byte[] mRecvDecodingTables_pos = new byte[N_GROUPS];                     //        6 byte
        //---------------
        //    60798 byte

        int[] mTt = null;                                                                     //  3600000 byte
        byte[] mLl8;                                                                   //   900000 byte
        //---------------
        //  4560782 byte
        //===============

        Data(int blockSize100k) {
            super();

            this.mLl8 = new byte[blockSize100k * BZip2Constants.BASE_BLOCK_SIZE];
        }

        /**
         * Initializes the {@link #mTt} array.
         *
         * This method is called when the required length of the array
         * is known.  I don't initialize it at construction time to
         * avoid unneccessary memory allocation when compressing small
         * files.
         */
        int[] initTT(int length) {
            int[] ttShadow = this.mTt;

            // tt.length should always be >= length, but theoretically
            // it can happen, if the compressor mixed small and large
            // blocks.  Normally only the last block will be smaller
            // than others.
            if ((ttShadow == null) || (ttShadow.length < length)) {
                this.mTt = ttShadow = new int[length];
            }

            return ttShadow;
        }

    }
}

