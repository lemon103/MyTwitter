import net.sf.ehcache.Element
import grails.converters.*

class StatusController {
	
	def twitterCache
	
	//def index = { redirect(action:list,params:params) }
	def index = {
		def messages = twitterCache.get(principalInfo.username)?.value
		if(!messages){
			messages = findStatusMessages()
			twitterCache.put new Element(principalInfo.username, messages)
		}
		def feedOutput = {
			title = "Twitter clone RSS feed"
			description = "Twitter clone RSS feed"
			link = g.link(controller:"status", absolute:true)
			for(m in messages){
				entry("${m.person.userRealName} posted:") {
					link = g.link(controller:"status", absolute:true)
					m.message
				}
			}
		}
		
		def followings = findFollowings()

		withFormat{
			html( [messages:messages, followings:followings] )
			xml {
				render messages as XML
			}
			rss {
				render(feedType:"rss", feedOutput)
				
			}
		}
		
	}
	private findFollowings(){
		def p = lookupPerson()
/*		def following = Person.withCriteria {
			projections {property "username"}
			following {
				eq('username', p.username)
			}
		}
	*/
		println "DEBUG: Found ${p.userRealName} followers: ${p.following.collect { it }}"
		return p.following.collect { it }
	}
	private findStatusMessages(){
		def p = lookupPerson()
		
		def messages = Status.withCriteria {
			or {
				person {
					eq('username', p.username)
				}
				if(p.following){
					inList('person', p.following)
				}
			}
			maxResults 10
			order 'dateCreated', 'desc'
		}
		println "here " + messages.count()
		return messages
	}
	
	// the delete, save and update actions only accept POST requests
	static allowedMethods = [delete:'POST', save:'POST', update:'POST']
	
	def list = {
		params.max = Math.min( params.max ? params.max.toInteger() : 10,  100)
		[ statusInstanceList: Status.list( params ), statusInstanceTotal: Status.count() ]
	}
	
	def show = {
		def statusInstance = Status.get( params.id )
		
		if(!statusInstance) {
			flash.message = "Status not found with id ${params.id}"
			redirect(action:list)
		}
		else { return [ statusInstance : statusInstance ]
		}
	}
	
	def delete = {
		def statusInstance = Status.get( params.id )
		if(statusInstance) {
			try {
				statusInstance.delete(flush:true)
				flash.message = "Status ${params.id} deleted"
				redirect(action:list)
			}
			catch(org.springframework.dao.DataIntegrityViolationException e) {
				flash.message = "Status ${params.id} could not be deleted"
				redirect(action:show,id:params.id)
			}
		}
		else {
			flash.message = "Status not found with id ${params.id}"
			redirect(action:list)
		}
	}
	
	def edit = {
		def statusInstance = Status.get( params.id )
		
		if(!statusInstance) {
			flash.message = "Status not found with id ${params.id}"
			redirect(action:list)
		}
		else {
			return [ statusInstance : statusInstance ]
		}
	}
	
	def update = {
		def status = new Status(params)
		status.person = lookupPerson()
		status.save()
		
		twitterCache.remove(principalInfo.username)
		
		def messages = findStatusMessages()
		
		render template:"message", var:"status", collection:messages
	}
	
	def follow = {
		def p = Person.get(params.id)
		
		if(p) {
			def current = lookupPerson()
			current.addToFollowing(p)
			current.save()
			
		}
		
		redirect action:"index"
		
	}
	
	private lookupPerson(){
		Person.findByUsername(principalInfo.username)
	}
	
	def create = {
		def statusInstance = new Status()
		statusInstance.properties = params
		return ['statusInstance':statusInstance]
	}
	
	def save = {
		def statusInstance = new Status(params)
		if(!statusInstance.hasErrors() && statusInstance.save()) {
			flash.message = "Status ${statusInstance.id} created"
			redirect(action:show,id:statusInstance.id)
		}
		else {
			render(view:'create',model:[statusInstance:statusInstance])
		}
	}
}
