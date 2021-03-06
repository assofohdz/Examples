/*
 * Copyright (c) 2018, Asser Fahrenholz
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package infinity.map;

import java.awt.Image;
import java.awt.image.MemoryImageSource;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

import javax.swing.JPanel;

public class BitMap extends JPanel {
    private static final long serialVersionUID = -1228135782001412920L;
    public final static int BI_RGB = 0; // No compression
    public final static int BI_RLE8 = 1; // RLE 8-bit / pixel
    public final static int BI_RLE4 = 2; // RLE 4-bit / pixel
    public final static int BI_BITFIELDS = 3; // Bitfields

    private final BufferedInputStream m_stream;

    private byte[] fileHeader;
    private byte[] infoHeader;
    private ByteArray fileData;

    // private String fh_type;
    private int m_size;
    // private int m_offset;
    private int m_width;
    private int m_height;
    private int m_bitCount;
    private int m_compressionType;
    private int m_colorsUsed;

    private int[] m_colorTable;
    private int[] m_image;

    private boolean m_validBMP = false;
    public boolean hasELVL = false; // since ELVL headers are so interlocked with the bitmap, this is
    // the appropriate place
    public int ELvlOffset = -1;

    public BitMap(final BufferedInputStream stream) {
        m_stream = stream;
    }

    public void readBitMap(final boolean trans) {
        fileData = new ByteArray();

        // Read 14 bytes for file header
        fileHeader = readIn(14);
        ByteArray array = new ByteArray(fileHeader);
        // fh_type = array.readString(0, 2);

        if (array.readString(0, 2).equals("BM")) {
            m_validBMP = true;
        } else {
            if (array.readString(0, 4).equals("elvl")) {
                hasELVL = true;
                ELvlOffset = 0;
            }

            return;
        }
        m_size = array.readLittleEndianInt(2);
        // m_offset = array.readLittleEndianInt(10);

        if (m_size != 49718) { // possible elvl header... check reserved bits to confirm
            final int offset = array.readLittleEndianShort(6);
            if (offset == 49720) { // currently this is the only place for the eLvl Header
                ELvlOffset = 49720;
                hasELVL = true;
            }
        }

        // Read 40 bytes for info header
        infoHeader = readIn(40);
        array = new ByteArray(infoHeader);
        m_width = array.readLittleEndianInt(4);
        m_height = array.readLittleEndianInt(8);
        m_bitCount = array.readLittleEndianShort(14);
        m_compressionType = array.readLittleEndianInt(16);
        m_colorsUsed = array.readLittleEndianInt(20);

        // Create our image container
        m_image = new int[m_width * m_height];

        // If it is 8 bits or less it has a color table
        if (m_bitCount <= 8) {
            // Define our color tables/colors used
            m_colorTable = new int[(int) Math.pow(2, m_bitCount)];
            m_colorsUsed = (int) Math.pow(2, m_bitCount);
            // Read in the color table
            for (int i = 0; i < m_colorsUsed; i++) {
                final byte c[] = readIn(4);
                array = new ByteArray(c);
                m_colorTable[i] = (array.readLittleEndianInt(0) & 0xffffff) + 0xff000000;

                // Make black transparent. SS specific need, will adjust to be dynamic
                if (m_colorTable[i] == 0xff000000 && trans) {
                    m_colorTable[i] = m_colorTable[i] & 0x00000000;
                }
            }
        }

        if (m_compressionType == BI_RGB && m_bitCount <= 8) {
            readInRGB();
        } else if (m_compressionType == BI_RLE8 && m_bitCount == 8) {
            readInRLE8();
        }
    }

    public void readInRGB() {

        final int shift[] = new int[8 / m_bitCount];
        for (int i = 0; i < 8 / m_bitCount; i++) {
            shift[i] = 8 - ((i + 1) * m_bitCount);
        }

        // Create a mask for each pixel dependant on # of bitCount
        final int mask = (1 << m_bitCount) - 1;

        // How much padding after each line. Bitmaps pad to 32bits
        int pad = 4 - (int) Math.ceil(m_width * m_bitCount / 8.0) % 4;
        if (pad == 4) {
            pad = 0;
        }

        int y = m_height - 1;
        int x = 0;
        int bit = 0;

        int a = readByte();
        for (int i = 0; i < m_height * m_width; i++) {
            m_image[y * m_width + x] = m_colorTable[a >> shift[bit] & mask];

            bit++;
            x++;
            if (x >= m_width) {
                bit = 0;
                x = 0;
                y--;
                // Pad to 32 bits after each line
                for (int j = 0; j < pad; j++) {
                    readByte();
                }
                a = readByte();
            }
            if (bit >= 8 / m_bitCount) {
                bit = 0;
                a = readByte();
            }
        }

    }

    /**
     * Reads RLE 8 bit bitmaps
     */
    public void readInRLE8() {

        int y = m_height - 1;
        int x = 0;

        int a = readByte();
        int b = readByte();
        while (((a != 0) || (b != 1))) {

            if (a == 0) {
                if (b == 0) {
                    y--;
                    x = 0;
                } else if (b == 2) {
                    x += readByte();
                    y -= readByte();
                } else if (b >= 3) {
                    for (int i = 0; i < b; i++) {
                        m_image[y * m_width + x] = m_colorTable[readByte()];
                        x++;
                    }
                    if (Math.round(b / 2.0) != b / 2.0) {
                        readByte();
                    }
                }
            } else {
                for (int i = 0; i < a; i++) {
                    m_image[y * m_width + x] = m_colorTable[b];
                    x++;
                }
            }
            a = readByte();
            b = readByte();
        }
    }

    public byte[] readIn(final int n) {

        final byte[] b = new byte[n];
        try {
            m_stream.read(b);
            // fileData.addByteArray( b );
            return b;
        } catch (@SuppressWarnings("unused") final IOException e) {
            return new byte[0];
        }
    }

    public int readByte() {
        try {
            final byte[] b = new byte[1];
            m_stream.read(b);
            // fileData.addByteArray( b );
            return b[0] & 255;
        } catch (@SuppressWarnings("unused") final IOException e) {
            return 0;
        }
    }

    public void appendTo(final BufferedOutputStream out) throws IOException {
        // Write bitmap File Data
        out.write(fileData.getByteArray(), 0, fileData.size());
        out.close();
    }

    public Image getImage() {
        return createImage(new MemoryImageSource(m_width, m_height, m_image, 0, m_width));
    }

    /**
     * Reads in a square tile of any size from the topleft. Good for getting the
     * first image in any of the /graphics/*.bm2
     *
     * @param size the pixel size of the image to load
     * @return the image loaded
     */
    public Image getImage(final int size) {

        final int image[] = new int[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image[y * size + x] = m_image[y * m_width + x];
            }
        }
        return createImage(new MemoryImageSource(size, size, image, 0, size));
    }

    public Image getImage(final int width, final int height) {

        final int image[] = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image[y * width + x] = m_image[y * m_width + x];
            }
        }
        return createImage(new MemoryImageSource(width, height, image, 0, width));
    }

    public boolean isBitMap() {
        return m_validBMP;
    }

    public int getFileSize() {
        return m_size;
    }

    @Override
    public int getWidth() {
        return m_width;
    }

    @Override
    public int getHeight() {
        return m_height;
    }

    public int[] getImageData() {
        return m_image;
    }
}
