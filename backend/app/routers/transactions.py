from datetime import datetime
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from .. import crud, schemas
from ..database import get_db

router = APIRouter(prefix="/api/transactions", tags=["transactions"])


@router.get("", response_model=list[schemas.TransactionOut])
def list_transactions(
    start: Optional[datetime] = Query(None, description="开始时间 ISO 8601"),
    end: Optional[datetime] = Query(None, description="结束时间 ISO 8601"),
    kind: Optional[str] = Query(None, pattern="^(income|expense)$"),
    category_id: Optional[int] = None,
    limit: int = Query(200, ge=1, le=1000),
    offset: int = Query(0, ge=0),
    db: Session = Depends(get_db),
):
    return crud.list_transactions(
        db,
        start=start,
        end=end,
        kind=kind,
        category_id=category_id,
        limit=limit,
        offset=offset,
    )


@router.post("", response_model=schemas.TransactionOut, status_code=status.HTTP_201_CREATED)
def create_transaction(payload: schemas.TransactionCreate, db: Session = Depends(get_db)):
    if not crud.get_category(db, payload.category_id):
        raise HTTPException(status_code=400, detail="分类不存在")
    return crud.create_transaction(db, payload)


@router.get("/{tx_id}", response_model=schemas.TransactionOut)
def get_transaction(tx_id: int, db: Session = Depends(get_db)):
    obj = crud.get_transaction(db, tx_id)
    if not obj:
        raise HTTPException(status_code=404, detail="账单不存在")
    return obj


@router.put("/{tx_id}", response_model=schemas.TransactionOut)
def update_transaction(
    tx_id: int, payload: schemas.TransactionUpdate, db: Session = Depends(get_db)
):
    if payload.category_id is not None and not crud.get_category(db, payload.category_id):
        raise HTTPException(status_code=400, detail="分类不存在")
    obj = crud.update_transaction(db, tx_id, payload)
    if not obj:
        raise HTTPException(status_code=404, detail="账单不存在")
    return obj


@router.delete("/{tx_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_transaction(tx_id: int, db: Session = Depends(get_db)):
    if not crud.delete_transaction(db, tx_id):
        raise HTTPException(status_code=404, detail="账单不存在")
    return None
