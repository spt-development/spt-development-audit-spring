````
  ____  ____ _____   ____                 _                                  _   
 / ___||  _ \_   _| |  _ \  _____   _____| | ___  _ __  _ __ ___   ___ _ __ | |_ 
 \___ \| |_) || |   | | | |/ _ \ \ / / _ \ |/ _ \| '_ \| '_ ` _ \ / _ \ '_ \| __|
  ___) |  __/ | |   | |_| |  __/\ V /  __/ | (_) | |_) | | | | | |  __/ | | | |_ 
 |____/|_|    |_|   |____/ \___| \_/ \___|_|\___/| .__/|_| |_| |_|\___|_| |_|\__|
                                                 |_|                                           
 audit-spring -------------------------------------------------------------------
````

[![build_status](https://github.com/spt-development/spt-development-audit-spring/actions/workflows/build.yml/badge.svg)](https://github.com/spt-development/spt-development-audit-spring/actions)

Adds auditing to your project through the use of annotations typically added to Service methods.

Usage
=====

Register the Aspect as a Spring Bean manually or by adding the  
[spt-development/spt-development-audit-spring-boot](https://github.com/spt-development/spt-development-audit-spring-boot)
starter to your project's pom.

    @Configuration
    public class AuditConfig {
        private final String appName;
        private final String appVersion;
        private final AuditEventCommand auditEventCommand;

        public AuditConfig(
                @Value("${spring.application.name}")
                final String appName,
                final AuditEventCommand auditEventCommand,
                final BuildProperties buildProperties) {

            this.appName = appName;
            this.appVersion = buildProperties.getVersion();
            this.auditEventCommand = auditEventCommand;
        }

        @Bean
        public Auditor auditor() {
            return new Auditor(appName, appVersion, auditEventWriter(), authenticationAdapterFactory());
        }

        ...
    }

The Auditor aspect requires an `AuditEventWriter` and an `AuthenticationAdapterFactory` bean that can again be registered
manually or with the starter.

    @Bean
    public AuditEventWriter auditEventWriter() {
        return new Slf4jAuditEventWriter();
    }

    @Bean
    public AuthenticationAdapterFactory authenticationAdapterFactory() {
        return new AuthenticationAdapterFactory();
    }

The `Slf4jAuditEventWriter` simply logs the audit events with SLF4j which for production probably isn't particularly
useful and should be replaced either with `JmsAuditEventWriter` or a custom implementation of `AuditEventWriter` that
writes the audit events to a database for instance.

The `AuthenticationAdapterFactory` is used by the `Auditor` aspect to retrieve details about the currently logged-in
user. Anonymous, and basic username/password authentication are supported by the `DefaultAuthenticationAdapterFactory`
implementation. If using the default implementation, it may be necessary to customize how the user details are 
retrieved - the default implementation for username/password returns `null` for the user ID for instance - this is 
possible by providing custom factories for creating the adapters, for example:

    @Bean
    public AuthenticationAdapterFactory authenticationAdapterFactory() {
        return new DefaultAuthenticationAdapterFactory()
                .withUsernamePasswordFactory(auth -> new MyAuthenticationAdapter((MyPrincipal)auth.getPrincipal()));
    }

For other types of authentication such as OAuth2, it will be necessary to implement a custom 
`AuthenticationAdapterFactory`.

Auditing methods
----------------

Auditing is applied by annotating methods with the `@Audited` annotation. It is envisaged that auditing will be applied
predominantly to methods belonging to classes annotated with the `org.springframework.stereotype.Service`
annotation, however there is nothing in the implementation that prevents other methods being annotated with the
`@Audited` annotation instead/as well.

The first example shows the simplest form of auditing. The `@Audited` annotation is added to the method to be audited 
and the `type` and `subType` must be set. This will result in an `AuditEvent` being generated with the `type` and 
`subType` set as specified, along with the application details, authentication details relating to the current user, 
host name and timestamp.

    @Audited(type = "SomeDomain", subType = "CREATE")
    public void auditedMethod(Map<String, String> details) {
        ...
    }

The next two examples show how to add an ID to the audit event based on one of the method arguments - either the 
argument value itself in the first instance, or a field from the argument in the second example.

    @Audited(type = "SomeDomain", subType = "UPDATE")
    public void auditedMethod(@Audited.Id long id) {
        ...
    }
    
    @Audited(type = "SomeDomain", subType = "UPDATE")
    public void auditedMethod(Audited.Id("idField") Domain details) {
        ...
    }

Next are examples of how to use the return value for the ID value. Syntactically, the `@Audited.Id` annotation is applied
to the method in these examples, however writing the code as shown below shows the intent of the annotation in a more
readable way.

    @Audited(type = "SomeDomain", subType = "CREATE") {
    public @Audited.Id String auditedMethod() {
        return "Some ID value";
    }
    
    @Audited(type = "SomeDomain", subType = "CREATE") {
    public @Audited.Id("idField") Domain auditedMethod() {
        return new Domain("ID");
    }

To add further context to the audit events generated, apply the `@Audited.Detail` annotation to the method arguments.
This annotation can be applied to multiple arguments, however if multiple arguments are annotated, the name must be 
annotated. The annotated arguments are included as a JSON object in the adit event.

    @Audited(type = "SomeDomain", subType = "UPDATE") {
    public void auditedMethod(@Audited.Detail Domain details) {
        ...
    }
    
    @Audited(type = "SomeDomain", subType = "UPDATE") {
    public void auditedMethod(@Audited.Detail("arg1") Domain details1, @Audited.Detail("arg2") Domain details2) {
        ...
    }

Building locally
================

To build the library, run the following maven command:

    $ mvn clean install

Release
=======

To build a release and upload to Maven Central push to `main`.
