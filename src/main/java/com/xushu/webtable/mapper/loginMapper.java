package com.xushu.webtable.mapper;

import com.xushu.webtable.common.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface loginMapper {
    @Select("select * from user where user_name=#{userName}")
    public User findName(String userName);
    @Select("select * from user where email= #{email}")
    public User findEmail(String email);
}
