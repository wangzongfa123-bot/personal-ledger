from datetime import datetime
from typing import Optional

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from .. import crud, schemas
from ..database import get_db

router = APIRouter(prefix="/api/stats", tags=["stats"])


@router.get("/summary", response_model=schemas.StatsSummary)
def stats_summary(
    start: Optional[datetime] = Query(None),
    end: Optional[datetime] = Query(None),
    db: Session = Depends(get_db),
):
    return crud.stats_summary(db, start=start, end=end)
