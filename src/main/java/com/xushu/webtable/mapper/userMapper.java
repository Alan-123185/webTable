package com.xushu.webtable.mapper;

import com.xushu.webtable.common.User;
import com.xushu.webtable.common.selectUserBean;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface userMapper {

    @Insert("insert into banned_user(user_id,reason) values #{userId},#{reason})")
    void ban(Integer userId,String reason);

    @Select("select COUNT(*) from banned_user where user_id=#{userId}")
    Integer findBannedUser(Integer userId);

    @Delete("delete from banned_user where user_id=#{userId}")
    void unban(Integer userId);

    List<selectUserBean> selectUserByName(String userName);

    @Update("update user set status=0 where id=#{userId}")
    void unbanforstatus(Integer userId);

    List<selectUserBean> selectBannedByName(String userName);

}
