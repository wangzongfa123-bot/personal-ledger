# 后端服务（Python + FastAPI）

提供 RESTful API 与 SQLite 数据库，供 Web 前端、Java 桌面客户端等多端共享。

## 启动

```bash
cd backend
python -m venv .venv
source .venv/bin/activate          # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

启动后：

- Web UI：http://localhost:8000/
- API 文档（Swagger）：http://localhost:8000/docs
- API 文档（ReDoc）：http://localhost:8000/redoc

数据库文件位于 `backend/data/ledger.db`，首次启动会自动创建并填充默认分类。

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET    | `/api/categories`            | 分类列表 |
| POST   | `/api/categories`            | 新增分类 |
| DELETE | `/api/categories/{id}`       | 删除分类 |
| GET    | `/api/transactions`          | 账单列表（支持时间/类型/分类筛选） |
| POST   | `/api/transactions`          | 新增账单 |
| GET    | `/api/transactions/{id}`     | 账单详情 |
| PUT    | `/api/transactions/{id}`     | 修改账单 |
| DELETE | `/api/transactions/{id}`     | 删除账单 |
| GET    | `/api/stats/summary`         | 统计：收入、支出、结余、按分类汇总 |
| GET    | `/api/health`                | 健康检查 |
