package kr.kro.airbob.common.lock.exception;

public class LockAcquisitionFailedException extends RuntimeException {
    public LockAcquisitionFailedException(String lockName) {
        super("락 획득 실패: " + lockName);
    }
}
