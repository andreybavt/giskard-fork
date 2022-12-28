import mlflow

from giskard.core.core import SupportedModelTypes
from giskard.core.model import Model


class SKLearnModel(Model):
    def __init__(self, clf, model_type: SupportedModelTypes, name: str = None, data_preparation_function=None,
                 feature_names=None, classification_threshold=0.5, classification_labels=None) -> None:

        if classification_labels is None and hasattr(clf, 'classes_'):
            classification_labels = list(getattr(clf, 'classes_'))
        if feature_names is None and hasattr(clf, 'feature_names_in_'):
            feature_names = list(getattr(clf, 'feature_names_in_'))

        super().__init__(clf, model_type, name, data_preparation_function, feature_names,
                         classification_threshold, classification_labels)

    @classmethod
    def read_model_from_local_dir(cls, local_path):
        return mlflow.sklearn.load_model(local_path)

    def _raw_predict(self, data):
        if self.is_regression:
            return self.clf.predict(data)
        else:
            return self.clf.predict_proba(data)