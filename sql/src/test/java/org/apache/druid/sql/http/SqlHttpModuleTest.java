/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.druid.sql.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Providers;
import org.apache.druid.guice.DruidGuiceExtensions;
import org.apache.druid.guice.LifecycleModule;
import org.apache.druid.guice.annotations.JSR311Resource;
import org.apache.druid.guice.annotations.Json;
import org.apache.druid.server.security.AuthorizerMapper;
import org.apache.druid.sql.SqlStatementFactory;
import org.apache.druid.sql.SqlStatementFactoryFactory;
import org.apache.druid.sql.calcite.run.NativeSqlEngine;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Set;

@RunWith(EasyMockRunner.class)
public class SqlHttpModuleTest
{
  @Mock
  private ObjectMapper jsonMpper;
  @Mock
  private SqlStatementFactoryFactory sqlStatementFactoryFactory;

  private SqlHttpModule target;
  private Injector injector;

  @Before
  public void setUp()
  {
    EasyMock.expect(sqlStatementFactoryFactory.factorize(EasyMock.capture(Capture.newInstance())))
            .andReturn(EasyMock.mock(SqlStatementFactory.class))
            .anyTimes();
    EasyMock.replay(sqlStatementFactoryFactory);

    target = new SqlHttpModule();
    injector = Guice.createInjector(
        new LifecycleModule(),
        new DruidGuiceExtensions(),
        binder -> {
          binder.bind(ObjectMapper.class).annotatedWith(Json.class).toInstance(jsonMpper);
          binder.bind(SqlStatementFactoryFactory.class).toInstance(sqlStatementFactoryFactory);
          binder.bind(AuthorizerMapper.class).toInstance(new AuthorizerMapper(Collections.emptyMap()));
          binder.bind(NativeSqlEngine.class).toProvider(Providers.of(new NativeSqlEngine(null, null)));
        },
        target
    );
  }

  @Test
  public void testSqlResourceIsInjectedAndNotSingleton()
  {
    SqlResource sqlResource = injector.getInstance(SqlResource.class);
    Assert.assertNotNull(sqlResource);
    SqlResource other = injector.getInstance(SqlResource.class);
    Assert.assertNotSame(other, sqlResource);
  }

  @Test
  public void testSqlResourceIsAvailableViaJersey()
  {
    Set<Class<?>> jerseyResourceClasses =
        injector.getInstance(Key.get(new TypeLiteral<Set<Class<?>>>() {}, JSR311Resource.class));
    Assert.assertEquals(1, jerseyResourceClasses.size());
    Assert.assertEquals(SqlResource.class, jerseyResourceClasses.iterator().next());
  }
}
