package com.sd.laborator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class TeacherMicroservice {
    private lateinit var messageManagerSocket: Socket
    private lateinit var heartbeatSocket: Socket
    private lateinit var teacherMicroserviceServerSocket: ServerSocket

    companion object Constants {
        // pentru testare, se foloseste localhost. pentru deploy, server-ul socket (microserviciul MessageManager) se identifica dupa un "hostname"
        // acest hostname poate fi trimis (optional) ca variabila de mediu
        val MESSAGE_MANAGER_HOST = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        const val MESSAGE_MANAGER_PORT = 1500
        val HEARTBEAT_HOST = System.getenv("HEARTBEAT_HOST") ?: "localhost"
        const val HEARTBEAT_PORT = 1900
        const val TEACHER_PORT = 1600
    }

    private fun subscribeToMessageManager() {
        try {
            messageManagerSocket = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
            messageManagerSocket.soTimeout = 3000
            println("M-am conectat la MessageManager!")
        } catch (e: Exception) {
            println("Nu ma pot conecta la MessageManager!")
            exitProcess(1)
        }
    }

    private fun subscribeToHearbeat() {
        try {
            heartbeatSocket = Socket(HEARTBEAT_HOST, HEARTBEAT_PORT)
            println("teacher")
            heartbeatSocket.getOutputStream().write(("teacher\n").toByteArray())

        } catch (e: Exception) {
            println("Nu ma pot conecta la HeartBeat!")
            exitProcess(1)
        }
    }

    public fun run() = runBlocking {
        // microserviciul se inscrie in lista de "subscribers" de la MessageManager prin conectarea la acesta
        subscribeToMessageManager()
        subscribeToHearbeat()

        // se porneste un socket server TCP pe portul 1600 care asculta pentru conexiuni
        teacherMicroserviceServerSocket = ServerSocket(TEACHER_PORT)

        println("TeacherMicroservice se executa pe portul: ${teacherMicroserviceServerSocket.localPort}")
        println("Se asteapta cereri (intrebari)...")

        while (true) {
            // se asteapta conexiuni din partea clientilor ce doresc sa puna o intrebare
            // (in acest caz, din partea aplicatiei client GUI)
            val clientConnection = teacherMicroserviceServerSocket.accept()


            // se foloseste un thread separat pentru tratarea fiecarei conexiuni client
            launch(Dispatchers.IO) {
                println("S-a primit o cerere de la: ${clientConnection.inetAddress.hostAddress}:${clientConnection.port}")

                // se citeste intrebarea dorita
                val clientBufferReader = BufferedReader(InputStreamReader(clientConnection.inputStream))
                val receivedMessage = clientBufferReader.readLine()
                println("mesajul este: ${receivedMessage}")
                // intrebarea este redirectionata catre microserviciul

                if(receivedMessage!="Dummy") {
                    println("Trimit catre MessageManager: ${"intrebare ${messageManagerSocket.localPort} $receivedMessage\n"}")
                    messageManagerSocket.getOutputStream()
                        .write(("intrebare ${messageManagerSocket.localPort} $receivedMessage\n").toByteArray())

                    // se asteapta raspuns de la MessageManager
                    val messageManagerBufferReader = BufferedReader(InputStreamReader(messageManagerSocket.inputStream))
                    try {

                        val receivedResponse = messageManagerBufferReader.readLine()

                        // se trimite raspunsul inapoi clientului apelant
                        println("Am primit raspunsul: \"$receivedResponse\"")
                        clientConnection.getOutputStream().write((receivedResponse + "\n").toByteArray())
                    } catch (e: SocketTimeoutException) {
                        println("Nu a venit niciun raspuns in timp util.")
                        clientConnection.getOutputStream().write("Nu a raspuns nimeni la intrebare\n".toByteArray())
                    } finally {
                        // se inchide conexiunea cu clientul
                        clientConnection.close()
                    }
                } else {
                    launch(Dispatchers.IO){
                        while(true){
                            val buff = BufferedReader(InputStreamReader(clientConnection.inputStream))
                            val msg=buff.readLine()
                            heartbeatSocket.getOutputStream().write(("teacher\n").toByteArray())
                        }
                    }
                }
            }


        }
    }
}

fun main(args: Array<String>) {
    val teacherMicroservice = TeacherMicroservice()
    teacherMicroservice.run()
}