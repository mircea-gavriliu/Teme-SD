package org.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.messaging.Processor
import org.springframework.integration.annotation.Transformer
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.DateFormat
import java.text.SimpleDateFormat
@EnableBinding(Processor::class)
@SpringBootApplication
class ProcessPipeMicroservice
{
    @Transformer
        (inputChannel = Processor.INPUT, outputChannel =Processor.OUTPUT)
    fun transform(inputString: String): Any
    {
        var command :String
        var parameters = inputString.split("---")
        when(parameters[0])
        {
            "fortune" ->{ command = RunCommand("fortune") }
            "cowsay" ->{ command = RunCommand("cowsay " + parameters[parameters.size - 1]) }
            "lolcat" ->{ File("test_pentri_procesor.txt").writeText(parameters[parameters.size - 1])
                command = RunCommand("test_pentri_procesor.txt )
            }
            else ->{
                return parameters[parameters.size - 1]
            }
        }
        var commandAfterProcessing = ""
        for(i in 1 until parameters.size)
        {
            commandAfterProcessing += parameters[i]+"~"
        }
        commandAfterProcessing += "$command"
        return commandAfterProcessing
    }

    fun RunCommand(cmd:String):String
    {
        var output: String = ""
        try {
            var line: String?
            val p = Runtime.getRuntime().exec(cmd)
            val input = BufferedReader(InputStreamReader(p.inputStream))
            while (input.readLine().also { line = it } != null) {
                output+=line+"\n"
            }
            input.close()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return output
    }
}
fun main(args: Array<String>)
{
    runApplication<ProcessPipeMicroservice>(*args)
}