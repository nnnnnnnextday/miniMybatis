package com.chenxi.jdbc;

import com.chenxi.config.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;

public class Connections {

    public static Connection getConnection(Configuration configuration) {
        Connection connection = null;
        try {
            //加载驱动
            Class.forName(configuration.getDriver());
            connection = DriverManager.getConnection(configuration.getUrl(), configuration.getUserName(), configuration.getPassWord());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connection;

    }

}
