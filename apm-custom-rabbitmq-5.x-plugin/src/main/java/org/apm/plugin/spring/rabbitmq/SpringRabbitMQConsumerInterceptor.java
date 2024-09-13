package org.apm.plugin.spring.rabbitmq;

import com.rabbitmq.client.Channel;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.lang.reflect.Method;

/**
 * SpringRabbitMQConsumerInterceptor
 *
 * @author Aasee
 * @date 2024-09-13
 */
public class SpringRabbitMQConsumerInterceptor implements InstanceMethodsAroundInterceptor {
    public static final String OPERATE_NAME_PREFIX = "SpringRabbitMQ/";
    public static final String CONSUMER_OPERATE_NAME_SUFFIX = "/Consumer";

    /**
     * 在拦截方法前执行
     */
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {

        // 解析参数
        Message message = (Message) allArguments[1];
        MessageProperties properties = message.getMessageProperties();
        Channel channel = (Channel) allArguments[0];
        String url = channel.getConnection().getAddress().toString().replace("/", "") + ":" + channel.getConnection().getPort();
        // 初始化span，这里创建了一个EntrySpan
        ContextCarrier contextCarrier = new ContextCarrier();
        AbstractSpan activeSpan = ContextManager.createEntrySpan(OPERATE_NAME_PREFIX + "Topic/" +
                properties.getReceivedExchange() + "Queue/" + properties.getReceivedRoutingKey() +
                CONSUMER_OPERATE_NAME_SUFFIX, null).start(System.currentTimeMillis());
        // 自定义标签，记录方法入参
        Tags.MQ_BROKER.set(activeSpan, url);
        Tags.MQ_TOPIC.set(activeSpan, properties.getReceivedExchange());
        Tags.MQ_QUEUE.set(activeSpan, properties.getReceivedRoutingKey());
        activeSpan.setComponent(ComponentsDefine.RABBITMQ_CONSUMER);
        SpanLayer.asMQ(activeSpan);

        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            if (properties.getHeaders() != null && properties.getHeaders().get(next.getHeadKey()) != null) {
                next.setHeadValue(properties.getHeaders().get(next.getHeadKey()).toString());
            }
        }
        // 提取上下文到当前线程
        ContextManager.extract(contextCarrier);

    }

    /**
     * 在拦截方法后执行
     */
    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        // 停止日志记录，移除上下文
        ContextManager.stopSpan();
        // 返回方法出参
        return ret;

    }

    /**
     * 记录方法异常
     */
    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        AbstractSpan span = ContextManager.activeSpan();
        span.errorOccurred();
        span.log(t);
        Tags.MQ_STATUS.set(span, "consumer_error");
    }
}
