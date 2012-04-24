package biz.onomato.frskydash.sim;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.util.Logger;

/**
 * simulator for iterating available files on a given path and providing this as
 * input to the server. Can be used to iterate log files from previous test
 * flights.
 * 
 * thread start/stop
 * http://stackoverflow.com/questions/680180/where-to-stop-destroy
 * -threads-in-android-service-class
 * 
 * FIXME: keeps running on closing app
 * 
 * @author hcpl
 * 
 */
public class FileSimulatorThread extends Thread {

	/**
	 * the files assigned to this thread for iteration
	 */
	private File[] files = null;

	/**
	 * the server to commit bytes to
	 */
	private FrSkyServer server = null;

	/**
	 * interval set between buffers
	 */
	private final int INTERVAL_IN_MS = 20;

	/**
	 * default ctor
	 * 
	 * @param server
	 */
	public FileSimulatorThread(FrSkyServer server) {
		this.server = server;
	}

	private volatile Thread runner;

	public synchronized void startThread() {
		if (runner == null) {
			runner = new Thread(this);
			runner.start();
		}
	}

	public synchronized void stopThread() {
		if (runner != null) {
			Thread moribund = runner;
			runner = null;
			moribund.interrupt();
		}
	}

	public synchronized boolean isThreadRunning() {
		// FIXME fails onresume of sim activity if already run
		// return Thread.currentThread() == runner;
		// return runner == null;
		return Thread.currentThread().isAlive();
	}

	@Override
	public void run() {
		// as long as this thread is in the running state keep looking for files
		// on the given path
		while (Thread.currentThread() == runner) {
			// init is here so that we can properly close it
			// afterwards
			InputStream is = null;
			// now iterate these files
			for (File file : files) {
				// try catch block inside so we don't block all
				// the files if one fails. If byte buffering fails complete file
				// will be skipped though (adapt if desired)
				try {
					// open the file
					is = new FileInputStream(file);
					// iterated based on state of thread and on length of file
					// Get the size of the file
					long length = file.length();
					// You cannot create an array using a long type.
					// It needs to be an int type.
					// Before converting to an int type, check
					// to ensure that file is not larger than Integer.MAX_VALUE.
					if (length > Integer.MAX_VALUE) {
						// File is too large
					}
					// Create the byte array to hold the data
					byte[] bytes = new byte[(int) length];
					// Read in the bytes
					int offset = 0;
					int numRead = 0;
					while (offset < bytes.length
							&& (numRead = is.read(bytes, offset, bytes.length
									- offset)) >= 0) {
						offset += numRead;
					}
					// Ensure all the bytes have been read in
					if (offset < bytes.length) {
						throw new IOException("Could not completely read file "
								+ file.getName());
					}
					// read file in at once and then chop based on
					// start/stop byte and provide like that to server. This way
					// the frames aren't broken
					int start = -1, stop = -1;
					// iterate
					for (int i = 0; i < bytes.length
							&& Thread.currentThread() == runner; i++) {
						// if start not set then we're looking for the first
						// 0x7D byte
						if (start < 0 && bytes[i] == 0x7E) {
							start = i;
						}
						// otherwise we already have a start index so we
						// only need to check for the stop byte
						else if (start >= 0 && stop < 0 && bytes[i] == 0x7E) {
							// check if these aren't just start/stop bytes next
							// to each other
							if (start == i - 1)
								start = i;
							else
								stop = i;
						}
						// if we have both start and stop byte registered we can
						// pass a frame to the server
						else if (start >= 0 && stop >= 0) {
							byte[] buffer = new byte[stop - start + 1];
							System.arraycopy(bytes, start, buffer, 0, stop
									- start + 1);
							// pass to server
							server.handleByteBuffer(buffer);
							// reset start and stop
							start = -1;
							stop = -1;
							// now wait a bit
							Thread.sleep(INTERVAL_IN_MS);
						}
						// otherwise keep looking for start stop bytes
					}
				} catch (InterruptedException e) {
					Logger.e(this.getClass().toString(),
							"Problem waiting interval file cycle thread", e);
				} catch (Exception e) {
					Logger.e(this.getClass().toString(),
							"Problem reading file or iterating content", e);
				} finally {
					// proper clean up of input stream
					if (is != null) {
						try {
							is.close();
						} catch (IOException e) {
							Logger.e(this.getClass().toString(),
									"Problem closing file", e);
						}
					}
				}
			}
		}
	}

	/**
	 * set the files to iterate with this thread
	 * 
	 * @param files
	 */
	public void setFiles(File[] files) {
		this.files = files;
	}

}
