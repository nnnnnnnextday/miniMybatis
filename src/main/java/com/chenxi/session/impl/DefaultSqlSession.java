package com.chenxi.session.impl;

import com.chenxi.config.Configuration;
import com.chenxi.config.MappedStatement;
import com.chenxi.executor.Executor;
import com.chenxi.executor.impl.DefaultExecutor;
import com.chenxi.proxy.MappedProxy;
import com.chenxi.session.SqlSession;

import java.lang.reflect.Proxy;
import java.util.List;

// TODO 是SqlSession接口的实现类，要完成Configuration的单例生产和通过Executor执行器组件来完成交互数据库
public class DefaultSqlSession implements SqlSession {

    private Configuration configuration;

    private final Executor executor;

    public DefaultSqlSession(Configuration configuration) {
        super();
        this.configuration = configuration;
        this.executor = new DefaultExecutor(configuration);
    }

    @Override
    public <T> T selectOne(String statement, Object parameter) {
        List<T> list = this.selectList(statement, parameter);
        if (list == null || list.size() == 0) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        } else {
            throw new RuntimeException("数据过多，有误");
        }
    }

    @Override
    public <E> List<E> selectList(String statement, Object parameter) {
        MappedStatement smt = this.configuration.getStatementMap().get(statement);
        return executor.query(smt, parameter);
    }

    //动态代理
    @Override
    public <T> T getMapper(Class<T> type) {
        MappedProxy mappedProxy = new MappedProxy(this);
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, mappedProxy);
    }
}
