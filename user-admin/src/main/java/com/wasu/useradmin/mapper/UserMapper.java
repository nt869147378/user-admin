package com.wasu.useradmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wasu.useradmin.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User> {

    @Select("select @@IDENTITY")
    int searchId();

    @Select("select count(*) from user where username = #{username} ")
    int searchByName(@Param("username") String username);

    @Select("select * from user where username = #{username} ")
    User searchUserByName(@Param("username") String username);
}
