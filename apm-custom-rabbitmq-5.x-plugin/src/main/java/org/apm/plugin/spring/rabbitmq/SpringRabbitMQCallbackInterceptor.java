package org.apm.plugin.spring.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.AbstractTag;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * SpringRabbitMQCallbackInterceptor
 *
 * @author Aasee
 * @date 2024-09-13
 */
@Slf4j
public class SpringRabbitMQCallbackInterceptor implements InstanceMethodsAroundInterceptor {
    public static final String OPERATE_NAME_PREFIX = "SpringRabbitMQ/";
    public static final String CALLBACK_OPERATE_NAME_SUFFIX = "Callback/";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        AbstractSpan span = ContextManager.createLocalSpan(OPERATE_NAME_PREFIX+CALLBACK_OPERATE_NAME_SUFFIX + method.getName());
        log.info("插件打印span：" + span);
        span.setComponent(ComponentsDefine.RABBITMQ_CONSUMER);

        log.info("插件打印method：" + method.getName());
        log.info("插件打印参数：" + Arrays.toString(allArguments));
        if (method.getName().equals("confirm") && allArguments.length > 0 && allArguments[0] instanceof CorrelationData) {
            CorrelationData correlationData = (CorrelationData) allArguments[0];
            if (correlationData.getId() != null) {
                span.tag("mq.correlation_id", correlationData.getId());
            }
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        AbstractSpan span = ContextManager.activeSpan();
        span.errorOccurred();
        span.log(t);
    }
}