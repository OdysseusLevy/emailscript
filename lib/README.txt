Notes on the libraries we are using

javax.mail.jar          JavaMail this is the standard email library for Java

** Utilities **

commons-lang3       	Curently used just for StringEscapeUtils in the SpamUrlParser
guava-18.0.jar:     	Has some nice convienence utilities for Java
jsr305-1.3.9.jar    	Annotations used by guava. Scala compiler barfs without this

** Templates (mustache) **

compiler-0.8.17.jar     Mustache template support

** Serialization **

snakeyaml-*.jar       	Yaml input and output
gson-*                  Convert objects to JSON and back

** Internet support **

bee-client_*.jar        Support for HTTP queries
dnsjava                 Allows more control over dns queries (api is saner too)

** Scripting libraries **

groovy-all          	Groovy support
jruby               	Ruby support

** Google contacts api support **

gdata-*                	Used for GoogleContacts support
google-oauth-client*   	Used for logging into GoogleContacts service 

** Logger support **

logback-classic*
logback-core*
scalalogging-sl4j
slf4j-api
