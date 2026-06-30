package com.xushu.webtable.mapper;

import com.xushu.webtable.common.File;
import com.xushu.webtable.common.shareinfo;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface fileMapper {
    List<File> selectFile(String originalFileName, Integer userId, Integer parentId);

    void deleteFile(List<Integer> ids);

    @Select("select * from file where id=#{id}")
    File selectFileById(Integer id);

    @Insert("insert into file_link (file_id,link_name,lose_time) values (#{fileId},#{link},#{time1})")
    void share(Integer fileId, String link, LocalDateTime time1);

    @Select("select * from file_link where link_name=#{link}")
    shareinfo findlink(String link);

    @Delete("delete from file_link where file_id=#{fileId}")
    void deletelink(Integer fileId);

    @Select("select * from file_link where file_id=#{fileId}")
    List<shareinfo> selectlinkByfileId(Integer Fileid);

    @Update("update file set user_id=null where id=#{id0}")
    void cleanower(Integer id0);//消除原作者本人删除时候的所有权，直接把源文件userId设置为null

    //写一个根据父文件夹查找的方法
    @Select("select * from file where parent_id=#{parentId}")
    List<File> selectByParentId(Integer parentId);

    @Update("update file set original_file_name=#{newName} where id=#{fileId}")
    Integer rename(Integer fileId, String newName);

    @Update("update file set parent_id=#{parentId} where id= #{fileId}")
    void movedir(Integer fileId, Integer parentId);

}

