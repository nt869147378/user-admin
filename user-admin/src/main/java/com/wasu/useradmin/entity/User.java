package com.wasu.useradmin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("user")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private int id;//自增id
    private String username;//用户名
    private String password;//密码
    private int sex;//性别 1-男 0-女
    private String phone;//手机号
    private String address;//地址
}
