package com.xushu.webtable.mapper;

import com.xushu.webtable.common.File;
import com.xushu.webtable.common.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface uploadMapper {
    
    // 根据 MD5 查询文件是否存在
    @Select("select * from file where file_name=#{md5} and ref_count>0 limit 1")
    File selectByMd5(String md5);
    
    // 插入文件记录（新架构：包含 ref_count）
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into file (file_name, original_file_name, user_id, file_size, path, create_time, ref_count,is_folder,parent_id) values (#{fileName}, #{originalFileName}, #{userId}, #{fileSize}, #{path}, #{createTime},#{refCount},#{isFolder},#{parentId})")
    void insertRecord(File file);
    
    // 引用次数 +1
    @Update("update file set ref_count = ref_count + 1 where id=#{id}")
    void updateRefCount(Integer id);

    //引用次数 -1
    @Update("update file set ref_count = ref_count - 1 where id= #{id} and ref_count>0")
    void updateRefCount2(Integer id);

    @Select("select * from user where id=#{userId}")
    User selectUserById(Integer userId);

    @Update ("update user set volume =#{volume} where id=#{id}")
    void updateVolume(User user);

}
