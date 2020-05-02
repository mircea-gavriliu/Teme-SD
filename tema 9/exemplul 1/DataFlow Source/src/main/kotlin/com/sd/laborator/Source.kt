package org.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.messaging.Source
import org.springframework.context.annotation.Bean
import org.springframework.integration.annotation.InboundChannelAdapter
import org.springframework.integration.annotation.Poller
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import java.io.File
import java.io.InputStream

@EnableBinding(Source::class)
@SpringBootApplication
class SourcePipeMicroservice {
    @Bean
    @InboundChannelAdapter(value = Source.OUTPUT, poller =[Poller(fixedDelay = "10000", maxMessagesPerPoll = "1")])
    fun pipeMessageSource(): () -> Message<String> {
        val pipeStream: InputStream = File("/home/gav/mircea/test.txt").inputStream()
        var command = pipeStream.bufferedReader().use { it.readText() }
        command = command.replace("|","---").replace(" ","");
        return { MessageBuilder.withPayload(command).build()}
    }
}

fun main(args: Array<String>) {
    runApplication<SourcePipeMicroservice>(*args)
}