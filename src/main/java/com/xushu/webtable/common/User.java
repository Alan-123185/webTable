package com.xushu.webtable.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Long id;
    private String userName;
    private String password;
    private Long volume;
    private String email;
    private Long AllVolume;
    private Integer role;
    public User(String userName, String password,String email){
        this.userName=userName;
        this.password=password;
        this.email=email;
        this.AllVolume=1073741824L;//用户的额定总容量，之后可以通过其他途径改变
    }
    public User(String userName, String password,String email,Integer role){
        this.email=email;
        this.userName=userName;
        this.password=password;
        this.role=role;
        this.AllVolume=1073741824L;//用户的额定总容量，之后可以通过其他途径改变
    }
}
