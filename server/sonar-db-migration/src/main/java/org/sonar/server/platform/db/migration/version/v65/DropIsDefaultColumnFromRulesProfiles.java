/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.sonar.db.Database;
import org.sonar.db.dialect.MsSql;
import org.sonar.server.platform.db.migration.sql.DropColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class DropIsDefaultColumnFromRulesProfiles extends DdlChange {

  private static final String TABLE_NAME = "rules_profiles";
  private static final String COLUMN_NAME = "is_default";

  public DropIsDefaultColumnFromRulesProfiles(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (getDialect().getId().equals(MsSql.ID)) {
      // this should be handled automatically by DropColumnsBuilder
      dropMssqlConstraints();
    }

    context.execute(new DropColumnsBuilder(getDialect(), TABLE_NAME, COLUMN_NAME).build());
  }

  private void dropMssqlConstraints() throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection();
      PreparedStatement pstmt = connection
        .prepareStatement("SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE where TABLE_NAME = '" + TABLE_NAME + "' AND COLUMN_NAME = '" + COLUMN_NAME + "'");
      ResultSet rs = pstmt.executeQuery()) {
      while (rs.next()) {
        String constraintName = rs.getString(1);
        dropMssqlConstraint(connection, constraintName);
      }
    }
  }

  private void dropMssqlConstraint(Connection connection, String constraintName) throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.executeUpdate("ALTER TABLE " + TABLE_NAME + " DROP CONSTRAINT " + constraintName);
    }
  }
}
