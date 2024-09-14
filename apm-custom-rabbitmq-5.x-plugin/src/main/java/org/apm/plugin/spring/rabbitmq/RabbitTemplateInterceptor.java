package org.apm.plugin.spring.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;

import java.lang.reflect.Method;
import java.util.Arrays;

// 1. 定义一个拦截器来捕获RabbitTemplate的send方法(convertAndSend的底层方法)
@Slf4j
public class RabbitTemplateInterceptor implements InstanceMethodsAroundInterceptor {

    private static final String SW_CONTEXT_KEY = "SW_CONTEXT";
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        log.info("传递方法：" + method.getName() + ", 传递参数：" + Arrays.toString(allArguments));
        if (method.getName().equals("send") && allArguments.length >= 2 && allArguments[3] instanceof CorrelationData) {
            CorrelationData correlationData = (CorrelationData) allArguments[3];
            String id = correlationData.getId();
            log.info("传递参数 id: " + id);
            Message message = (Message) allArguments[2];
            log.info("传递参数 message: " + message);
            MessageProperties properties = message.getMessageProperties();

            log.info("传递参数 properties前: " + properties);
            // todo 因为callback中只接收correlationData，需要往此处加入当前快照或其他可识别的头部信息
            log.info("传递参数 correlationData: " + correlationData);

            ContextCarrier contextCarrier = new ContextCarrier();
            ContextManager.inject(contextCarrier);

            StringBuilder contextData = new StringBuilder();
            CarrierItem next = contextCarrier.items();
            while (next.hasNext()) {
                next = next.next();
                contextData.append(next.getHeadKey()).append("=").append(next.getHeadValue()).append(";");
            }
            // properties
            properties.setHeader(SW_CONTEXT_KEY, contextData.toString());
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}