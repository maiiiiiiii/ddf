/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.pax.web.jetty;

import static org.codice.ddf.pax.web.jetty.ClientInfoFilter.CLIENT_INFO_KEY;
import static org.codice.ddf.pax.web.jetty.ClientInfoFilter.SERVLET_CONTEXT_PATH;
import static org.codice.ddf.pax.web.jetty.ClientInfoFilter.SERVLET_REMOTE_ADDR;
import static org.codice.ddf.pax.web.jetty.ClientInfoFilter.SERVLET_REMOTE_HOST;
import static org.codice.ddf.pax.web.jetty.ClientInfoFilter.SERVLET_SCHEME;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.util.ThreadContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Ensure our client info properties get set during the life time of the filter, and are cleaned up
 * after the fact.
 */
@RunWith(MockitoJUnitRunner.class)
public class ClientInfoFilterTest {
  private static final String MOCK_REMOTE_ADDRESS = "0.0.0.0";

  private static final String MOCK_REMOTE_HOST = "localhost";

  private static final String MOCK_SCHEME = "http";

  private static final String MOCK_CONTEXT_PATH = "/example/path";

  @Mock private ServletContext mockServletContext;

  @Mock private HttpServletRequest mockRequest;

  @Mock private HttpServletResponse mockResponse;

  @Mock private ProxyHttpFilterChain mockFilterChain;

  private ClientInfoFilter clientInfoFilter;

  @Before
  public void setup() throws Exception {
    when(mockRequest.getRemoteAddr()).thenReturn(MOCK_REMOTE_ADDRESS);
    when(mockRequest.getRemoteHost()).thenReturn(MOCK_REMOTE_HOST);
    when(mockRequest.getScheme()).thenReturn(MOCK_SCHEME);
    when(mockRequest.getServletContext()).thenReturn(mockServletContext);
    when(mockServletContext.getContextPath()).thenReturn(MOCK_CONTEXT_PATH);

    clientInfoFilter = new ClientInfoFilter();
  }

  @Test
  public void testClientInfoPresentInMap() throws Exception {
    doAnswer(invocationOnMock -> assertThatMapIsAccurate())
        .when(mockFilterChain)
        .doFilter(mockRequest, mockResponse);
    clientInfoFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
    assertThatMapIsNull();
  }

  @Test(expected = RuntimeException.class)
  public void testClientInfoCleansUpOnException() throws Exception {
    doThrow(RuntimeException.class).when(mockFilterChain).doFilter(mockRequest, mockResponse);
    try {
      clientInfoFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
    } finally {
      assertThatMapIsNull();
    }
  }

  private Object assertThatMapIsAccurate() throws Exception {
    Map<String, String> clientInfoMap = (Map<String, String>) ThreadContext.get(CLIENT_INFO_KEY);
    assertThat(clientInfoMap, notNullValue());
    assertThat(clientInfoMap.get(SERVLET_REMOTE_ADDR), is(MOCK_REMOTE_ADDRESS));
    assertThat(clientInfoMap.get(SERVLET_REMOTE_HOST), is(MOCK_REMOTE_HOST));
    assertThat(clientInfoMap.get(SERVLET_SCHEME), is(MOCK_SCHEME));
    assertThat(clientInfoMap.get(SERVLET_CONTEXT_PATH), is(MOCK_CONTEXT_PATH));
    return null;
  }

  private void assertThatMapIsNull() throws Exception {
    Map<String, String> clientInfoMap = (Map<String, String>) ThreadContext.get(CLIENT_INFO_KEY);
    assertThat(clientInfoMap, nullValue());
  }
}
