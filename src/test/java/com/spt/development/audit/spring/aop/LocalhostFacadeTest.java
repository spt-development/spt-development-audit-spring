package com.spt.development.audit.spring.aop;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class LocalhostFacadeTest {

    @Test
    void getServerHostName_happyPath_shouldReturnLocalMachineName() throws Exception {
        final String result = createFacade().getServerHostName();

        assertThat(result, is(notNullValue()));
        assertThat(result, is(not(StringUtils.EMPTY)));
    }

    private LocalhostFacade createFacade() {
        return new LocalhostFacade();
    }
}