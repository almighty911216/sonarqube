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
package org.sonar.server.startup;

import org.sonar.core.filter.FilterColumnDto;

import org.sonar.api.web.FilterColumn;

import org.sonar.api.web.Criterion;

import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.web.Filter;
import org.sonar.api.web.FilterTemplate;
import org.sonar.core.filter.CriterionDto;
import org.sonar.core.filter.FilterDao;
import org.sonar.core.filter.FilterDto;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.core.template.LoadedTemplateDto;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.test.MoreConditions.contains;

public class RegisterNewFiltersTest {
  private RegisterNewFilters register;
  private FilterDao filterDao;
  private LoadedTemplateDao loadedTemplateDao;
  private FilterTemplate filterTemplate;

  @Before
  public void init() {
    filterDao = mock(FilterDao.class);
    loadedTemplateDao = mock(LoadedTemplateDao.class);
    filterTemplate = mock(FilterTemplate.class);

    register = new RegisterNewFilters(new FilterTemplate[] {filterTemplate}, filterDao, loadedTemplateDao);
  }

  @Test
  public void should_insert_filters_on_start() {
    when(loadedTemplateDao.countByTypeAndKey(eq(LoadedTemplateDto.FILTER_TYPE), anyString())).thenReturn(0);
    when(filterTemplate.createFilter()).thenReturn(Filter.create());

    register.start();

    verify(filterDao).insert(any(FilterDto.class));
    verify(loadedTemplateDao).insert(any(LoadedTemplateDto.class));
  }

  @Test
  public void should_insert_nothing_if_no_template_is_available() {
    register = new RegisterNewFilters(filterDao, loadedTemplateDao);
    register.start();

    verify(filterDao, never()).insert(any(FilterDto.class));
    verify(loadedTemplateDao, never()).insert(any(LoadedTemplateDto.class));
  }

  @Test
  public void should_insert_nothing_if_templates_are_alreday_loaded() {
    when(loadedTemplateDao.countByTypeAndKey(eq(LoadedTemplateDto.FILTER_TYPE), anyString())).thenReturn(1);

    register.start();

    verify(filterDao, never()).insert(any(FilterDto.class));
    verify(loadedTemplateDao, never()).insert(any(LoadedTemplateDto.class));
  }

  @Test
  public void should_register_filter() {
    when(filterTemplate.createFilter()).thenReturn(Filter.create());

    FilterDto filterDto = register.register("Fake", filterTemplate.createFilter());

    assertThat(filterDto).isNotNull();
    verify(filterDao).insert(filterDto);
    verify(loadedTemplateDao).insert(eq(new LoadedTemplateDto("Fake", LoadedTemplateDto.FILTER_TYPE)));
  }

  @Test
  public void should_not_recreate_filter() {
    when(filterDao.findFilter("Fake")).thenReturn(new FilterDto());

    FilterDto filterDto = register.register("Fake", null);

    assertThat(filterDto).isNull();
    verify(filterDao, never()).insert(filterDto);
    verify(loadedTemplateDao).insert(eq(new LoadedTemplateDto("Fake", LoadedTemplateDto.FILTER_TYPE)));
  }

  @Test
  public void should_create_dto_from_extension() {
    when(filterTemplate.createFilter()).thenReturn(Filter.create()
        .setShared(true)
        .setFavouritesOnly(false)
        .setDefaultPeriod("list")
        .setResourceKeyLike("*KEY*")
        .setResourceNameLike("*NAME*")
        .setLanguage("java")
        .setSearchFor("TRK,BRC")
        .setPageSize(200)
        .add(Criterion.create().setFamily("metric").setKey("complexity").setOperator("<").setValue(12f))
        .add(FilterColumn.create().setFamily("metric").setKey("distance").setOrderIndex(1L).setSortDirection("ASC"))
        );

    FilterDto dto = register.createDtoFromExtension("Fake", filterTemplate.createFilter());
    CriterionDto criteriaResourceKeyDto = Iterables.get(dto.getCriteria(), 0);
    CriterionDto criteriaResourceNameDto = Iterables.get(dto.getCriteria(), 1);
    CriterionDto criteriaLangageDto = Iterables.get(dto.getCriteria(), 2);
    CriterionDto criteriaSearchForDto = Iterables.get(dto.getCriteria(), 3);

    assertThat(dto.getUserId()).isNull();
    assertThat(dto.getName()).isEqualTo("Fake");
    assertThat(dto.isShared()).isTrue();
    assertThat(dto.isFavourites()).isFalse();
    assertThat(dto.getDefaultView()).isEqualTo("list");
    assertThat(dto.getPageSize()).isEqualTo(200L);

    assertThat(dto.getCriteria()).hasSize(5);
    assertThat(dto.getCriteria()).satisfies(contains(new CriterionDto().setFamily("key").setOperator("=").setTextValue("*KEY*")));
    assertThat(dto.getCriteria()).satisfies(contains(new CriterionDto().setFamily("name").setOperator("=").setTextValue("*NAME*")));
    assertThat(dto.getCriteria()).satisfies(contains(new CriterionDto().setFamily("language").setOperator("=").setTextValue("java")));
    assertThat(dto.getCriteria()).satisfies(contains(new CriterionDto().setFamily("qualifier").setOperator("=").setTextValue("TRK,BRC")));
    assertThat(dto.getCriteria()).satisfies(contains(new CriterionDto().setFamily("metric").setKey("complexity").setOperator("<").setValue(12f).setVariation(false)));

    assertThat(dto.getColumns()).hasSize(1);
    assertThat(dto.getColumns()).satisfies(contains(new FilterColumnDto().setFamily("metric").setKey("distance").setOrderIndex(1L).setSortDirection("ASC").setVariation(false)));
  }
}
