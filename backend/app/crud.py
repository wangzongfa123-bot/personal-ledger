from datetime import datetime
from typing import Optional

from sqlalchemy import func
from sqlalchemy.orm import Session, joinedload

from . import models, schemas


# ---------- Category ----------

def list_categories(db: Session) -> list[models.Category]:
    return db.query(models.Category).order_by(models.Category.id).all()


def get_category(db: Session, category_id: int) -> Optional[models.Category]:
    return db.query(models.Category).filter(models.Category.id == category_id).first()


def get_category_by_name(db: Session, name: str) -> Optional[models.Category]:
    return db.query(models.Category).filter(models.Category.name == name).first()


def create_category(db: Session, payload: schemas.CategoryCreate) -> models.Category:
    obj = models.Category(name=payload.name, kind=payload.kind, icon=payload.icon)
    db.add(obj)
    db.commit()
    db.refresh(obj)
    return obj


def delete_category(db: Session, category_id: int) -> bool:
    obj = get_category(db, category_id)
    if not obj:
        return False
    db.delete(obj)
    db.commit()
    return True


# ---------- Transaction ----------

def _tx_query(db: Session):
    return db.query(models.Transaction).options(joinedload(models.Transaction.category))


def list_transactions(
    db: Session,
    start: Optional[datetime] = None,
    end: Optional[datetime] = None,
    kind: Optional[str] = None,
    category_id: Optional[int] = None,
    limit: int = 200,
    offset: int = 0,
) -> list[models.Transaction]:
    q = _tx_query(db)
    if start:
        q = q.filter(models.Transaction.occurred_at >= start)
    if end:
        q = q.filter(models.Transaction.occurred_at <= end)
    if kind:
        q = q.filter(models.Transaction.kind == kind)
    if category_id:
        q = q.filter(models.Transaction.category_id == category_id)
    return (
        q.order_by(models.Transaction.occurred_at.desc(), models.Transaction.id.desc())
        .offset(offset)
        .limit(limit)
        .all()
    )


def get_transaction(db: Session, tx_id: int) -> Optional[models.Transaction]:
    return _tx_query(db).filter(models.Transaction.id == tx_id).first()


def create_transaction(db: Session, payload: schemas.TransactionCreate) -> models.Transaction:
    obj = models.Transaction(
        amount=payload.amount,
        kind=payload.kind,
        note=payload.note,
        occurred_at=payload.occurred_at or datetime.utcnow(),
        category_id=payload.category_id,
    )
    db.add(obj)
    db.commit()
    db.refresh(obj)
    return get_transaction(db, obj.id)


def update_transaction(
    db: Session, tx_id: int, payload: schemas.TransactionUpdate
) -> Optional[models.Transaction]:
    obj = get_transaction(db, tx_id)
    if not obj:
        return None
    data = payload.model_dump(exclude_none=True)
    for key, value in data.items():
        setattr(obj, key, value)
    db.commit()
    db.refresh(obj)
    return get_transaction(db, tx_id)


def delete_transaction(db: Session, tx_id: int) -> bool:
    obj = db.query(models.Transaction).filter(models.Transaction.id == tx_id).first()
    if not obj:
        return False
    db.delete(obj)
    db.commit()
    return True


# ---------- Stats ----------

def stats_summary(
    db: Session,
    start: Optional[datetime] = None,
    end: Optional[datetime] = None,
) -> schemas.StatsSummary:
    q = db.query(models.Transaction)
    if start:
        q = q.filter(models.Transaction.occurred_at >= start)
    if end:
        q = q.filter(models.Transaction.occurred_at <= end)

    income = (
        q.with_entities(func.coalesce(func.sum(models.Transaction.amount), 0.0))
        .filter(models.Transaction.kind == "income")
        .scalar()
        or 0.0
    )
    expense = (
        q.with_entities(func.coalesce(func.sum(models.Transaction.amount), 0.0))
        .filter(models.Transaction.kind == "expense")
        .scalar()
        or 0.0
    )
    count = q.count()

    cat_q = (
        db.query(
            models.Category.id,
            models.Category.name,
            models.Transaction.kind,
            func.sum(models.Transaction.amount),
            func.count(models.Transaction.id),
        )
        .join(models.Transaction, models.Transaction.category_id == models.Category.id)
    )
    if start:
        cat_q = cat_q.filter(models.Transaction.occurred_at >= start)
    if end:
        cat_q = cat_q.filter(models.Transaction.occurred_at <= end)
    cat_q = cat_q.group_by(
        models.Category.id, models.Category.name, models.Transaction.kind
    ).order_by(func.sum(models.Transaction.amount).desc())

    by_category = [
        schemas.StatsCategoryItem(
            category_id=row[0],
            category_name=row[1],
            kind=row[2],
            total=float(row[3] or 0.0),
            count=int(row[4] or 0),
        )
        for row in cat_q.all()
    ]

    return schemas.StatsSummary(
        income=float(income),
        expense=float(expense),
        balance=float(income) - float(expense),
        count=int(count),
        by_category=by_category,
    )


def seed_default_categories(db: Session) -> None:
    if db.query(models.Category).count() > 0:
        return
    defaults = [
        ("餐饮", "expense", "🍚"),
        ("交通", "expense", "🚌"),
        ("购物", "expense", "🛍"),
        ("居住", "expense", "🏠"),
        ("娱乐", "expense", "🎮"),
        ("医疗", "expense", "💊"),
        ("其他支出", "expense", "📦"),
        ("工资", "income", "💼"),
        ("奖金", "income", "🎁"),
        ("其他收入", "income", "💰"),
    ]
    for name, kind, icon in defaults:
        db.add(models.Category(name=name, kind=kind, icon=icon))
    db.commit()
