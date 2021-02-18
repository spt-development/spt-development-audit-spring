package com.spt.development.audit.spring;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation added to methods to be audited. For a more detailed explanation see the
 * <a href="https://github.com/spt-development/spt-development-test/blob/main/README.md">README</a>.
 */
@Documented
@Target({ METHOD })
@Retention(RUNTIME)
public @interface Audited {

    /**
     * Default [unset] value/type - the value/type <em>must</em> be set otherwise an exception will be thrown.
     */
    String NONE = "NONE";

    /**
     * The type of audit event - typically this will be an identifier for the domain that the audit event relates to.
     *
     * @return the type of audit event.
     */
    @AliasFor("type")
    String value() default NONE;

    /**
     * The type of audit event - typically this will be an identifier for the domain that the audit event relates to.
     *
     * @return the type of audit event.
     */
    @AliasFor("value")
    String type() default NONE;

    /**
     * The sub-type of the audit event - typically this will be an identifier for the type of action that is being audited,
     * such as CREATE or DELETE.
     *
     * @return the sub-type of the audit event.
     */
    String subType();

    /**
     * Annotation added to method arguments or methods to specify the argument (or return value in the case of methods),
     * to retrieve the ID from to add to the {@link AuditEvent}. Only one argument (or the method itself) should be
     * annotated with this annotation.
     */
    @Target({ METHOD, PARAMETER })
    @Retention(RUNTIME)
    @interface Id {

        /**
         * If specified, then the name of the field to use from the argument annotated or return value if a method
         * itself is annotated, to retrieve the ID from.
         *
         * @return the field name.
         */
        @AliasFor("field")
        String value() default "";

        /**
         * If specified, then the name of the field to use from the argument annotated or return value if a method
         * itself is annotated, to retrieve the ID from.
         *
         * @return the field name.
         */
        @AliasFor("value")
        String field() default "";
    }

    /**
     * Annotation added to method arguments to specify argument(s) to retrieve the details from to add to the
     * {@link AuditEvent}. Multiple arguments can be annotated with this annotation, all of which will be used to build
     * up the details to add to the {@link AuditEvent}.
     */
    @Target({ PARAMETER })
    @Retention(RUNTIME)
    @interface Detail {

        /**
         * If specified, then the name that will be used within the {@link AuditEvent#details} JSON object to assign the
         * annotated argument value to. If multiple arguments are annotated with this annotation, then this field is
         * mandatory.
         *
         * @return the details field name.
         */
        @AliasFor("name")
        String value() default "";

        /**
         * If specified, then the name that will be used within the {@link AuditEvent#details} JSON object to assign the
         * annotated argument value to. If multiple arguments are annotated with this annotation, then this field is
         * mandatory.
         *
         * @return the details field name.
         */
        @AliasFor("value")
        String name() default "";
    }
}
