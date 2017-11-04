package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(value = { "graphRoles" })
public class TermedUser {

    private final String username;
    private final String password;
    private final String appRole;


    // Jackson constructor
    private TermedUser() {
        this("", "", "");
    }

    public TermedUser(String username, String password, String appRole) {
        this.username = username;
        this.password = password;
        this.appRole = appRole;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAppRole() {
        return appRole;
    }

    @Override
    public String toString() {
        return "TermedUser{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", appRole='" + appRole + '\'' +
                '}';
    }
}
