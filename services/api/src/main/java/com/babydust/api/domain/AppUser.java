package com.babydust.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String openid;

    @Column(nullable = false)
    private String nickname;

    protected AppUser() {
    }

    public AppUser(String openid, String nickname) {
        this.openid = openid;
        this.nickname = nickname;
    }

    public String getOpenid() {
        return openid;
    }

    public String getNickname() {
        return nickname;
    }
}
