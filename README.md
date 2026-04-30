# personal-ledger · 多端记账程序

一个 **Python + Java** 实现的多端个人记账程序。

- **后端**：Python + FastAPI + SQLite，提供 RESTful API
- **Web 端**：原生 HTML / CSS / JavaScript，由后端直接托管
- **桌面端**：Java + JavaFX，通过 HTTP 调用后端 API
- 三端共享同一份数据，任意一端记账，其他端刷新即可看到

## 目录结构

```
personal-ledger/
├── backend/         # Python FastAPI 后端 + SQLite
│   ├── app/
│   │   ├── main.py              # 应用入口
│   │   ├── database.py          # 数据库连接
│   │   ├── models.py            # ORM 模型
│   │   ├── schemas.py           # Pydantic 模型
│   │   ├── crud.py              # 数据访问层
│   │   └── routers/             # 路由：分类 / 账单 / 统计
│   ├── data/                    # SQLite 数据文件（自动生成）
│   └── requirements.txt
├── web/             # Web 前端（由后端 /static 托管）
│   ├── index.html
│   ├── style.css
│   └── app.js
├── desktop/         # Java + JavaFX 桌面客户端
│   ├── pom.xml
│   └── src/main/java/com/ledger/
│       ├── App.java
│       ├── api/ApiClient.java   # 调用后端 REST API
│       ├── model/               # POJO（Jackson 反序列化）
│       └── ui/MainController.java
└── README.md
```

## 功能

- 记一笔（支出 / 收入、金额、分类、时间、备注）
- 账单流水（按类型 / 分类筛选、删除）
- 统计概览（总收入、总支出、结余、笔数、按分类汇总）
- 默认分类：餐饮 / 交通 / 购物 / 居住 / 娱乐 / 医疗 / 工资 / 奖金 …

## 快速开始

### 1) 启动后端（Python）

```bash
cd backend
python -m venv .venv
source .venv/bin/activate           # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

启动后：

- Web UI：<http://localhost:8000/>
- Swagger API 文档：<http://localhost:8000/docs>

### 2) 使用 Web 端

打开 <http://localhost:8000/> 即可使用。

### 3) 启动 Java 桌面端

```bash
cd desktop
mvn javafx:run
```

桌面端默认连接 `http://127.0.0.1:8000`，可在窗口顶部修改 API 地址后点「连接」切换。

> 需要 JDK 17+ 和 Maven 3.8+。

## 多端协同示例

1. 启动后端 → Web 端添加几笔账单
2. 启动桌面端，看到与 Web 端同步的统计与流水
3. 在桌面端删除一条账单 → Web 端刷新页面或点击「刷新」即可看到变更

## API 概览

| 方法   | 路径                       | 说明                          |
| ------ | -------------------------- | ----------------------------- |
| GET    | `/api/health`              | 健康检查                      |
| GET    | `/api/categories`          | 分类列表                      |
| POST   | `/api/categories`          | 新建分类                      |
| DELETE | `/api/categories/{id}`     | 删除分类                      |
| GET    | `/api/transactions`        | 账单列表（支持筛选）           |
| POST   | `/api/transactions`        | 新增账单                      |
| GET    | `/api/transactions/{id}`   | 账单详情                      |
| PUT    | `/api/transactions/{id}`   | 修改账单                      |
| DELETE | `/api/transactions/{id}`   | 删除账单                      |
| GET    | `/api/stats/summary`       | 统计概览（含按分类汇总）       |

详见 <http://localhost:8000/docs>。

## 技术栈

| 模块  | 技术 |
| ----- | ---- |
| 后端  | Python 3.10+, FastAPI, SQLAlchemy, Pydantic, SQLite |
| Web   | 原生 HTML / CSS / JavaScript（fetch API） |
| 桌面  | JDK 17+, JavaFX 21, Jackson, Java HttpClient |
| 通信  | HTTP / JSON |

## 后续可扩展

- 用户体系 + JWT 鉴权 → 多用户共享同一后端
- 月度 / 年度图表（Web 引入 Chart.js，桌面端用 JavaFX Charts）
- 导入 / 导出 CSV
- 移动端：再加一个 Android（Kotlin/Java）或 Flutter 客户端复用同一份 API
