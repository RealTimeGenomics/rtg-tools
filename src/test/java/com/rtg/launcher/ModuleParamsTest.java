/*
 * Copyright (c) 2014. Real Time Genomics Limited.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rtg.launcher;

import static com.rtg.util.StringUtils.LS;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import com.rtg.launcher.ModuleParams.ModuleParamsBuilder;
import com.rtg.util.TestUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.test.params.TestParams;

import junit.framework.TestCase;


/**
 */
public class ModuleParamsTest extends TestCase {

  static final class MockModuleParamsBuilder extends ModuleParamsBuilder<MockModuleParamsBuilder> {
    @Override
    protected MockModuleParamsBuilder self() {
      return this;
    }

  }

  private static final class MockModuleParams extends ModuleParams {

    MockModuleParams(MockModuleParamsBuilder builder) {
      super(builder);
    }
    MockModuleParams(final CFlags flags) {
      super(flags.getName());
    }
    @Override
    public File directory() {
      return null;
    }
    @Override
    public File file(final String name) {
      return null;
    }
  }

  ModuleParams getParams(final String[] args, final String name) {
    final Appendable out = new StringWriter();
    final CFlags flags = new CFlags(name, out, null);
    flags.setFlags(args);
    return new MockModuleParams(flags);
  }

  public void testEquals() throws Exception {
    final ModuleParams a1 = getParams(new String[] {}, "testCliParams");
    final ModuleParams a2 = getParams(new String[] {}, "testCliParams");
    final ModuleParams b = getParams(new String[] {}, "boo");
    TestUtils.equalsHashTest(new ModuleParams[][] {{a1, a2}, {b}});

    assertEquals("testCliParams" + LS, a1.toString());
    assertEquals("testCliParams", a1.name());

    assertEquals("boo" + LS, b.toString());
    assertEquals("boo", b.name());


    a1.close();
    a2.close();
    b.close();
  }

  public void testBuilderDefaults() throws IOException {
    try (MockModuleParams mp = new MockModuleParams(new MockModuleParamsBuilder())) {
      assertEquals("ModuleParams", mp.name());
    }
  }

  public void testBuilder() {
    final MockModuleParamsBuilder builder = new MockModuleParamsBuilder();
    assertEquals(builder, builder.name("blah"));
  }

  public void testOmnes() {
    new TestParams(ModuleParams.class, ModuleParamsBuilder.class).check();
  }

}
