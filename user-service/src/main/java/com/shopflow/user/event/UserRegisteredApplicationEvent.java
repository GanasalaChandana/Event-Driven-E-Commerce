package com.shopflow.user.event;

import org.springframework.context.ApplicationEvent;

public class UserRegisteredApplicationEvent extends ApplicationEvent {

    private final String userEmail;
    private final String userName;

    public UserRegisteredApplicationEvent(Object source, String userEmail, String userName) {
        super(source);
        this.userEmail = userEmail;
        this.userName = userName;
    }

    public String getUserEmail() { return userEmail; }
    public String getUserName() { return userName; }
}
