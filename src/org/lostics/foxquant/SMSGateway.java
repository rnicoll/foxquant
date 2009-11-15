// $Id: SMSGateway.java 685 2009-11-08 01:12:26Z  $

package org.lostics.foxquant;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.Collections;
import java.util.Set;

/**
 * SMS gateway for sending messages via the TxtLocal API. Works as a separate
 * thread to avoid issues with blocking.
 */
public class SMSGateway extends Thread {
    public  static final int    MIN_FROM_LENGTH = 3;
    public  static final int    MAX_FROM_LENGTH = 11;

    public  static final int    MAX_MESSAGE_LENGTH  = 160;

    /** Maximum length of the message queue. */
    public  static final int    MAX_QUEUE_LENGTH = 100;

    private static final String ENCODE_CHARSET = "US-ASCII";
    private static final String DEFAULT_FROM = "Fox Quant";
    private static final String TXTLOCAL_URL = "https://www.txtlocal.com/sendsmspost.php";

    private final String from;
    private final String userName;
    private final String userPwd;

    private final BlockingQueue<Message> messageQueue = new ArrayBlockingQueue<Message>(100);

    /**
     * @throws IllegalArgumentException if the from name is too long or short.
     * See {@link #MIN_FROM_LENGTH} and {@link #MAX_FROM_LENGTH}.
     */
    public   SMSGateway(final String setFrom,
        final String setUsername, final String setPassword)
        throws IllegalArgumentException {
        super();

        if (null != setFrom) {
            SMSGateway.validateFrom(setFrom);
            this.from = setFrom;
        } else {
            this.from = DEFAULT_FROM;
        }

        this.userName = setUsername;
        this.userPwd = setPassword;
        
        this.setName("SMS");
    }

    public void close() {
        this.interrupt();

        try {
            // Wait 500 millis for this thread to exit, then give up.
            this.join(500);
        } catch(InterruptedException e) {
            // Yikes, we were quitting already, no need to nag.
        }

        return;
    }

    /**
     * Send a text message to the given phone number.
     *
     * @return true if the message was successfully queued, false otherwise.
     * @throws IllegalArgumentException if the message is too long.
     */
    public boolean sendMessage(final PhoneNumber phoneNumber, final String smsText)
        throws IllegalArgumentException {
        final Message message = new Message(phoneNumber, smsText);

        return sendMessage(message);
    }

    /**
     * Send a text message to the given phone number.
     *
     * @return true if the message was successfully queued, false otherwise.
     * @throws IllegalArgumentException if the message is too long.
     */
    public boolean sendMessage(final Set<PhoneNumber> phoneNumbers, final String smsText)
        throws IllegalArgumentException {
        final Message message = new Message(phoneNumbers, smsText);

        return sendMessage(message);
    }

    /**
     * Send a text message to the given phone number.
     *
     * @return true if the message was successfully queued, false otherwise.
     */
    public boolean sendMessage(final Message message) {
        return this.messageQueue.offer(message);
    }

    /**
     * Send a text message to the given phone number. This is the internal
     * method to be called from the messaging thread.
     *
     * @throws UnsupportedEncodingException if it was unable to form the URL correctly.
     * @throws MalformedURLException if it was unable to form the URL correctly.
     * @throws IOException if there was a problem contacting the server.
     */
    private void doSendMessage(final Message message)
        throws UnsupportedEncodingException, MalformedURLException, IOException {
        boolean firstNumber = true;
        final String smsText = message.text;
        final StringBuffer urlStr = new StringBuffer(TXTLOCAL_URL).append("?");

        // urlStr.append("test=1&");
        urlStr.append("uname=").append(URLEncoder.encode(userName, ENCODE_CHARSET)).append("&");
        urlStr.append("pword=").append(URLEncoder.encode(userPwd, ENCODE_CHARSET)).append("&");
        urlStr.append("message=").append(URLEncoder.encode(smsText, ENCODE_CHARSET)).append("&");
        urlStr.append("from=").append(URLEncoder.encode(this.from, ENCODE_CHARSET)).append("&");
        urlStr.append("selectednums=");
        for (PhoneNumber phoneNumber: message.phoneNumbers) {
            if (!firstNumber) {
                // Insert a comma, pre-encoded for use in a URL
                urlStr.append("%2C");
            } else {
                firstNumber = false;
            }
            urlStr.append(phoneNumber.numberStr);
        }

        final URL url = new URL(urlStr.toString());
        final URLConnection conn = url.openConnection();

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        try {
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                // System.out.println(inputLine);
            }
        } finally {
            in.close();
        }
    }

    public void run() {
        // Run until interrupted
        while (true) {
            Message message;

            try {
                message = this.messageQueue.poll(5, TimeUnit.SECONDS);
            } catch(InterruptedException e) {
                // Notification to exit
                return;
            }
            
            while (null != message) {
                try {
                    doSendMessage(message);
                } catch(IOException e) {
                    System.err.println("SMS thread shutting down due to: "
                        + e);
                    e.printStackTrace();
                }

                message = this.messageQueue.poll();
            }
        }
    }

    /**
     * Checks the given from name (the name that SMSes show as sent by) is valid.
     *
     * @throws IllegalArgumentException if the from name is too long or short.
     * See {@link #MIN_FROM_LENGTH} and {@link #MAX_FROM_LENGTH}.
     */
    public static void validateFrom(final String from)
        throws IllegalArgumentException {
        int fromLength = from.length();

        if (fromLength < MIN_FROM_LENGTH) {
            throw new IllegalArgumentException("From name \""
                + from + "\" is shorter than the minimum of "
                + MIN_FROM_LENGTH + " characters.");
        } else if (fromLength > MAX_FROM_LENGTH) {
            throw new IllegalArgumentException("From name \""
                + from + "\" is longer than the maximum of "
                + MAX_FROM_LENGTH + " characters.");
        }
    }

    public static class Message extends Object {
        private final String text;
        private final Set<PhoneNumber> phoneNumbers;

        /**
         * @throws IllegalArgumentException if setText is over
         * MAX_MESSAGE_LENGTH characters.
         */
        public      Message(final PhoneNumber setPhoneNumber, final String setText)
            throws IllegalArgumentException {
            this(Collections.singleton(setPhoneNumber), setText);
        }

        /**
         * @throws IllegalArgumentException if setText is over
         * MAX_MESSAGE_LENGTH characters.
         */
        public      Message(final Set<PhoneNumber> setPhoneNumbers, final String setText)
            throws IllegalArgumentException {
            if (setText.length() > MAX_MESSAGE_LENGTH) {
                throw new IllegalArgumentException("Message too long at "
                    + setText.length() + " characters, maximum length "
                    + MAX_MESSAGE_LENGTH + " characters.");
            }

            this.phoneNumbers = setPhoneNumbers;
            this.text = setText;
        }
    }

    /**
     * Class to wrap around phone numbers, to provide type safety and input
     * validation.
     */
    public static class PhoneNumber extends Object {
        private final String numberStr;

        /**
         * @throws IllegalArgumentException if the given number string is not
         * a valid phone number.
         */
        public      PhoneNumber(final String setNumberStr)
            throws IllegalArgumentException {
            final char[] chars = setNumberStr.toCharArray();

            if (chars.length < 3) {
                throw new IllegalArgumentException("Phone number is far too short to be a valid phone number; require at least 3 characters.");
            }

            for (int charIdx = 0; charIdx < chars.length; charIdx++) {
                if (!Character.isDigit(chars[charIdx])) {
                    throw new IllegalArgumentException("Non-numeric character in number: "
                        + chars[charIdx] + " at character "
                        + (charIdx + 1) + ".");
                }
            }
            this.numberStr = setNumberStr;
        }

        public String toString() {
            // XXX: Should be able to work with country codes that aren't
            // 2 digits.
            return "+"
                + this.numberStr.substring(0, 2) + "-"
                + this.numberStr.substring(2, this.numberStr.length());
        }
    }
}
