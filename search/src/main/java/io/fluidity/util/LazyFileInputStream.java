/*
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package io.fluidity.util;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class LazyFileInputStream extends InputStream {

    /**
     * the underlying input stream
     */
    private FileInputStream in;

    /**
     * FileDescriptor to use
     */
    private FileDescriptor fd;

    /**
     * File to use
     */
    private File file;

    /**
     * Creates a new <code>LazyFileInputStream</code> for the given file. If the
     * file is unreadably, a FileNotFoundException is thrown.
     *
     * @param file
     * @throws java.io.FileNotFoundException
     */
    public LazyFileInputStream(File file)
            throws FileNotFoundException {
        // check if we can read from the file
        if (!file.canRead()) {
            throw new FileNotFoundException(file.getPath());
        }
        this.file = file;
    }

    /**
     * Creates a new <code>LazyFileInputStream</code> for the given file
     * desciptor.
     *
     * @param fdObj
     */
    public LazyFileInputStream(FileDescriptor fdObj) {
        this.fd = fdObj;
    }

    /**
     * Creates a new <code>LazyFileInputStream</code> for the given file. If the
     * file is unreadably, a FileNotFoundException is thrown.
     *
     * @param name
     * @throws java.io.FileNotFoundException
     */
    public LazyFileInputStream(String name) throws FileNotFoundException {
        this(new File(name));
    }

    /**
     * Opens the underlying file input stream in neccessairy.
     *
     * @throws java.io.IOException
     */
    public void open() throws IOException {
        if (in == null) {
            if (file != null) {
                in = new FileInputStream(file);
            } else if (fd != null) {
                in = new FileInputStream(fd);
            } else {
                throw new IOException("Stream already closed.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public int read() throws IOException {
        open();
        return in.read();
    }

    /**
     * {@inheritDoc}
     */
    public int available() throws IOException {
        open();
        return in.available();
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        if (in != null) {
            in.close();
        }
        in = null;
        file = null;
        fd = null;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void reset() throws IOException {
        open();
        in.reset();
    }

    /**
     * {@inheritDoc}
     */
    public boolean markSupported() {
        try {
            open();
            return in.markSupported();
        } catch (IOException e) {
            throw new IllegalStateException(e.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void mark(int readlimit) {
        try {
            open();
            in.mark(readlimit);
        } catch (IOException e) {
            throw new IllegalStateException(e.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    public long skip(long n) throws IOException {
        open();
        return in.skip(n);
    }

    /**
     * {@inheritDoc}
     */
    public int read(byte[] b) throws IOException {
        open();
        return in.read(b);
    }

    /**
     * {@inheritDoc}
     */
    public int read(byte[] b, int off, int len) throws IOException {
        open();
        return in.read(b, off, len);
    }
}