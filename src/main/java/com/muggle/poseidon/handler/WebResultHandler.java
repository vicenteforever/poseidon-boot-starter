package com.muggle.poseidon.handler;

import com.muggle.poseidon.base.ResultBean;
import com.muggle.poseidon.base.exception.BasePoseidonCheckException;
import com.muggle.poseidon.base.exception.BasePoseidonException;
import com.muggle.poseidon.listener.ExceptionEvent;
import com.muggle.poseidon.util.UserInfoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
@ConditionalOnProperty(prefix = "poseidon", name = "auto", havingValue = "true", matchIfMissing = false)
public class WebResultHandler {

    public WebResultHandler() {
        log.info(">>>>>>>>>>>>>>>>>>>>> WebResultHandler init <<<<<<<<<<<<<<<<<<<<");
    }

    private static final Logger log = LoggerFactory.getLogger(WebResultHandler.class);

    @Autowired
    ApplicationContext applicationContext;


    /**
     * 自定义异常
     *
     * @param e
     * @param req
     * @return
     */
    @ExceptionHandler(value = {BasePoseidonException.class})
    public ResultBean poseidonExceptionHandler(BasePoseidonException e, HttpServletRequest req) {
        log.error("业务异常",e);
        ResultBean error = ResultBean.error(e.getMessage());
        return error;
    }

    /**
     * 参数未通过校验
     *
     * @param e
     * @param req
     * @return
     */
    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public ResultBean MethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest req) {
        log.error("参数未通过校验",e);
        ResultBean error = ResultBean.error(e.getMessage());
        return error;
    }

    /**
     * 错误的请求方式
     *
     * @param e
     * @param req
     * @return
     */
    @ExceptionHandler(value = {HttpRequestMethodNotSupportedException.class})
    public ResultBean notsupported(Exception e, HttpServletRequest req) {
        log.error("错误的请求方式",e);
        ResultBean error = ResultBean.error("错误的请求方式");
        return error;
    }



    /**
     * 自定义异常
     *
     * @param e
     * @param req
     * @return
     */
    @ExceptionHandler(value = {BasePoseidonCheckException.class})
    public ResultBean checked(Exception e, HttpServletRequest req) {
        log.error("自定义异常",e);
        ResultBean error = ResultBean.error(e.getMessage());
        return error;
    }

    /**
     * 未知异常，需要通知到管理员,对于线上未知的异常，我们应该严肃处理：先将消息传给MQ中心(该平台未实现) 然后日志写库
     * 这里的处理方式是抛出事件
     * @param e
     * @param req
     * @return
     */
    @ExceptionHandler(value = {Exception.class})
    public ResultBean exceptionHandler(Exception e, HttpServletRequest req) {
        try {
            UserDetails userInfo = UserInfoUtils.getUserInfo();
            ExceptionEvent exceptionEvent = new ExceptionEvent(String.format("系统异常: [ %s ] 时间戳： [%d]  ", e.getMessage(),System.currentTimeMillis()), this);
            applicationContext.publishEvent(exceptionEvent);
            log.error("系统异常：" + req.getMethod() + req.getRequestURI()+" user: "+userInfo.toString() , e);
            return ResultBean.error("系统异常");
        }catch (Exception err){
            log.error("紧急！！！ 严重的异常",err);
            return ResultBean.error("系统发生严重的错误");
        }
    }
}