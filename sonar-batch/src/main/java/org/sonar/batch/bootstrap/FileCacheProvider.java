/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.config.Settings;
import org.sonar.home.cache.FileCache;
import org.sonar.home.cache.FileCacheBuilder;
import org.sonar.home.log.Slf4jLog;

public class FileCacheProvider extends ProviderAdapter {
  private FileCache cache;

  public FileCache provide(Settings settings) {
    if (cache == null) {
      String home = settings.getString("sonar.userHome");
      cache = new FileCacheBuilder().setLog(new Slf4jLog(FileCache.class)).setUserHome(home).build();
    }
    return cache;
  }
}
