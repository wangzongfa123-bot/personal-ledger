import logging
from pathlib import Path

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse
from fastapi.staticfiles import StaticFiles

from . import crud, models
from .database import Base, SessionLocal, engine
from .routers import categories, stats, transactions

logger = logging.getLogger("ledger")


def create_app() -> FastAPI:
    Base.metadata.create_all(bind=engine)
    with SessionLocal() as db:
        crud.seed_default_categories(db)

    app = FastAPI(title="Personal Ledger API", version="1.0.0")

    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    app.include_router(categories.router)
    app.include_router(transactions.router)
    app.include_router(stats.router)

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError):
        try:
            body = (await request.body()).decode("utf-8", errors="replace")
        except Exception:
            body = "<unreadable>"
        logger.warning(
            "422 %s %s | errors=%s | body=%s",
            request.method, request.url.path, exc.errors(), body,
        )
        return JSONResponse(
            status_code=422,
            content={"detail": exc.errors(), "body_received": body},
        )

    @app.get("/api/health", tags=["meta"])
    def health():
        return {"status": "ok"}

    web_dir = Path(__file__).resolve().parent.parent.parent / "web"
    if web_dir.exists():
        app.mount("/static", StaticFiles(directory=web_dir), name="static")

        @app.get("/", include_in_schema=False)
        def index():
            return FileResponse(web_dir / "index.html")

    return app


app = create_app()


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
