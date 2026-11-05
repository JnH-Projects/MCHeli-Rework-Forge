package com.jnhprojects.mcheli.workshop;

/** Something the workshop cannot do, with a message for the player to read. */
public class WorkshopException extends Exception {
    public WorkshopException(String message) {
        super(message);
    }
}
