// $Id: TwitterGateway.java 685 2009-11-08 01:12:26Z  $

package org.lostics.foxquant;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.Collections;
import java.util.Set;

import org.apache.log4j.Logger;

import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 * Twitter gateway for sending status updates via Twitter.
 */
public class TwitterGateway extends Thread {
    public  static final int    MAX_MESSAGE_LENGTH  = 140;

    /** Maximum length of the message queue. */
    public  static final int    MAX_QUEUE_LENGTH = 100;

    private static final Logger log = Logger.getLogger(TwitterGateway.class);

    private final String userName;
    private final String password;
    
    private boolean stop = false;
    
    // Once the constructor returns, most only be dealt with by the gateway
    // thread.
    private final Twitter twitter;

    private final BlockingQueue<String> messageQueue = new ArrayBlockingQueue<String>(MAX_QUEUE_LENGTH);
    
    public   TwitterGateway(final String setUsername, final String setPassword) {
        super();
        this.userName = setUsername;
        this.password = setPassword;
        this.twitter = new Twitter(this.userName, this.password);
        
        this.setName("Twitter");
    }

    public void close() {
        this.stop = true;
        this.interrupt();

        try {
            // Wait 1 second for this thread to exit, then give up.
            this.join(100);
        } catch(InterruptedException e) {
            // Yikes, we were quitting already, no need to nag.
        }

        return;
    }

    /**
     * Updates Twitter status.
     *
     * @return true if the message was successfully queued, false otherwise.
     * @throws IllegalArgumentException if message is over MAX_MESSAGE_LENGTH
     * characters.
     */
    public boolean updateStatus(final String message) {
        final boolean success;
        
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Twitter statuses cannot be longer than "
                + MAX_MESSAGE_LENGTH + "characters. Supplied message \""
                + message + "\" is "
                + message.length() + " characters.");
        }
        
        return this.messageQueue.offer(message);
    }

    public void run() {
        // Run until the stop flag is set
        while (!this.stop) {
            String message;
            
            try {
                message = this.messageQueue.poll(5, TimeUnit.SECONDS);
            } catch(InterruptedException e) {
                if (this.stop) {
                    return;
                }
                message = null;
            }
            
            while (null != message) {
                try {
                    twitter.updateStatus(message);
                } catch(TwitterException e) {
                    log.error("Twitter exception: ", e);
                }

                message = this.messageQueue.poll();
            }
        }
    }
}
