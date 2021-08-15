package com.chenxi.session;

//TODO  是mybatis暴露给外部的接口

import java.util.List;

public interface SqlSession {

    <T> T selectOne(String statement, Object parameter);

    <E> List<E> selectList(String statement, Object parameter);

    <T> T getMapper(Class<T> type);

}
