package cn.kunter.mybatis.pure.exception;

/**
 * MyBatis Pure 框架的核心异常类。 用于封装和抛出在框架执行过程中发生的各种异常。
 */
public class MyBatisPureException extends RuntimeException {

    /**
     * 构造一个新的 MyBatis Pure 异常，指定详细信息。
     * @param message 异常详细信息
     */
    public MyBatisPureException(String message) {
        super(message);
    }

    /**
     * 构造一个新的 MyBatis Pure 异常，指定详细信息和原因。
     * @param message 异常详细信息
     * @param cause 异常发生的原因
     */
    public MyBatisPureException(String message, Throwable cause) {
        super(message, cause);
    }

}
