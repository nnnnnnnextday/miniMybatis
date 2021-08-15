package com.chenxi.session;

//TODO  1.读取配置文件，解析信息，填充到configuration中。2.生产SqlSession

import com.chenxi.config.Configuration;

import com.chenxi.config.MappedStatement;
import com.chenxi.session.impl.DefaultSqlSession;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;

public class SqlSessionFactory {
    private final Configuration configuration = new Configuration();


    //记录mapper.xml存放的文件夹位置
    private static final String MAPPER_CONFIG_LOCATION = "mapper";

    //记录数据路连接信息存放的文件
    private static final String DB_CONFIG_FILE = "db.properties";

    public SqlSessionFactory() {
        //完成数据的解析和填充
        loadDBInfo();
        loadMappersInfo();
    }

    //读取数据库配置文件信息
    private void loadDBInfo() {
        //加载数据库信息配置文件
        InputStream stream = SqlSessionFactory.class.getClassLoader().getResourceAsStream(DB_CONFIG_FILE);
        Properties properties = new Properties();
        try {
            properties.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //将数据库配置信息写入configuration对象中
        configuration.setDriver(properties.get("driver").toString());
        configuration.setUrl(properties.get("url").toString());
        configuration.setUserName(properties.get("username").toString());
        configuration.setPassWord(properties.get("password").toString());
    }

    //获取指定文件下的所有mapper.xml文件
    private void loadMappersInfo() {
        URL resource = null;
        resource = SqlSessionFactory.class.getClassLoader().getResource(MAPPER_CONFIG_LOCATION);
        File file = new File(resource.getFile());
        if (file.isDirectory()) {
            File[] mappers = file.listFiles();
            //遍历文件夹下所有的mapper.xml文件，解析后，注册到configuration中
            for (File mapper : mappers) {
                loadMapper(mapper);
            }
        }
    }

    //对mapper.xml文件解析
    private void loadMapper(File mapper) {
        //创建SAXReader对象,通过read方法读取一个文件,转换成Document对象
        SAXReader saxReader = new SAXReader();
        Document document = null;
        try {
            document = saxReader.read(mapper);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        //获取根节点元素对象<mapper>,依次获取命名空间，select标签和节点，将信息记录到MappedStatement对象并put入Configuration对象中
        Element rootElement = document.getRootElement();
        String namespace = rootElement.attribute("namespace").getData().toString();
        List<Element> selects = rootElement.elements("select");
        for (Element element : selects) {
            MappedStatement statement = new MappedStatement();
            String id = element.attribute("id").getData().toString();
            String resultType = element.attribute("resultType").getData().toString();
            String sql = element.getData().toString();
            String sourceId = namespace + "." + id;
            statement.setSourceId(sourceId);
            statement.setNamespace(namespace);
            statement.setResultType(resultType);
            statement.setSql(sql);
            configuration.getStatementMap().put(sourceId, statement);
        }

    }

    public SqlSession openSession()
    {
        //只传引用
        return new DefaultSqlSession(configuration);
    }
}
