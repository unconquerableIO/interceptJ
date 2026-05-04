/**
 * XGBoost integration for the interceptJ pipeline.
 *
 * <p>Provides a {@link io.unconquerable.intercept.detect.Detector} implementation backed by
 * an XGBoost model, returning a {@link io.unconquerable.intercept.detect.DetectedScore} whose
 * value is the raw prediction produced by the model. Wire it into any interceptJ pipeline:
 *
 * <pre>{@code
 * Interceptor.interceptor()
 *     .detect(features, xgboostDetector)
 *     .decide(decider)
 *     .onBlock(()   -> Response.status(403).build())
 *     .onProceed(() -> service.handle(request))
 *     .result();
 * }</pre>
 *
 * @author Rizwan Idrees
 */
package io.unconquerable.intercept.xgboost;