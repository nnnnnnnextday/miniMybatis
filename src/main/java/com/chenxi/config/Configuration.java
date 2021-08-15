package com.chenxi.config;

//TODO 脱离Spring框架，对应mybatis-config.xml。用于读取配置文件，包括db.properties,和各类mapper.xml,这里为了简化，配置文件只与数据库有关

import java.util.HashMap;
import java.util.Map;

public class Configuration {

    private String driver;
    private String url;
    private String userName;
    private String passWord;

    private Map<String, MappedStatement> statementMap = new HashMap<String, MappedStatement>();

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassWord() {
        return passWord;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public Map<String, MappedStatement> getStatementMap() {
        return statementMap;
    }

    public void setStatementMap(Map<String, MappedStatement> statementMap) {
        this.statementMap = statementMap;
    }
}
