/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.opentsdb;

import static ch.ethz.ssh2.ChannelCondition.CLOSED;
import static ch.ethz.ssh2.ChannelCondition.EOF;
import static ch.ethz.ssh2.ChannelCondition.EXIT_SIGNAL;
import static ch.ethz.ssh2.ChannelCondition.EXIT_STATUS;
import static ch.ethz.ssh2.ChannelCondition.STDERR_DATA;
import static ch.ethz.ssh2.ChannelCondition.STDOUT_DATA;
import static ch.ethz.ssh2.ChannelCondition.TIMEOUT;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

/**
 * <p>Title: WrappedSession</p>
 * <p>Description: A wrapped SSH session</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.opentsdb.WrappedSession</code></p>
 */

public class WrappedSession {
	/** The delegate session */
	protected Session session = null;
	/** The parent connection */
	protected final ExtendedConnection conn;
	/** Indicates if this session is open */
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	
	/** The command timeout in ms. */
	protected long commandTimeout = 1000;
	/** The size of the output stream buffer in bytes */
	protected int outputBufferSize = 1024;
	/** The size of the error stream buffer in bytes */
	protected int errorBufferSize = 128;
	
	protected PushbackInputStream pbis = null;
	
	
	
	/** The bit mask of all channel conditions */
	public static final int ALL_CONDITIONS = CLOSED | EOF | EXIT_SIGNAL | EXIT_STATUS | STDERR_DATA | STDOUT_DATA | TIMEOUT;
	
	public static final int[] ALL_CONDITION_VALUES = new int[] {CLOSED , EOF , EXIT_SIGNAL , EXIT_STATUS , STDERR_DATA , STDOUT_DATA , TIMEOUT};

	public static String PROMPT = "GO1GO2GO3";
	public final static char LF = '\n';
	public final static char CR = '\r';	
	
	/**
	 * Creates a new WrappedSession
	 * @param session The delegate session
	 * @param conn The parent connection
	 */
	public WrappedSession(final Session session, final ExtendedConnection conn) {
		this.session = session;
		this.conn = conn;
	}
	
	/**
	 * Reconnects this session
	 * @return true if the session was reconnected, false otherwise
	 */
	public boolean reconnect() {
		if(!connected.get()) {
			try {
				session = conn.openSession();
				connected.set(true);
			} catch (Exception ex) {
				connected.set(false);
			}
		}
		return connected.get();
	}
	
	/**
	 * Indicates if this session is open
	 * @return true if this session is open, false otherwise
	 */
	public boolean isOpen() {
		return connected.get();
	}
	

	/**
	 * @throws IOException
	 * @see ch.ethz.ssh2.Session#requestDumbPTY()
	 */
	public void requestDumbPTY() throws IOException {
		session.requestDumbPTY();
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return session.hashCode();
	}

	/**
	 * @param term
	 * @throws IOException
	 * @see ch.ethz.ssh2.Session#requestPTY(java.lang.String)
	 */
	public void requestPTY(String term) throws IOException {
		session.requestPTY(term);
	}

	/**
	 * @param term
	 * @param term_width_characters
	 * @param term_height_characters
	 * @param term_width_pixels
	 * @param term_height_pixels
	 * @param terminal_modes
	 * @throws IOException
	 * @see ch.ethz.ssh2.Session#requestPTY(java.lang.String, int, int, int, int, byte[])
	 */
	public void requestPTY(String term, int term_width_characters,
			int term_height_characters, int term_width_pixels,
			int term_height_pixels, byte[] terminal_modes) throws IOException {
		session.requestPTY(term, term_width_characters, term_height_characters,
				term_width_pixels, term_height_pixels, terminal_modes);
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return session.equals(obj);
	}

	/**
	 * @param term_width_characters
	 * @param term_height_characters
	 * @param term_width_pixels
	 * @param term_height_pixels
	 * @throws IOException
	 * @see ch.ethz.ssh2.Session#requestWindowChange(int, int, int, int)
	 */
	public void requestWindowChange(int term_width_characters,
			int term_height_characters, int term_width_pixels,
			int term_height_pixels) throws IOException {
		session.requestWindowChange(term_width_characters,
				term_height_characters, term_width_pixels, term_height_pixels);
	}

	/**
	 * @param hostname
	 * @param port
	 * @param cookie
	 * @param singleConnection
	 * @throws IOException
	 * @see ch.ethz.ssh2.Session#requestX11Forwarding(java.lang.String, int, byte[], boolean)
	 */
	public void requestX11Forwarding(String hostname, int port, byte[] cookie,
			boolean singleConnection) throws IOException {
		session.requestX11Forwarding(hostname, port, cookie, singleConnection);
	}

	/**
	 * @param cmd
	 * @throws IOException
	 * @see ch.ethz.ssh2.Session#execCommand(java.lang.String)
	 */
	public void execShellCommand(String cmd) throws IOException {
		execShellCommand(cmd, null);
	}
	
	
	  
	  
	  public void execShellCommand2(String cmd) throws IOException {
		  requestDumbPTY();
		  startShell();
		  final Collection<String> lines = new LinkedList<String>();
		  pbis = new PushbackInputStream(new StreamGobbler(getStdout()));
		   writeCmd(getStdin(), pbis, "PS1=" + PROMPT);
	        readTillPrompt(pbis, null);
		  writeCmd(getStdin(), pbis, cmd);
	      readTillPrompt(pbis, lines);
	      System.out.println("Out: " + join(lines, Character.toString(LF)));
	  }

	/**
	 * @param cmd
	 * @param charsetName
	 * @throws IOException
	 * @see ch.ethz.ssh2.Session#execCommand(java.lang.String, java.lang.String)
	 */
	public void execShellCommand(String cmd, String charsetName) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream(outputBufferSize); 
		ByteArrayOutputStream error = new ByteArrayOutputStream(errorBufferSize);

//		session.getStdin().write(("export PS1=" + PROMPT + LF).getBytes());
//		session.getStdin().flush();
		System.out.println("=====================================================");
		session.getStdin().write((cmd + "\n").getBytes());
		session.getStdin().flush();
//		session.execCommand(cmd, charsetName);
		while(true) {			
			int cond = session.waitForCondition(ALL_CONDITIONS, 1000);
			System.out.println("State: " + getState() + ", CONDITIONS: " + Arrays.toString(ChannelCond.enabled(cond)));
			if(((cond & (CLOSED|TIMEOUT)) != 0)) {
				System.out.println("Output:" + new String(output.toByteArray()));
				break;
				//throw new RuntimeException("Command [" + cmd + "] timed out or connection was closed");
			}
			if(((cond & STDOUT_DATA) != 0)) {
				int[] result = pipe(getStdout(), output);
				System.out.println("Read [" + result[0] + "] bytes, Outcome: " + result[1]);
			}
			if(((cond & STDERR_DATA) != 0)) {
				pipe(getStderr(), error);
			}			
		}
		System.out.println("=====================================================");
	}
	
	
	
	/**
	 * Copies all readable bytes from the input stream to the output stream
	 * @param is The input stream to read from
	 * @param os The output stream to write to
	 * @param bufferSize the buffer size in bytes
	 * @return The total number of bytes transferred
	 * @throws IOException Thrown on any I/O error in the transfer
	 */
	public static int[] pipe(final InputStream is, final OutputStream osx, final int bufferSize) throws IOException {
		OutputStream os = osx != null ? osx : new ByteArrayOutputStream(bufferSize);
		byte[] buffer = new byte[bufferSize];
		int bytesCopied = 0;
		int totalBytesCopied = 0;
		int[] result = new int[2];
		boolean firstRead = false;
		while((bytesCopied = is.read(buffer))!= -1) {
			if(!firstRead) {
				firstRead = true;
			} else {
				if(is.available()<1) {
					result[0] = totalBytesCopied;
					result[1] = 2; 
					return result;					
				}
			}
			totalBytesCopied += bytesCopied;
			os.write(buffer, 0, bytesCopied);
			if(bytesCopied < bufferSize) {
				result[0] = totalBytesCopied;
				result[1] = 1;
				if(osx==null) {
					System.out.println("INIT OUTPUT:\n" + new String(((ByteArrayOutputStream)os).toByteArray()) + "\n---DONE" );
				}
				return result;
			}
		}
		result[0] = totalBytesCopied;
		result[1] = 0; 		
		return result;
	}

	/**
	 * Copies all readable bytes from the input stream to the output stream using a 1024 byte buffer
	 * @param is The input stream to read from
	 * @param os The output stream to write to
	 * @return The total number of bytes transferred
	 * @throws IOException Thrown on any I/O error in the transfer
	 */
	public static int[] pipe(final InputStream is, final OutputStream os) throws IOException {
		return pipe(is, os, 1024);
	}

	
	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return session.toString();
	}

	
	/**
	 * @throws IOException
	 * @see ch.ethz.ssh2.Session#startShell()
	 */
	public void startShell() throws IOException {
		session.requestDumbPTY();
		session.startShell();
		int[] result = pipe(getStdout(), null);
		System.out.println("Shell Start Result:" + Arrays.toString(result));		
		getStdin().write(("PS1=" + PROMPT + LF).getBytes());
		getStdin().flush();
		result = pipe(getStdout(), null);
		System.out.println("Shell Start Result:" + Arrays.toString(result));
		
//		pbis = new PushbackInputStream(new StreamGobbler(getStdout()));
//		writeCmd(getStdin(), pbis, "PS1=" + PROMPT);
//		readTillPrompt(pbis, null);
	}

	/**
	 * @param name
	 * @throws IOException
	 * @see ch.ethz.ssh2.Session#startSubSystem(java.lang.String)
	 */
	public void startSubSystem(String name) throws IOException {
		session.startSubSystem(name);
	}

	/**
	 * @return
	 * @see ch.ethz.ssh2.Session#getState()
	 */
	public int getState() {
		return session.getState();
	}

	/**
	 * @return
	 * @see ch.ethz.ssh2.Session#getStdout()
	 */
	public InputStream getStdout() {
		return session.getStdout();
	}

	/**
	 * @return
	 * @see ch.ethz.ssh2.Session#getStderr()
	 */
	public InputStream getStderr() {
		return session.getStderr();
	}

	/**
	 * @return
	 * @see ch.ethz.ssh2.Session#getStdin()
	 */
	public OutputStream getStdin() {
		return session.getStdin();
	}

	/**
	 * @param timeout
	 * @return
	 * @throws IOException
	 * @deprecated
	 * @see ch.ethz.ssh2.Session#waitUntilDataAvailable(long)
	 */
	public int waitUntilDataAvailable(long timeout) throws IOException {
		return session.waitUntilDataAvailable(timeout);
	}

	/**
	 * @param condition_set
	 * @param timeout
	 * @return
	 * @throws IOException
	 * @see ch.ethz.ssh2.Session#waitForCondition(int, long)
	 */
	public int waitForCondition(int condition_set, long timeout)
			throws IOException {
		return session.waitForCondition(condition_set, timeout);
	}

	/**
	 * @return
	 * @see ch.ethz.ssh2.Session#getExitStatus()
	 */
	public Integer getExitStatus() {
		return session.getExitStatus();
	}

	/**
	 * @return
	 * @see ch.ethz.ssh2.Session#getExitSignal()
	 */
	public String getExitSignal() {
		return session.getExitSignal();
	}

	/**
	 * 
	 * @see ch.ethz.ssh2.Session#close()
	 */
	public void close() {
		session.close();
	}

	/**
	 * Returns the command timeout in ms.
	 * @return the command timeout in ms.
	 */
	public final long getCommandTimeout() {
		return commandTimeout;
	}

	/**
	 * Sets the command timeout in ms.
	 * @param commandTimeout the command timeout in ms.
	 */
	public final void setCommandTimeout(long commandTimeout) {
		this.commandTimeout = commandTimeout;
	}
	
	public static String join(final Collection<String> str, final String sep) {
	    final StringBuilder sb = new StringBuilder();
	    final Iterator<String> i = str.iterator();
	    while (i.hasNext()) {
	      sb.append(i.next());
	      if (i.hasNext()) {
	        sb.append(sep);
	      }
	    }
	    return sb.toString();
	  }	

	public static void writeCmd(final OutputStream os,
			final PushbackInputStream is,
			final String cmd) throws IOException {
		System.out.println("In: " + cmd);
		os.write(cmd.getBytes());
		os.write(LF);
		skipTillEndOfCommand(is);
	}

	public static void readTillPrompt(final InputStream is,
			final Collection<String> lines) throws IOException {
		final StringBuilder cl = new StringBuilder();
		boolean eol = true;
		int match = 0;
		while (true) {
			final char ch = (char) is.read();
			if(65535==(int)ch) return;
			switch (ch) {
			case CR:
			case LF:
				if (!eol) {
					if (lines != null) {
						lines.add(cl.toString());
					}
					cl.setLength(0);
				}
				eol = true;
				break;
			default:
				if (eol) {
					eol = false;
				}
				cl.append(ch);
				break;
			}

			if (cl.length() > 0
					&& match < PROMPT.length()
					&& cl.charAt(match) == PROMPT.charAt(match)) {
				match++;
				if (match == PROMPT.length()) {
					return;
				}
			} else {
				match = 0;
			}
		}
	}

	public static void skipTillEndOfCommand(final PushbackInputStream is) throws IOException {
		boolean eol = false;
		while (true) {
			final char ch = (char) is.read();
			switch (ch) {
			case CR:
			case LF:
				eol = true;
				break;
			default:
				if (eol) {
					is.unread(ch);
					return;
				}
			}
		}
	}	
	
}
