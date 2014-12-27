Notes on the libraries we are using

commons-lang3       Curently used just for StringEscapeUtils
dnsjava             Allows more control over dns queries (api is saner too)
guava-18.0.jar:     Used mainly for the CacheBuilder
jsr305-1.3.9.jar    Annotations used by guava. Scala compiler barfs without this
snakeyaml           Yaml input and output
JavaDKIM            Email dkim support
javax.mail          JavaMail

** Scripting libraries **

groovy-all          Groovy support
jruby               Ruby support

** Google contacts api support **

gdata*              Used for GoogleContacts support
google-oauth        Used for logging into GoogleContacts service 

** Logger support **

logback*
scalalogging-sl4j
slf4j-api
