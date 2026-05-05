package io.unconquerable.intercept.functional;

import java.util.function.Function;

public sealed interface Either<L, R> permits Either.Left, Either.Right {

    record Left<L, R>(L value) implements Either<L, R> {}

    record Right<L, R>(R value) implements Either<L, R> {}

    static <L, R> Either<L, R> left(L value) {
        return new Left<>(value);
    }

    static <L, R> Either<L, R> right(R value) {
        return new Right<>(value);
    }

    default boolean isLeft() {
        return this instanceof Left<L, R>;
    }

    default boolean isRight() {
        return this instanceof Right<L, R>;
    }

    default <T> T fold(Function<? super L, ? extends T> onLeft, Function<? super R, ? extends T> onRight) {
        return switch (this) {
            case Left<L, R> l -> onLeft.apply(l.value());
            case Right<L, R> r -> onRight.apply(r.value());
        };
    }

    default <T> Either<T, R> mapLeft(Function<? super L, ? extends T> mapper) {
        return switch (this) {
            case Left<L, R> l -> Either.left(mapper.apply(l.value()));
            case Right<L, R> r -> Either.right(r.value());
        };
    }

    default <T> Either<L, T> mapRight(Function<? super R, ? extends T> mapper) {
        return switch (this) {
            case Left<L, R> l -> Either.left(l.value());
            case Right<L, R> r -> Either.right(mapper.apply(r.value()));
        };
    }

    default <T> Either<T, R> flatMapLeft(Function<? super L, ? extends Either<T, R>> mapper) {
        return switch (this) {
            case Left<L, R> l -> mapper.apply(l.value());
            case Right<L, R> r -> Either.right(r.value());
        };
    }

    default <T> Either<L, T> flatMapRight(Function<? super R, ? extends Either<L, T>> mapper) {
        return switch (this) {
            case Left<L, R> l -> Either.left(l.value());
            case Right<L, R> r -> mapper.apply(r.value());
        };
    }
}