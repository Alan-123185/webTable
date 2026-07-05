package com.xushu.webtable.mapper;

import com.xushu.webtable.common.File;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface recycleBinMapper {

    void deletebinfile(List<Integer> ids);

    @Select("select count(*) from recycle_bin where file_name=#{fileName}")
    int binfilecount(String fileName);

    List<Integer> getrubish();


    List<File> getRecycleFiles(Integer userId);

    @Insert("Insert into recycle_bin " +
            "(file_id,user_id,create_time,file_size,file_name,recycle_time,path,is_folder,parent_id,original_file_name) " +
            "values " +
            "(#{f.id},#{f.userId},#{f.createTime},#{f.fileSize},#{f.fileName},#{cycleTime},#{f.path},#{f.isFolder},#{f.parentId},#{f.originalFileName})")
    Integer recyclefile(@Param("f") File f, @Param("cycleTime") LocalDateTime cycleTime);

    File selectFileInBin(Integer Id);

    @Select("select recycle_time from recycle_bin where id=#{Id}")
    LocalDateTime getrecycletime(Integer Id);



}
