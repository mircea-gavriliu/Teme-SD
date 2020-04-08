import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.util.*

class TaskManagerMicroservice {
    private var taskManagerSocket: ServerSocket
    private var receiveInfoMessageObservable: Observable<String>
    private val subscriptions = CompositeDisposable()
    private val microserviceConnectionConnections: MutableList<Socket> = mutableListOf()

    companion object Constants {

        const val filePath="/Users/mirceagavriliu/Downloads/Okazii/TaskManagerMicroservice/src/resources/log.txt"
        val myfile = File(filePath)
        const val TASK_MANAGER_PORT = 2000
        const val MONITORIZATION_DURATION: Long = 60_000
    }

    init {
        taskManagerSocket = ServerSocket(TASK_MANAGER_PORT)
        taskManagerSocket.setSoTimeout(MONITORIZATION_DURATION.toInt())

        Files.write(myfile.toPath(),("\n\nLogurile pentru TaskManager: la data de ${LocalDateTime.now()} \n\n").toByteArray(),StandardOpenOption.APPEND)
        println("TaskManager se executa pe portul: ${taskManagerSocket.localPort}")

        // se creeaza obiectul Observable cu care se genereaza evenimente cand se primesc oferte de la bidderi
        receiveInfoMessageObservable = Observable.create<String> { emitter ->
            // se asteapta conexiuni din partea bidderilor
            while (true) {
                try {
                    val microserviceConnection = taskManagerSocket.accept()
                    microserviceConnectionConnections.add(microserviceConnection)

                    // se citeste mesajul de la bidder de pe socketul TCP
                    val bufferReader = BufferedReader(InputStreamReader(microserviceConnection.inputStream))
                    var receivedMessage = bufferReader.readLine()

                    // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                    if (receivedMessage == null) {
                        // deci subscriber-ul respectiv a fost deconectat
                        bufferReader.close()
                        microserviceConnection.close()

                        //emitter.onError(Exception("Eroare: Bidder-ul ${microserviceConnection.port} a fost deconectat."))
                    }

                    emitter.onNext(receivedMessage)


                } catch (e: SocketTimeoutException) {
                    // daca au trecut cele 15 secunde de la pornirea licitatiei, inseamna ca licitatia s-a incheiat
                    // se emite semnalul Complete pentru a incheia fluxul de oferte
                    emitter.onComplete()
                    break
                }
            }
        }
    }

    private fun receiveInfoMessages() {
        // se incepe prin a primi ofertele de la bidderi
        val receiveBidsSubscription = receiveInfoMessageObservable.subscribeBy(
            onNext = {
                val message = Message.deserialize(it.toByteArray())
                println(message)
                logInfos(message)
            },
            onComplete = {
                println("Monitorizarea s-a incheiat! ")
                Files.write(myfile.toPath(),("Monitorizare incheiata la data de: la data de ${LocalDateTime.now()} \n\n").toByteArray(),StandardOpenOption.APPEND)
                finalExecution()
            },
            onError = { println("Eroare: $it") }
        )
        subscriptions.add(receiveBidsSubscription)
    }

    private fun logInfos(message:Message) {
        Files.write(myfile.toPath(),("${message.timestamp} ${message.name}: ${message.body} \n\n").toByteArray(),StandardOpenOption.APPEND)
    }

    private fun finalExecution(){
        subscriptions.dispose()
        microserviceConnectionConnections.forEach{
            it.close()
        }
    }



    fun run() {
        receiveInfoMessages()
    }
}

fun main(args: Array<String>) {
    var taskManagerMicroservice= TaskManagerMicroservice()
    taskManagerMicroservice.run()
}