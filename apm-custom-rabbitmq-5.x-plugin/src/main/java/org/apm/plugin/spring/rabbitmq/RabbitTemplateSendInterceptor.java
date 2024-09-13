package org.apm.plugin.spring.rabbitmq;

import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;

import java.lang.reflect.Method;

public class RabbitTemplateSendInterceptor implements InstanceMethodsAroundInterceptor {
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        AbstractSpan span = ContextManager.createExitSpan("RabbitMQ/Producer/Send", "RabbitMQ");
        span.setComponent(ComponentsDefine.RABBITMQ_PRODUCER);

        if (allArguments.length > 2 && allArguments[2] instanceof Message) {
            Message message = (Message) allArguments[2];
            MessageProperties properties = message.getMessageProperties();
            if (properties != null) {
                ContextCarrier contextCarrier = new ContextCarrier();
                ContextManager.inject(contextCarrier);
                for (CarrierItem it = contextCarrier.items(); it.hasNext(); ) {
                    CarrierItem item = it.next();
                    properties.setHeader(item.getHeadKey(), item.getHeadValue());
                }
            }
        }

        if (allArguments.length > 3 && allArguments[3] instanceof CorrelationData) {
            CorrelationData correlationData = (CorrelationData) allArguments[3];
            span.tag("mq.correlation_id", correlationData.getId());
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}