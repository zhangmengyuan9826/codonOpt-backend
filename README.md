# Codon Optimization Project

## 项目简介

Codon Optimization (密码子优化) 项目

## 技术栈

- Java 1.8
- Spring Boot 2.7.18
- Maven

## 项目结构

```
codonOpt/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── codonopt/
│   │   └── resources/
│   └── test/
│       └── java/
└── pom.xml
```

## 快速开始

### 编译项目

```bash
mvn clean compile
```

### 运行项目

```bash
mvn spring-boot:run
```

### 打包项目

```bash
mvn clean package
```

### 运行 JAR

```bash
java -jar target/codonOpt-1.0.0.jar
```

## 配置说明

- 默认端口: 8080
- 上下文路径: /
- 配置文件: src/main/resources/application.yml

## 开发说明

项目使用 Spring Boot 框架，支持热部署和快速开发。
