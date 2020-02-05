package io.precognito.services.security;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import javax.ws.rs.core.MediaType;

/**
 */
@RegisterForReflection
public class UserMeta {

    @FormParam("username")
    @PartType(MediaType.TEXT_PLAIN)
    public String username;

    @FormParam("password")
    @PartType(MediaType.TEXT_PLAIN)
    public String pwd;

    public UserMeta(){}
    public UserMeta(String username, String password) {
        this.username = username;
        this.pwd = password;

    }

    public boolean auth(String userDomain, String userHashes) {
        return username.contains(userDomain) && userHashes.contains(""+pwd.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserMeta userMeta = (UserMeta) o;

        if (username != null ? !username.equals(userMeta.username) : userMeta.username != null) return false;
        return pwd != null ? pwd.equals(userMeta.pwd) : userMeta.pwd == null;
    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (pwd != null ? pwd.hashCode() : 0);
        return result;
    }
}
