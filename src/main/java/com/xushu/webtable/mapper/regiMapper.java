package com.xushu.webtable.mapper;

import com.xushu.webtable.common.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface regiMapper {
    @Insert("insert into user (user_name,password,volume) values (#{userName},#{password},#{volume})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    public Integer register(User user);
}
