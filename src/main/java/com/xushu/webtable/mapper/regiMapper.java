package com.xushu.webtable.mapper;

import com.xushu.webtable.common.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface regiMapper {
    /**
     * 注册用户
     * @Options 注解说明：
     * - useGeneratedKeys = true: 指示 MyBatis 使用数据库自动生成的主键（如 MySQL 的 AUTO_INCREMENT）。
     * - keyProperty = "id": 指定将生成的主键值设置到传入的 User 对象的哪个属性中（即 user.setId(...)）。
     * - keyColumn = "id": 指定数据库表中对应的主键列名。
     *
     * 注意：当使用 useGeneratedKeys 时，通常建议返回类型为 void 或 int/Integer（表示受影响行数），
     * 生成的主键会直接回填到 user 对象中，而不是通过返回值获取。
     */
    @Insert("insert into user (user_name,password,all_volume,email,role) values (#{userName},#{password},#{AllVolume},#{email},#{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int register(User user);

}
