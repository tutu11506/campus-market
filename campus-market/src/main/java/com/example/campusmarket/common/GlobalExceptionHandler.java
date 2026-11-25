package com.example.campusmarket.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 * 捕获所有 Controller 抛出的异常，统一返回 Result 格式的错误响应，
 * 避免前端收到一堆错误的 HTML 堆栈信息。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 静态资源不存在（如商品图片文件丢失），返回真正的 404，
     * 避免被兜底处理器伪装成 200 的 JSON，便于排查且让浏览器正确触发 img 的 error 事件
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /**
     * 处理所有未捕获的异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.error("服务器繁忙，请稍后再试");
    }

    /**
     * 处理参数校验异常（@Valid 校验失败时抛出）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.warn("参数校验失败：{}", msg);
        return Result.error(msg);
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<Void> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常：", e);
        return Result.error("数据不存在，请刷新后重试");
    }

    /**
     * 处理自定义业务异常（如果以后需要的话）
     * 可以定义一个 BusinessException 类，这里专门处理
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数不合法：{}", e.getMessage());
        return Result.error(e.getMessage());
    }

    // 业务回滚抛出的状态异常（如订单完成时商品更新失败，事务已回滚）
    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalStateException(IllegalStateException e) {
        log.warn("业务状态异常：{}", e.getMessage());
        return Result.error(e.getMessage());
    }

    // 缺少必填请求参数（如 /goods/status 未传 status）
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少必填参数：{}", e.getParameterName());
        return Result.error("缺少必填参数：" + e.getParameterName());
    }

    // 参数类型不匹配（如 /goods/abc）
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不正确：{}", e.getName());
        return Result.error("参数类型不正确：" + e.getName());
    }

    // 请求体 JSON 格式错误
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误：{}", e.getMessage());
        return Result.error("请求体格式错误，请检查 JSON");
    }

    // 上传文件超过大小限制
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("上传文件过大：{}", e.getMessage());
        return Result.error("上传文件过大，请压缩后重试");
    }

    // 数据完整性违规（唯一键冲突、外键约束等）
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("数据完整性违规：{}", e.getMessage());
        return Result.error("数据操作失败，可能存在重复或关联约束");
    }

    // 方法级权限不足
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限不足：{}", e.getMessage());
        return Result.error("没有权限执行此操作");
    }

    // 越权访问（如修改他人商品、查看他人订单）：统一返回 HTTP 403
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Result<Void>> handleForbidden(ForbiddenException e) {
        log.warn("越权访问：{}", e.getMessage());
        Result<Void> body = new Result<>();
        body.setCode(403);
        body.setMsg(e.getMessage());
        body.setData(null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // 参数约束校验失败（@Validated 路径参数等）
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.warn("约束校验失败：{}", msg);
        return Result.error(msg);
    }
}
