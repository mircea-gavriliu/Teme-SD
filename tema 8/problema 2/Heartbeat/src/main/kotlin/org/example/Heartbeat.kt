package org.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

class Heartbeat {
    private val subscribers: HashMap<Int, Socket>
    private lateinit var heartbeatSocket: ServerSocket

    companion object Constants {
        const val Heartbeat_PORT = 1900
        const val TEACHER_PORT = 1600
        const val MESSAGE_MANAGER_PORT = 1500

        val TEACHER_HOST = System.getenv("TEACHER_HOST") ?: "localhost"
        val MESSAGE_MANAGER_HOST = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"

    }

    init {
        subscribers = hashMapOf()
    }

    private fun broadcastMessage(message: String) {
        subscribers.forEach {
            it?.value?.getOutputStream()?.write((message + "\n").toByteArray())
        }
    }


    public fun run() = runBlocking {
        // se porneste un socket server TCP pe portul 1500 care asculta pentru conexiuni
        heartbeatSocket = ServerSocket(Heartbeat_PORT)
        println("HearbeatMicroservice se executa pe portul: ${heartbeatSocket.localPort}")
        println("Se asteapta conexiuni si mesaje...")
        heartbeatSocket.soTimeout = 5000

        while (true) {
            // se asteapta conexiuni din partea clientilor subscriberi
            try {
                val clientConnection = heartbeatSocket.accept()

                // se porneste un thread separat pentru tratarea conexiunii cu clientul
                launch(Dispatchers.IO) {
                    println("Subscriber conectat: ${clientConnection.inetAddress.hostAddress}:${clientConnection.port}")

                    // adaugarea in lista de subscriberi trebuie sa fie atomica!


                    val bufferReader = BufferedReader(InputStreamReader(clientConnection.inputStream))
                    while (true) {
                        // se citeste raspunsul de pe socketul TCP
                        val receivedMessage = bufferReader.readLine()


                        when (receivedMessage) {
                            "teacher" -> {
                                if (!subscribers.containsKey(TEACHER_PORT)) {
                                    subscribers[TEACHER_PORT] = Socket(TEACHER_HOST, TEACHER_PORT)
                                }
                            }
                            "messageManager" ->{
                                if (!subscribers.containsKey(MESSAGE_MANAGER_PORT)) {
                                    println("intru")
                                    subscribers[MESSAGE_MANAGER_PORT] = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
                                }
                            }
                            "student" ->{
                                subscribers[clientConnection.port] = clientConnection
                            }
                        }
                        // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                        if (receivedMessage == null) {
                            // deci subscriber-ul respectiv a fost deconectat
                            println("Subscriber-ul ${clientConnection.port} este deconectat.")
                            bufferReader.close()
                            clientConnection.close()
                            break
                        }

                        println("Subscriber-ul ${clientConnection.port} este in viata")

                    }
                }
            } catch (e: SocketTimeoutException) {
                broadcastMessage("Dummy")
            }
        }
    }
}

fun main(args: Array<String>) {
    val heartbeat = Heartbeat()
    heartbeat.run()
}