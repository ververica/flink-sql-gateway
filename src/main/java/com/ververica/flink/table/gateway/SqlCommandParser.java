/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.flink.table.gateway;

import org.apache.flink.sql.parser.ddl.SqlAlterDatabase;
import org.apache.flink.sql.parser.ddl.SqlAlterTable;
import org.apache.flink.sql.parser.ddl.SqlCreateDatabase;
import org.apache.flink.sql.parser.ddl.SqlCreateTable;
import org.apache.flink.sql.parser.ddl.SqlCreateView;
import org.apache.flink.sql.parser.ddl.SqlDropDatabase;
import org.apache.flink.sql.parser.ddl.SqlDropTable;
import org.apache.flink.sql.parser.ddl.SqlDropView;
import org.apache.flink.sql.parser.ddl.SqlUseCatalog;
import org.apache.flink.sql.parser.ddl.SqlUseDatabase;
import org.apache.flink.sql.parser.dml.RichSqlInsert;
import org.apache.flink.sql.parser.dql.SqlShowCatalogs;
import org.apache.flink.sql.parser.dql.SqlShowDatabases;
import org.apache.flink.sql.parser.dql.SqlShowFunctions;
import org.apache.flink.sql.parser.dql.SqlShowTables;
import org.apache.flink.sql.parser.impl.FlinkSqlParserImpl;
import org.apache.flink.sql.parser.validate.FlinkSqlConformance;

import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.parser.SqlParser;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple parser for determining the type of command and its parameters.
 */
public final class SqlCommandParser {

	private SqlCommandParser() {
		// private
	}

	/**
	 * Parse the given statement and return corresponding SqlCommandCall.
	 *
	 * <p>only `show current_catalog`, `show current_database`, `show modules`, `set`, `reset`,
	 * `describe`, `explain` are parsed through regex matching,
	 * other commands are parsed through sql parser.
	 *
	 * <p>throw {@link SqlParseException} if the statement contains multiple sub-statements separated by semicolon
	 * or there is a parse error.
	 *
	 * <p>NOTES: sql parser only parses the statement to get the corresponding SqlCommand,
	 * do not check whether the statement is valid here.
	 */
	public static Optional<SqlCommandCall> parse(String stmt, boolean isBlinkPlanner) {
		// normalize
		String stmtForRegexMatch = stmt.trim();
		// remove ';' at the end
		if (stmtForRegexMatch.endsWith(";")) {
			stmtForRegexMatch = stmtForRegexMatch.substring(0, stmtForRegexMatch.length() - 1).trim();
		}

		// only parse gateway specific statements
		for (SqlCommand cmd : SqlCommand.values()) {
			if (cmd.hasPattern()) {
				final Matcher matcher = cmd.pattern.matcher(stmtForRegexMatch);
				if (matcher.matches()) {
					final String[] groups = new String[matcher.groupCount()];
					for (int i = 0; i < groups.length; i++) {
						groups[i] = matcher.group(i + 1);
					}
					return cmd.operandConverter.apply(groups)
						.map((operands) -> new SqlCommandCall(cmd, operands));
				}
			}
		}

		return parseStmt(stmt, isBlinkPlanner);
	}

	/**
	 * Flink Parser only supports partial Operations, so we directly use Calcite Parser here.
	 * Once Flink Parser supports all Operations, we should use Flink Parser instead of Calcite Parser.
	 */
	private static Optional<SqlCommandCall> parseStmt(String stmt, boolean isBlinkPlanner) {
		SqlParser.Config config = createSqlParserConfig(isBlinkPlanner);
		SqlParser sqlParser = SqlParser.create(stmt, config);
		SqlNodeList sqlNodes;
		try {
			sqlNodes = sqlParser.parseStmtList();
			// no need check the statement is valid here
		} catch (org.apache.calcite.sql.parser.SqlParseException e) {
			throw new SqlParseException("Failed to parse statement.", e);
		}
		if (sqlNodes.size() != 1) {
			throw new SqlParseException("Only single statement is supported now");
		}

		final String operand;
		final SqlCommand cmd;
		SqlNode node = sqlNodes.get(0);
		if (node.getKind().belongsTo(SqlKind.QUERY)) {
			cmd = SqlCommand.SELECT;
			operand = stmt;
		} else if (node instanceof RichSqlInsert) {
			cmd = ((RichSqlInsert) node).isOverwrite() ? SqlCommand.INSERT_OVERWRITE : SqlCommand.INSERT_INTO;
			operand = stmt;
		} else if (node instanceof SqlShowTables) {
			cmd = SqlCommand.SHOW_TABLES;
			operand = null;
		} else if (node instanceof SqlCreateTable) {
			cmd = SqlCommand.CREATE_TABLE;
			operand = stmt;
		} else if (node instanceof SqlDropTable) {
			cmd = SqlCommand.DROP_TABLE;
			operand = stmt;
		} else if (node instanceof SqlAlterTable) {
			cmd = SqlCommand.ALTER_TABLE;
			operand = stmt;
		} else if (node instanceof SqlCreateView) {
			cmd = SqlCommand.CREATE_VIEW;
			operand = stmt;
		} else if (node instanceof SqlDropView) {
			cmd = SqlCommand.DROP_VIEW;
			operand = stmt;
		} else if (node instanceof SqlShowDatabases) {
			cmd = SqlCommand.SHOW_DATABASES;
			operand = null;
		} else if (node instanceof SqlCreateDatabase) {
			cmd = SqlCommand.CREATE_DATABASE;
			operand = stmt;
		} else if (node instanceof SqlDropDatabase) {
			cmd = SqlCommand.DROP_DATABASE;
			operand = stmt;
		} else if (node instanceof SqlAlterDatabase) {
			cmd = SqlCommand.ALTER_DATABASE;
			operand = stmt;
		} else if (node instanceof SqlShowCatalogs) {
			cmd = SqlCommand.SHOW_CATALOGS;
			operand = null;
		} else if (node instanceof SqlShowFunctions) {
			cmd = SqlCommand.SHOW_FUNCTIONS;
			operand = null;
		} else if (node instanceof SqlUseCatalog) {
			cmd = SqlCommand.USE_CATALOG;
			operand = ((SqlUseCatalog) node).getCatalogName();
		} else if (node instanceof SqlUseDatabase) {
			cmd = SqlCommand.USE;
			operand = ((SqlUseDatabase) node).getDatabaseName().toString();
			// TODO remove `DESCRIBE` and supports `DESCRIBE TABLE`
			// } else if (node instanceof SqlDescribeTable) {
			//	cmd = SqlCommand.DESCRIBE;
			// TODO remove `EXPLAIN` and supports `EXPLAIN PLAN`
			// } else if (node instanceof SqlExplain) {
			// 	md = SqlCommand.EXPLAIN;
		} else {
			cmd = null;
			operand = null;
		}

		if (cmd == null) {
			return Optional.empty();
		} else {
			// use the origin given statement to make sure
			// users can find the correct line number when parsing failed
			if (operand == null) {
				return Optional.of(new SqlCommandCall(cmd));
			} else {
				return Optional.of(new SqlCommandCall(cmd, new String[] { operand }));
			}
		}
	}

	/**
	 * A temporary solution. We can't get the default SqlParser config through table environment now.
	 */
	private static SqlParser.Config createSqlParserConfig(boolean isBlinkPlanner) {
		if (isBlinkPlanner) {
			return SqlParser
				.configBuilder()
				.setParserFactory(FlinkSqlParserImpl.FACTORY)
				.setConformance(FlinkSqlConformance.DEFAULT)
				.setLex(Lex.JAVA)
				.setIdentifierMaxLength(256)
				.build();
		} else {
			return SqlParser
				.configBuilder()
				.setParserFactory(FlinkSqlParserImpl.FACTORY)
				.setConformance(FlinkSqlConformance.DEFAULT)
				.setLex(Lex.JAVA)
				.build();
		}
	}

	// --------------------------------------------------------------------------------------------

	private static final Function<String[], Optional<String[]>> NO_OPERANDS =
		(operands) -> Optional.of(new String[0]);

	private static final Function<String[], Optional<String[]>> SINGLE_OPERAND =
		(operands) -> Optional.of(new String[] { operands[0] });

	private static final int DEFAULT_PATTERN_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;

	/**
	 * Supported SQL commands.
	 */
	public enum SqlCommand {
		SELECT,

		INSERT_INTO,

		INSERT_OVERWRITE,

		CREATE_TABLE,

		ALTER_TABLE,

		DROP_TABLE,

		CREATE_VIEW,

		DROP_VIEW,

		CREATE_DATABASE,

		ALTER_DATABASE,

		DROP_DATABASE,

		USE_CATALOG,

		USE,

		SHOW_CATALOGS,

		SHOW_DATABASES,

		SHOW_TABLES,

		SHOW_FUNCTIONS,

		// keep this for compatibility
		EXPLAIN(
			"EXPLAIN\\s+(.*)",
			SINGLE_OPERAND),

		// keep this for compatibility
		DESCRIBE(
			"DESCRIBE\\s+(.*)",
			SINGLE_OPERAND),

		SHOW_CURRENT_CATALOG(
			"SHOW\\s+CURRENT\\s+CATALOG",
			NO_OPERANDS),

		SHOW_CURRENT_DATABASE(
			"SHOW\\s+CURRENT\\s+DATABASE",
			NO_OPERANDS),

		SHOW_MODULES(
			"SHOW\\s+MODULES",
			NO_OPERANDS),

		SET(
			"SET(\\s+(\\S+)\\s*=(.*))?", // whitespace is only ignored on the left side of '='
			(operands) -> {
				if (operands.length < 3) {
					return Optional.empty();
				} else if (operands[0] == null) {
					return Optional.of(new String[0]);
				}
				return Optional.of(new String[] { operands[1], operands[2] });
			}),

		RESET(
			"RESET",
			NO_OPERANDS);

		public final Pattern pattern;
		public final Function<String[], Optional<String[]>> operandConverter;

		SqlCommand(String matchingRegex, Function<String[], Optional<String[]>> operandConverter) {
			this.pattern = Pattern.compile(matchingRegex, DEFAULT_PATTERN_FLAGS);
			this.operandConverter = operandConverter;
		}

		SqlCommand() {
			this.pattern = null;
			this.operandConverter = null;
		}

		@Override
		public String toString() {
			return super.toString().replace('_', ' ');
		}

		boolean hasPattern() {
			return pattern != null;
		}
	}

	/**
	 * Call of SQL command with operands and command type.
	 */
	public static class SqlCommandCall {
		public final SqlCommand command;
		public final String[] operands;

		public SqlCommandCall(SqlCommand command, String[] operands) {
			this.command = command;
			this.operands = operands;
		}

		public SqlCommandCall(SqlCommand command) {
			this(command, new String[0]);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			SqlCommandCall that = (SqlCommandCall) o;
			return command == that.command && Arrays.equals(operands, that.operands);
		}

		@Override
		public int hashCode() {
			int result = Objects.hash(command);
			result = 31 * result + Arrays.hashCode(operands);
			return result;
		}

		@Override
		public String toString() {
			return command + "(" + Arrays.toString(operands) + ")";
		}
	}
}
