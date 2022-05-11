package com.oneQuset.oneQuset.Domain.Entity.user;

import com.oneQuset.oneQuset.Domain.Entity.enum_type.Role;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter @Setter
public class User {
    @Id
    @GeneratedValue
    private String id;
    private String nickname;
    private String profile_image; // CLOB

    @Enumerated(EnumType.STRING)
    private Role role;

}
