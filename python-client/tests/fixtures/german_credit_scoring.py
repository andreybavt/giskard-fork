import logging

import pandas as pd
import pytest
from sklearn import model_selection
from sklearn.compose import ColumnTransformer
from sklearn.dummy import DummyClassifier
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

from giskard.ml_worker.core.giskard_dataset import GiskardDataset
from giskard.ml_worker.core.model import GiskardModel
from giskard.ml_worker.utils.logging import Timer
from tests import path

logger = logging.getLogger(__name__)
input_types = {
    "account_check_status": "category",
    "duration_in_month": "numeric",
    "credit_history": "category",
    "purpose": "category",
    "credit_amount": "numeric",
    "savings": "category",
    "present_emp_since": "category",
    "installment_as_income_perc": "numeric",
    "sex": "category",
    "personal_status": "category",
    "other_debtors": "category",
    "present_res_since": "numeric",
    "property": "category",
    "age": "numeric",
    "other_installment_plans": "category",
    "housing": "category",
    "credits_this_bank": "numeric",
    "job": "category",
    "people_under_maintenance": "numeric",
    "telephone": "category",
    "foreign_worker": "category",
}


@pytest.fixture()
def german_credit_data() -> GiskardDataset:
    logger.info("Reading german_credit_prepared.csv")
    df = pd.read_csv(
        path("test_data/german_credit_prepared.csv"),
        keep_default_na=False,
        na_values=["_GSK_NA_"],
    )
    return GiskardDataset(
        df=df,
        column_types=df.dtypes.apply(lambda x: x.name).to_dict(),
        target="default",
        feature_types=input_types,
    )


@pytest.fixture()
def german_credit_catboost(german_credit_data) -> GiskardModel:
    from catboost import CatBoostClassifier
    from sklearn import model_selection

    timer = Timer()
    feature_types = {i: input_types[i] for i in input_types if i != "default"}

    columns_to_encode = [key for key in feature_types.keys() if feature_types[key] == "category"]

    credit = german_credit_data.df

    Y = credit["default"]
    X = credit.drop(columns="default")
    X_train, X_test, Y_train, Y_test = model_selection.train_test_split(
        X, Y, test_size=0.20, random_state=30, stratify=Y
    )
    cb = CatBoostClassifier(iterations=2, learning_rate=1, depth=2)
    cb.fit(X_train, Y_train, columns_to_encode)

    model_score = cb.score(X_test, Y_test)
    timer.stop(f"Trained model with score: {model_score}")

    return GiskardModel(
        prediction_function=cb.predict_proba,
        model_type="classification",
        feature_names=list(input_types),
        classification_threshold=0.5,
        classification_labels=cb.classes_,
    )


@pytest.fixture()
def german_credit_test_data(german_credit_data):
    df = pd.DataFrame(german_credit_data.df).drop(columns=["default"])
    column_types = german_credit_data.column_types
    return GiskardDataset(
        df=df,
        feature_types=input_types,
        column_types={c: column_types[c] for c in column_types if c != "default"},
        target=None,
    )


@pytest.fixture()
def german_credit_model(german_credit_data) -> GiskardModel:
    timer = Timer()

    columns_to_scale = [key for key in input_types.keys() if input_types[key] == "numeric"]

    numeric_transformer = Pipeline([("imputer", SimpleImputer(strategy="median")), ("scaler", StandardScaler())])

    columns_to_encode = [key for key in input_types.keys() if input_types[key] == "category"]

    categorical_transformer = Pipeline(
        [
            ("imputer", SimpleImputer(strategy="constant", fill_value="missing")),
            ("onehot", OneHotEncoder(handle_unknown="ignore", sparse=False)),
        ]
    )

    preprocessor = ColumnTransformer(
        transformers=[
            ("num", numeric_transformer, columns_to_scale),
            ("cat", categorical_transformer, columns_to_encode),
        ]
    )
    clf = Pipeline(steps=[("preprocessor", preprocessor), ("classifier", LogisticRegression(max_iter=100))])

    Y = german_credit_data.df["default"]
    X = german_credit_data.df.drop(columns="default")
    X_train, X_test, Y_train, Y_test = model_selection.train_test_split(
        X, Y, test_size=0.20, random_state=30, stratify=Y  # NOSONAR
    )
    clf.fit(X_train, Y_train)

    model_score = clf.score(X_test, Y_test)
    timer.stop(f"Trained model with score: {model_score}")

    return GiskardModel(
        prediction_function=clf.predict_proba,
        model_type="classification",
        feature_names=list(input_types),
        classification_threshold=0.5,
        classification_labels=clf.classes_,
    )


@pytest.fixture()
def german_credit_always_default_model(german_credit_data) -> GiskardModel:
    X = german_credit_data.df.drop(columns="default")
    y = german_credit_data.df["default"]

    dummy = DummyClassifier(strategy="constant", constant="Default")
    dummy.fit(X, y)

    return GiskardModel(
        prediction_function=dummy.predict_proba,
        model_type="classification",
        feature_names=list(input_types),
        classification_threshold=0.5,
        classification_labels=dummy.classes_,
    )
