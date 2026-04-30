from datetime import datetime
from typing import Literal, Optional

from pydantic import BaseModel, Field, ConfigDict


Kind = Literal["income", "expense"]


class CategoryBase(BaseModel):
    name: str = Field(..., min_length=1, max_length=50)
    kind: Kind = "expense"
    icon: Optional[str] = None


class CategoryCreate(CategoryBase):
    pass


class CategoryOut(CategoryBase):
    id: int
    model_config = ConfigDict(from_attributes=True)


class TransactionBase(BaseModel):
    amount: float = Field(..., gt=0, description="金额必须大于 0")
    kind: Kind = "expense"
    note: Optional[str] = Field(None, max_length=255)
    occurred_at: Optional[datetime] = None
    category_id: int


class TransactionCreate(TransactionBase):
    pass


class TransactionUpdate(BaseModel):
    amount: Optional[float] = Field(None, gt=0)
    kind: Optional[Kind] = None
    note: Optional[str] = Field(None, max_length=255)
    occurred_at: Optional[datetime] = None
    category_id: Optional[int] = None


class TransactionOut(BaseModel):
    id: int
    amount: float
    kind: Kind
    note: Optional[str]
    occurred_at: datetime
    created_at: datetime
    category: CategoryOut

    model_config = ConfigDict(from_attributes=True)


class StatsCategoryItem(BaseModel):
    category_id: int
    category_name: str
    kind: Kind
    total: float
    count: int


class StatsSummary(BaseModel):
    income: float
    expense: float
    balance: float
    count: int
    by_category: list[StatsCategoryItem]
