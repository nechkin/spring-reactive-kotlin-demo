package com.example.server

import io.netty.channel.ChannelOption
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class NettyServerConfiguration {

    /**
     * An example how to customize netty with Spring Boot
     */
    @Bean
    fun nettyServerCustomizer() = NettyServerCustomizer { httpServer ->
        /*
         * cannot customize eventLoopGroup like this, since it will be already setup for autoconfigured
         *
        nettyReactiveWebServerFactory
        options.tcpConfiguration { tcpMapper ->
            tcpMapper.bootstrap { bootstrapMapper ->
                bootstrapMapper.group(DefaultEventLoopGroup())
            }
        }
        * see commented nettyReactiveWebServerFactory below
        */

        /**
         * Worker threads for the Event Loop are set int reactor-netty LoopResources#DEFAULT_IO_WORKER_COUNT
         * (can be set with reactor.netty.ioWorkerCount system property)
         * See DefaultLoopResources constructor, how workerCount is set
         * This worketCount is used to init e.g. a EpollEventLoopGroup
         */

        // can use Spring Boot server.connection-timeout property instead
        httpServer.tcpConfiguration { tcpServer ->
            tcpServer.selectorOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
        }
    }

    /**
     * Apply customizer to the Netty Web Server Factory configured by Spring Boot
     */
    @Bean
    fun nettyReactiveWebServerFactoryBeanPostProcessor() = object : BeanPostProcessor {
        override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any? {
            if (bean is NettyReactiveWebServerFactory) {
                bean.addServerCustomizers(nettyServerCustomizer())
            }
            return bean
        }
    }

    // To replace NettyReactiveWebServerFactory (e.g. to use custom event loop group)
//    @Bean
//    open fun eventLoopNettyCustomizer() = NettyServerCustomizer { httpServer ->
//        val parentGroup = NioEventLoopGroup(8)
//        val childGroup = NioEventLoopGroup(8)
//        httpServer.tcpConfiguration { tcpServer ->
//            tcpServer
//                .bootstrap { serverBootstrap ->
//                    serverBootstrap
//                        .group(parentGroup, childGroup)
//                        .channel(NioServerSocketChannel::class.java)
//                }
//        }
//    }
//
//    @Bean
//    open fun nettyReactiveWebServerFactory(): NettyReactiveWebServerFactory {
//        val webServerFactory = NettyReactiveWebServerFactory()
//        webServerFactory.addServerCustomizers(eventLoopNettyCustomizer())
//        return webServerFactory
//    }
}