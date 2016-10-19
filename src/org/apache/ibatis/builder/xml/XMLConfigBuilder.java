/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;

/**
 * 解析mybatis的配置文件信息类，以configuration标签开始。 
 * @author Clinton Begin
 */
public class XMLConfigBuilder extends BaseBuilder {

  private boolean parsed;	//是否已经被解析标志
  private XPathParser parser;	//解析帮助
  private String environment;	//默认的数据库配置的id
  private ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();	//

  public XMLConfigBuilder(Reader reader) {
    this(reader, null, null);
  }

  public XMLConfigBuilder(Reader reader, String environment) {
    this(reader, environment, null);
  }

  public XMLConfigBuilder(Reader reader, String environment, Properties props) {
    this(new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  public XMLConfigBuilder(InputStream inputStream) {
    this(inputStream, null, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment) {
    this(inputStream, environment, null);
  }

  /**
   * 由SqlSessionFactoryBuilder调用
   * @param inputStream：外部传入的流，由mybatis配置文件产生
   * @param environment：外部传入的数据库环境，mybatis配置信息中
   * @param props：外部传入的Properties配置
   */
  public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
	  //先创建一个XPathParser对象，再创建当前的类对象
	  this(new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  /**
   * 构造函数，初始化参数
   * @param parser
   * @param environment
   * @param props
   */
  private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {
    super(new Configuration());
    ErrorContext.instance().resource("SQL Mapper Configuration");
    this.configuration.setVariables(props);		//给父类中的成员变量设置值
    this.parsed = false;	//尚未解析
    this.environment = environment;	//数据库环境
    this.parser = parser;	//XPathParser解析器
  }

  /**
   * @description 配置文件解析， <configuration></>标签为起始标签
   * @author xudj
   * @date 2016年4月20日 下午5:36:24
   * @return
   */
  public Configuration parse() {
    if (parsed) {	//解析过了则抛出异常
      throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    parsed = true;		//设置解析标志
    parseConfiguration(parser.evalNode("/configuration"));	//解析configuration标签，返回XNode对象
    return configuration;
  }

  /**
   * @description 解析配置文件configuration标签下的子节点(共11个)
   * @author xudj
   * @date 2016年4月20日 下午5:37:41
   * @param XNode 节点解析帮助类
   */
  private void parseConfiguration(XNode root) {
    try {
    	//加载settings下的配置
      Properties settings = settingsAsPropertiess(root.evalNode("settings"));
      
      //issue #117 read properties first
      propertiesElement(root.evalNode("properties"));	//解析properties配置
      loadCustomVfs(settings);	//设置VFS，根据setting中配置的vfsImpl TODO
      typeAliasesElement(root.evalNode("typeAliases"));	//别名配置
      pluginElement(root.evalNode("plugins"));	//插件(拦截器)
      objectFactoryElement(root.evalNode("objectFactory"));	//对象工厂
      objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));//对象包装工厂
      reflectionFactoryElement(root.evalNode("reflectionFactory"));
      settingsElement(settings);//根据settings的之标签配置全局设置
      // read it after objectFactory and objectWrapperFactory issue #631
      environmentsElement(root.evalNode("environments"));//数据库环境配置加载
      databaseIdProviderElement(root.evalNode("databaseIdProvider"));
      typeHandlerElement(root.evalNode("typeHandlers"));
      mapperElement(root.evalNode("mappers"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }

  /**
   * @description 将节点settings配置信息加到Properties中返回
   * @author xudj
   * @date 2016年4月22日 下午3:48:17
   * @param context：settings节点
   * @return
   */
  private Properties settingsAsPropertiess(XNode context) {
	  //没有配置settings
    if (context == null) {
      return new Properties();
    }
    //查询settings下的setting配置
    Properties props = context.getChildrenAsProperties();
    // Check that all settings are known to the configuration class
    //检查settings中的配置的key是否存在
    MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
    for (Object key : props.keySet()) {
      if (!metaConfig.hasSetter(String.valueOf(key))) {
        throw new BuilderException("The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
      }
    }
    return props;
  }

  private void loadCustomVfs(Properties props) throws ClassNotFoundException {
    String value = props.getProperty("vfsImpl");
    if (value != null) {
      String[] clazzes = value.split(",");//配置多个时，使用全限定名并用逗号隔开
      for (String clazz : clazzes) {
        if (!clazz.isEmpty()) {
          configuration.setVfsImpl(Resources.classForName(clazz));//设置对应的类型
        }
      }
    }
  }

  /**
   * @description 配置信息中的别名 
   * @author xudj
   * @date 2016年4月22日 下午4:51:01
   * @param parent
   */
  private void typeAliasesElement(XNode parent) {
	  //有typeAliases配置
    if (parent != null) {
    	//遍历孩子节点
      for (XNode child : parent.getChildren()) {
    	  //如果节点名是package，即配置了package
        if ("package".equals(child.getName())) {
          String typeAliasPackage = child.getStringAttribute("name");
          //设置别名，参数时package配置的要扫描的包名
          configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
        } else {
          String alias = child.getStringAttribute("alias");
          String type = child.getStringAttribute("type");
          try {
            Class<?> clazz = Resources.classForName(type);
            //typeAliasRegistry为父类中的成员
            if (alias == null) {
              typeAliasRegistry.registerAlias(clazz);
            } else {
              typeAliasRegistry.registerAlias(alias, clazz);
            }
          } catch (ClassNotFoundException e) {
            throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
          }
        }
      }
    }
  }

  /**
   * @description 插件(拦截器)初始化，对于配置文件plugins标签
   * @author xudj
   * @date 2016年9月8日 下午10:47:36
   * @param parent
   * @throws Exception
   */
  private void pluginElement(XNode parent) throws Exception {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        String interceptor = child.getStringAttribute("interceptor");
        Properties properties = child.getChildrenAsProperties();
      //通过反射获取对象，先从<plugin interceptor="">中获取interceptor元素值，并先从类型别名typeAliasRegistry中查询
        Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
        interceptorInstance.setProperties(properties);	//执行实现了Interceptor的类中的setProperties()方法获取配置中的值
        configuration.addInterceptor(interceptorInstance);	//添加到配置信息中
      }
    }
  }

  /**
   * @description 解析objectFactory标签
   * @author xudj
   * @date 2016年9月17日 下午5:20:16
   * @param context
   * @throws Exception
   */
  private void objectFactoryElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      Properties properties = context.getChildrenAsProperties();//获取当前节点的孩子节点转换成Properties对象
      ObjectFactory factory = (ObjectFactory) resolveClass(type).newInstance();
      factory.setProperties(properties);
      configuration.setObjectFactory(factory);
    }
  }

  private void objectWrapperFactoryElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).newInstance();
      configuration.setObjectWrapperFactory(factory);
    }
  }

  private void reflectionFactoryElement(XNode context) throws Exception {
    if (context != null) {
       String type = context.getStringAttribute("type");
       ReflectorFactory factory = (ReflectorFactory) resolveClass(type).newInstance();
       configuration.setReflectorFactory(factory);
    }
  }

  /**
   * @description properties配置(引用外部的配置信息)<properties resource="" url="">
   * 注意：同名的配置信息，<property name="" value=""/>配置会被resource或url中的同名配置覆盖，resource或url中的会被在创建SqlSessionFactory应用的外部properties配置的同名属性覆盖。
   * @author xudj
   * @date 2016年4月22日 下午4:04:21
   * @param context：properties配置节点
   * @throws Exception
   */
  private void propertiesElement(XNode context) throws Exception {
	  //存在的话
    if (context != null) {
    	//获取子节点的配置信息<property name="" value=""/>
      Properties defaults = context.getChildrenAsProperties();
      //resource
      String resource = context.getStringAttribute("resource");
      //url
      String url = context.getStringAttribute("url");
      //如果同时配置resource和url的话则报异常
      if (resource != null && url != null) {
        throw new BuilderException("The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
      }
      //注意：defaults此时为<property name="" value=""/>的配置信息，接下来，resource或url中的同名配置信息将会覆盖defaults的。
      if (resource != null) {//配置了resource
        defaults.putAll(Resources.getResourceAsProperties(resource));
      } else if (url != null) {
        defaults.putAll(Resources.getUrlAsProperties(url));
      }
      //获取创建sqlSessionFactory传的串
      Properties vars = configuration.getVariables();
      if (vars != null) {
        defaults.putAll(vars);	//注意：会覆盖配置信息里面的同名配置
      }
      parser.setVariables(defaults);//重置XpathParser中的配置信息
      configuration.setVariables(defaults);//同理。配置信息设置到Configuration中
    }
  }

  /**
   * @description 设置settings中的配置。props.getProperty("","");  第一个参数表示要获取的props的key，第二参数为默认值。
   * @author xudj
   * @date 2016年9月17日 下午5:32:03
   * @param props
   * @throws Exception
   */
  private void settingsElement(Properties props) throws Exception {
    configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));//自动映射行为
    configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));//二级缓存
    configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));//代理工厂
    configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));//懒加载
    configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), true));//懒加载时，调用某属性是否加载所有属性，和lazyLoadingEnabled同时配置
    configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));//是否允许单一语句返回多结果集（需要兼容驱动）。
    configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));//是否使用列标签(别名)代替列名(pojo的属性名)
    configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));//是否使用JDBC的getGenereatedKeys方法获取主键并赋值到keyProperty设置的属性中
    configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));//执行器类型
    configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));//设置数据库回应超时时间，单位：秒
    configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));//每次批量返回的结果行数
    configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));//是否将列名按驼峰命名转换
    configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));//是否允许在嵌套语句中使用分页(行分界)，默认不允许
    configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));//本地缓存机制,默认为session(会话中)
    configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));//当没有为参数提供特定的 JDBC 类型时，为空值指定 JDBC 类型
    configuration.setLazyLoadTriggerMethods(stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));//指定触发延迟加载的对象的方法(另有get，set ,is)
    configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));//允许在嵌套语句中使用分页（ResultHandler）(自定义的ResultMap)
    configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));//指定动态 SQL生成 的默认语言
    configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));//resultType="map"时，是否将没有值的key放入map中
    configuration.setLogPrefix(props.getProperty("logPrefix"));//指定 MyBatis 增加到日志名称的前缀
    configuration.setLogImpl(resolveClass(props.getProperty("logImpl")));//指定 MyBatis 所用日志的具体实现，未指定时将自动查找
    configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
  }

  /**
   * @description 解析数据库配置environment标签
   * @author xudj
   * @date 2016年9月20日 下午9:15:01
   * @param context
   * @throws Exception
   */
  private void environmentsElement(XNode context) throws Exception {
    if (context != null) {	//配置environments标签情况下
      if (environment == null) {
        environment = context.getStringAttribute("default");	//<environments default="environment"></environments>
      }
      for (XNode child : context.getChildren()) {
        String id = child.getStringAttribute("id");	//<environment id=""></environment>
        if (isSpecifiedEnvironment(id)) {//返回false则不做处理，只处理environments的default属性值与某个environment标签的id属性值向相等的那个environment标签的配置
          TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));//根据transactionManager标签中的type元素值创建TransactionFactory对象，默认可配置"jdbc"和"managed"
          DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));//
          DataSource dataSource = dsFactory.getDataSource();
          Environment.Builder environmentBuilder = new Environment.Builder(id)
              .transactionFactory(txFactory)
              .dataSource(dataSource);
          configuration.setEnvironment(environmentBuilder.build());
        }
      }
    }
  }

  private void databaseIdProviderElement(XNode context) throws Exception {
    DatabaseIdProvider databaseIdProvider = null;
    if (context != null) {
      String type = context.getStringAttribute("type");
      // awful patch to keep backward compatibility
      if ("VENDOR".equals(type)) {
          type = "DB_VENDOR";
      }
      Properties properties = context.getChildrenAsProperties();
      databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
      databaseIdProvider.setProperties(properties);
    }
    Environment environment = configuration.getEnvironment();
    if (environment != null && databaseIdProvider != null) {
      String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
      configuration.setDatabaseId(databaseId);
    }
  }

  /**
   * @description 创建事务管理器工厂
   * @author xudj
   * @date 2016年9月20日 下午9:21:57
   * @param context：environment标签下的transactionManager标签
   * @return
   * @throws Exception
   */
  private TransactionFactory transactionManagerElement(XNode context) throws Exception {
    if (context != null) {//配置了<transactionManager type=""/>标签的情况下
      String type = context.getStringAttribute("type");//获取type属性的值
      Properties props = context.getChildrenAsProperties();//获取该标签下的<property name="" value=""/>元素集合中的键值对存储在Properties对象中
      TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a TransactionFactory.");
  }

  /**
   * @description 创建数据库工厂
   * @author xudj
   * @date 2016年9月20日 下午9:34:52
   * @param context：environment标签下的dataSource标签
   * @return
   * @throws Exception
   */
  private DataSourceFactory dataSourceElement(XNode context) throws Exception {
    if (context != null) {//<dataSource type="">
      String type = context.getStringAttribute("type");//dataSource标签的type属性的值
      Properties props = context.getChildrenAsProperties();//获取该标签下的<property name="" value=""/>元素集合中的键值对存储在Properties对象中
      DataSourceFactory factory = (DataSourceFactory) resolveClass(type).newInstance();//获取datasource工程
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a DataSourceFactory.");
  }

  private void typeHandlerElement(XNode parent) throws Exception {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        if ("package".equals(child.getName())) {
          String typeHandlerPackage = child.getStringAttribute("name");
          typeHandlerRegistry.register(typeHandlerPackage);
        } else {
          String javaTypeName = child.getStringAttribute("javaType");
          String jdbcTypeName = child.getStringAttribute("jdbcType");
          String handlerTypeName = child.getStringAttribute("handler");
          Class<?> javaTypeClass = resolveClass(javaTypeName);
          JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
          Class<?> typeHandlerClass = resolveClass(handlerTypeName);
          if (javaTypeClass != null) {
            if (jdbcType == null) {
              typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
            } else {
              typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
            }
          } else {
            typeHandlerRegistry.register(typeHandlerClass);
          }
        }
      }
    }
  }

  private void mapperElement(XNode parent) throws Exception {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        if ("package".equals(child.getName())) {
          String mapperPackage = child.getStringAttribute("name");
          configuration.addMappers(mapperPackage);
        } else {
          String resource = child.getStringAttribute("resource");
          String url = child.getStringAttribute("url");
          String mapperClass = child.getStringAttribute("class");
          if (resource != null && url == null && mapperClass == null) {
            ErrorContext.instance().resource(resource);
            InputStream inputStream = Resources.getResourceAsStream(resource);
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
            mapperParser.parse();
          } else if (resource == null && url != null && mapperClass == null) {
            ErrorContext.instance().resource(url);
            InputStream inputStream = Resources.getUrlAsStream(url);
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
            mapperParser.parse();
          } else if (resource == null && url == null && mapperClass != null) {
            Class<?> mapperInterface = Resources.classForName(mapperClass);
            configuration.addMapper(mapperInterface);
          } else {
            throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
          }
        }
      }
    }
  }

  /**
   * @description 判断是否是属于专门指定的一个Environment，根据id与default判断
   * @author xudj
   * @date 2016年9月27日 下午8:50:40
   * @param id
   * @return
   */
  private boolean isSpecifiedEnvironment(String id) {
    if (environment == null) {	//没有指定environments标签的default属性的值，抛异常No environment specified：没有environment被指定
      throw new BuilderException("No environment specified.");
    } else if (id == null) {//没有指定environment标签的id属性的值时，Environment必须有一个id属性
      throw new BuilderException("Environment requires an id attribute.");
    } else if (environment.equals(id)) {//environments的default属性值与某个environment标签的id属性值向相等时才返回true
      return true;
    }
    return false;
  }

}
