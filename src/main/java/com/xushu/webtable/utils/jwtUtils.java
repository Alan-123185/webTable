package com.xushu.webtable.utils;

import com.xushu.webtable.common.Const;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class jwtUtils {
    @Value("${jwt.secret-key}")
    private String keyConfig;
    @Value("${jwt.expiration}")
    private long expConfig;

    private static String secret_key;
    private static long expiration;

    @PostConstruct
    public void init() {
    // 将配置的密钥进行 Base64 编码后赋值给静态变量，供静态方法使用
        secret_key = Base64.getEncoder().encodeToString(keyConfig.getBytes());
    // 将配置的过期时间赋值给静态变量
        expiration = expConfig;
    }
    public static String makejwt(String username,Long id,Integer role){
        // 计算过期时间：当前时间 + 3600秒（1小时）
        Date date=new Date(System.currentTimeMillis()+expiration);
        // 创建Map存储要放入token中的数据
        Map<String,Object> map=new HashMap<>();
        map.put(Const.JWT_CLAIM_USERNAME,username);  // 存入用户名
        map.put(Const.JWT_CLAIM_ID,id);    // 存入用户ID
        map.put(Const.JWT_CLAIM_ROLE,role);   //权限标记
        
        // 构建JWT令牌
        String jwtstr = Jwts.builder()
                .signWith(SignatureAlgorithm.HS256,secret_key)  // 使用HS256算法和密钥签名
                .addClaims(map)          // 添加自定义 claims（用户信息）
                .setExpiration(date)     // 设置过期时间
                .compact();              // 生成最终的token字符串
        
        return jwtstr;
    }
    public static Map<String,Object> parsejwt(String jwtstr){
        // 解析JWT并获取payload部分
        Claims body = Jwts.parser()
                .setSigningKey(secret_key)   // 设置签名密钥用于验证
                .parseClaimsJws(jwtstr)      // 解析并验证JWT
                .getBody();                  // 获取payload部分
        
        // Claims继承自Map<String, Object>，可以直接返回
        return body;
    }
}
