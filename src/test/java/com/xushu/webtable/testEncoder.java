package com.xushu.webtable;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class testEncoder {
    public static void main(String[] args) {
        String str = "Ycy54088dbd";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodedStr = encoder.encode(str);
        System.out.println(encodedStr);
    }
}
