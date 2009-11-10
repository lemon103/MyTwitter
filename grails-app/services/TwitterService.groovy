
class TwitterService {
	boolean transactional = true
	static exposes = ['jms']
	static destination = "queue.twitter"
	
	def twitterCache
	def onMessage(obj){
		println "GOT MESSAGE $obj"
		
		Person p = obj.person
		def following = Person.withCriteria {
			projections {property "username"}
			following {
				eq('username', p.username)
			}
		}
		for(username in following) {
			twitterCache.remove(username)
		}
	}
}
