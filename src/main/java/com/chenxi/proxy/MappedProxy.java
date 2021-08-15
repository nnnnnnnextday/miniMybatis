package com.chenxi.proxy;

import com.chenxi.session.SqlSession;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;

public class MappedProxy implements InvocationHandler {

    private SqlSession sqlSession;

    public MappedProxy(SqlSession sqlSession) {
        super();
        this.sqlSession = sqlSession;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> returnType = method.getReturnType();
        //判断返回值是否为Collection的子类,若是则直接返回集合类
        if (Collection.class.isAssignableFrom(returnType)){
            //实现转发，通过拼接得到参数（类 + . + 方法名 + 参数）
            return sqlSession.selectList(method.getDeclaringClass().getName() + "." + method.getName(), args == null ? null : args[0]);
        } else {
            return sqlSession.selectOne(method.getDeclaringClass().getName() + "." + method.getName(), args == null ? null : args[0]);
        }
    }
}
