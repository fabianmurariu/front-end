/*
 * Copyright (c) Neo4j Sweden AB (http://neo4j.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencypher.v9_0.frontend.phases

import org.opencypher.v9_0.ast.Statement
import org.opencypher.v9_0.frontend.helpers.TestContext
import org.opencypher.v9_0.frontend.helpers.TestState
import org.opencypher.v9_0.parser.ParserFixture.parser
import org.opencypher.v9_0.rewriting.Deprecations
import org.opencypher.v9_0.rewriting.Deprecations.deprecatedFeaturesIn4_X
import org.opencypher.v9_0.util.DeprecatedOctalLiteralSyntax
import org.opencypher.v9_0.util.InputPosition
import org.opencypher.v9_0.util.InternalNotification
import org.opencypher.v9_0.util.OpenCypherExceptionFactory
import org.opencypher.v9_0.util.RecordingNotificationLogger
import org.opencypher.v9_0.util.test_helpers.CypherFunSuite

class SyntaxDeprecationWarningsTest extends CypherFunSuite {

  test("should warn about deprecation octal syntax") {
    check(deprecatedFeaturesIn4_X, "RETURN 01277") should equal(Set(
      DeprecatedOctalLiteralSyntax(InputPosition(7, 1, 8))
    ))
  }

  private def check(deprecations: Deprecations, query: String): Set[InternalNotification] = {
    val logger = new RecordingNotificationLogger()
    SyntaxDeprecationWarnings(deprecations).transform(TestState(Some(parse(query))), TestContext(logger))
    logger.notifications
  }

  private def parse(queryText: String): Statement = parser.parse(queryText.replace("\r\n", "\n"), OpenCypherExceptionFactory(None))

}
