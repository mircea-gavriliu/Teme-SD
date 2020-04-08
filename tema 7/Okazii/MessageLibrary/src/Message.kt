import java.text.SimpleDateFormat
import java.util.*

class Message private constructor(val sender: String, val body: String, val timestamp: Date, val name:String, val phone:String, val email:String) {
    companion object {
        fun create(sender: String, body: String, name:String="",phone: String="",email: String=""): Message {
            return Message(sender, body, Date(),name,phone,email)
        }

        fun deserialize(msg: ByteArray): Message {
            val msgString = String(msg)
            val arrayParams=msgString.split(' ', limit = 6)
            val timestamp=arrayParams[0]
            val sender=arrayParams[1]
            val body=arrayParams[5]
            val name=arrayParams[2]
            val phone=arrayParams[3]
            val email=arrayParams[4]

            return Message(sender, body, Date(timestamp.toLong()),name,phone,email)
        }
    }

    fun serialize(): ByteArray {
        return "${timestamp.time} $sender $name $phone $email $body\n".toByteArray()
    }

    override fun toString(): String {
        val dateString = SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(timestamp)
        return "[$dateString] $sender name:$name phone:$phone email:$email >>> $body"
    }
}

fun main(args: Array<String>) {
    val msg = Message.create("localhost:4848", "test mesaj", "Gavriliu Mircea", "0754021773", "gavriliumircea@yahoo.ro")
    println(msg)
    val serialized = msg.serialize()
    val deserialized = Message.deserialize(serialized)
    println(deserialized)
}