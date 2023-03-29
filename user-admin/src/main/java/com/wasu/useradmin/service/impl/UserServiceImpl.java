package com.wasu.useradmin.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.wasu.useradmin.entity.User;
import com.wasu.useradmin.mapper.UserMapper;
import com.wasu.useradmin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;

    //搜索所有的User
    @Override
    public List<User> searchAllUsers() {
        List<User> userList = userMapper.selectList(null);
        return userList;
    }

    //搜索当前自增Id
    @Override
    public int searchId() {
        return userMapper.searchId();
    }

    //通过username查找user，返回结果为人数
    @Override
    public int searchByName(String username) {
        return userMapper.searchByName(username);
    }

    @Override
    public int updateById(User user) {
        return userMapper.updateById(user);
    }

    @Override
    public User searchById(int id) {
        return userMapper.selectById(id);
    }

    @Override
    public int insert(User user) {
        return userMapper.insert(user);
    }

    @Override
    public User searchUserByName(String name) {
        return userMapper.searchUserByName(name);
    }

    @Override
    public List<User> search(Wrapper wrapper) {
        return userMapper.selectList(wrapper);
    }
}
