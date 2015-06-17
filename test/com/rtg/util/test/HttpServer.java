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
package com.rtg.util.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal HTTP Server for use in tests.
 *
 */
public class HttpServer {

  /**
   * Interface to implement for handling pages.
   */
  public interface Handler {
    /**
     * Handler for pages.
     *
     * @param out stream to rewrite to
     * @param request the request URI, minus the host and query parts
     * @param headers any headers passed in
     * @param get parameters via GET
     * @param post parameters via POST
     */
    void doPage(PrintStream out, String request, Map<String, String> headers, Map<String, String> get, Map<String, String> post);
  }

  private static final Pattern ESCAPED_CHAR = Pattern.compile("%([0-9A-Fa-f]{2})");
  private class Server extends Thread {
    private final ServerSocket mSocket;
    private boolean mDone = false;
    public Server(final ServerSocket s) {
      mSocket = s;
    }

    public void close() throws IOException {
      mSocket.close();
      this.interrupt();
      mDone = true;
      while (this.isAlive()) {
        try {
          this.join();
        } catch (InterruptedException e) {
          // keep waiting until thread is finished
        }
      }
    }

    @Override
    public void run() {
      try {
        while (!mDone) {
          final Socket s = mSocket.accept();
          process(s, mHandler);
        }
      } catch (final SocketException e) {
        // caused by the socket being closed.  If so, we're done.
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    }

    //see http://www.w3.org/Protocols/rfc1945/rfc1945
    private void process(final Socket s, final Handler h) {
      try {
        try (PrintStream out = new PrintStream(s.getOutputStream())) {
          final BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
          final String line = br.readLine();
          if (line == null) {
            unimplemented(out);
          } else {
            final String[] parts = line.split("\\s+");
            final String method = parts[0];
            String requestURL = parts[1];
            //final String httpVersion = parts.length == 3 ? parts[2] : null;

            if (requestURL.startsWith("/")) {
              requestURL = "http://localhost" + requestURL;
            }
            final URL u = new URL(requestURL);

            final Map<String, String> get = decodeQuery(u.getQuery());
            final Map<String, String> headers = readHeaders(br);
            final Map<String, String> post;
            // if this were a real HTTP server, the headers wouldn't be case sensitive
            if ("POST".equals(method) && headers.containsKey("Content-Length")) {
              post = readPostData(br, Integer.parseInt(headers.get("Content-Length")));
            } else {
              post = new HashMap<>();
            }

            if (h == null) {
              unimplemented(out);
            } else {
              h.doPage(out, u.getPath(), headers, get, post);
            }
          }
        }
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    }

    private void unimplemented(final PrintStream out) {
      out.println("HTTP/1.0 " + HttpURLConnection.HTTP_INTERNAL_ERROR);
    }

    private Map<String, String> readHeaders(BufferedReader br) throws IOException {
      final Map<String, String> headers = new HashMap<>();
      String line = br.readLine();
      while (line != null && !line.trim().equals("")) {
        final String[] parts = line.split(":\\s+", 2);
        if (parts.length == 2) {
          final String name = parts[0];
          final StringBuilder content = new StringBuilder(parts[1]);
          line = br.readLine();
          while (line != null && line.startsWith(" ")) {
            content.append(line);
            line = br.readLine();
          }
          headers.put(name, content.toString());
        } else {
          line = br.readLine();
        }
      }
      return headers;
    }

    private Map<String, String> readPostData(BufferedReader br, int contentLength) throws IOException {
      final StringBuilder sb = new StringBuilder();
      final char[] buffer = new char[contentLength];
      int pos = 0;
      int len;
      while (pos < contentLength && (len = br.read(buffer, 0, contentLength - pos)) > 0) {
        sb.append(buffer, 0, len);
        pos += len;
      }
      return decodeQuery(sb.toString());
    }

    private Map<String, String> decodeQuery(String query) {
      final Map<String, String> values = new HashMap<>();
      if (query == null) {
        return values;
      }

      for (final String pair : query.split("&")) {
        final String[] parts = pair.split("=");
        values.put(decode(parts[0]), parts.length > 1 ? decode(parts[1]) : "");
      }

      return values;
    }

    private String decode(String s) {
      final Matcher m = ESCAPED_CHAR.matcher(s);
      final StringBuffer sb = new StringBuffer();
      while (m.find()) {
        final char c = (char) Integer.parseInt(m.group(1), 16);
        switch (c) {
          case '$':
            m.appendReplacement(sb, "\\$");
            break;
          case '\\':
            m.appendReplacement(sb, "\\\\");
            break;
          default:
            m.appendReplacement(sb, Character.toString(c));
        }
      }
      m.appendTail(sb);
      return sb.toString();
    }
  }

  private final ServerSocket mSocket;
  private final Server mServer;
  private Handler mHandler = null;

  /**
   * Constructs an HttpServer on a random port.
   *
   * @throws IOException if an i/o related exception occurs
   */
  public HttpServer() throws IOException {
    this(0);
  }

  /**
   * Constructs an HttpServer on a specified port.
   *
   * @param port the port to listen on (or 0 for random)
   * @throws IOException if an i/o related exception occurs
   */
  public HttpServer(final int port) throws IOException {
    mSocket = new ServerSocket(port);
    mServer = new Server(mSocket);
    mServer.setDaemon(true);
  }

  /**
   * Starts the HttpServer
   */
  public void start() {
    mServer.start();
  }

  /**
   * Stops the HttpServer
   *
   * @throws IOException if an exception occurs closing the socket
   */
  public void stop() throws IOException {
    mServer.close();
  }

  /**
   * Gets the port the HttpServer is running on.
   *
   * @return the port
   */
  public int getPort() {
    return mSocket.getLocalPort();
  }

  /**
   * Sets the page handler.
   *
   * @param h the handler
   */
  public void setHandler(final Handler h) {
    mHandler = h;
  }

}
