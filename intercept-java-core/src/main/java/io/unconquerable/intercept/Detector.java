package io.unconquerable.intercept;

/**
 * Detector
 *
 * @param <T>
 * @author Rizwan Idrees
 */
@FunctionalInterface
public interface Detector<T> {


    DetectionResult detect(T target);


    class DetectionResult {

        public enum Status {DETECTED, NOT_DETECTED, SKIPPED}

        private final Status status;
        private final String reason;

        private DetectionResult(Status status, String reason) {
            this.status = status;
            this.reason = reason;
        }

        public static DetectionResult detected() {
            return new DetectionResult(Status.DETECTED, null);
        }

        public static DetectionResult notDetected() {
            return new DetectionResult(Status.NOT_DETECTED, null);
        }

        public static DetectionResult skipped(String reason) {
            return new DetectionResult(Status.SKIPPED, reason);
        }

        public boolean isDetected() {
            return status == Status.DETECTED;
        }

        public boolean wasSkipped() {
            return status == Status.SKIPPED;
        }

        public Status status() {
            return status;
        }

        public String reason() {
            return reason;
        }

        @Override
        public String toString() {
            return reason != null ? status + "(" + reason + ")" : status.toString();
        }
    }

}
