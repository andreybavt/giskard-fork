import warnings
from typing import Optional, Sequence

from .logger import logger
from .registry import DetectorRegistry
from .result import ScanResult
from ..core.model_validation import validate_model
from ..datasets.base import Dataset
from ..models.base import BaseModel

MAX_ISSUES_PER_DETECTOR = 15


class Scanner:
    def __init__(self, params: Optional[dict] = None, only=None):
        if isinstance(only, str):
            only = [only]

        self.params = params or dict()
        self.only = only

    def analyze(self, model: BaseModel, dataset: Dataset, verbose=True) -> ScanResult:
        """Runs the analysis of a model and dataset, detecting issues."""
        validate_model(model=model, validate_ds=dataset)

        maybe_print("Running scan…", verbose=verbose)

        # Collect the detectors
        detectors = self.get_detectors(tags=[model.meta.model_type.value])

        if not detectors:
            raise RuntimeError("No issue detectors available. Scan will not be performed.")

        logger.debug(f"Running detectors: {[d.__class__.__name__ for d in detectors]}")

        # @TODO: this should be selective to specific warnings
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")

            issues = []
            for detector in detectors:
                maybe_print(f"Running detector {detector.__class__.__name__}…", end="", verbose=verbose)
                detected_issues = detector.run(model, dataset)
                detected_issues = sorted(detected_issues, key=lambda i: -i.importance)[:MAX_ISSUES_PER_DETECTOR]
                maybe_print(f" {len(detected_issues)} issues detected.", verbose=verbose)
                issues.extend(detected_issues)

        maybe_print(
            f"Scan completed: {len(issues) or 'no'} issue{'s' if len(issues) != 1 else ''} found.", verbose=verbose
        )

        return ScanResult(issues)

    def get_detectors(self, tags: Optional[Sequence[str]] = None) -> Sequence:
        """Returns the detector instances."""
        detectors = []
        classes = DetectorRegistry.get_detector_classes(tags=tags)

        # Filter detector classes
        if self.only:
            only_classes = DetectorRegistry.get_detector_classes(tags=self.only)
            keys_to_keep = set(only_classes.keys()).intersection(classes.keys())
            classes = {k: classes[k] for k in keys_to_keep}

        # Configure instances
        for name, detector_cls in classes.items():
            kwargs = self.params.get(name) or dict()
            detectors.append(detector_cls(**kwargs))

        return detectors


def maybe_print(*args, **kwargs):
    if kwargs.pop("verbose", True):
        print(*args, **kwargs)
