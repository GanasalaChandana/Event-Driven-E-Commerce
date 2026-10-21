package com.shopflow.inventory.exception;

public class InvalidStockAdjustmentException extends RuntimeException {
    public InvalidStockAdjustmentException(String message) {
        super(message);
    }
}
