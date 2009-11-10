//import java.io.Serializable

class Status implements Serializable  {
	String message
	Person person
	Date dateCreated
	
	static constraints = {
		message(blank:false, size:1..140)
	}
	
	//transient jmsTemplate
	transient defaultJmsTemplate
	transient afterInsert = {
		try {
			//jmsTemplate.convertAndSend("queue.twitter", this)
			//sendJMSMessage("queue.twitter", this)
			defaultJmsTemplate.convertAndSend("queue.twitter", this)
			println "sent"
		}
		catch(e){
			log.error "error sending JMS message: ${e.message}"
		}
	}
}
