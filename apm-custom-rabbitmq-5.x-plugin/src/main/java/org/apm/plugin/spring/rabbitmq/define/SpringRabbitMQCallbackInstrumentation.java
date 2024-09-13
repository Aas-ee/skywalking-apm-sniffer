package org.apm.plugin.spring.rabbitmq.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.NameMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * SpringRabbitMQCallbackInstrumentation
 *
 * @author Aasee
 * @date 2024-09-13
 */
public class SpringRabbitMQCallbackInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {
    // 替换成你需要修改的类路径
    private static final String ENHANCE_CLASS = "com.amcmj.rabbitmq.configure.Callback";
    // 拦截器的类名
    private static final String INTERCEPT_CLASS = "org.apm.plugin.spring.rabbitmq.SpringRabbitMQCallbackInterceptor";
    /**
     * 拦截构造方法
     */
    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    /**
     * 定义要拦截类的方法，以及对应的拦截器
     */
    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[] {
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    // 这里是要拦截的方法
                    return named("confirm").or(named("returnedMessage"));
                }

                @Override
                public String getMethodsInterceptor() {
                    // 定义拦截器的类名
                    return INTERCEPT_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    // 如果有要改方法参数的需求，这里可以设置成true
                    return false;
                }
            }
        };
    }

    /**
     * 定义要拦截的类名
     */
    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName(ENHANCE_CLASS);
    }
}