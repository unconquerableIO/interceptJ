package io.unconquerable.intercept;

import io.unconquerable.intercept.ConditionalDetector.ConditionalDetectorBuilder;

public final class Detectors {

    private Detectors() {
    }

    public static <T> ConditionalDetectorBuilder<T> detector(Detector<T> detector){
        return  ConditionalDetector.detector(detector);
    }

}
