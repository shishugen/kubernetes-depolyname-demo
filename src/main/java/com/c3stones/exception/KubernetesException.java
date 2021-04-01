package com.c3stones.exception;

/**
 * @ClassName: KubernetesException
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/4/1 9:23
 */
public class KubernetesException extends RuntimeException {

    public KubernetesException() {
    }

    public KubernetesException(String message) {
        super(message);
    }

    public KubernetesException(String message, Throwable cause) {
        super(message, cause);
    }

    public KubernetesException(Throwable cause) {
        super(cause);
    }

    public KubernetesException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
