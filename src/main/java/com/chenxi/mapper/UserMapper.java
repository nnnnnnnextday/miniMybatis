package com.chenxi.mapper;

import com.chenxi.pojo.User;

import java.util.List;

public interface UserMapper {
    User selectUserById(Long id);

    List<User> selectList();
}
