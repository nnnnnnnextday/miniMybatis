package com.chenxi.executor;

//TODO Mybatis的核心接口之一，SqlSession的功能都基于它来实现

import com.chenxi.config.MappedStatement;

import java.util.List;

public interface Executor {

    <E> List<E> query(MappedStatement statement, Object parameter);

}
