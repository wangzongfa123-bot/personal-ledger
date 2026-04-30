# 桌面客户端（Java + JavaFX）

通过 HTTP 调用 Python 后端的 REST API，实现与 Web 端共享同一份数据。

## 环境

- JDK 17+
- Maven 3.8+
- 后端服务已启动（默认 `http://127.0.0.1:8000`）

## 运行

```bash
cd desktop

# 直接运行（推荐：自动下载 JavaFX 依赖）
mvn -q javafx:run

# 或先编译再运行
mvn -q -DskipTests package
mvn -q javafx:run
```

启动后顶部可以修改 API 地址，点击「连接」即切换到指定后端。

## 功能

- 记账（支出 / 收入、分类、日期、备注）
- 总收入 / 总支出 / 结余 / 笔数 概览
- 流水列表（按类型 / 分类筛选、删除）
- 与 Web 端 / API 端实时共享数据
