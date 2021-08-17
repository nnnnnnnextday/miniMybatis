## Mybatis核心流程

### Mybatis的使用所需要的要素

1. 实体类
2. mapper接口
3. mapper xml文件
4. Mybatis核心配置文件(如果脱离了Spring，那么则需要mybatis-config.xml配置文件，涉及到自动驼峰，懒加载，插件，环境等设置)

### Mybatis最“神奇”的地方

例如：在脱离Spring框架的情况下，有如下代码

```java
//--------第一阶段-------
//读取mybatis配置文件创建SqlSessionFactory
String resource = "mybatis-config.xml";
InputStream inputStream = Resources.getResourceAsStream(resource);
sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

//--------第二阶段---------
//获取sqlSession
SqlSession sqlSession = sqlSessionFactory.openSession();
//获取对应的mapper
TUserMapper mapper = sqlSession.getMapper(TUserMapper.class);

//--------第三阶段----------
//执行查询语句并返回数据
TUser user = mapper.selectByPrimaryKey(2);
```

Mybatis最“神奇”的地方在于，其第二阶段能够将没有实现类的mapper接口进行实例化。

### Mybatis核心流程的三阶段

初始化阶段：读取XML配置文件和注解中的信息，创建配置对象，并且完成模块初始化工作。

代理阶段：封装iBatis的编程模型，为Mapper接口初始化打基础。（承上启下）

数据读写阶段：完成SQL解析，参数映射，SQL执行和结果反射。

## 具体实现之第一阶段

### mybatis核心配置文件的简化

简化到只配置数据库的必要信息。

其他配置文件，也只需要配置数据库访问所需信息。

Configuration类则映射核心配置文件，那么本项目的Configuration类只需要

```java
private String driver;
private String url;
private String userName;
private String passWord;
```

### Mybatis SQL语句信息四要素

namespace id resultType SQL语句

mybatis里则是通过MappedStatement来封装信息的。

#### 那么如何把配置文件信息解析并填充到Configuration类和MappedStatement类？

答案：**SqlSessionFactory，该类在初始化过程中完成配置文件的解析和相关类的填充**

在Mybatis的SqlSessionFactory相关源码中:

```java
private final Configuration configuration
```

定义为final的原因是：

* Configuration对象的生产是符合单例模式的，加上final，保证其规范和数据安全。
* 从性能方面考虑，Configuration对象在整个过程中都会使用，final字段会被缓存，提升了读效率。
* 源码底层是使用数据库池化技术的。

本项目中实现配置文件的解析和相关填充，同Mybatis源码一样，是通过dom4j已有的方法实现的。

#### SqlSession相关

SqlSessionFactory是用来生产连接信息的，是单例的，是惟一的（只返回引用，而不是new出来的）；

而SqlSession是用来访问一次数据库的，代表了一次与数据库的连接，是Mybatis对外提供数据访问的**唯一**API，第三阶段的mapper.selectByPrimaryKey(2)来访问数据库，其实等价于sqlSession.selectOne("...(路径).selectByPrimaryKey",2),这也是iBatis的本质。但实际上sqlSession的功能都是基于Excutor（执行器组件，可以通过query()等方法来访问数据库并进行映射）实现的，是一种“委托”的思想。

**至此，第一阶段完成。该阶段涉及到的文件有配置文件，Configuration类，MappedStatement类，pojo类以及最重要的SqlSessionFactory（当然和数据库交互的SqlSession接口也可以在这个阶段完成，因为毕竟SqlSessionFactory需要一个只传递引用且返回SqlSession的openSession方法）**

## 具体实现之第二阶段（重点：如何实现“通过一次连接，拿到mapper的实现类”？）

第二阶段的核心代码：

```java
SqlSession sqlSession = sqlSessionFactory.openSession();//获取sqlSession
TUserMapper mapper = sqlSession.getMapper(TUserMapper.class);//获取对应的mapper
```

### SqlSession的实现：DefaultSqlSession

DefaultSqlSession负责两点：

* 对外提供数据访问API：

  根据需要，定义方法即可

* 对内将请求转发给Excutor：

  Configuration类中有：

```java
private Map<String, MappedStatement> statementMap = new HashMap<String, MappedStatement>();
```

​		则sqlSession可以传来用于定位SQL语句的参数，而从Configuration实例中的statementMap中get(key)来得到MappedStatement类并作为query()方法的参数。

### 为什么可以通过Mapper接口就能访问数据库？

其实mapper.selectByPrimaryKey(2)在内部会转换成sqlSession.selectOne("...(路径).selectByPrimaryKey",2)。

但这是基于配置文件和动态代理的，具体表现为：

* 找到session中对应的方法执行
* 找到命名空间和方法名
* 传递参数

例如：在第三阶段的

```java
TUser user = mapper.selectByPrimaryKey(id);
```

执行时，其实Mybatis内部将请求转发给了SqlSession。

#### 那么是如何转发的，又是如何确定具体调用DefaultSqlSession的哪一个API？

* 找到session中对应的方法执行是通过命名空间和方法名参数来确定的。

### getMapper方法的实现（动态代理）

```java
public <T> T getMapper(Class<T> type) {
        MappedProxy mappedProxy = new MappedProxy(this);
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, mappedProxy);
    }
```

#### 动态代理的原理机制

为什么Java的动态代理实现必须基于接口？（类不能多层继承）

为什么运行期间，可以生成接口的实现类？

其实这里是直接在自定义的DefaultSqlSession类中实现了getMapper方法，即在自定义的getMapper方法里调用反射的newProxyInstance方法来产生mapper的实现类。但是在Mybatis源码中，这个调用顺序是很复杂的，从官方SqlSession类的getMapper开始，往下是调用configuration实例的getMapper方法，再往下是调用mapperRegistry的getMapper方法。在mapperRegistry的getMapper方法中，会通过mapperProxyFactory这个代理工厂的newInstance方法来返回mapper的实现类。所以才说是动态代理和动态工厂的结合。

#### Pass:其实整个调用顺序（Mapper的SQL方法---Mybatis转交--->sqlSession的selectOne/selectList方法------>executor的query方法）中的第二步就是涉及到了invoke方法（invoke方法中会选择使用sqlSession的selectOne还是selectList方法）。本质就是：SqlSession获取Mapper接口时（getMapper），通过MapperProxyFactory对象实例化MapperProxy动态代理Mapper接口，执行Mapper接口的方法时，动态代理反射调用MapperProxy的invoke方法，根据接口与方法找到对应MappedStatement，用执行器组件的query方法来执行SQL。

![image-20210815150235280](https://github.com/nnnnnnnextday/miniMybatis/blob/master/image/089a30ff4311abe244188da89e0a93d.png)

![image-20210815150241222](https://github.com/nnnnnnnextday/miniMybatis/blob/master/image/4610e9c5a714083e137c86ad136d3c5.png)

![image-20210815150245446](https://github.com/nnnnnnnextday/miniMybatis/blob/master/image/accf393f599d23e243d23815be4201d.png)

#### 本项目中动态代理的实现

关键词：Proxy，invocationHandler

Proxy：有一个静态方法，newProxyInstance()，这里配合第二阶段的代码理解

```java
SqlSession sqlSession = sqlSessionFactory.openSession();//获取sqlSession
TUserMapper mapper = sqlSession.getMapper(TUserMapper.class);//获取对应的mapper
```

项目里根本没有mapper的实现类，但是只要把class对象传进去，就能得到实现类，这就是newProxyInstance()方法的功劳，newProxyInstance()方法有三个参数：loader, interfaces, h

```java
//动态代理
    @Override
    public <T> T getMapper(Class<T> type) {
        MappedProxy mappedProxy = new MappedProxy(this);
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, mappedProxy);
    }
```

* loader：传进来的接口和其实现类，必然是同一个加载器。
* interface：传进来的接口即可
* h：“逻辑”，即接口中定义的方法，这里就需要讲到invocationHandler。

invocationHandler：这个接口只有一个方法 invoke(Object proxy, Method method, Object[] args)

“逻辑”即写在这个方法里。

**本项目中通过MappedProxy implements InvocationHandler来实现该接口**

```java
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    //最重要的三步    
    //拿到该方法的返回类型
    Class<?> returnType = method.getReturnType();
    //判断返回值是否为Collection的子类
    if (Collection.class.isAssignableFrom(returnType)) {
        //实现转发，通过拼接得到参数（类 + . + 方法名 + 参数）
        return sqlSession.selectList(method.getDeclaringClass().getName() + "." + method.getName(), args == null ? null : args[0]);
    } else {
        return sqlSession.selectOne(method.getDeclaringClass().getName() + "." + method.getName(), args == null ? null : args[0]);
    }
}
```

至此，第二阶段完成。本阶段主要是完成将Mapper接口实例化，让Mybatis完成转发请求到iBatis下进行原方法的查询。涉及到的文件有DefaultSqlSession类，MappedProxy类，Excutor接口和DefaultExcutor类

## 具体实现之第三阶段

补齐DefaultExcutor中的一些query方法和JDBC代码即可，涉及到反射。

Excutor内部底层遵循的规范就是JDBC规范。

反射：Mybatis在查询之后，根据不同的SQL语句，返回的对象类型是不一定一样的。

反射处理代码：

```java
private <E> void handleResult(ResultSet resultSet, List<E> ret, String className) {
        Class<E> clazz = null;
        //通过反射获取类的对象
        try {
            clazz = (Class<E>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            while (resultSet.next()) {
                //通过反射实例化对象
                Object model = clazz.newInstance();
                //使用反射工具将ResultSet中的数据填充到entity中
                long id = resultSet.getLong("id");
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");
                User user = (User) model;
                user.setId(id);
                user.setUsername(username);
                user.setPassword(password);
                ret.add((E) user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
```

