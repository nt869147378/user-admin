package com.wasu.useradmin.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.wasu.useradmin.entity.User;

import java.util.List;
import java.util.Map;

public interface UserService {
    List<User> searchAllUsers();
    int searchId();
    int searchByName(String name);
    int updateById(User user);
    User searchById(int id);
    int insert(User user);
    User searchUserByName(String name);
    List<User> search(Wrapper wrapper);
}
